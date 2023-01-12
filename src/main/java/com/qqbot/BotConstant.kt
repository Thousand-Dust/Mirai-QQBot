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

    //--------- 天气接口 ---------
    /**
     * Geo城市搜索
     * @param location(必选)需要查询地区的名称，支持文字、以英文逗号分隔的经度,纬度坐标（十进制，最多支持小数点后两位）
     * LocationID或Adcode（仅限中国城市）。例如 location=北京 或 location=116.41,39.92
     * @param key(必选)用户认证key，请参考如何获取你的KEY。支持数字签名方式进行认证。例如 key=123456789ABC
     * @param adm 城市的上级行政区划，可设定只在某个行政区划范围内进行搜索，用于排除重名城市或对结果进行过滤。例如 adm=beijing
     * @param range 搜索范围，可设定只在某个国家或地区范围内进行搜索，国家和地区名称需使用ISO 3166 所定义的国家代码。如果不设置此参数，搜索范围将在所有城市。例如 range=cn
     * @param number 返回结果的数量，取值范围1-20，默认返回10个结果。
     * @param lang 多语言设置，更多语言可选值参考语言代码。当数据不匹配你设置的语言时，将返回英文或其本地语言结果。
     * @return 返回数据是JSON格式并进行了Gzip压缩，数据类型均为字符串。
     */
    const val GeoCitySearch = "https://geoapi.qweather.com/v2/city/lookup"
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