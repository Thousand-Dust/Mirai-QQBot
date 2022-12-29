package com.qqbot.group

import com.qqbot.Info
import com.qqbot.Utils
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.event.events.MemberMuteEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import java.lang.Integer.max
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.math.min
import kotlin.random.Random

/**
 * 群消息处理类
 * TODO: 将群消息处理的架构修改为：按系统分开解耦，如：群管系统，积分系统，主人系统
 * @author Thousand-Dust
 */
class GroupHandler(my: Member) : BaseGroupHandler(my) {

    private lateinit var myGroup: Group
    private lateinit var database: GroupDatabase

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    //封印列表
    private val sealMap = HashMap<Long, Int>()

    //24小时毫秒数
    private val dayMs = 24 * 60 * 60 * 1000

    override fun onCreate(group: Group): Boolean {
        this.myGroup = group
        this.database = GroupDatabase(group.id)
        return true
    }

    override fun onRemove(group: Group) {
        database.close()
    }

    override fun acceptMessage(event: GroupMessageEvent) {
        addCache(event)

        coroutineScope.launch {
            try {
                val message = event.message
                //检查封印
                run {
                    val senderId = event.sender.id
                    val num = sealMap[senderId] ?: return@run
                    if (num <= 0) {
                        sealMap.remove(senderId)
                        return@run
                    }
                    sealMap[senderId] = num - 1
                    event.message.recall()
                    return@launch
                }

                //管理员命令识别
                if (managerCommand(event)) {
                    return@launch
                }

                //检测刷屏
                if (checkFrequentSending(event)) {
                    return@launch
                }

                //发言增加积分
                val member = event.sender
                var memberData = database.getMember(member.id)
                //增加的积分，随机1~2
                val addScore = 1 + (System.currentTimeMillis() % 2).toInt()
                if (memberData == null) {
                    memberData = MemberData(member.id, member.nameCardOrNick, addScore)
                    database.add(memberData)
                } else {
                    memberData.name = member.nameCardOrNick
                    memberData.score += addScore
                    database.setMember(memberData)
                }

                //公开命令识别
                if (publicCommand(event)) {
                    return@launch
                }

                //临时代码，用于测试 TODO: 删除
                if (message.size == 2 && message[1].toString() == "菜单") {
                    event.subject.sendMessage("主人系统(还没写)\n群主系统(还没写)\n群管系统(群主及管理员可用)\n积分系统(公开可用)")
                    return@launch
                }

                if (violationDetection(event)) {
                    return@launch
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onMemberJoin(event: MemberJoinEvent) {
        runBlocking {
            event.group.sendMessage(
                MessageChainBuilder().append("欢迎新人").append(At(event.member.id)).append(" 加入本群").build()
            )
        }
    }

    override fun onMemberLeave(event: MemberLeaveEvent) {
        runBlocking {
            if (event is MemberLeaveEvent.Quit) {
                event.group.sendMessage(At(event.member.id) + " 退出了本群")
                return@runBlocking
            }
            if (event is MemberLeaveEvent.Kick && event.operator != null) {
                event.group.sendMessage(At(event.member.id) + "被" + At(event.operator!!.id) + " 踢出了群聊")
                return@runBlocking
            }
        }
    }

    /**
     * 其他违规行为检测
     */
    private suspend fun violationDetection(event: GroupMessageEvent): Boolean {
        if (event.sender.isOperator() || !my.isOperator()) return false
        val message = event.message
        if (message.stream().filter(At::class::isInstance).count() > 15) {
            message.recall()
            event.sender.mute(dayMs / 1000)
            val at = At(event.sender.id)
            event.group.sendMessage(at + " 违规行为，艾特人数过多")
            return true
        }
        return false
    }

    /**
     * 检测刷屏
     */
    private suspend fun checkFrequentSending(event: GroupMessageEvent): Boolean {
        if (cacheSize() < 1 || event.sender.isOperator() || !my.isOperator()) return false

        val senderId = event.sender.id
        val message = event.message
        val lastTime = message.time

        //30秒内连续发送的消息
        if (cacheSize() >= Info.CHECK_EVENT_COUNT_MAX) {
            //连续发送的消息数
            var count = 0
            //倒序遍历最后 [Info.CHECK_EVENT_COUNT_MAX] 条消息
            for (i in cacheSize() - 1 downTo cacheSize() - Info.CHECK_EVENT_COUNT_MAX) {
                val cache = getCache(i)
                //同一个人一分钟内发送的消息
                if (cache.sender.id == senderId && lastTime - message.time < 30) {
                    count++
                } else {
                    break
                }
            }
            //消息数大于 [Info.CHECK_EVENT_COUNT_MAX] 条消息
            if (count > Info.CHECK_EVENT_COUNT_MAX) {
                violationMute(event.sender, event.group)
                return true
            }
        }

        val checkedMsgRecord = cacheStreamCall { stream ->
            //跳过事件缓存的前面，留下最后 [com.qqbot.Info.CHECK_EVENT_COUNT] 条缓存用于检测
            stream.skip((max(0, cacheSize() - Info.CHECK_EVENT_COUNT)).toLong())
                .filter { it.sender.id == senderId }
        }.collect(Collectors.toList())

        //获取消息发送者 [Info.CHECK_EVENT_TIME] 秒内的消息记录
        val messageRecord = checkedMsgRecord.filter {
            lastTime - it.message.time < Info.CHECK_EVENT_TIME
        }
        //判断达到 [Info.CHECK_EVENT_COUNT] 条，判断为刷屏
        if (messageRecord.count() >= Info.CHECK_EVENT_COUNT_MAX) {
            violationMute(event.sender, event.group)
            return true
        }
        //checkedMsgRecord列表最后 [Info.CHECK_EVENT_COUNT_MAX1] 条记录为重复内容，判断为刷屏
        if (checkedMsgRecord.count() >= Info.CHECK_EVENT_COUNT_MAX1) {
            for (i in checkedMsgRecord.count() - 1 downTo checkedMsgRecord.count() - Info.CHECK_EVENT_COUNT_MAX1) {
                if (!Utils.messageChainEqual(message, checkedMsgRecord[i].message)) {
                    return false
                }
            }
            violationMute(event.sender, event.group)
        }

        return false
    }

    /**
     * 违规禁言
     */
    private suspend fun violationMute(sender: Member, group: Group) {
        val senderId = sender.id
        //修改群员违规信息
        var memberData = database.getMember(senderId)
        if (memberData == null) {
            memberData = MemberData(senderId, sender.nameCardOrNick)
            database.add(memberData)
        }
        //判断48小时内是否有多次违规
        if (System.currentTimeMillis() - memberData.lastViolationTime < (dayMs * 2)) {
            memberData.violationCount += 1
        } else {
            memberData.violationCount = 1
        }
        memberData.lastViolationTime = System.currentTimeMillis()
        database.setMember(memberData)
        //计算惩罚时间
        val muteTime = when (memberData.violationCount) {
            1 -> Info.CHECK_EVENT_PUNISH_TIME
            2 -> Info.CHECK_EVENT_PUNISH_TIME * 3
            3 -> Info.CHECK_EVENT_PUNISH_TIME * 6
            else -> dayMs / 1000
        }
        sender.mute(muteTime)

        //将目标被禁言时间格式化为 时:分:秒
        val hour = muteTime / 3600
        val minute = muteTime % 3600 / 60
        val stringBuilder = StringBuilder()
        if (hour != 0) {
            stringBuilder.append(hour).append("小时")
        }
        if (minute != 0) {
            stringBuilder.append(minute).append("分钟")
        }
        group.sendMessage("禁止刷屏，关小黑屋$stringBuilder")
    }

    /**
     * 群管理员命令
     * @return 是否处理了该事件
     */
    private suspend fun managerCommand(event: GroupMessageEvent): Boolean {
        val message = event.message
        //消息发送对象
        val sender = event.sender
        //消息发送者是否为管理员
        val isPermission = sender.permission.isOperator() || sender.id == Info.SuperManagerId
        if (!(isPermission && message[1] is PlainText)) {
            return false
        }
        //命令消息（消息头）
        val commandMessage = message[1]
        //识别命令
        if (message.size == 2) {
            when (commandMessage.toString()) {
                ManagerCommand.群管系统.name -> {
                    event.group.sendMessage(
                        "管理员操作：\n" +
                                "踢出群聊：${ManagerCommand.踢}/${ManagerCommand.t}+@目标\n" +
                                "踢出并拉黑：${ManagerCommand.踢黑}/${ManagerCommand.tb}+@目标\n" +
                                "禁言：${ManagerCommand.禁言}/${ManagerCommand.ban}+@目标+时间和单位(默认10m) (s秒,m分钟,h小时,d天)\n" +
                                "解禁：${ManagerCommand.解禁}/${ManagerCommand.kj}+@目标\n" +
                                "撤回：" + ManagerCommand.撤回 + "+@目标+撤回数量(默认10)\n" +
                                "撤回关键词：" + ManagerCommand.撤回关键词 + "+(空格)+关键词\n" +
                                "封印：" + ManagerCommand.封印 + "+@目标+封印层数\n" +
                                "解除封印：" + ManagerCommand.解除封印 + "+@目标\n" +
                                "查询消息记录：" + ManagerCommand.查询消息记录 + "@目标+查询数量(默认5)\n" +
                                "全员禁言：" + ManagerCommand.开启全员禁言 + "\n" +
                                "关闭全员禁言：" + ManagerCommand.关闭全员禁言 + "\n" +
                                "解除所有群员的禁言：" + ManagerCommand.全部解禁 + "\n" +
                                "其他功能待更新..."
                    )
                    return true
                }
                ManagerCommand.开启全员禁言.name -> {
                    return muteAll(event.group)
                }
                ManagerCommand.关闭全员禁言.name -> {
                    return unmuteAll(event.group)
                }
                ManagerCommand.全部解禁.name -> {
                    return memberUnmuteAll(event.group)
                }
                else -> {
                    if (commandMessage.toString().split(" ").size == 2 && recallKeyword(message, event.group)) {
                        //撤回关键词
                        return true
                    }
                }
            }
            return false
        }
        if (message.size >= 3) {
            when (commandMessage.toString()) {
                ManagerCommand.踢.name, ManagerCommand.t.name -> {
                    return kick(message[2], event.group)
                }
                ManagerCommand.踢黑.name, ManagerCommand.tb.name -> {
                    return kick(message[2], event.group, true)
                }
                ManagerCommand.禁言.name, ManagerCommand.ban.name -> {
                    return mute(message[2], isSingleMessageEmpty(message, 3, PlainText("10m")), event.group)
                }
                ManagerCommand.解禁.name, ManagerCommand.kj.name -> {
                    return unmute(message[2], event.group)
                }
                ManagerCommand.撤回.name -> {
                    return recall(message[2], isSingleMessageEmpty(message, 3, PlainText("10")), event.group)
                }
                ManagerCommand.封印.name -> {
                    return seal(message[2], isSingleMessageEmpty(message, 3, PlainText("5")), event.group)
                }
                ManagerCommand.解除封印.name -> {
                    return unseal(message[2], event.group)
                }
                ManagerCommand.查询消息记录.name -> {
                    return queryMessage(message[2], isSingleMessageEmpty(message, 3, PlainText("5")), event.group)
                }
            }
            return false
        }
        return false
    }

    private suspend fun publicCommand(event: GroupMessageEvent): Boolean {
        val message = event.message
        message.sourceOrNull
        //命令消息（消息头）
        val commandMessage = message[1]
        //必须群主或管理，且消息是纯文本开头

        if (commandMessage is PlainText) {
            val group = event.group
            val sender = event.sender
            //识别命令
            if (message.size == 2) {
                when (commandMessage.toString()) {
                    MemberCommand.积分系统.name -> {
                        event.group.sendMessage(
                            "积分系统：\n" +
                                    "每条发言随机增加1~2积分，禁言每分钟消耗20积分，解除禁言每分钟消耗10积分\n" +
                                    "签到：" + MemberCommand.签到 + "\n" +
                                    "转账：" + MemberCommand.转账 + "@目标+积分数量\n" +
                                    "查询积分：" + MemberCommand.我的积分 + "\n" +
                                    "积分排行榜：" + MemberCommand.积分排行榜 + "\n" +
                                    "禁言：" + MemberCommand.kban + "@目标+时间和单位 (s秒,m分钟,h小时)\n" +
                                    "解禁：" + MemberCommand.kj + "@目标\n" +
                                    "其他功能待更新..."
                        )
                        return true
                    }
                    MemberCommand.签到.name -> {
                        return sign(sender, group)
                    }
                    MemberCommand.我的积分.name -> {
                        val memberData = database.getMember(sender.id)
                        if (memberData == null) {
                            event.group.sendMessage(At(sender.id) + " 你的积分为：0")
                        } else {
                            event.group.sendMessage(At(sender.id) + " 你的积分为：${memberData.score}")
                        }
                        return true
                    }
                    MemberCommand.积分排行榜.name -> {
                        return scoreRanking(group)
                    }
                }
                return false
            }
            if (message.size >= 3) {
                when (commandMessage.toString()) {
                    MemberCommand.转账.name -> {
                        return transfer(message[2], message[3], sender, group)
                    }
                    MemberCommand.kban.name -> {
                        return scoreMute(message[2], message[3], sender, event.group)
                    }
                    MemberCommand.kj.name -> {
                        return scoreUnmute(message[2], sender, event.group)
                    }
                }
                return false
            }
            return false
        }

        if (message.size == 3 && commandMessage is At) {
            if (commandMessage.target == my.id) {
                val group = event.group
                /*val singleMessage = message[2]
                if (singleMessage !is PlainText || message.size > 3) {
                    group.sendMessage(message.quote() + "不支持的消息类型")
                    return true
                }
                val strMsg = singleMessage.contentToString()
                try {
                    group.sendMessage(message.quote() + chatGPTManager.getChatResponse(event.sender.id, strMsg))
                } catch (e: IllegalStateException) {
                    if (!e.toString().contains("Send message failed")) {
                        group.sendMessage(message.quote() + "错误：$e")
                    }
                } catch (e: Exception) {
                    group.sendMessage(message.quote() + "错误：$e")
                }*/
                group.sendMessage("小冰暂不支持聊天哦")
                return true
            }
            return false
        }
        return false
    }

    /**
     * 机器人权限检查
     * @param group 群
     * @param target 被操作对象 如果不为空，则检查机器人是否有对其操作的权限
     * @param sender 操作者 如果不为空，那么 [target] 不能为空，检查机器人和 [sender]是否有对 [target] 操作的权限
     * @return 机器人是否有权限可以执行操作
     */
    private suspend fun checkPermission(group: Group, target: Member? = null, sender: Member? = null): Boolean {
        if (group.botPermission.isOperator()) {
            return true
        }
        if (target != null && sender != null) {
        }
        group.sendMessage("机器人权限不足")
        return false
    }

    /**
     * 判断[SingleMessage]是否为空或只有空格
     * @param index 需要判断的 [SingleMessage] 在 [message] 的位置
     * @param default 如上[SingleMessage]为空字符时返回的默认值
     */
    private fun isSingleMessageEmpty(message: MessageChain, index: Int, default: SingleMessage): SingleMessage {
        return if (message.size < index + 1 || message[index].toString().replace(" ", "")
                .isEmpty()
        ) default else message[index]
    }

    /**
     * 踢出群成员
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun kick(targetMessage: SingleMessage, group: Group, block: Boolean = false): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        member.kick("", block)
        group.sendMessage(if (block) "成功踢出并拉黑" else "踢出成功")
        return true
    }

    /**
     * 解除成员禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun unmute(targetMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        member.unmute()
        group.sendMessage("已解除禁言")

        return true
    }

    /**
     * 撤回群员消息
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param countMessage 撤回消息数量 [SingleMessage] (PlainText)
     */
    private suspend fun recall(targetMessage: SingleMessage, countMessage: SingleMessage, group: Group): Boolean {
        if (cacheSize() < 1) return false

        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        //撤回消息的数量
        val count = countMessage.toString().replace(" ", "").toInt()
        //已撤回的数量
        var index = 0
        for (i in cacheLastIndex() downTo 0) {
            val event = getCache(i)
            if (event.sender.id == targetId) {
                try {
                    event.message.recall()
                    index++
                } catch (e: IllegalStateException) {
                }
                if (index >= count) {
                    break
                }
            }
        }
        return true
    }

    /**
     * 根据关键词撤回群员消息
     */
    private suspend fun recallKeyword(message: MessageChain, group: Group): Boolean {
        if (cacheSize() < 1) return false

        //拼接的消息内容
        val messageStr = message.stream().filter(MessageContent::class::isInstance).map(SingleMessage::toString)
            .collect(Collectors.joining()).trim()
        //命令和字符分割处
        val index = messageStr.indexOf(" ")
        if (index != 5) {
            return false
        }
        val command = messageStr.substring(0, index)
        //撤回的关键词
        val keyword = messageStr.substring(index + 1)
        if (command != ManagerCommand.撤回关键词.name) {
            return false
        }
        for (i in cacheLastIndex() downTo cacheLastIndex() - min(128, cacheLastIndex())) {
            val event = getCache(i)
            if (event.message.toString().contains(keyword)) {
                try {
                    event.message.recall()
                } catch (e: IllegalStateException) {
                }
            }
        }
        group.sendMessage("已撤回包含关键词 \"$keyword\" 的消息")
        return true
    }

    /**
     * 封印群员（群员封印状态下发消息会直接被撤回）
     */
    private suspend fun seal(targetMessage: SingleMessage, countMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return true
        }
        if (member.permission.level >= my.permission.level) {
            group.sendMessage("权限不足")
            return true
        }
        sealMap[member.id] = countMessage.toString().replace(" ", "").toInt()

        group.sendMessage("封印成功")

        return true
    }

    /**
     * 封印群员（群员封印状态下发消息会直接被撤回）
     */
    private suspend fun unseal(targetMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false

        if (sealMap.remove(targetId) != null) {
            group.sendMessage("已解除封印")
        }

        return true
    }

    /**
     * 查询群成员历史消息
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param countMessage 查询消息数量 [SingleMessage] 类型应该为: [PlainText]
     */
    private suspend fun queryMessage(targetMessage: SingleMessage, countMessage: SingleMessage, group: Group): Boolean {
        if (cacheSize() < 1) return false
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        //查询的数量
        val count = countMessage.toString().replace(" ", "").toInt()
        //一查询的数量
        var index = 0

        val forwardMessage = ForwardMessageBuilder(group)

        //将消息拼接
        for (i in cacheLastIndex() downTo 0) {
            val event = getCache(i)
            if (event.sender.id == targetId) {
                forwardMessage.add(event)
                index++
                if (index >= count) {
                    break
                }
            }
        }
        forwardMessage.reverse()

        group.sendMessage(forwardMessage.build())
        return true
    }

    /**
     * 禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param timeMessage 禁言时间 [SingleMessage]
     */
    private suspend fun mute(targetMessage: SingleMessage, timeMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        if (timeMessage !is PlainText) {
            return false
        }
        val timeMessageStr = timeMessage.toString().lowercase(Locale.getDefault()).replace(" ", "")

        //时间
        var time = timeMessageStr.substring(0, if (timeMessageStr.length > 1) timeMessageStr.length - 1 else 1).toInt()
        //时间单位
        var unit = timeMessageStr[timeMessageStr.length - 1]
        if (unit in '0'..'9') {
            //未写时间单位
            unit = 's'
            time = timeMessageStr.substring(0, timeMessageStr.length).toInt()
        }

        //识别时间单位
        when (unit) {
            's', '秒' -> {
                member.mute(time)
            }
            'm', '分' -> {
                member.mute(time * 60)
            }
            'h', '时' -> {
                member.mute(time * 60 * 60)
            }
            'd', '天' -> {
                member.mute(time * 60 * 60 * 24)
            }

            else -> {
                group.sendMessage("时间单位错误，可选单位：秒(s) 分(m) 时(h) 天(d)")
                return true
            }
        }
        group.sendMessage("禁言成功")
        return true
    }

    /**
     * 开启全员禁言
     */
    private suspend fun muteAll(group: Group): Boolean {
        group.settings.isMuteAll = true
        group.sendMessage("全员禁言开启")
        return true
    }

    /**
     * 关闭全员禁言
     */
    private suspend fun unmuteAll(group: Group): Boolean {
        group.settings.isMuteAll = false
        group.sendMessage("全员禁言关闭")
        return true
    }

    /**
     * 解除所有群员的禁言
     */
    private fun memberUnmuteAll(group: Group): Boolean {
        coroutineScope.launch {
            group.members.filter { it.isMuted }.let {
                if (it.isEmpty()) {
                    group.sendMessage("没有群员被禁言")
                    return@let
                }
                it.forEach { member ->
                    member.unmute()
                }
                group.sendMessage("已解除所有群员的禁言")
            }
        }
        return true
    }

    /**
     * 签到
     */
    private suspend fun sign(sender: Member, group: Group): Boolean {
        //生成随机数位签到的积分
        val now = System.currentTimeMillis()
        val memberData = database.getMember(sender.id)

        //成员首次签到
        if (memberData == null || memberData.lastSignTime == 0L) {
            database.add(
                MemberData(sender.id, sender.nameCardOrNick, 50, now, 1, 0, 0)
            )
            group.sendMessage("签到成功，首次签到获得50积分！")
            return true
        }

        val calendar: Calendar = Calendar.getInstance()
        //计算出今天的0点
        calendar.timeInMillis = now
        val todayZero = getDayTime(calendar)
        //计算出上次签到的0点
        calendar.timeInMillis = memberData.lastSignTime
        val lastZero = getDayTime(calendar)
        //判断是否是同一天
        if (lastZero == todayZero) {
            group.sendMessage(At(sender.id) + "今天已经签到过了！")
        } else {
            //判断上次的0点+24小时等于今天的0点
            if (lastZero + dayMs == todayZero) {
                //连续签到
                memberData.continueSignCount += 1
            } else {
                //非连续签到
                memberData.continueSignCount = 1
            }
            //连续签到奖励
            val fromScore = min(7 + memberData.continueSignCount * 3, 38)
            val untilScore = min(36 + memberData.continueSignCount * 6, 88)
            //生成随机数为签到的积分
            val randomScore = Random.nextInt(fromScore, untilScore)
            memberData.score += randomScore
            memberData.lastSignTime = now
            database.setMember(memberData)
            group.sendMessage(At(sender.id) + "签到成功，已连续签到${memberData.continueSignCount}天，获得${randomScore}积分！")
        }
        return true
    }

    /**
     * 积分转账
     */
    private suspend fun transfer(
        targetMessage: SingleMessage,
        scoreMessage: SingleMessage,
        sender: Member,
        group: Group
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        //被转账对象
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        if (scoreMessage !is PlainText) {
            return false
        }
        val scoreMessageStr = scoreMessage.toString().replace(" ", "")
        val score = scoreMessageStr.toIntOrNull()
        if (score == null) {
            group.sendMessage("积分格式错误")
            return true
        }
        if (score <= 0) {
            group.sendMessage("积分必须大于0")
            return true
        }
        val senderData = database.getMember(sender.id)
        if (senderData == null) {
            group.sendMessage("你还没有积分哦！")
            return true
        }
        if (senderData.score < score) {
            group.sendMessage("你的积分不足")
            return true
        }
        //扣除转账人积分
        senderData.score -= score
        database.setMember(senderData)
        //增加被转账人积分
        val targetData = database.getMember(member.id)
        if (targetData == null) {
            database.add(MemberData(member.id, member.nameCardOrNick, score, 0, 0, 0, 0))
        } else {
            targetData.score += score
            database.setMember(targetData)
        }
        group.sendMessage("转账成功")
        return true
    }

    /**
     * 积分排行榜
     */
    private suspend fun scoreRanking(group: Group): Boolean {
        val memberDataList = database.getTopTen()
        if (memberDataList.isEmpty()) {
            group.sendMessage("暂无积分数据")
        } else {
            val stringBuilder = StringBuilder("积分排行榜：\n")
            //找出重复的名字
            val repeatNameList = ArrayList<String>(10)
            for (i in memberDataList.indices) {
                for (j in i + 1 until memberDataList.size) {
                    if (memberDataList[i].name == memberDataList[j].name) {
                        repeatNameList.add(memberDataList[i].name)
                    }
                }
            }

            memberDataList.forEachIndexed { index, memberData ->
                stringBuilder.append(index + 1)
                    .append("、")
                    .append(memberData.name)
                //如果名字重复，加上QQ号
                if (repeatNameList.contains(memberData.name)) {
                    stringBuilder.append("(")
                        .append(memberData.id)
                        .append(")")
                }
                stringBuilder.append("：")
                    .append(memberData.score)
                    .append("\n")
            }
            group.sendMessage(stringBuilder.toString())
        }
        return true
    }

    /**
     * 积分禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param timeMessage 禁言时间 [SingleMessage]
     */
    private suspend fun scoreMute(
        targetMessage: SingleMessage,
        timeMessage: SingleMessage,
        sender: Member,
        group: Group
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val targetMember = group[targetId]
        if (targetMember == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        if (timeMessage !is PlainText) {
            return false
        }
        if (targetMember.muteTimeRemaining > 0) {
            group.sendMessage("该成员已被禁言，请解禁后再操作！")
            return true
        }
        val timeMessageStr = timeMessage.toString().lowercase(Locale.getDefault()).replace(" ", "")

        //时间
        var time = timeMessageStr.substring(0, if (timeMessageStr.length > 1) timeMessageStr.length - 1 else 1).toInt()
        //时间单位
        var unit = timeMessageStr[timeMessageStr.length - 1]
        if (unit in '0'..'9') {
            //未写时间单位
            unit = 's'
            time = timeMessageStr.substring(0, timeMessageStr.length).toInt()
        }

        val seconds: Int
        when (unit) {
            's', '秒' -> {
                seconds = time
            }
            'm', '分' -> {
                seconds = time * 60
            }
            'h', '时' -> {
                seconds = time * 60 * 60
            }

            else -> {
                group.sendMessage("时间单位错误，可选单位：秒(s) 分(m) 时(h)")
                return true
            }
        }

        if (seconds < 30) {
            group.sendMessage("你太快了，最少要30秒哦！")
            return true
        }
        if (seconds > 60 * 60 * 2) {
            group.sendMessage("最多只能禁言2小时哦！")
            return true
        }

        //计算积分消耗
        val score = seconds / 3
        //查询积分
        val memberData = database.getMember(sender.id)
        //判断积分是否足够
        if (memberData == null || memberData.score < score) {
            group.sendMessage("积分不足")
            return true
        }
        //扣除积分
        memberData.score -= score
        database.setMember(memberData)

        //禁言
        targetMember.mute(seconds)

        group.sendMessage("禁言成功，消耗积分：$score")
        return true
    }

    /**
     * 积分解禁
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun scoreUnmute(targetMessage: SingleMessage, sender: Member, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val targetMember = group[targetId]
        if (targetMember == null) {
            group.sendMessage("群成员不存在")
            return false
        }

        //目标被禁言时间
        val muteTime = targetMember.muteTimeRemaining

        //计算消耗的积分
        val score = muteTime / 6
        if (score < 1) {
            group.sendMessage("该成员未被禁言")
            return true
        }
        //查询积分
        val memberData = database.getMember(sender.id)
        //判断积分是否足够
        if (memberData == null || memberData.score < score) {
            //将目标被禁言时间格式化为 时:分:秒
            val hour = muteTime / 3600
            val minute = muteTime % 3600 / 60
            val second = muteTime % 60
            val stringBuilder = StringBuilder()
            if (hour != 0) {
                stringBuilder.append(hour).append("小时")
            }
            if (minute != 0) {
                stringBuilder.append(minute).append("分钟")
            }
            if (second != 0) {
                stringBuilder.append(second).append("秒")
            }
            group.sendMessage("对方被禁言时间剩余：${stringBuilder}，需要消耗积分：${score}。积分不足")
            return true
        }
        //禁言
        targetMember.unmute()

        //扣除积分
        memberData.score -= score
        database.setMember(memberData)

        group.sendMessage("解禁成功，消耗积分：$score")
        return true
    }

    /**
     * 判断 [SingleMessage] 是否可以拼接发送
     */
    private fun isSpliceSending(singleMessage: SingleMessage): Boolean {
        return singleMessage.let { it is PlainText || it is Image || it is At || it is AtAll }
    }

    /**
     * 时间戳保留到天
     */
    private fun getDayTime(calendar: Calendar): Long {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    enum class ManagerCommand {
        群管系统,
        踢,
        t,
        踢黑,
        tb,
        禁言,
        ban,
        解禁,
        kj,
        全部解禁,
        撤回,
        撤回关键词,
        封印,
        解除封印,
        查询消息记录,
        开启全员禁言,
        关闭全员禁言,
    }

    enum class MemberCommand {
        积分系统,
        签到,
        转账,
        我的积分,
        积分排行榜,
        kban,
        kj,
    }

}