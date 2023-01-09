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
}

object HttpUrl {
    //摸鱼人日历接口
    const val FishCalendar = "https://j4u.ink/moyuya"
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