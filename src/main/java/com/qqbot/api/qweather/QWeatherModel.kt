package com.qqbot.api.qweather

import com.alibaba.fastjson.JSONObject
import com.qianyun.ly.ApiCallback1
import com.qqbot.HttpUrl
import com.qqbot.HttpUtils
import com.qqbot.TimeMillisecond
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.lang.NullPointerException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

/**
 * @author Thousand_Dust
 */
class QWeatherModel(private val key: String) : QWeatherContract.Model {

    private fun getCallback(callback: ApiCallback1<JSONObject>): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFail(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body
                if (body == null) {
                    callback.onFail(NullPointerException("response body is null"))
                    return
                }
                callback.onSuccess(JSONObject.parseObject(body.string()))
            }
        }
    }

    /**
     * 查询城市
     * @param location 城市名
     * @param adm [location] 所属上级行政区
     * @param number 返回结果数量
     * @param callback 结果回调
     */
    override fun searchCity(location: String, adm: String?, number: String, callback: ApiCallback1<JSONObject>) {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        adm?.let {
            params["adm"] = adm
        }
        params["number"] = number
        HttpUtils.get(HttpUrl.GeoCitySearch, params, getCallback(callback))
    }

    /**
     * 查询实时天气
     * @param location 城市id
     * @param callback 结果回调
     */
    override fun nowWeather(location: String, callback: ApiCallback1<JSONObject>) {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        HttpUtils.get(HttpUrl.NowWeather, params, getCallback(callback))
    }

    /**
     * 查询七日天气
     * @param location 城市id
     * @param callback 结果回调
     */
    override fun sevenDayWeather(location: String, callback: ApiCallback1<JSONObject>) {//将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        HttpUtils.get(HttpUrl.SevenDayWeather, params, getCallback(callback))
    }

    /**
     * 查询今日全部天气指数
     * @param location 城市id
     * @param callback 结果回调
     */
    override fun todayWeatherIndex(location: String, callback: ApiCallback1<JSONObject>) {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        params["type"] = "0"
        HttpUtils.get(HttpUrl.TodayWeatherIndex, params, getCallback(callback))
    }

    /**
     * 查询日出日落
     * @param location 城市id
     * @param futureDay 未来第几天
     * @param callback 结果回调
     */
    override fun sunRiseSunSet(location: String, futureDay: Int, callback: ApiCallback1<JSONObject>) {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        params["date"] = getFutureDate(futureDay)
        HttpUtils.get(HttpUrl.SunRise, params, getCallback(callback))
    }

    /**
     * 查询月升月落和月相
     * @param location 城市id
     * @param futureDay 未来第几天
     * @param callback 结果回调
     */
    override fun moonRiseMoonSet(location: String, futureDay: Int, callback: ApiCallback1<JSONObject>) {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        params["date"] = getFutureDate(futureDay)
        HttpUtils.get(HttpUrl.MoonRise, params, getCallback(callback))
    }

    /**
     * 获取未来几天的时间，格式为yyyyMMdd
     */
    private fun getFutureDate(futureDay: Int): String {
        //将futureDay转为yyyyMMdd格式
        val dateFormat = SimpleDateFormat("yyyyMMdd")
        return dateFormat.format(Date(System.currentTimeMillis() + futureDay * TimeMillisecond.DAY))
    }

}