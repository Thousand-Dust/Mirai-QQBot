package com.qqbot.group.msg.proc

import com.qqbot.Info
import com.qqbot.database.group.GroupDatabaseImpl
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupHandler
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage

/**
 * 群 主人系统
 */
class GroupMaster(groupHandler: GroupHandler, database: GroupDatabaseImpl) : GroupMsgProc(groupHandler, database) {

    private enum class Command {
        开机,
        关机,
        加积分,
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        return command(event)
    }

    override fun getName(): String {
        return "主人系统"
    }

    override fun getDesc(): String {
        return "主人系统(仅机器人主人可用)"
    }

    override fun getMenu(event: GroupMessageEvent): String? {
        return """
            ${Command.开机.name} - 开机
            ${Command.关机.name} - 关机
            ${Command.加积分.name} - 加积分+@目标+积分数量
        """.trimIndent()
    }

    private suspend fun command(event: GroupMessageEvent): Boolean {
        if (event.sender.id != Info.RootManagerId) {
            return false
        }
        val message = event.message

        val commandMessage = message[1]
        if (message.size == 2) {
            when (commandMessage.contentToString()) {
                Command.开机.name -> {
                }
                Command.关机.name -> {
                }
            }
            return false
        }
        if (message.size >= 3) {
            when (commandMessage.contentToString()) {
                Command.加积分.name -> {
                    return addScore(message[2], message[3])
                }
            }
        }
        return false
    }

    /**
     * 增加积分
     */
    private suspend fun addScore(targetMessage: SingleMessage, countMessage: SingleMessage): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        //增加的积分数量
        val count = countMessage.toString().replace(" ", "").toInt()
        //获取目标对象
        val target = myGroup[targetId]
        if (target == null) {
            myGroup.sendMessage("群成员不存在")
            return false
        }

        //获取目标的信息
        var targetData = database.getMember(targetId)
        if (targetData == null) {
            targetData = MemberData(targetId, target.nameCardOrNick)
            database.addMember(targetData)
        }
        targetData.score += count
        database.setMember(targetData)
        myGroup.sendMessage("已为"+At(targetId)+"增加${count}积分")

        return true
    }

}