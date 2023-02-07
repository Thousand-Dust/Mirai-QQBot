package com.qqbot.api.qweather

import com.alibaba.fastjson.JSONObject
import com.qianyun.ly.ApiCallback1

/**
 * @author Thousand_Dust
 */
class QWeatherContract {

    internal interface Model {
        /**
         * 查询城市
         * @param location 城市名
         * @param adm [location] 所属上级行政区
         * @param number 返回结果数量
         * @param callback 结果回调
         */
        fun searchCity(location: String, adm: String?, number: String, callback: ApiCallback1<JSONObject>)

        /**
         * 查询实时天气
         * @param location 城市id
         * @param callback 结果回调
         */
        fun nowWeather(location: String, callback: ApiCallback1<JSONObject>)

        /**
         * 查询三日天气
         * @param location 城市id
         * @param callback 结果回调
         */
        fun sevenDayWeather(location: String, callback: ApiCallback1<JSONObject>)

        /**
         * 查询今日全部天气指数
         * @param location 城市id
         * @param callback 结果回调
         */
        fun todayWeatherIndex(location: String, callback: ApiCallback1<JSONObject>)

        /**
         * 查询日出日落
         * @param location 城市id
         * @param futureDay 未来第几天
         * @param callback 结果回调
         */
        fun sunRiseSunSet(location: String, futureDay: Int, callback: ApiCallback1<JSONObject>)

        /**
         * 查询月升月落和月相
         * @param location 城市id
         * @param futureDay 未来第几天
         * @param callback 结果回调
         */
        fun moonRiseMoonSet(location: String, futureDay: Int, callback: ApiCallback1<JSONObject>)
    }

    interface View {
        fun onSearchCityFail(e: Exception)
        fun onSearchCitySuccess(response: JSONObject)
        fun onNowWeatherFail(e: Exception)
        fun onNowWeatherSuccess(response: JSONObject)
        fun onSevenDayWeatherFail(e: Exception)
        fun onSevenDayWeatherSuccess(response: JSONObject)
        fun onTodayWeatherIndexFail(e: Exception)
        fun onTodayWeatherIndexSuccess(response: JSONObject)
        fun onSunRiseSunSetFail(e: Exception)
        fun onSunRiseSunSetSuccess(response: JSONObject)
        fun onMoonRiseMoonSetFail(e: Exception)
        fun onMoonRiseMoonSetSuccess(response: JSONObject)
    }

    interface Presenter {
        /**
         * 查询城市
         * @param location 城市名
         * @param adm [location] 所属上级行政区
         * @param number 返回结果数量
         */
        fun searchCity(location: String, adm: String? = null, number: String = "1")

        /**
         * 查询实时天气
         * @param location 城市id
         */
        fun nowWeather(location: String)

        /**
         * 查询七日天气
         * @param location 城市id
         */
        fun sevenDayWeather(location: String)

        /**
         * 查询今日全部天气指数
         */
        fun todayWeatherIndex(location: String)

        /**
         * 查询日出日落
         * @param location 城市id
         * @param futureDay 未来第几天
         */
        fun sunRiseSunSet(location: String, futureDay: Int)

        /**
         * 查询月升月落和月相
         */
        fun moonRiseMoonSet(location: String, futureDay: Int)
    }

}