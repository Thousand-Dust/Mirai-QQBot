package com.qqbot.weather

import com.alibaba.fastjson.JSONObject
import com.qqbot.HttpUrl
import com.qqbot.HttpUtils

/**
 * 天气查询
 */
class QWeather(private val key: String) {

    /**
     * 查询城市
     */
    fun searchCity(city: String): JSONObject? {
        val result = HttpUtils.get(HttpUrl.GeoCitySearch + "?location=$city&key=$key&range=cn") ?: return null
        return JSONObject.parseObject(result.string())
    }

}