package com.qqbot.group

import com.qqbot.TimeMillisecond
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import net.mamoe.mirai.contact.*

/**
 * 机器人权限检查
 * 检查机器人是否有权限对目标执行操作
 * 群主(主人) > 群管理员(群管) > 群员
 * @param database 群数据库
 * @param group 群
 * @param target 被操作对象，则检查机器人是否有对其操作的权限
 * @param sender 操作者 如果不为空，检查机器人和 [sender]是否有对 [target] 操作的权限
 * @param isSendMsg 权限不足是否发送消息
 * @return 机器人是否有权限可以执行操作
 */
suspend fun checkPermission(
    database: GroupDatabase,
    group: Group,
    target: Member,
    sender: Member? = null,
    isSendMsg: Boolean = true
): Boolean {
    if (!group.botPermission.isOperator()) {
        if (isSendMsg) {
            group.sendMessage("机器人权限不足！")
        }
        return false
    }
    //检查机器人是否有对目标执行操作的权限
    if (group.botPermission.level <= target.permission.level) {
        if (isSendMsg) {
            group.sendMessage("机器人权限不足")
        }
        return false
    }
    val targetData = database.getMember(target.id) ?: MemberData(target.id, target.nameCardOrNick)
    if (sender != null) {
        val senderData = database.getMember(sender.id) ?: MemberData(sender.id, sender.nameCardOrNick)
        //检查操作者是否有对目标执行操作的权限
        if (sender.permission.level != 0 && target.permission.level != 0 && sender.permission.level <= target.permission.level) {
            if (isSendMsg) {
                group.sendMessage("操作者权限不足")
            }
            return false
        }
        if (targetData.permission != 0 && senderData.permission != 0 && senderData.permission <= targetData.permission) {
            if (isSendMsg) {
                group.sendMessage("操作者权限不足")
            }
            return false
        }
    }
    if (group.botPermission.level <= targetData.permission) {
        if (isSendMsg) {
            group.sendMessage("机器人权限不足")
        }
        return false
    }
    return true
}
