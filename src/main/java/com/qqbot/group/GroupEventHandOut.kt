package com.qqbot.group

import com.qqbot.ai.ChatGPTManager
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.*

/**
 * 群消息事件分发处理类
 * @author Thousand-Dust
 */
class GroupEventHandOut(private val bot: Bot, private val chatGPTManager: ChatGPTManager) {

    private val groupHandlers = HashMap<Long, GroupEventHandler>()

    init {
        val groups = bot.groups
        for (group in groups) {
            val groupHandler = GroupHandler(group[bot.id]!!, chatGPTManager)
            if (!groupHandler.onCreate(group)) {
                continue
            }
            groupHandlers[group.id] = groupHandler
        }
    }

    fun subScribe() {
        bot.eventChannel.let {
            it.subscribeAlways<GroupMessageEvent> { event ->
                event.toString()
                groupHandlers[event.group.id]?.acceptMessage(event)
            }
            it.subscribeAlways<MemberJoinEvent> { event ->
                groupHandlers[event.group.id]?.onMemberJoin(event)
            }
            it.subscribeAlways<MemberLeaveEvent> { event ->
                groupHandlers[event.group.id]?.onMemberLeave(event)
            }
            it.subscribeAlways<BotJoinGroupEvent> { event ->
                val group = event.group
                val groupHandler = GroupHandler(group[bot.id]!!, chatGPTManager)
                if (groupHandler.onCreate(group)) {
                    groupHandlers[group.id] = groupHandler
                }
            }
            it.subscribeAlways<BotLeaveEvent> { event ->
                groupHandlers.remove(event.groupId)
            }
            Unit
        }
    }

}