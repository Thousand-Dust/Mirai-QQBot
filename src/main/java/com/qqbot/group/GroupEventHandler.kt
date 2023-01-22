package com.qqbot.group

import com.qqbot.Info
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.BotGroupPermissionChangeEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import java.util.*
import java.util.stream.Stream

/**
 * 群事件处理器，每个群单独一个实例
 */
abstract class GroupEventHandler(val myGroup: Group, val my: Member) {

    //群消息事件缓存
    private var eventCache = LinkedList<GroupMessageEvent>()

    /**
     * 被创建时调用
     * @return 是否启用此群的事件订阅
     */
    abstract fun onCreate(): Boolean

    /**
     * 不需要再接收此群的事件时调用
     */
    abstract fun onRemove()

    /**
     * 收到消息时调用
     */
    abstract fun acceptMessage(event: GroupMessageEvent)

    /**
     * 有群员入群时调用
     */
    abstract fun onMemberJoin(event: MemberJoinEvent)

    /**
     * 有群员退群时调用
     */
    abstract fun onMemberLeave(event: MemberLeaveEvent)

    /**
     * 机器人在群的权限变更时调用
     */
    abstract fun onMyPermissionChange(event: BotGroupPermissionChangeEvent)

    /**
     * 添加事件到缓存
     */
    @Synchronized
    protected fun addCache(event: GroupMessageEvent) {
        //添加消息到缓存列表，缓存列表如果大于或等于最大大小则删除第一条缓存的事件
        if (eventCache.size >= Info.EVENT_CACHE_MAX_SIZE) {
            eventCache.removeFirst()
        }
        event.source
        eventCache.add(event)
    }

    @Synchronized
    fun getCache(index: Int): GroupMessageEvent {
        return eventCache[index]
    }

    @Synchronized
    fun cacheSize(): Int {
        return eventCache.size
    }

    @Synchronized
    fun cacheLastIndex(): Int {
        return eventCache.lastIndex
    }

    @Synchronized
    fun <E> cacheStreamCall(call: (Stream<GroupMessageEvent>) -> E): E {
        return call.invoke(eventCache.stream())
    }

}