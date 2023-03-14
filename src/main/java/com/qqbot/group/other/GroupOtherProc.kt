package com.qqbot.group.other

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.BotGroupPermissionChangeEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent

/**
 * 群其他事件处理器，每个群单独一个实例
 * @author Thousand-Dust
 */
abstract class GroupOtherProc(protected val myGroup: Group) {

    /**
     * 收到入群申请时调用
     */
    abstract fun onMemberJoinRequest(event: MemberJoinRequestEvent)

    /**
     * 新群员加入后调用
     */
    abstract fun onMemberJoin(event: MemberJoinEvent)

    /**
     * 群员退出群后调用
     */
    abstract fun onMemberLeave(event: MemberLeaveEvent)

    abstract fun onMyPermissionChange(event: BotGroupPermissionChangeEvent)

    /**
     * 不需要再接收此群的消息时调用
     */
    open fun onRemove() {
        //do nothing
    }

}