package com.qqbot

import okhttp3.OkHttpClient
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.IOException

object HttpUtils {

    val instance = OkHttpClient()

    /**
     * 发送阻塞get请求
     */
    @Throws(IOException::class)
    fun get(url: String, params: Map<String, String>? = null): ResponseBody? {
        val paramsStr = StringBuilder()
        params?.forEach {
            paramsStr.append(it.key).append("=").append(it.value).append("&")
        }
        val request = Request.Builder()
            .url(url + if (paramsStr.isNotEmpty()) "?" + paramsStr.substring(0, paramsStr.length - 1) else "")
            .build()
        val response = instance.newCall(request).execute()
        return response.body
    }

    /**
     * 发送非阻塞get请求
     */
    fun get(url: String, params: Map<String, String>? = null, callback: Callback) {
        val paramsStr = StringBuilder()
        params?.forEach {
            paramsStr.append(it.key).append("=").append(it.value).append("&")
        }
        val request = Request.Builder()
            .url(url + if (paramsStr.isNotEmpty()) "?" + paramsStr.substring(0, paramsStr.length - 1) else "")
            .build()
        instance.newCall(request).enqueue(callback)
    }

    /**
     * 发送阻塞post请求
     */
    @Throws(IOException::class)
    fun post(url: String, body: String): ResponseBody? {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json;charset=utf-8".toMediaType()))
            .build()
        val response = instance.newCall(request).execute()
        return response.body
    }

    /**
     * 发送非阻塞post请求
     */
    fun post(url: String, body: String, callback: Callback) {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json;charset=utf-8".toMediaType()))
            .build()
        instance.newCall(request).enqueue(callback)
    }
}