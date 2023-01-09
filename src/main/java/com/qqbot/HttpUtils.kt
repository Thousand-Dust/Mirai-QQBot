package com.qqbot

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object HttpUtils {
    val instance = OkHttpClient()

    @Throws(IOException::class)
    fun get(url: String): String {
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()
        val response = instance.newCall(request).execute()
        return response.body!!.string()
    }

    @Throws(IOException::class)
    fun post(url: String, body: String): String {
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json;charset=utf-8".toMediaType()))
            .build()
        val response = instance.newCall(request).execute()
        return response.body!!.string()
    }

    /**
     * get方法下载文件
     */
    @Throws(IOException::class)
    fun download(url: String, path: String) {
        val request = okhttp3.Request.Builder()
            .url(url)
            .build()
        val response = instance.newCall(request).execute()
        Utils.writeFile(path, response.body!!.bytes(), false)
    }
}