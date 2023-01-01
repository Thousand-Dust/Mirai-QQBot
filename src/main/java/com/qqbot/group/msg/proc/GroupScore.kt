package com.qqbot.group.msg.proc

import com.qqbot.TimeMillisecond
import com.qqbot.TimeSecond
import com.qqbot.Utils
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage
import net.mamoe.mirai.message.data.sourceOrNull
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min
import kotlin.random.Random

/**
 * 积分系统
 * @author Thousand-Dust
 */
class GroupScore(groupHandler: GroupEventHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    enum class MemberCommand {
        签到,
        转账,
        我的积分,
        积分排行榜,
        kban,
        kj,
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        //发言增加积分
        val member = event.sender
        var memberData = database.getMemberData(member.id)
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

        return command(event)
    }

    override fun getName(): String {
        return "积分系统"
    }

    override fun getDesc(): String {
        return "积分系统(公开可用)"
    }

    override fun getMenu(event: GroupMessageEvent): String {
        return "积分系统：\n" +
                "每条发言随机获得1~2积分，禁言每分钟消耗20积分，解除禁言每分钟消耗10积分\n" +
                "签到：" + MemberCommand.签到 + "\n" +
                "转账：" + MemberCommand.转账 + "@目标+积分数量\n" +
                "查询积分：" + MemberCommand.我的积分 + "\n" +
                "积分排行榜：" + MemberCommand.积分排行榜 + "\n" +
                "禁言：" + MemberCommand.kban + "@目标+时间和单位 (s秒,m分钟,h小时)\n" +
                "解禁：" + MemberCommand.kj + "@目标\n" +
                "其他功能待更新..."
    }

    /**
     * 指令识别
     * @return 是否处理了指令
     */
    private suspend fun command(event: GroupMessageEvent): Boolean {
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
                    MemberCommand.签到.name -> {
                        return sign(sender, group)
                    }
                    MemberCommand.我的积分.name -> {
                        val memberData = database.getMemberData(sender.id)
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
                        return scoreTransfer(message[2], message[3], sender, group)
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
     * 签到
     */
    private suspend fun sign(sender: Member, group: Group): Boolean {
        //生成随机数位签到的积分
        val now = System.currentTimeMillis()
        val memberData = database.getMemberData(sender.id)

        //成员首次签到
        if (memberData == null) {
            database.add(
                MemberData(sender.id, sender.nameCardOrNick, 50, now, 1)
            )
            group.sendMessage("签到成功，首次签到获得50积分！")
            return true
        }
        if (memberData.lastSignTime == 0L) {
            memberData.lastSignTime = now
            memberData.continueSignCount = 1
            memberData.score += 50
            database.setMember(memberData)
            group.sendMessage("签到成功，首次签到获得50积分！")
            return true
        }

        val calendar: Calendar = Calendar.getInstance()
        //计算出今天的0点
        calendar.timeInMillis = now
        val todayZero = Utils.getDayTime(calendar)
        //计算出上次签到的0点
        calendar.timeInMillis = memberData.lastSignTime
        val lastZero = Utils.getDayTime(calendar)
        //判断是否是同一天
        if (lastZero == todayZero) {
            group.sendMessage(At(sender.id) + "今天已经签到过了！")
        } else {
            //判断上次的0点+24小时等于今天的0点
            if (lastZero + TimeMillisecond.DAY == todayZero) {
                //连续签到
                memberData.continueSignCount += 1
            } else {
                //非连续签到
                memberData.continueSignCount = 1
            }
            //连续签到奖励
            val fromScore = min(7 + memberData.continueSignCount * 3, 38)
            val untilScore = min(30 + memberData.continueSignCount * 5, 78)
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
    private suspend fun scoreTransfer(
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
        val senderData = database.getMemberData(sender.id)
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
        val targetData = database.getMemberData(member.id)
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
                seconds = time * TimeSecond.MINUTE
            }
            'h', '时' -> {
                seconds = time * TimeSecond.HOUR
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
        if (seconds > TimeSecond.HOUR) {
            group.sendMessage("最多只能禁言1小时哦！")
            return true
        }

        //计算积分消耗
        val score = seconds / 3
        //查询积分
        val memberData = database.getMemberData(sender.id)
        //判断积分是否足够
        if (memberData == null || memberData.score < score) {
            group.sendMessage("积分不足，需要积分：$score")
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
        val memberData = database.getMemberData(sender.id)
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

}