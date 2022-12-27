package com.qqbot;

public class Info {

    public static final long SuperManagerId = 2984613883L;

    //群消息事件缓存最大大小
    public static final int EVENT_CACHE_MAX_SIZE = 1024;

    //群刷屏检测引用的历史消息数量
    public static final int CHECK_EVENT_COUNT = 30;

    //刷屏检测选取的消息时间范围，例如10，则是距最后一条消息10秒内的消息
    public static final int CHECK_EVENT_TIME = 10;

    //刷屏检测判定为刷屏的不重复消息数量
    public static final int CHECK_EVENT_COUNT_MAX = 7;

    //刷屏检测判定为刷屏的重复消息数量
    public static final int CHECK_EVENT_COUNT_MAX1 = 4;

    //判定刷屏后的惩罚时间，单位秒
    public static final int CHECK_EVENT_PUNISH_TIME = 60 * 10;

}
