package com.qqbot.group.msg

import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.GroupEventHandler
import net.mamoe.mirai.event.events.GroupMessageEvent
import java.util.stream.Stream

/**
 * 群消息处理器，每个群单独一个实例
 * @author Thousand-Dust
 */
abstract class GroupMsgProc(protected val groupHandler: GroupEventHandler, protected val database: GroupDatabase) {

    protected val myGroup = groupHandler.myGroup
    protected val my = groupHandler.my

    /**
     * 收到消息需要处理时调用
     * @return 是否已经处理了消息
     */
    abstract suspend fun process(event: GroupMessageEvent): Boolean

    /**
     * 获取群消息处理器的名字
     */
    abstract fun getName(): String

    /**
     * 获取群消息处理器在菜单的描述
     */
    abstract fun getDesc(): String

    /**
     * 显示群消息处理器的功能菜单
     * @return 用户可使用的功能菜单
     */
    abstract fun getMenu(event: GroupMessageEvent): String?

    fun getCache(index: Int): GroupMessageEvent {
        return groupHandler.getCache(index)
    }

    fun cacheSize(): Int {
        return groupHandler.cacheSize()
    }

    fun cacheLastIndex(): Int {
        return groupHandler.cacheLastIndex()
    }

    fun <E> cacheStreamCall(call: (Stream<GroupMessageEvent>) -> E): E {
        return groupHandler.cacheStreamCall(call)
    }

}