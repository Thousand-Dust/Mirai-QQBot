package com.qqbot.group

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.event.events.MemberMuteEvent

/**
 * 群事件处理接口
 * 当开始订阅群消息时被 [GroupEventHandOut] 创建
 * @author Thousand-Dust
 */
interface GroupEventHandler {

    /**
     * 被创建时调用
     * @return 是否启用此群的事件订阅
     */
    fun onCreate(group: Group): Boolean

    /**
     * 不需要再接收此群的事件时调用
     */
    fun onRemove(group: Group)

    /**
     * 收到消息时调用
     */
    fun acceptMessage(event: GroupMessageEvent)

    /**
     * 有群员入群时调用
     */
    fun onMemberJoin(event: MemberJoinEvent)

    /**
     * 有群员退群时调用
     */
    fun onMemberLeave(event: MemberLeaveEvent)

    /**
     * 群成员被禁言时调用
     */
    fun onMemberMute(event: MemberMuteEvent)

}