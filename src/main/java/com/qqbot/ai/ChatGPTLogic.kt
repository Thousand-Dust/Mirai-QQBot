package com.qqbot.ai

import com.alibaba.fastjson.JSON
import com.qqbot.HttpUtils
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.NullPointerException
import java.util.*

class ChatGPTLogic {

    companion object {
        private var authorization: String = ""
        private var sessionToken: String = ""
        private val overdueTime = 8 * 60_000
        private var lastFileTime = 0L

        init {
            /*val file = File("token.txt")
            val updateInvoke: () -> Unit = {
                file.absolutePath
                if (file.exists() && file.lastModified() != lastFileTime) {
                    lastFileTime = file.lastModified()
                    sessionToken = String(Utils.readFile(file.path))
                }
            }
            updateInvoke.invoke()

            thread {
                while (true) {
                    updateInvoke.invoke()
                    Thread.sleep(3000)
                }
            }*/
        }
    }

    private var conversationId: String? = null
    private var parentId: String = UUID.randomUUID().toString()
    private var head = Headers.Builder()
    private val session_token = "__Secure-next-auth.session-token"
    var expiredTime = System.currentTimeMillis() + overdueTime
        private set

    fun getChatResponse(prompt: String): String {
        expiredTime = System.currentTimeMillis() + overdueTime
        val data = mapOf(
            "action" to "next",
            "conversation_id" to conversationId,
            "parent_message_id" to parentId,
            "model" to "text-davinci-002-render",
            "messages" to listOf(
                mapOf(
                    "id" to UUID.randomUUID().toString(),
                    "role" to "user",
                    "content" to mapOf(
                        "content_type" to "text",
                        "parts" to listOf(prompt)
                    )
                )
            )
        )

        val body = JSON.toJSONString(data)
        val cookie = "$session_token=$sessionToken; __Secure-next-auth.callback-url=https://chat.openai.com/"

//        println("post body --> ${body}")
        body.toRequestBody()
        val response = try {
            var request = Request.Builder()
                .url("https://chat.openai.com/backend-api/conversation")
                .headers(head.add("cookie", cookie).build())
                .post(body.toRequestBody("application/json;charset=utf-8".toMediaType()))
                .build()
            HttpUtils.instance.newCall(request).execute()
        } catch (e: IOException) {
            throw IOException("请求超时，请重试, " + e.message)
        }
        val str = response.body?.string() ?: throw Exception("error:没有响应体")
        response.close()
        if (str.startsWith("<!DOCTYPE")) {
            refreshSession()
            throw Exception("error:未初始化")
        }
        try {
            val lines = str.split("\n")
            val lastLine = lines.last { it.endsWith("}") }
            val result = lastLine.substring(5)
//            println("res-->$result")
            val json = JSON.parseObject(result)
            json.getJSONObject("detail")?.getString("code")?.let {
                it == "invalid_api_key" || it == "token_expired"
                refreshSession()
//                return getChatResponse(prompt, conversationId)
                throw RuntimeException("秘钥或令牌过期")
            }
            val msg = json.getJSONObject("message")
                .getJSONObject("content")
                .getJSONArray("parts")
                .getString(0)!!
            conversationId = json.getString("conversation_id")
            parentId = json.getJSONObject("message").getString("id")
            return msg
        } catch (e: Exception) {
            refreshSession()
            throw Exception("请求chat api出错\n${str}")
        }
    }

    fun refreshSession() {
        // Set cookies
        val cookie = "$session_token=$sessionToken"

        val urlSession = "https://chat.openai.com/api/auth/session"
        var request = Request.Builder()
            .url(urlSession)
            .headers(
                Headers.Builder()
                    .add("cookie", cookie)
                    .add(
                        "user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
                    ).build()
            )
            .get()
            .build()
        val response = HttpUtils.instance.newCall(request).execute()
        //get token from cookie
        /*sessionToken = response.headers.values("set-cookie").let {
            for (str in  it) {
                val strSplit = str.split("=")
                if (strSplit[0] == session_token) {
                    return@let strSplit[1].split(";")[0]
                }
            }
            response.close()
            throw RuntimeException("解析response失败: token不存在")
        }*/
        val bodyStr = response.body?.string() ?: throw NullPointerException("body.string() == null")
        authorization = JSON.parseObject(bodyStr).getString("accessToken")
            ?: throw RuntimeException("解析response失败:${bodyStr}")
        response.close()
        refreshHeaders()
    }

    private fun refreshHeaders() {
        head = Headers.Builder()
        head.run {
            add("Host", "chat.openai.com")
            add("Accept", "text/event-stream")
            add("Authorization", "Bearer $authorization")
            add("Content-Type", "application/json")
            add(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
            )
            add("X-Openai-Assistant-App-Id", "")
            add("Connection", "close")
            add("Accept-Language", "zh-CN,zh;q=0.9")
            add("Referer", "https://chat.openai.com/chat")
        }
    }
}