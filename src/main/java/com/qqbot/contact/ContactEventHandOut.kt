package com.qqbot.contact

import com.qqbot.ai.ChatGPTManager
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.FriendAddEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent

class ContactEventHandOut(private val bot: Bot) {

    private val contactHandler = ContactHandler()

    init {
        contactHandler.onCreate(bot)
    }

    fun subScribe() {
        bot.eventChannel.let {
            it.subscribeAlways<FriendMessageEvent> {
                contactHandler.onFriendMessageEvent(this)
            }
            it.subscribeAlways<NewFriendRequestEvent> {
                contactHandler.onNewFriendRequest(this)
            }
            it.subscribeAlways<FriendAddEvent> {
                contactHandler.onFriendAddEvent(this)
            }
        }
    }

}