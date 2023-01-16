package com.qqbot.weather

import com.alibaba.fastjson.JSONObject
import com.qqbot.HttpUrl
import com.qqbot.HttpUtils
import com.qqbot.TimeMillisecond
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

/**
 * 天气查询
 */
class QWeather(private val key: String) {

    /**
     * 状态码枚举
     */
    enum class StatusCode(val code: String) {
        SUCCESS("200") {
            override fun toString() = "请求成功"
        },
        NO_DATA("204") {
            override fun toString() = "请求成功，但你查询的地区暂时没有你需要的数据"
        },
        PARAM_ERROR("400") {
            override fun toString() = "请求错误，可能包含错误的请求参数或缺少必选的请求参数"
        },
        AUTH_ERROR("401") {
            override fun toString() = "认证失败，可能使用了错误的KEY、数字签名错误、KEY的类型错误（如使用SDK的KEY去访问Web API）"
        },
        OVER_LIMIT("402") {
            override fun toString() = "超过访问次数或余额不足以支持继续访问服务，你可以充值、升级访问量或等待访问量重置"
        },
        NO_PERMISSION("403") {
            override fun toString() = "无访问权限，可能是绑定的PackageName、BundleID、域名IP地址不一致，或者是需要额外付费的数据"
        },
        NOT_FOUND("404") {
            override fun toString() = "查询的数据或地区不存在"
        },
        OVER_QPM("429") {
            override fun toString() = "超过限定的QPM（每分钟访问次数），请参考QPM说明"
        },
        NO_RESPONSE("500") {
            override fun toString() = "无响应或超时，接口服务异常请联系我们"
        },
    }

    /**
     * 天气指数枚举
     */
    enum class IndicesType(val type: String) {
        运动("1"),
        洗车("2"),
        穿衣("3"),
        钓鱼("4"),
        紫外线("5"),
        旅游("6"),
        花粉("7"),
        舒适度("8"),
        感冒("9"),
        化妆("13"),
        晾晒("14"),
        交通("15"),
        防晒("16"),
    }

    /**
     * 获取状态码枚举
     */
    fun getStatusCode(code: String): StatusCode? {
        return StatusCode.values().find { it.code == code }
    }

    /**
     * 查询城市
     * @param location 城市名
     * @param adm [location] 所属上级行政区
     * @param number 返回结果数量
     */
    fun searchCity(location: String, adm: String? = null, number: String = "1"): JSONObject? {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        adm?.let {
            params["adm"] = adm
        }
        params["number"] = number
        val result = HttpUtils.get(HttpUrl.GeoCitySearch, params) ?: return null
        return JSONObject.parseObject(result.string())
    }

    /**
     * 查询实时天气
     * @param location 城市id
     */
    fun nowWeather(location: String): JSONObject? {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        val result = HttpUtils.get(HttpUrl.NowWeather, params) ?: return null
        return JSONObject.parseObject(result.string())
    }

    /**
     * 查询三日天气
     * @param location 城市id
     */
    fun threeDayWeather(location: String): JSONObject? {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        val result = HttpUtils.get(HttpUrl.ThreeDayWeather, params) ?: return null
        return JSONObject.parseObject(result.string())
    }

    /**
     * 查询今日全部天气指数
     */
    fun todayWeatherIndex(location: String): JSONObject? {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        params["type"] = "0"
        val result = HttpUtils.get(HttpUrl.TodayWeatherIndex, params) ?: return null
        return JSONObject.parseObject(result.string())
    }

    /**
     * 查询日出日落
     * @param location 城市id
     * @param futureDay 未来第几天
     */
    fun sunRiseSunSet(location: String, futureDay: Int): JSONObject? {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        params["date"] = getFutureDate(futureDay)
        val result = HttpUtils.get(HttpUrl.SunRise, params) ?: return null
        return JSONObject.parseObject(result.string())
    }

    /**
     * 查询月升月落和月相
     */
    fun moonRiseMoonSet(location: String, futureDay: Int): JSONObject? {
        //将参数转为get请求参数
        val params = HashMap<String, String>()
        params["key"] = key
        params["location"] = location
        params["date"] = getFutureDate(futureDay)
        val result = HttpUtils.get(HttpUrl.MoonRise, params) ?: return null
        return JSONObject.parseObject(result.string())
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