package com.qqbot.group

import com.qqbot.Info
import com.qqbot.TimeMillisecond
import com.qqbot.TimeSecond
import com.qqbot.Utils
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import com.qqbot.group.msg.proc.GroupManager
import com.qqbot.group.msg.proc.GroupOwner
import com.qqbot.group.msg.proc.GroupScore
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import java.lang.Integer.max
import java.util.stream.Collectors

/**
 * 单个群的消息处理类
 * @author Thousand-Dust
 */
class GroupHandler(myGroup: Group, my: Member) : GroupEventHandler(myGroup, my) {

    private val database = GroupDatabase(myGroup.id)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    //群主系统
    private val groupOwner = GroupOwner(this, database)

    //群管系统
    private val groupManager = GroupManager(this, database)

    //积分系统
    private val scoreManager = GroupScore(this, database)

    override fun onCreate(): Boolean {
        return true
    }

    override fun onRemove() {
        database.close()
    }

    override fun acceptMessage(event: GroupMessageEvent) {
        addCache(event)

        coroutineScope.launch {
            try {
                val message = event.message

                //显示菜单
                if (message.size == 2 && message[1] is PlainText) {
                    //发送的菜单消息内容
                    var menuStr: String? = null

                    val msgStr = message[1].toString()
                    if (msgStr == "菜单") {
                        //TODO：需要优化，暂时没想到好的办法
                        menuStr = StringBuilder()
                            .append("菜单：\n")
                            .append("主人系统(还没写)\n")
                            .append(groupOwner.getDesc())
                            .append("\n")
                            .append(groupManager.getDesc())
                            .append("\n")
                            .append(scoreManager.getDesc())
                            .toString()
                    } else if (msgStr == groupOwner.getName()) {
                        menuStr = groupOwner.getMenu(event)
                    } else if (msgStr == groupManager.getName()) {
                        menuStr = groupManager.getMenu(event)
                    } else if (msgStr == scoreManager.getName()) {
                        menuStr = scoreManager.getMenu(event)
                    }
                    if (menuStr != null) {
                        event.subject.sendMessage(menuStr)
                    }
                }

                /**
                 * 群主系统
                 */
                if (groupOwner.process(event)) {
                    return@launch
                }

                //群管系统
                if (groupManager.process(event)) {
                    return@launch
                }

                //检测刷屏
                if (checkFrequentSending(event)) {
                    return@launch
                }

                //积分系统
                if (scoreManager.process(event)) {
                    return@launch
                }

                //检测违规消息
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
                event.group.sendMessage(At(event.member.id) + "被" + event.operator!!.nameCardOrNick + " 踢出了群聊")
                return@runBlocking
            }
        }
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
                if (cache.sender.id == senderId && lastTime - message.time <= 30) {
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
            return true
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
            database.addMember(memberData)
        }
        //判断48小时内是否有多次违规
        if (System.currentTimeMillis() - memberData.lastViolationTime < (TimeMillisecond.DAY * 2)) {
            memberData.violationCount += 1
        } else {
            memberData.violationCount = 1
        }
        memberData.lastViolationTime = System.currentTimeMillis()
        database.setMember(memberData)
        //计算惩罚时间
        val muteTime = when (memberData.violationCount) {
            1 -> TimeSecond.MINUTE * 10
            2 -> TimeSecond.MINUTE * 30
            3 -> TimeSecond.HOUR
            4 -> TimeSecond.HOUR * 6
            else -> TimeSecond.DAY
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
     * 其他违规行为检测
     */
    private suspend fun violationDetection(event: GroupMessageEvent): Boolean {
        if (event.sender.isOperator() || !my.isOperator()) return false

        //检测非法艾特全体成员
        //方式1：使用模块将全部成员逐一艾特
        val message = event.message
        if (message.stream().filter(At::class::isInstance).count() > 15) {
            message.recall()
            event.sender.mute(TimeSecond.DAY)
            val at = At(event.sender.id)
            event.group.sendMessage(at + " 违规行为，艾特人数过多")
            return true
        }

        //检测使用xml卡片发假红包
        val msgStr = message.toString()
        if (msgStr.contains("<?xml") || msgStr.contains("brief=\"[QQ红包]")) {
            message.recall()
            event.sender.mute(TimeSecond.DAY)
            val at = At(event.sender.id)
            event.group.sendMessage(at + " 违规行为，发送假红包")
            return true
        }
        return false
    }

}