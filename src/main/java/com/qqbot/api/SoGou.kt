package com.qqbot.api

import com.qqbot.HttpUrl
import com.qqbot.HttpUtils

/**
 * 搜狗文本转语音
 */
fun sogouTextToAudio(text: String, speed: String = "1", lang: String = "zh-CHS", from: String = "translateweb", speaker: String = "2"): ByteArray? {
    val params = HashMap<String, String>()
    params["text"] = text
    params["speed"] = speed
    params["lang"] = lang
    params["from"] = from
    params["speaker"] = speaker
    val responseBody = HttpUtils.get(HttpUrl.sogouTranslation, params) ?: return null
    return responseBody.bytes()
}