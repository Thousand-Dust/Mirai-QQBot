package com.qqbot.api.qweather

import com.alibaba.fastjson.JSONObject
import com.qqbot.HttpUrl
import com.qqbot.HttpUtils
import com.qqbot.TimeMillisecond
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

object QWeatherUrl {
    /**
     * Geo城市搜索 (GET)
     * https://dev.qweather.com/docs/api/geoapi/city-lookup/
     */
    const val GeoCitySearch = "https://geoapi.qweather.com/v2/city/lookup"

    /**
     * 实时天气 (GET)
     * https://dev.qweather.com/docs/api/weather/weather-now/
     */
    const val NowWeather = "https://devapi.qweather.com/v7/weather/now"

    /**
     * 7天天气预报 (GET)
     * https://dev.qweather.com/docs/api/weather/weather-daily-forecast/
     */
    const val ThreeDayWeather = "https://devapi.qweather.com/v7/weather/7d"

    /**
     * 今日天气指数预报
     * https://dev.qweather.com/docs/api/indices/indices-forecast/
     */
    const val TodayWeatherIndex = "https://devapi.qweather.com/v7/indices/1d"

    /**
     * 日出日落 (GET)
     * https://dev.qweather.com/docs/api/astronomy/sunrise-sunset
     */
    const val SunRise = "https://devapi.qweather.com/v7/astronomy/sun"

    /**
     * 月升月落和月相 (GET)
     * https://dev.qweather.com/docs/api/astronomy/moon-and-moon-phase/
     */
    const val MoonRise = "https://devapi.qweather.com/v7/astronomy/moon"

    /**
     * 搜狗翻译文本转语音
     */
    const val sogouTranslation = "https://fanyi.sogou.com/reventondc/synthesis"
}

/**
 * 状态码枚举
 */
enum class QWeatherCode(val code: String) {
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