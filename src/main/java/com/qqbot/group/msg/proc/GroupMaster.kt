package com.qqbot.group.msg.proc

import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.GroupHandler
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.event.events.GroupMessageEvent

/**
 * 群 主人系统
 */
class GroupMaster(groupHandler: GroupHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    private enum class Command {
        开机,
        关机,
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        return false
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
        return false
    }

}