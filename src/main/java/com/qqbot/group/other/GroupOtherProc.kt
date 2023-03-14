package com.qqbot.group.other

import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.*

/**
 * 群其他事件处理器，每个群单独一个实例
 * @author Thousand-Dust
 */
abstract class GroupOtherProc(protected val myGroup: Group) {

    /**
     * 收到入群申请时调用
     * @return 是否已处理此事件
     */
    abstract fun onMemberJoinRequest(event: MemberJoinRequestEvent): Boolean

    /**
     * 新群员加入后调用
     * @return 是否已处理此事件
     */
    abstract fun onMemberJoin(event: MemberJoinEvent): Boolean

    /**
     * 群员退出群后调用
     * @return 是否已处理此事件
     */
    abstract fun onMemberLeave(event: MemberLeaveEvent): Boolean

    /**
     * 群权限变更时调用
     * @return 是否已处理此事件
     */
    abstract fun onMemberPermissionChange(event: MemberPermissionChangeEvent): Boolean

    /**
     * 不需要再接收此群的消息时调用
     */
    open fun onRemove() {
        //do nothing
    }

}