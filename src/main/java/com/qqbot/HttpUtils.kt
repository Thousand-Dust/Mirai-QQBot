package com.qqbot

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.IOException
import java.util.zip.GZIPInputStream

object HttpUtils {
    val instance = OkHttpClient()

    /**
     * 发送get请求
     */
    @Throws(IOException::class)
    fun get(url: String): ResponseBody? {
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()
        val response = instance.newCall(request).execute()
        return response.body
    }

    /**
     * 发送post请求
     */
    @Throws(IOException::class)
    fun post(url: String, body: String): ResponseBody? {
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json;charset=utf-8".toMediaType()))
            .build()
        val response = instance.newCall(request).execute()
        return response.body
    }
}