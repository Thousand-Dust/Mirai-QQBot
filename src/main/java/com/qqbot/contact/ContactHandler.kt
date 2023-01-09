package com.qqbot.contact

import com.qqbot.ai.ChatGPTManager
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.FriendAddEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.PlainText
import java.util.concurrent.*
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy

/**
 * 好友相关事件
 */
class ContactHandler : ContactEventHandler {

    private lateinit var myBot: Bot

    private val executorService: ExecutorService = ThreadPoolExecutor(
        10, 10,
        10000, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(10),
        Executors.defaultThreadFactory(),
        AbortPolicy()
    )

    override fun onCreate(bot: Bot) {
        this.myBot = bot
    }

    override fun onNewFriendRequest(newFriendRequestEvent: NewFriendRequestEvent) {
    }

    override fun onFriendAddEvent(friendAddEvent: FriendAddEvent) {
    }

    override fun onFriendMessageEvent(friendMessageEvent: FriendMessageEvent) {
        runBlocking {
            val friend = friendMessageEvent.friend
            if (friend.id == myBot.id) {
                return@runBlocking
            }
            friend.sendMessage("小冰暂不支持聊天哦")
            /*val singleMessage = friendMessageEvent.message[1]
            if (singleMessage !is PlainText) {
                friend.sendMessage("不支持的消息类型")
                return@runBlocking
            }
            val message = singleMessage.contentToString()
            try {
                friend.sendMessage(chatGPTManager.getChatResponse(friend.id, message))
            } catch (e: IllegalStateException) {
                if (!e.toString().contains("Send message failed")) {
                    friend.sendMessage("错误：$e")
                }
            } catch (e: Exception) {
                friend.sendMessage("错误：$e")
            }*/
        }
    }

}