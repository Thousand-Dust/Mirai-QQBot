package com.qqbot.group.msg.proc

import com.qqbot.*
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.checkPermission
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.isBotMuted
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 积分系统
 * @author Thousand-Dust
 */
class GroupScore(groupHandler: GroupEventHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    private enum class Command {
        签到,
        转账,
        我的积分,
        积分排行榜,
        kban,
        kj,
        抢劫,
        抢劫规则,
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        //在小鳄鱼群关闭积分系统
        if (myGroup.id == 167902070L) {
            return false
        }
        //发言增加积分
        val member = event.sender
        var memberData = database.getMember(member.id)
        //增加的积分，随机0~1
        var addScore = (System.currentTimeMillis() % 2).toInt()
        //消息字符串长度大于5，增加1积分
        if (event.message.contentToString().length >= 5) {
            addScore += 1
        }

        if (memberData == null) {
            memberData = MemberData(member.id, member.nameCardOrNick, addScore)
            database.addMember(memberData)
        } else {
            memberData.score += addScore
            database.setMember(memberData)
        }

        return command(event)
    }

    override fun getName(): String {
        return "积分系统"
    }

    override fun getDesc(): String {
        //在小鳄鱼群关闭积分系统
        if (myGroup.id == 167902070L) {
            return "积分系统(已关闭)"
        }
        return "积分系统(公开可用)"
    }

    override fun getMenu(event: GroupMessageEvent): String? {
        //在小鳄鱼群关闭积分系统
        if (myGroup.id == 167902070L) {
            return null
        }
        return "积分系统：\n" +
                "每条发言随机获得1~2积分，禁言每分钟消耗20积分，解除禁言每分钟消耗10积分\n" +
                "签到：" + Command.签到 + "\n" +
                "转账：" + Command.转账 + "@目标+积分数量 (额外扣除15%)\n" +
                "查询积分：" + Command.我的积分 + "\n" +
                "积分排行榜：" + Command.积分排行榜 + "\n" +
                "禁言：" + Command.kban + "@目标+时间和单位 (s秒,m分钟,h小时)\n" +
                "解禁：" + Command.kj + "@目标\n" +
//                "抢劫：" + Command.抢劫 + "@目标+积分数量\n" +
//                "抢劫规则：" + Command.抢劫规则 + "\n" +
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
                    Command.签到.name -> {
                        return sign(sender, group)
                    }
                    Command.我的积分.name -> {
                        val memberData = database.getMember(sender.id)
                        if (memberData == null) {
                            event.group.sendMessage(At(sender.id) + " 你的积分为：0")
                        } else {
                            event.group.sendMessage(At(sender.id) + " 你的积分为：${memberData.score}")
                        }
                        return true
                    }
                    Command.积分排行榜.name -> {
                        return scoreRanking(group)
                    }
                    /*Command.抢劫规则.name -> {
                        return robRule(group)
                    }*/
                }
                return false
            }
            if (message.size >= 3) {
                when (commandMessage.toString()) {
                    Command.转账.name -> {
                        return scoreTransfer(message[2], message[3], sender, group)
                    }
                    Command.kban.name -> {
                        return scoreMute(message[2], message[3], sender, event.group)
                    }
                    Command.kj.name -> {
                        return scoreUnmute(message[2], sender, event.group)
                    }
                    /*Command.抢劫.name -> {
                        return scoreRob(sender, message[2], message[3], event.group)
                    }*/
                }
                return false
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
        val memberData = database.getMember(sender.id)

        //成员首次签到
        if (memberData == null) {
            database.addMember(
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
            var isReset = false
            if (lastZero + TimeMillisecond.DAY == todayZero) {
                //连续签到
                memberData.continueSignCount += 1
                if (memberData.continueSignCount >= 7) {
                    isReset = true
                }
            } else {
                //非连续签到
                memberData.continueSignCount = 1
            }
            //连续签到奖励
            val fromScore = min(10 + memberData.continueSignCount * 3, 31)
            val untilScore = min(30 + memberData.continueSignCount * 4, 58)
            //生成随机数为签到的积分
            val randomScore = Random.nextInt(fromScore, untilScore)
            memberData.score += randomScore
            memberData.lastSignTime = now
            val msg = At(sender.id) + "签到成功，已连续签到${memberData.continueSignCount}天，获得${randomScore}${if (isReset) "+20" else ""}积分！${if (isReset) "\n签到7天，连续签到已重置，额外奖励20积分！" else ""}"
            if (isReset) {
                memberData.continueSignCount = 0
                memberData.score += 20
            }
            database.setMember(memberData)
            group.sendMessage(msg)
        }
        return true
    }

    /**
     * 积分转账，并扣除手续费15%
     */
    private suspend fun scoreTransfer(
        targetMessage: SingleMessage,
        scoreMessage: SingleMessage,
        sender: Member,
        group: Group
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        //被转账对象
        val target = group[targetId]
        if (target == null) {
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
        //需要扣除的积分
        val needScore = (score * 1.15).toInt()
        if (senderData.score < needScore) {
            group.sendMessage("你的积分不足，需要${needScore}积分，其中额外扣除的积分为${(score * 0.15).toInt()}")
            return true
        }
        //扣除转账人积分
        senderData.score -= needScore
        database.setMember(senderData)
        //增加被转账人积分
        val targetData = database.getMember(target.id)
        if (targetData == null) {
            database.addMember(MemberData(target.id, target.nameCardOrNick, score, 0, 0, 0, 0))
        } else {
            targetData.score += score
            database.setMember(targetData)
        }
        group.sendMessage("转账成功，额外扣除积分${(score * 0.15).toInt()}")
        return true
    }

    /**
     * 抢劫规则
     */
    private suspend fun robRule(group: Group): Boolean {
        group.sendMessage(
            "积分抢劫，成功率为45%\n" +
                    "如果成功，抢劫者获得抢劫积分，并被禁言抢劫积分的0.1分钟\n" +
                    "如果失败，被抢劫者扣除抢劫的积分，并被禁言抢劫积分的0.1分钟\n" +
                    "最多可抢劫对方的积分的10%\n" +
                    "抢劫者起码要拥有抢劫积分的3倍\n" +
                    "抢劫者或被抢劫者积分少于200不可抢劫\n" +
                    "抢劫者积分大于被抢劫者的1.5倍，成功率为30%\n" +
                    "对方最近十分钟没有发言不可抢劫"
        )
        return true
    }

    /**
     * 积分抢劫，成功率为45%
     * 如果成功，抢劫者获得抢劫积分，并被禁言抢劫积分的0.1分钟
     * 如果失败，被抢劫者扣除抢劫的积分，并被禁言抢劫积分的0.1分钟
     * 最多可抢劫对方的积分的10%
     * 抢劫者起码要拥有抢劫积分的3倍
     * 抢劫者或被抢劫者积分少于200不可抢劫
     * 抢劫者积分大于被抢劫者的1.5倍，成功率为30%
     * 对方最近十分钟没有发言不可抢劫
     * @param sender 抢劫者
     * @param targetMessage 被抢劫者
     * @param scoreMessage 抢劫积分
     * @param group 群
     */
    private suspend fun scoreRob(
        sender: Member,
        targetMessage: SingleMessage,
        scoreMessage: SingleMessage,
        group: Group
    ): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        //被抢劫对象
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
        val targetData = database.getMember(member.id)
        if (targetData == null) {
            group.sendMessage("对方还没有积分哦！")
            return true
        }
        if (senderData.score < score * 3) {
            group.sendMessage("你的积分不足，需要${score * 3}积分")
            return true
        }
        if (targetData.score < 200 || senderData.score < 200) {
            group.sendMessage("你或对方积分少于200不可抢劫")
            return true
        }
        if (score > targetData.score * 0.1) {
            group.sendMessage("最多只可抢劫对方的积分的10%")
            return true
        }
        if (score < 10) {
            group.sendMessage("最少抢劫10积分")
            return true
        }
        val success = if (senderData.score > targetData.score * 1.5) {
            Random.nextInt(0, 100) < 30
        } else {
            Random.nextInt(0, 100) < 45
        }
        if (success) {
            //抢劫成功
            senderData.score += score
            targetData.score -= score
            database.setMember(senderData)
            database.setMember(targetData)
            group.sendMessage("抢劫成功，你获得了${score}积分。并被警察关进了监狱")
            sender.mute(max(5, (score * 0.1).toInt()) * TimeSecond.MINUTE)
        } else {
            //抢劫失败
            senderData.score -= score
            targetData.score += score
            database.setMember(senderData)
            database.setMember(targetData)
            group.sendMessage("抢劫失败，你失去了${score}积分。并被警察关进了监狱")
            sender.mute(max(5, (score * 0.1).toInt()) * TimeSecond.MINUTE)
        }
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
                val name = memberDataList[i].name
                for (j in i + 1 until memberDataList.size) {
                    if (name == memberDataList[j].name) {
                        repeatNameList.add(name)
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
        val target = group[targetId]
        if (target == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        if (timeMessage !is PlainText) {
            return false
        }
        //检查权限
        if (!checkPermission(database, group, target, sender)) {
            return false
        }
        if (target.muteTimeRemaining > 0) {
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
        val senderData = database.getMember(sender.id)
        //判断积分是否足够
        if (senderData == null || senderData.score < score) {
            group.sendMessage("积分不足，需要积分：$score")
            return true
        }
        //扣除积分
        senderData.score -= score
        database.setMember(senderData)

        //禁言
        target.mute(seconds)

        group.sendMessage("禁言成功，消耗积分：$score")
        return true
    }

    /**
     * 积分解禁
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun scoreUnmute(targetMessage: SingleMessage, sender: Member, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val target = group[targetId]
        if (target == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        //检查权限
        if (!checkPermission(database, group, target, sender)) {
            return false
        }

        //目标被禁言时间
        val muteTime = target.muteTimeRemaining

        //计算消耗的积分
        val score = muteTime / 6
        if (score < 1) {
            group.sendMessage("该成员未被禁言")
            return true
        }
        //查询积分
        val senderData = database.getMember(sender.id)
        //判断积分是否足够
        if (senderData == null || senderData.score < score) {
            group.sendMessage("对方被禁言时间剩余：${timeFormat(muteTime * TimeMillisecond.SECOND)}，需要消耗积分：${score}。积分不足")
            return true
        }
        //禁言
        target.unmute()

        //扣除积分
        senderData.score -= score
        database.setMember(senderData)

        group.sendMessage("解禁成功，消耗积分：$score")
        return true
    }

}