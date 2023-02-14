package com.qqbot.group.msg.proc

import com.qqbot.Info
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.GroupDatabaseImpl
import com.qqbot.group.GroupHandler
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupMessagePostSendEvent
import net.mamoe.mirai.event.events.MessagePreSendEvent

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
        return null
    }

    private fun command(event: GroupMessageEvent): Boolean {
        if (event.sender.id != Info.RootManagerId) {
            return false
        }
        val message = event.message

        if (message.size == 2) {
            val commandMessage = message[0]
            when (commandMessage.contentToString()) {
                Command.开机.name -> {
                }
                Command.关机.name -> {
                }
                Command.加积分.name -> {
                }
            }
        }
        return false
    }

}