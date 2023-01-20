package com.qqbot

/**
 * 基本信息
 */
object Info {
    const val RootManagerId = 2984613883L

    //群消息事件缓存最大大小
    const val EVENT_CACHE_MAX_SIZE = 512

    //群刷屏检测引用的历史消息数量
    const val CHECK_EVENT_COUNT = 30

    //刷屏检测选取的消息时间范围，例如10，则是距最后一条消息10秒内的消息
    const val CHECK_EVENT_TIME = 8

    //刷屏检测判定为刷屏的不重复消息数量
    const val CHECK_EVENT_COUNT_MAX = 6

    //刷屏检测判定为刷屏的重复消息数量
    const val CHECK_EVENT_COUNT_MAX1 = 3

    //和风天气key
    const val QWEATHER_KEY = "90c730ae25934fb38951fd7b91e2e07e"
}

object HttpUrl {
    //摸鱼人日历接口
    const val FishCalendar = "https://j4u.ink/moyuya"

    //--------- 天气接口 ---------
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
     * 3天天气预报 (GET)
     * https://dev.qweather.com/docs/api/weather/weather-daily-forecast/
     */
    const val ThreeDayWeather = "https://devapi.qweather.com/v7/weather/3d"

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
 * 时间毫秒常量
 */
object TimeMillisecond {
    const val SECOND = 1000L
    const val MINUTE = 60 * SECOND
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR
    const val WEEK = 7 * DAY
    const val MONTH = 30 * DAY
    const val YEAR = 365 * DAY
}

/**
 * 时间秒常量
 */
object TimeSecond {
    const val SECOND = 1
    const val MINUTE = 60
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR
    const val WEEK = 7 * DAY
    const val MONTH = 30 * DAY
    const val YEAR = 365 * DAY
}