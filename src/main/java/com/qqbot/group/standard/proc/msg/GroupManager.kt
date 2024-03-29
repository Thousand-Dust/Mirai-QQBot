package com.qqbot.group.standard.proc.msg

import com.qqbot.Info
import com.qqbot.TimeMillisecond
import com.qqbot.database.group.GroupDatabaseImpl
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.checkPermission
import com.qqbot.group.isOperator
import com.qqbot.group.msg.GroupMsgProc
import com.qqbot.timeFormat
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.sourceTime
import java.util.*
import kotlin.math.min

/**
 * 群管系统
 * @author Thousand-Dust
 */
class GroupManager(groupHandler: GroupEventHandler, database: GroupDatabaseImpl) :
    GroupMsgProc(groupHandler, database) {

    private enum class Command {
        踢,
        t,
        踢黑,
        tb,
        禁言,
        ban,
        解禁,
        kj,
        撤回,
        撤回关键词,
        封印,
        解除封印,
        封印列表,
        查询消息记录,
        开启全员禁言,
        关闭全员禁言,
        禁言列表,
        全部解禁,
        清屏,
    }

    //封印列表
    private val sealMap = HashMap<Long, Int>()

    override suspend fun process(event: GroupMessageEvent): Boolean {
        //检查封印
        run {
            val senderId = event.sender.id
            val num = (sealMap[senderId] ?: return@run) - 1
            sealMap[senderId] = num
            if (num <= 0) {
                sealMap.remove(senderId)
            }
            event.message.recall()
            return true
        }
        if (myGroup.isBotMuted) {
            return false
        }

        return command(event)
    }

    override fun getName(): String {
        return "群管系统"
    }

    override fun getDesc(): String {
        return "群管系统(群主管理可用)"
    }

    override fun getMenu(event: GroupMessageEvent): String? {
        //拥有管理员权限才能使用
        val isOperator = event.sender.isOperator()
        val isRoot = event.sender.id == Info.RootManagerId
        val isCustomPermissions = database.getMember(event.sender.id)?.isOperator() == true
        if (!(isOperator || isRoot || isCustomPermissions)) {
            return null
        }

        return """
                群管系统：
                踢出群聊：${Command.踢}/${Command.t}+@目标
                踢出并拉黑：${Command.踢黑}/${Command.tb}+@目标
                禁言：${Command.禁言}/${Command.ban}+@目标+时间和单位(默认10m) (s秒,m分钟,h小时,d天)
                解禁：${Command.解禁}/${Command.kj}+@目标
                撤回：${Command.撤回}+[@目标(可选)]+撤回数量(默认10)
                撤回关键词：${Command.撤回关键词}+关键词
                封印：${Command.封印}+@目标+封印层数
                解除封印：${Command.解除封印}+@目标
                查看封印列表：${Command.封印列表}
                查询消息记录：${Command.查询消息记录}@目标+查询数量(默认5)
                开启/关闭全员禁言：(开启/关闭)全员禁言
                查看被禁言群员列表：${Command.禁言列表}
                解除所有群员的禁言：${Command.全部解禁}
                清屏： ${Command.清屏}
                """.trimIndent()
    }

    /**
     * 群管理员命令
     * @return 是否处理了该事件
     */
    private suspend fun command(event: GroupMessageEvent): Boolean {
        val message = event.message
        //消息发送对象
        val sender = event.sender
        //消息发送者是否为管理员
        val isOperator = event.sender.isOperator()
        val isRoot = event.sender.id == Info.RootManagerId
        val isCustomPermissions = database.getMember(event.sender.id)?.isOperator() == true
        val isPermission = isOperator || isRoot || isCustomPermissions
        if (!(isPermission && message[1] is PlainText)) {
            return false
        }
        //命令消息（消息头）
        val command = message[1] as PlainText
        //识别命令
        if (message.size == 2) {
            when (command.toString()) {
                Command.封印列表.name -> {
                    return sealList()
                }
                Command.开启全员禁言.name -> {
                    return muteAll()
                }
                Command.关闭全员禁言.name -> {
                    return unmuteAll()
                }
                Command.禁言列表.name -> {
                    return memberMuteList()
                }
                Command.全部解禁.name -> {
                    return memberUnmuteAll()
                }
                Command.清屏.name -> {
                    return clearScreen()
                }
                else -> {
                    val comMsgStr = command.toString()
                    if (comMsgStr.startsWith(Command.撤回关键词.name)) {
                        return recallKeyword(message, comMsgStr.substring(Command.撤回关键词.name.length))
                    } else if (comMsgStr.startsWith(Command.撤回.name)) {
                        return recallAny(message, comMsgStr.substring(Command.撤回.name.length))
                    }
                }
            }
            return false
        }
        if (message.size >= 3) {
            when (command.toString()) {
                Command.踢.name, Command.t.name -> {
                    return kick(message[2], event.sender)
                }
                Command.踢黑.name, Command.tb.name -> {
                    return kick(message[2], event.sender, true)
                }
                Command.禁言.name, Command.ban.name -> {
                    return mute(
                        message[2],
                        isSingleMessageEmpty(message, 3, PlainText("10m")),
                        event.sender
                    )
                }
                Command.解禁.name, Command.kj.name -> {
                    return unmute(message[2], event.sender)
                }
                Command.撤回.name -> {
                    return recall(
                        message[2],
                        isSingleMessageEmpty(message, 3, PlainText("10")),
                        event.sender
                    )
                }
                Command.封印.name -> {
                    return seal(message[2], isSingleMessageEmpty(message, 3, PlainText("5")), event.sender)
                }
                Command.解除封印.name -> {
                    return unseal(message[2])
                }
                Command.查询消息记录.name -> {
                    return queryMessage(message[2], isSingleMessageEmpty(message, 3, PlainText("5")))
                }
            }
            return false
        }
        return false
    }

    /**
     * 踢出群成员
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun kick(
        targetMessage: SingleMessage,
        sender: Member,
        block: Boolean = false
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val target = myGroup[targetId]
        if (target == null) {
            myGroup.sendMessage("找不到该成员")
            return true
        }
        //检查权限
        if (!checkPermission(database, myGroup, target, sender)) {
            return true
        }
        target.kick("", block)
        myGroup.sendMessage(if (block) "成功踢出并拉黑" else "踢出成功")
        return true
    }

    /**
     * 禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param timeMessage 禁言时间 [SingleMessage]
     */
    private suspend fun mute(
        targetMessage: SingleMessage,
        timeMessage: SingleMessage,
        sender: Member
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val target = myGroup[targetId]
        if (target == null) {
            myGroup.sendMessage("找不到该成员")
            return true
        }
        if (timeMessage !is PlainText) {
            return false
        }
        //检查权限
        if (!checkPermission(database, myGroup, target, sender)) {
            return true
        }
        val timeMessageStr = timeMessage.toString().toLowerCase(Locale.getDefault()).replace(" ", "")

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
                target.mute(time)
            }
            'm', '分' -> {
                target.mute(time * 60)
            }
            'h', '时' -> {
                target.mute(time * 60 * 60)
            }
            'd', '天' -> {
                target.mute(time * 60 * 60 * 24)
            }

            else -> {
                myGroup.sendMessage("时间单位错误，可选单位：秒(s) 分(m) 时(h) 天(d)")
                return true
            }
        }
        myGroup.sendMessage("禁言成功")
        return true
    }

    /**
     * 解除成员禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun unmute(targetMessage: SingleMessage, sender: Member): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val target = myGroup[targetId]
        if (target == null) {
            myGroup.sendMessage("找不到该成员")
            return true
        }
        //检查权限
        if (myGroup.botAsMember.permission.level <= target.permission.level) {
            myGroup.sendMessage("机器人权限不足")
            return true
        }
        target.unmute()
        myGroup.sendMessage("已解除禁言")

        return true
    }

    /**
     * 撤回群员消息
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param countMessage 撤回消息数量 [SingleMessage] (PlainText)
     */
    private suspend fun recall(
        targetMessage: SingleMessage,
        countMessage: SingleMessage,
        sender: Member
    ): Boolean {
        if (cacheSize() < 1) return false

        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        myGroup[targetId]?.let {
            //检查权限
            if (!checkPermission(database, myGroup, it, sender)) {
                return true
            }
        }
        //撤回消息的数量
        val count = countMessage.toString().replace(" ", "").toInt()
        //已撤回的数量
        var index = 0
        if (targetId == this.myGroup.botAsMember.id) {
            //被撤回对象是机器人
            for (i in groupHandler.botSendEventCache.lastIndex downTo 0) {
                val event = groupHandler.botSendEventCache[i]
                try {
                    event.receipt?.recall() ?: continue
                    index++
                } catch (e: IllegalStateException) {
                }
                if (index >= count) {
                    break
                }
            }
        } else {
            //被撤回对象是其他群员
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
        }
        myGroup.sendMessage("撤回完毕")
        return true
    }

    /**
     * 根据关键词撤回群员消息
     */
    private suspend fun recallKeyword(senderMsg: MessageChain, keyword: String): Boolean {
        if (cacheSize() < 1) return false

        //撤回的消息数量
        var count = 0
        for (i in cacheLastIndex() downTo cacheLastIndex() - min(128, cacheLastIndex())) {
            val message = getCache(i).message
            if (message.toString().contains(keyword) && senderMsg.source != message.source) {
                try {
                    message.recall()
                    count++
                } catch (e: IllegalStateException) {
                }
            }
        }
        if (count == 0) {
            myGroup.sendMessage("未找到包含关键词 \"$keyword\" 的消息")
            return true
        }
        myGroup.sendMessage("已撤回包含关键词 \"$keyword\" 的${count}消息")
        return true
    }

    /**
     * 撤回任何已发送的消息
     */
    private suspend fun recallAny(senderMsg: MessageChain, countStr: String): Boolean {
        if (cacheSize() < 1) return false

        //需要撤回的消息数量
        var count = countStr.toIntOrNull() ?: return false
        //实际撤回的消息数量
        var index = 0
        for (i in cacheLastIndex() downTo cacheLastIndex() - min(256, cacheLastIndex())) {
            val message = getCache(i).message
            if (senderMsg.source != message.source) {
                try {
                    message.recall()
                    count--
                    index++
                } catch (e: IllegalStateException) {
                }
            }
            if (count <= 0) {
                break
            }
        }
        if (index == 0) {
            myGroup.sendMessage("未找到可撤回的消息（可能是由于消息未在机器人开机期间被记录）")
            return true
        }

        myGroup.sendMessage("已撤回${index}条消息")

        return true
    }

    /**
     * 封印群员（群员封印状态下发消息会直接被撤回）
     */
    private suspend fun seal(
        targetMessage: SingleMessage,
        countMessage: SingleMessage,
        sender: Member
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        val target = myGroup[targetId]
        if (target == null) {
            myGroup.sendMessage("找不到该成员")
            return true
        }
        //检查权限
        if (!checkPermission(database, myGroup, target, sender)) {
            return true
        }
        sealMap[target.id] = countMessage.toString().replace(" ", "").toInt()

        myGroup.sendMessage("封印成功")

        return true
    }

    /**
     * 解除群员封印
     */
    private suspend fun unseal(targetMessage: SingleMessage): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false

        if (sealMap.remove(targetId) != null) {
            myGroup.sendMessage("已解除封印")
        }

        return true
    }

    /**
     * 查看封印列表
     */
    private suspend fun sealList(): Boolean {
        if (sealMap.isEmpty()) {
            myGroup.sendMessage("封印列表为空")
            return true
        }
        //找出重复的名字
        val repeatNameList = ArrayList<String>(10)
        for (i in sealMap) {
            val name = myGroup[i.key]?.nameCardOrNick ?: "已退群成员"
            //从sealMap查找是否有重复的名字
            for (j in sealMap) {
                if (i.key == j.key) continue
                if (name == (myGroup[j.key]?.nameCardOrNick ?: "已退群成员")) {
                    repeatNameList.add(name)
                    break
                }
            }
        }
        val builder = StringBuilder()
        for ((key, value) in sealMap) {
            val nameOrNick = myGroup[key]?.nameCardOrNick ?: "已退群成员"
            builder.append(nameOrNick)
            if (repeatNameList.contains(nameOrNick)) {
                builder.append("(").append(key).append(")")
            }
            builder.append("：").append(value).append("条\n")
        }
        builder.deleteCharAt(builder.lastIndex)
        myGroup.sendMessage(builder.toString())
        return true
    }

    /**
     * 查询群成员历史消息
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param countMessage 查询消息数量 [SingleMessage] 类型应该为: [PlainText]
     */
    private suspend fun queryMessage(targetMessage: SingleMessage, countMessage: SingleMessage): Boolean {
        if (cacheSize() < 1) return false
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        //查询的数量
        val count = min(countMessage.toString().replace(" ", "").toInt(), 100)

        //已查询的数量
        var index = 0
        //消息字数
        var length = 0
        //消息图片数量
        var imageCount = 0

        val forwardMessage = ForwardMessageBuilder(myGroup)

        if (targetId == this.myGroup.botAsMember.id) {
            //被查询对象是机器人
            for (i in groupHandler.botSendEventCache.lastIndex downTo 0) {
                val event = groupHandler.botSendEventCache[i]
                val msg = event.message
                imageCount += msg.filterIsInstance<Image>().size
                length += msg.contentToString().length
                //判断消息内容是否超出可发送范围
                if (imageCount >= 45 || length >= 4500) {
                    forwardMessage.add(
                        this.myGroup.botAsMember,
                        MessageChainBuilder().append("查询的消息数量过多，已自动停止查询").build()
                    )
                    break
                }
                forwardMessage.add(this.myGroup.botAsMember, msg, event.receipt?.sourceTime ?: -1)
                index++
                if (index >= count) {
                    break
                }
            }
        } else {
            //被查询对象是其他群员
            for (i in cacheLastIndex() downTo 0) {
                val event = getCache(i)
                if (event.sender.id == targetId) {
                    val msg = event.message
                    imageCount += msg.filterIsInstance<Image>().size
                    length += msg.contentToString().length
                    //判断消息内容是否超出可发送范围
                    if (imageCount >= 45 || length >= 4500) {
                        forwardMessage.add(
                            this.myGroup.botAsMember,
                            MessageChainBuilder().append("查询的消息数量过多，已自动停止查询").build()
                        )
                        break
                    }
                    forwardMessage.add(event)
                    index++
                    if (index >= count) {
                        break
                    }
                }
            }
        }
        forwardMessage.reverse()

        myGroup.sendMessage(forwardMessage.build())
        return true
    }

    /**
     * 开启全员禁言
     */
    private suspend fun muteAll(): Boolean {
        myGroup.settings.isMuteAll = true
        myGroup.sendMessage("全员禁言开启")
        return true
    }

    /**
     * 关闭全员禁言
     */
    private suspend fun unmuteAll(): Boolean {
        myGroup.settings.isMuteAll = false
        myGroup.sendMessage("全员禁言关闭")
        return true
    }

    /**
     * 查看被禁言群员列表
     */
    private suspend fun memberMuteList(): Boolean {
        val muteList = myGroup.members.filter { it.isMuted }
        if (muteList.isEmpty()) {
            myGroup.sendMessage("当前没有被禁言的群员")
            return true
        }
        //找出重复的名字
        val repeatNameList = ArrayList<String>(10)
        for (i in muteList.indices) {
            val nameCardOrNick = muteList[i].nameCardOrNick
            for (j in i + 1 until muteList.size) {
                if (nameCardOrNick == muteList[j].nameCardOrNick) {
                    repeatNameList.add(nameCardOrNick)
                }
            }
        }
        val message = buildString {
            append("被禁言群员列表：\n")
            for (member in muteList) {
                append(member.nameCardOrNick)
                //名字重复则显示QQ号
                if (repeatNameList.contains(member.nameCardOrNick)) {
                    append("(").append(member.id).append(")")
                }
                append(": ")
                append(timeFormat(member.muteTimeRemaining * TimeMillisecond.SECOND))
                append("\n")
            }
            //删除最后一个换行符
            deleteCharAt(lastIndex)
        }
        myGroup.sendMessage(message)
        return true
    }

    /**
     * 解除所有群员的禁言
     */
    private suspend fun memberUnmuteAll(): Boolean {
        myGroup.members.filter { it.isMuted }.let {
            if (it.isEmpty()) {
                myGroup.sendMessage("没有群员被禁言")
                return@let
            }
            it.forEach { member ->
                member.unmute()
            }
            myGroup.sendMessage("已解除所有群员的禁言")
        }
        return true
    }

    /**
     * 清屏
     */
    private suspend fun clearScreen(): Boolean {
        myGroup.sendMessage(("清屏" + ("\n").repeat(20)).repeat(10))
        return true
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

}