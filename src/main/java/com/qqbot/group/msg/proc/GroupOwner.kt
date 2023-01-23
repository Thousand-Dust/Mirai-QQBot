package com.qqbot.group.msg.proc

import com.qqbot.Info
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.GroupPermission
import com.qqbot.group.GroupPermission.isOperator
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.SingleMessage

/**
 * 群主系统
 * @author Thousand-Dust
 */
class GroupOwner(groupHandler: GroupEventHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    private enum class Command {
        添加群管,
        删除群管,
        群管列表,
        开启发言检测,
        关闭发言检测,
        开启积分系统,
        关闭积分系统,
        开启入群验证,
        关闭入群验证,
        开启入群自动审核,
        关闭入群自动审核,
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        return command(event)
    }

    override fun getName(): String {
        return "群主系统"
    }

    override fun getDesc(): String {
        return "群主系统(群主专用)"
    }

    override fun getMenu(event: GroupMessageEvent): String? {
        if (!(event.sender.permission.isOwner() || event.sender.id == Info.RootManagerId)) {
            return null
        }
        return "群主系统：\n" +
                Command.添加群管 + "+@目标\n" +
                Command.删除群管 + "+@目标\n" +
                Command.群管列表
    }

    private suspend fun command(event: GroupMessageEvent): Boolean {
        val message = event.message
        //消息发送对象
        val sender = event.sender
        //消息发送者是否为群主
        val isOwner = sender.permission.isOwner()
        val isRoot = event.sender.id == Info.RootManagerId
        if (!(isOwner || isRoot) || message[1] !is PlainText) {
            return false
        }
        val command = message[1] as PlainText
        if (message.size == 2) {
            when (command.toString()) {
                Command.群管列表.name -> {
                    getManagerList(event.group)
                    return true
                }
            }
            return false
        }

        if (message.size >= 3) {
            when (command.toString()) {
                Command.添加群管.name -> {
                    addManager(message[2], event.group)
                    return true
                }
                Command.删除群管.name -> {
                    removeManager(message[2], event.group)
                    return true
                }
            }
            return false
        }
        return false
    }

    /**
     * 获取群管列表
     */
    private suspend fun getManagerList(group: Group) {
        val managerList = database.getPermissions(GroupPermission.ADMIN)
        if (managerList.isEmpty()) {
            group.sendMessage("当前群没有群管")
            return
        }
        //找出重复的名字
        val repeatNameList = ArrayList<String>(10)
        for (i in managerList.indices) {
            val name = managerList[i].name
            for (j in i + 1 until managerList.size) {
                if (name == managerList[j].name) {
                    repeatNameList.add(name)
                }
            }
        }
        val message = buildString {
            for (manager in managerList) {
                append(manager.name)
                //名字重复则显示QQ号
                if (repeatNameList.contains(manager.name)) {
                    append("(").append(manager.id).append(")")
                }
                append("\n")
            }
            //删除最后一个换行符
            deleteCharAt(lastIndex)
        }
        group.sendMessage(message)
    }

    /**
     * 添加群管
     * @param targetMessage 被添加的对象
     */
    private suspend fun addManager(targetMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val target = group[targetId]
        if (target == null) {
            group.sendMessage("找不到该成员")
            return false
        }
        val targetData = database.getMember(targetId)
        if (target.isOperator() && targetData != null && targetData.isOperator()) {
            group.sendMessage("该成员已经是群管")
            return true
        }
        if (targetData == null) {
            database.addMember(MemberData(targetId, target.nameCardOrNick, permission = GroupPermission.ADMIN))
            group.sendMessage("添加群管成功")
            return true
        }
        targetData.permission = GroupPermission.ADMIN
        database.setMember(targetData)
        group.sendMessage("添加群管成功")
        return true
    }

    /**
     * 删除群管
     */
    private suspend fun removeManager(targetMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val target = group[targetId]
        if (target == null) {
            group.sendMessage("找不到该成员")
            return false
        }
        val targetData = database.getMember(targetId)
        if (targetData == null || !targetData.isOperator()) {
            group.sendMessage("该成员不是群管")
            return true
        }
        targetData.permission = GroupPermission.MEMBER
        database.setMember(targetData)
        group.sendMessage("删除群管成功")
        return true
    }

}