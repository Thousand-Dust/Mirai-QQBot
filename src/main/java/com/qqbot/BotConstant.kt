package com.qqbot

/**
 * 基本信息
 */
object Info {

    const val RootManagerId = 2984613883L

    //单个群消息事件缓存最大大小
    const val EVENT_CACHE_MAX_SIZE = 512

    //单个群bot发送的消息事件缓存最大大小
    const val BOT_SEND_EVENT_CACHE_MAX_SIZE = 64

    //群刷屏检测引用的历史消息数量
    const val CHECK_EVENT_COUNT = 30

    //刷屏检测选取的消息时间范围，例如10，则是距最后一条消息10秒内的消息
    const val CHECK_EVENT_TIME = 10

    //刷屏检测判定为刷屏的不重复消息数量
    const val CHECK_EVENT_COUNT_MAX = 7

    //刷屏检测判定为刷屏的重复消息数量
    const val CHECK_EVENT_COUNT_MAX1 = 3

    //和风天气key
    const val QWEATHER_KEY = "90c730ae25934fb38951fd7b91e2e07e"

    //数据储存根目录
    const val DATA_ROOT_PATH = "data"

    //AI数据储存目录
    const val AI_DATA_PATH = "$DATA_ROOT_PATH/ai"

    //临时文件目录
    const val TEMP_PATH = "$DATA_ROOT_PATH/temp"

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
     * 7天天气预报 (GET)
     * https://dev.qweather.com/docs/api/weather/weather-daily-forecast/
     */
    const val SevenDayWeather = "https://devapi.qweather.com/v7/weather/7d"

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

/**
 * 将时间格式化为 天:时:分:秒:毫秒
 * @param time 时间（毫秒）
 * @param isShowDay 是否显示天
 * @param isShowHour 是否显示小时
 * @param isShowMinute 是否显示分钟
 * @param isShowSecond 是否显示秒
 * @param isShowMillisecond 是否显示毫秒
 * @return 格式化后的时间
 */
fun timeFormat(
    time: Long,
    isShowDay: Boolean = true,
    isShowHour: Boolean = true,
    isShowMinute: Boolean = true,
    isShowSecond: Boolean = true,
    isShowMillisecond: Boolean = true
): String {
    //各单位剩余的时间
    val day = time / TimeMillisecond.DAY
    val hour = time % TimeMillisecond.DAY / TimeMillisecond.HOUR
    val minute = time % TimeMillisecond.HOUR / TimeMillisecond.MINUTE
    val second = time % TimeMillisecond.MINUTE / TimeMillisecond.SECOND
    val millisecond = time % TimeMillisecond.SECOND

    //是否显示各单位，如果自身不等于0，或者比自身大的单位显示，并且比自身小的单位也显示，则自身也显示
    val isShowDay1 = isShowDay && day != 0L
    val isShowHour1 = isShowHour && hour != 0L || run {
        isShowDay1 && run {
            isShowMinute && minute != 0L || run {
                isShowSecond && second != 0L || run {
                    isShowMillisecond && millisecond != 0L
                }
            }
        }
    }
    val isShowMinute1 = isShowMinute && minute != 0L || run {
        isShowHour1 && run {
            isShowSecond && second != 0L || run {
                isShowMillisecond && millisecond != 0L
            }
        }
    }
    val isShowSecond1 = isShowSecond && second != 0L || run {
        isShowMinute1 && run {
            isShowMillisecond && millisecond != 0L
        }
    }
    val isShowMillisecond1 = isShowMillisecond && millisecond != 0L
    val result = buildString {
        if (isShowDay1) {
            append(day).append("天")
        }
        if (isShowHour1) {
            append(hour).append("小时")
        }
        if (isShowMinute1) {
            append(minute).append("分钟")
        }
        if (isShowSecond1) {
            append(second).append("秒")
        }
        if (isShowMillisecond1) {
            append(millisecond).append("毫秒")
        }
    }
    return result
}