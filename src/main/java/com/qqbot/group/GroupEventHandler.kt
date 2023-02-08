package com.qqbot.group

import com.qqbot.Info
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.*
import java.util.*
import java.util.stream.Stream

/**
 * 群事件处理器，每个群单独一个实例
 */
abstract class GroupEventHandler(val myGroup: Group) {

    //群消息事件缓存
    private val eventCache = object : LinkedList<GroupMessageEvent>() {
        override fun add(element: GroupMessageEvent): Boolean {
            //添加消息到缓存列表，缓存列表如果大于或等于最大大小则删除第一条缓存的事件
            if (size >= Info.EVENT_CACHE_MAX_SIZE) {
                removeFirst()
            }
            return super.add(element)
        }
    }
    //bot发送成功后的消息事件缓存
    val botSendEventCache = object : LinkedList<GroupMessagePostSendEvent>() {
        override fun add(element: GroupMessagePostSendEvent): Boolean {
            if (size >= Info.BOT_SEND_EVENT_CACHE_MAX_SIZE) {
                removeFirst()
            }
            return super.add(element)
        }
    }

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
     * bot发送消息后调用
     */
    fun acceptBotSendMessage(event: GroupMessagePostSendEvent) {
        if (event.exception == null && event.receipt != null) {
            //发送成功，添加到缓存
            botSendEventCache.add(event)
        }
    }

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
    protected fun addCache(event: GroupMessageEvent) = eventCache.add(event)

    @Synchronized
    fun getCache(index: Int): GroupMessageEvent = eventCache[index]

    @Synchronized
    fun cacheSize(): Int = eventCache.size

    @Synchronized
    fun cacheLastIndex() = eventCache.lastIndex

    @Synchronized
    fun <E> cacheStreamCall(call: (Stream<GroupMessageEvent>) -> E): E = call.invoke(eventCache.stream())

}