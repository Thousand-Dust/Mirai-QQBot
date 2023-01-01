package com.qqbot.group.msg.proc

import com.qqbot.group.GroupPermission.isOperator
import com.qqbot.Info
import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import java.util.*
import java.util.stream.Collectors
import kotlin.math.min

/**
 * 群管系统
 * @author Thousand-Dust
 */
class GroupManager(groupHandler: GroupEventHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    enum class ManagerCommand {
        群管系统,
        踢,
        t,
        踢黑,
        tb,
        禁言,
        ban,
        解禁,
        kj,
        全部解禁,
        撤回,
        撤回关键词,
        封印,
        解除封印,
        查询消息记录,
        开启全员禁言,
        关闭全员禁言,
    }

    //封印列表
    private val sealMap = HashMap<Long, Int>()

    override suspend fun process(event: GroupMessageEvent): Boolean {
        //检查封印
        run {
            val senderId = event.sender.id
            val num = sealMap[senderId] ?: return@run
            if (num <= 0) {
                sealMap.remove(senderId)
                return@run
            }
            sealMap[senderId] = num - 1
            event.message.recall()
            return true
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
        if (!(event.sender.isOperator() || database.getMemberData(event.sender.id)?.isOperator() == true)) {
            return null
        }

        return "管理员操作：\n" +
                "踢出群聊：${ManagerCommand.踢}/${ManagerCommand.t}+@目标\n" +
                "踢出并拉黑：${ManagerCommand.踢黑}/${ManagerCommand.tb}+@目标\n" +
                "禁言：${ManagerCommand.禁言}/${ManagerCommand.ban}+@目标+时间和单位(默认10m) (s秒,m分钟,h小时,d天)\n" +
                "解禁：${ManagerCommand.解禁}/${ManagerCommand.kj}+@目标\n" +
                "撤回：" + ManagerCommand.撤回 + "+@目标+撤回数量(默认10)\n" +
                "撤回关键词：" + ManagerCommand.撤回关键词 + "+(空格)+关键词\n" +
                "封印：" + ManagerCommand.封印 + "+@目标+封印层数\n" +
                "解除封印：" + ManagerCommand.解除封印 + "+@目标\n" +
                "查询消息记录：" + ManagerCommand.查询消息记录 + "@目标+查询数量(默认5)\n" +
                "全员禁言：" + ManagerCommand.开启全员禁言 + "\n" +
                "关闭全员禁言：" + ManagerCommand.关闭全员禁言 + "\n" +
                "解除所有群员的禁言：" + ManagerCommand.全部解禁 + "\n" +
                "其他功能待更新..."
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
        val isPermission = sender.permission.isOperator() || sender.id == Info.RootManagerId
        if (!(isPermission && message[1] is PlainText)) {
            return false
        }
        //命令消息（消息头）
        val commandMessage = message[1]
        //识别命令
        if (message.size == 2) {
            when (commandMessage.toString()) {
                ManagerCommand.开启全员禁言.name -> {
                    return muteAll(event.group)
                }
                ManagerCommand.关闭全员禁言.name -> {
                    return unmuteAll(event.group)
                }
                ManagerCommand.全部解禁.name -> {
                    return memberUnmuteAll(event.group)
                }
                else -> {
                    if (commandMessage.toString().split(" ").size == 2 && recallKeyword(message, event.group)) {
                        //撤回关键词
                        return true
                    }
                }
            }
            return false
        }
        if (message.size >= 3) {
            when (commandMessage.toString()) {
                ManagerCommand.踢.name, ManagerCommand.t.name -> {
                    return kick(message[2], event.group)
                }
                ManagerCommand.踢黑.name, ManagerCommand.tb.name -> {
                    return kick(message[2], event.group, true)
                }
                ManagerCommand.禁言.name, ManagerCommand.ban.name -> {
                    return mute(message[2], isSingleMessageEmpty(message, 3, PlainText("10m")), event.group)
                }
                ManagerCommand.解禁.name, ManagerCommand.kj.name -> {
                    return unmute(message[2], event.group)
                }
                ManagerCommand.撤回.name -> {
                    return recall(message[2], isSingleMessageEmpty(message, 3, PlainText("10")), event.group)
                }
                ManagerCommand.封印.name -> {
                    return seal(message[2], isSingleMessageEmpty(message, 3, PlainText("5")), event.group)
                }
                ManagerCommand.解除封印.name -> {
                    return unseal(message[2], event.group)
                }
                ManagerCommand.查询消息记录.name -> {
                    return queryMessage(message[2], isSingleMessageEmpty(message, 3, PlainText("5")), event.group)
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
    private suspend fun kick(targetMessage: SingleMessage, group: Group, block: Boolean = false): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        member.kick("", block)
        group.sendMessage(if (block) "成功踢出并拉黑" else "踢出成功")
        return true
    }

    /**
     * 解除成员禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     */
    private suspend fun unmute(targetMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        member.unmute()
        group.sendMessage("已解除禁言")

        return true
    }

    /**
     * 撤回群员消息
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param countMessage 撤回消息数量 [SingleMessage] (PlainText)
     */
    private suspend fun recall(targetMessage: SingleMessage, countMessage: SingleMessage, group: Group): Boolean {
        if (cacheSize() < 1) return false

        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        //撤回消息的数量
        val count = countMessage.toString().replace(" ", "").toInt()
        //已撤回的数量
        var index = 0
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
        return true
    }

    /**
     * 根据关键词撤回群员消息
     */
    private suspend fun recallKeyword(message: MessageChain, group: Group): Boolean {
        if (cacheSize() < 1) return false

        //拼接的消息内容
        val messageStr = message.stream().filter(MessageContent::class::isInstance).map(SingleMessage::toString)
            .collect(Collectors.joining()).trim()
        //命令和字符分割处
        val index = messageStr.indexOf(" ")
        if (index != 5) {
            return false
        }
        val command = messageStr.substring(0, index)
        //撤回的关键词
        val keyword = messageStr.substring(index + 1)
        if (command != ManagerCommand.撤回关键词.name) {
            return false
        }
        for (i in cacheLastIndex() downTo cacheLastIndex() - min(128, cacheLastIndex())) {
            val event = getCache(i)
            if (event.message.toString().contains(keyword)) {
                try {
                    event.message.recall()
                } catch (e: IllegalStateException) {
                }
            }
        }
        group.sendMessage("已撤回包含关键词 \"$keyword\" 的消息")
        return true
    }

    /**
     * 封印群员（群员封印状态下发消息会直接被撤回）
     */
    private suspend fun seal(targetMessage: SingleMessage, countMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return true
        }
        if (member.permission.level >= my.permission.level) {
            group.sendMessage("权限不足")
            return true
        }
        sealMap[member.id] = countMessage.toString().replace(" ", "").toInt()

        group.sendMessage("封印成功")

        return true
    }

    /**
     * 封印群员（群员封印状态下发消息会直接被撤回）
     */
    private suspend fun unseal(targetMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false

        if (sealMap.remove(targetId) != null) {
            group.sendMessage("已解除封印")
        }

        return true
    }

    /**
     * 查询群成员历史消息
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param countMessage 查询消息数量 [SingleMessage] 类型应该为: [PlainText]
     */
    private suspend fun queryMessage(targetMessage: SingleMessage, countMessage: SingleMessage, group: Group): Boolean {
        if (cacheSize() < 1) return false
        val targetId = if (targetMessage is At) targetMessage.target else return false
        if (countMessage !is PlainText) {
            return false
        }
        //查询的数量
        val count = countMessage.toString().replace(" ", "").toInt()
        //一查询的数量
        var index = 0

        val forwardMessage = ForwardMessageBuilder(group)

        //将消息拼接
        for (i in cacheLastIndex() downTo 0) {
            val event = getCache(i)
            if (event.sender.id == targetId) {
                forwardMessage.add(event)
                index++
                if (index >= count) {
                    break
                }
            }
        }
        forwardMessage.reverse()

        group.sendMessage(forwardMessage.build())
        return true
    }

    /**
     * 禁言
     * @param targetMessage 目标群成员 [SingleMessage] (@群成员)
     * @param timeMessage 禁言时间 [SingleMessage]
     */
    private suspend fun mute(targetMessage: SingleMessage, timeMessage: SingleMessage, group: Group): Boolean {
        val targetId = if (targetMessage is At) targetMessage.target else return false
        val member = group[targetId]
        if (member == null) {
            group.sendMessage("群成员不存在")
            return false
        }
        if (timeMessage !is PlainText) {
            return false
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

        //识别时间单位
        when (unit) {
            's', '秒' -> {
                member.mute(time)
            }
            'm', '分' -> {
                member.mute(time * 60)
            }
            'h', '时' -> {
                member.mute(time * 60 * 60)
            }
            'd', '天' -> {
                member.mute(time * 60 * 60 * 24)
            }

            else -> {
                group.sendMessage("时间单位错误，可选单位：秒(s) 分(m) 时(h) 天(d)")
                return true
            }
        }
        group.sendMessage("禁言成功")
        return true
    }

    /**
     * 开启全员禁言
     */
    private suspend fun muteAll(group: Group): Boolean {
        group.settings.isMuteAll = true
        group.sendMessage("全员禁言开启")
        return true
    }

    /**
     * 关闭全员禁言
     */
    private suspend fun unmuteAll(group: Group): Boolean {
        group.settings.isMuteAll = false
        group.sendMessage("全员禁言关闭")
        return true
    }

    /**
     * 解除所有群员的禁言
     */
    private suspend fun memberUnmuteAll(group: Group): Boolean {
        group.members.filter { it.isMuted }.let {
            if (it.isEmpty()) {
                group.sendMessage("没有群员被禁言")
                return@let
            }
            it.forEach { member ->
                member.unmute()
            }
            group.sendMessage("已解除所有群员的禁言")
        }
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