package com.qqbot.group

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.*

/**
 * 群事件分发处理类
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
            //群消息事件
            it.subscribeAlways<GroupMessageEvent> { event ->
                groupHandlers[event.group.id]?.acceptMessage(event)
            }
            //bot发送消息后事件
            it.subscribeAlways<GroupMessagePostSendEvent> { event ->
                groupHandlers[event.target.id]?.acceptBotSendMessage(event)
            }

            //有人申请加入群事件
            it.subscribeAlways<MemberJoinRequestEvent> { event ->
                groupHandlers[event.group?.id]?.onMemberJoinRequest(event)
            }
            //群成员加入群聊事件（已经加入）
            it.subscribeAlways<MemberJoinEvent> { event ->
                groupHandlers[event.group.id]?.onMemberJoin(event)
            }
            //群成员退群事件（已经退群）
            it.subscribeAlways<MemberLeaveEvent> { event ->
                groupHandlers[event.group.id]?.onMemberLeave(event)
            }

            //bot加入群事件
            it.subscribeAlways<BotJoinGroupEvent> { event ->
                val group = event.group
                val groupHandler = onCreateHandler(group)
                if (groupHandler.onCreate()) {
                    groupHandlers[group.id] = groupHandler
                }
            }
            //bot被踢出群事件
            it.subscribeAlways<BotLeaveEvent> { event ->
                groupHandlers.remove(event.groupId)?.onRemove()
            }
            //bot在群里的权限变化事件
            it.subscribeAlways<BotGroupPermissionChangeEvent> { event ->
                groupHandlers[event.group.id]?.onMyPermissionChange(event)
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