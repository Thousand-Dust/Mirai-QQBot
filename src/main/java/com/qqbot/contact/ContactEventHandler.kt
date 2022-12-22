package com.qqbot.contact

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.FriendAddEvent
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent

interface ContactEventHandler {

    fun onCreate(bot: Bot)

    fun onNewFriendRequest(newFriendRequestEvent: NewFriendRequestEvent)

    fun onFriendAddEvent(friendAddEvent: FriendAddEvent)

    fun onFriendMessageEvent(friendMessageEvent: FriendMessageEvent)

}