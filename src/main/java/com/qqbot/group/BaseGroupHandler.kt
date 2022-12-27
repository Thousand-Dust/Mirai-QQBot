package com.qqbot.group

import com.qqbot.Info
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.GroupMessageEvent
import java.util.*
import java.util.stream.Stream

abstract class BaseGroupHandler(protected val my: Member): GroupEventHandler {

    //群消息事件缓存
    private var eventCache = LinkedList<GroupMessageEvent>()

    /**
     * 添加事件到缓存
     */
    @Synchronized
    protected fun addCache(event: GroupMessageEvent) {
        //添加消息到缓存列表，缓存列表如果大于或等于最大大小则删除第一条缓存的事件
        if (eventCache.size >= Info.EVENT_CACHE_MAX_SIZE) {
            eventCache.removeFirst()
        }
        eventCache.add(event)
    }

    @Synchronized
    protected fun getCache(index: Int): GroupMessageEvent {
        return eventCache[index]
    }

    @Synchronized
    protected fun cacheSize(): Int {
        return eventCache.size
    }

    @Synchronized
    protected fun cacheLastIndex(): Int {
        return eventCache.lastIndex
    }

    @Synchronized
    protected fun <E> cacheStreamCall(call: (Stream<GroupMessageEvent>) -> E): E {
        return call.invoke(eventCache.stream())
    }

}