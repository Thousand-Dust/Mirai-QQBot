package com.qqbot.group.msg.proc

import com.qqbot.Info
import com.qqbot.TimeMillisecond
import com.qqbot.TimeSecond
import com.qqbot.Utils
import com.qqbot.ai.TextClassifier
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.checkPermission
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.time
import java.util.stream.Collectors
import kotlin.math.max

/**
 * 群消息检测系统
 */
class GroupCheck(groupHandler: GroupEventHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    private val textClassifier = TextClassifier("ai/classifier.bin")

    override suspend fun process(event: GroupMessageEvent): Boolean {
        if (checkPermission(database, event.group, event.sender, isSendMsg = false)) {
            if (brushScreen(event)) {
                return true
            }
            if (illegalMessage(event)) {
                return true
            }
        }
        if (checkDirtyWord(event)) {
            return true
        }

        return false
    }

    override fun getName(): String? {
        return null
    }

    override fun getDesc(): String? {
        return null
    }

    override fun getMenu(event: GroupMessageEvent): String? {
        return null
    }

    /**
     * 脏话检测 TODO：测试中
     */
    private suspend fun checkDirtyWord(event: GroupMessageEvent): Boolean {
        val message = event.message
        if (message.filterIsInstance<PlainText>().isEmpty()) return false

        val msgStr = message.contentToString()
        val msgStr1 = msgStr.replace(" ", "")
        if (msgStr.length > 128 || msgStr1.isEmpty()) return false

        val senderId = event.sender.id

        val label = textClassifier.categorize(msgStr)
        if (!myGroup.botPermission.isOperator()) {
            return false
        }
        when (label) {
            "脏话" -> {
                if (!checkPermission(database, event.group, event.sender, isSendMsg = false)) {
//                  event.message.recall()
                }
                event.group.sendMessage(At(event.sender) + " 请注意言辞！code: 001")
                return true
            }
            "色情" -> {
                if (checkPermission(database, event.group, event.sender, isSendMsg = false)) {
//                  event.message.recall()
                }
                event.group.sendMessage(At(event.sender) + " 请注意言辞！code: 002")
                return true
            }
            "广告" -> {
                if (!checkPermission(database, event.group, event.sender, isSendMsg = false)) return false
                event.message.recall()
                event.group.sendMessage(At(event.sender) + " 禁止打广告！")
            }
            "其他" -> {
                if (!checkPermission(database, event.group, event.sender, isSendMsg = false)) return false
                val historyMsg = cacheStreamCall { stream ->
                    //跳过事件缓存的前面，留下最后 [com.qqbot.Info.CHECK_EVENT_COUNT] 条缓存用于检测
                    stream.skip((Integer.max(0, cacheSize() - Info.CHECK_EVENT_COUNT)).toLong())
                        .filter { it.sender.id == senderId }
                }.collect(Collectors.toList())

                //无意义消息数量
                var count = 0
                //倒序遍历 historyMsg
                for (i in historyMsg.size - 1 downTo historyMsg.size - 6) {
                    if (i < 0) break
                    val msgToStr = historyMsg[i].message.contentToString()
                    val label1 = textClassifier.categorize(msgToStr)
                    if (label1 == "无意义" || label1 == "其他") {
                        count++
                    }
                }
                if (count >= 3) {
                    event.message.recall()
                    event.group.sendMessage(At(event.sender) + " 请不要频繁发送无意义消息刷屏！")
                    return true
                }
            }
        }

        return false
    }

    /**
     * 检测刷屏
     */
    private suspend fun brushScreen(event: GroupMessageEvent): Boolean {
        val senderId = event.sender.id
        val message = event.message
        val lastTime = message.time

        //35秒内连续发送的消息
        if (cacheSize() >= Info.CHECK_EVENT_COUNT_MAX) {
            //连续发送的消息数
            var count = 0
            //倒序遍历最后 [Info.CHECK_EVENT_COUNT_MAX] 条消息
            for (i in cacheSize() - 1 downTo cacheSize() - Info.CHECK_EVENT_COUNT) {
                val cache = getCache(i)
                //同一个人一分钟内发送的消息
                if (cache.sender.id == senderId && lastTime - cache.time <= 26) {
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
            stream.skip((Integer.max(0, cacheSize() - Info.CHECK_EVENT_COUNT)).toLong())
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
                if (!Utils.messageChainEqual(message, checkedMsgRecord[i].message) || lastTime - checkedMsgRecord[i].message.time > TimeSecond.MINUTE*10) {
                    return false
                }
            }
            violationMute(event.sender, event.group)
            return true
        }

        return false
    }

    /**
     * 其他违规行为检测
     */
    private suspend fun illegalMessage(event: GroupMessageEvent): Boolean {

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
        if (msgStr.contains("<?xml") && msgStr.contains("brief=\"[QQ红包]")) {
            message.recall()
            event.sender.mute(TimeSecond.DAY)
            val at = At(event.sender.id)
            event.group.sendMessage(at + " 违规行为，发送假红包")
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

}