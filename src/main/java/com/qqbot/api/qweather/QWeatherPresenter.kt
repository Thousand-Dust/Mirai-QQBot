package com.qqbot.api.qweather

import com.alibaba.fastjson.JSONObject
import com.qianyun.ly.ApiCallback1

class QWeatherPresenter(
    private val view: QWeatherContract.View,
    key: String
) : QWeatherContract.Presenter {

    private val model = QWeatherModel(key)

    /**
     * 查询城市
     * @param location 城市名
     * @param adm [location] 所属上级行政区
     * @param number 返回结果数量
     */
    override fun searchCity(location: String, adm: String?, number: String) {
        model.searchCity(location, adm, number, object : ApiCallback1<JSONObject> {
            override fun onFail(e: Exception) {
                view.onSearchCityFail(e)
            }

            override fun onSuccess(response: JSONObject) {
                view.onSearchCitySuccess(response)
            }
        })
    }

    /**
     * 查询实时天气
     * @param location 城市id
     */
    override fun nowWeather(location: String) {
        model.nowWeather(location, object : ApiCallback1<JSONObject> {
            override fun onFail(e: Exception) {
                view.onNowWeatherFail(e)
            }

            override fun onSuccess(response: JSONObject) {
                view.onNowWeatherSuccess(response)
            }
        })
    }

    /**
     * 查询七日天气
     * @param location 城市id
     */
    override fun sevenDayWeather(location: String) {
        model.sevenDayWeather(location, object : ApiCallback1<JSONObject> {
            override fun onFail(e: Exception) {
                view.onSevenDayWeatherFail(e)
            }

            override fun onSuccess(response: JSONObject) {
                view.onSevenDayWeatherSuccess(response)
            }
        })
    }

    /**
     * 查询今日全部天气指数
     */
    override fun todayWeatherIndex(location: String) {
        model.todayWeatherIndex(location, object : ApiCallback1<JSONObject> {
            override fun onFail(e: Exception) {
                view.onTodayWeatherIndexFail(e)
            }

            override fun onSuccess(response: JSONObject) {
                view.onTodayWeatherIndexSuccess(response)
            }
        })
    }

    /**
     * 查询日出日落
     * @param location 城市id
     * @param futureDay 未来第几天
     */
    override fun sunRiseSunSet(location: String, futureDay: Int) {
        model.sunRiseSunSet(location, futureDay, object : ApiCallback1<JSONObject> {
            override fun onFail(e: Exception) {
                view.onSunRiseSunSetFail(e)
            }

            override fun onSuccess(response: JSONObject) {
                view.onSunRiseSunSetSuccess(response)
            }
        })
    }

    /**
     * 查询月升月落和月相
     */
    override fun moonRiseMoonSet(location: String, futureDay: Int) {
        model.moonRiseMoonSet(location, futureDay, object : ApiCallback1<JSONObject> {
            override fun onFail(e: Exception) {
                view.onMoonRiseMoonSetFail(e)
            }

            override fun onSuccess(response: JSONObject) {
                view.onMoonRiseMoonSetSuccess(response)
            }
        })
    }
}