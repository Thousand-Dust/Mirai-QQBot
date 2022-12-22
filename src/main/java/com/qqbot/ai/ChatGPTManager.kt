package com.qqbot.ai

import java.util.*
import kotlin.collections.HashMap

class ChatGPTManager {

    private val chatGPTMap = HashMap<Long, ChatGPTLogic>()
    private val mapIdLinked = LinkedList<Long>()
    private val chatGptMaxSize = 100

    fun getChatResponse(id: Long, message: String): String {
        val chatGPTLogic = chatGPTMap[id].let {
            if (it == null || it.expiredTime < System.currentTimeMillis()) {
                //ai对象不存在或者过期

                if (chatGPTMap.size >= chatGptMaxSize) {
                    //超过最大缓存数量，清理第一条缓存
                    chatGPTMap.remove(mapIdLinked.removeFirst())
                }

                //缓存ai对象
                val temp = ChatGPTLogic()
                temp.refreshSession()
                chatGPTMap[id] = temp
                mapIdLinked.add(id)
                return@let temp
            }
            it
        }

        return chatGPTLogic.getChatResponse(message)
    }
}