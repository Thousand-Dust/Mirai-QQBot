package com.qqbot.group

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.*

/**
 * 群消息事件分发处理类
 * @author Thousand-Dust
 */
class GroupEventHandOut(private val bot: Bot, private val onCreateHandler: (Group) -> GroupEventHandler) {

    private val groupHandlers = HashMap<Long, GroupEventHandler>()
    private var coroutineScope: CoroutineScope? = null

    init {
        val groups = bot.groups
        for (group in groups) {
            val groupHandler = onCreateHandler(group)
            if (!groupHandler.onCreate()) {
                continue
            }
            groupHandlers[group.id] = groupHandler
        }
    }

    /**
     * 绑定事件
     */
    fun subScribe() {
        if (coroutineScope != null) {
            return
        }
        coroutineScope = CoroutineScope(SupervisorJob())
        bot.eventChannel.parentScope(coroutineScope!!).let {
            it.subscribeAlways<GroupMessageEvent> { event ->
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
                val groupHandler = onCreateHandler(group)
                if (groupHandler.onCreate()) {
                    groupHandlers[group.id] = groupHandler
                }
            }
            it.subscribeAlways<BotLeaveEvent> { event ->
                groupHandlers.remove(event.groupId)?.onRemove()
            }
            Unit
        }
    }

    /**
     * 取消绑定事件
     */
    fun unSubscribe() {
        coroutineScope?.cancel()
        coroutineScope = null
    }

}