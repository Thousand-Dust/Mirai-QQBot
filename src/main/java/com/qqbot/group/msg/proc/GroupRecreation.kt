package com.qqbot.group.msg.proc

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.qqbot.HttpUrl
import com.qqbot.HttpUtils
import com.qqbot.Info
import com.qqbot.Utils
import com.qqbot.api.QWeather
import com.qqbot.api.QWeatherCode
import com.qqbot.api.sogouTextToAudio
import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.GroupHandler
import com.qqbot.group.msg.GroupMsgProc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.isBotMuted
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class GroupRecreation(groupHandler: GroupHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    companion object {
        //摸鱼人日历图片储存路径
        private const val CalendarImagePath = Info.DATA_ROOT_PATH+"/calendar.png"
        private const val CalendarDataPath = Info.DATA_ROOT_PATH+"/calendar.txt"

        //语音文件缓存路径
        private const val AudioCachePath = Info.TEMP_PATH+"/audioCache/"

        //摸鱼人日历资源文件
        private var resource: ExternalResource? = null

        //摸鱼人日历图片id
        private var calendarId: String? = null

        //和风天气封装类
        private val weather = QWeather(Info.QWEATHER_KEY)

        /**
         * 获取摸鱼人日历图片消息
         */
        private suspend fun getCalendarMsg(url: String, pubTime: Long, bot: Bot, group: Group): Image {
            val json: JSONObject =
                if (File(CalendarDataPath).exists()) {
                    JSON.parseObject(String(Utils.readFile(CalendarDataPath)))
                } else {
                    JSONObject()
                }
            val calendarUrl: String? = json.getString("url")
            val calendarPubTime = json.getLong("pub_time") ?: 0
            calendarId = json.getString("id")

            val calendarImageFile = File(CalendarImagePath)
            if (calendarImageFile.exists()) {
                resource = calendarImageFile.toExternalResource()
            }
            if (url != calendarUrl || pubTime != calendarPubTime || calendarId == null || !Image.isUploaded(
                    bot,
                    resource!!.md5,
                    resource!!.size
                )
            ) {
                //下载图片
                Utils.writeFile(CalendarImagePath, HttpUtils.get(url)!!.bytes(), false)
                //上传图片
                resource?.close()
                resource = File(CalendarImagePath).toExternalResource()
                val image = group.uploadImage(resource!!)
                //更新图片id
                calendarId = image.imageId
                json["url"] = url
                json["id"] = calendarId
                json["pub_time"] = pubTime
                Utils.writeFile(CalendarDataPath, json.toJSONString().toByteArray(), false)
                return image
            }
            return Image(calendarId!!)
        }
    }

    private enum class Command {
        摸鱼人日历,
        说,
        实时天气,
        未来天气,
        天气指数,
        日出日落,
        月升月落,
    }

    /**
     * 城市id和名字
     */
    data class City(val id: String, val name: String) {
        override fun toString() = "$name($id)"
    }

    init {
        //创建temp文件夹
        val tempFile = File("temp")
        if (!tempFile.exists()) {
            tempFile.mkdir()
        }
        //创建语音文件缓存文件夹
        val audioCacheFile = File(AudioCachePath)
        if (!audioCacheFile.exists()) {
            audioCacheFile.mkdir()
        }
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        if (myGroup.isBotMuted) {
            return false
        }
        return command(event)
    }

    override fun getName(): String {
        return "其他功能"
    }

    override fun getDesc(): String {
        return "其他功能(公开可用)"
    }

    override fun getMenu(event: GroupMessageEvent): String {
        return "娱乐系统：\n" +
                "${Command.摸鱼人日历}\n" +
                "文字转语音：${Command.说}+文字\n" +
                "\n天气系统：\n" +
                "----------\n以下的(位置)可为：(省/市/区)级行政区，支持查询大多数国家。可模糊搜索指定上级行政区，用(空格)隔开（北京 朝阳）。注意：以下示例中的空格也需要输入\n----------\n" +
                "查询实时天气：${Command.实时天气} 位置\n" +
                "查询未来天气：${Command.未来天气} 第几天(1~7) 位置\n" +
                "查询天气指数：${Command.天气指数} 位置\n" +
                "查询日出日落：${Command.日出日落} 第几天 位置\n" +
                "查询月升月落：${Command.月升月落} 第几天 位置\n" +
                "天气服务由和风天气驱动"
    }

    private suspend fun command(event: GroupMessageEvent): Boolean {
        val message = event.message
        //命令消息（消息头）
        val commandMessage = message[1]

        if (message.size == 2) {
            when (commandMessage.toString()) {
                Command.摸鱼人日历.name -> {
                    fishCalendar(event.group)
                    return true
                }
                else -> {
                    val msgStr = commandMessage.toString().replace("\\s+".toRegex(), " ")
                    val splitMsg = msgStr.split(Pattern.compile(" "), 2)

                    if (msgStr.startsWith(Command.说.name)) {
                        say(msgStr.substring(Command.说.name.length), event.group)
                        return true
                    }
                    if (splitMsg.size == 2) {
                        when (splitMsg[0]) {
                            Command.实时天气.name -> realTimeWeather(splitMsg[1], event.group)
                            Command.未来天气.name -> threeDayWeather(splitMsg[1], event.group)
                            Command.天气指数.name -> weatherIndex(splitMsg[1], event.group)
                            Command.日出日落.name -> sunRiseAndSunSet(splitMsg[1], event.group)
                            Command.月升月落.name -> moonRiseAndMoonSet(splitMsg[1], event.group)
                            else -> return false
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 摸鱼人日历
     */
    private suspend fun fishCalendar(group: Group) = withContext(Dispatchers.IO) {
        //发送http请求摸鱼人日历
        try {
            val response = HttpUtils.get(HttpUrl.FishCalendar)!!.string()
            val json = JSON.parseObject(response)
            val code = json.getIntValue("code")
            if (code != 200) {
                val message = json.getString("message")
                group.sendMessage("请求失败：$message")
                return@withContext
            }
            val data = json.getJSONObject("data")
            val url = data.getString("moyu_url")
            val pubTime = data.getJSONArray("articles").getJSONObject(0).getLong("pub_time") ?: 0
            group.sendMessage(getCalendarMsg(url, pubTime, group.botAsMember.bot, group))
        } catch (e: IOException) {
            group.sendMessage("请求失败: $e")
        }
    }

    /**
     * 文字转语音
     */
    private suspend fun say(text: String, group: Group) = withContext(Dispatchers.IO) {
        val isIllegal = text.replace("[^\\u4e00-\\u9fa5a-zA-Z0-9]".toRegex(), "").let { it.isEmpty() || it.length > 128 }
        if (isIllegal) {
            group.sendMessage("请不要输入空白或过长内容")
            return@withContext
        }
        val file = File(AudioCachePath + text.hashCode() + ".mp3")
        val response = sogouTextToAudio(text)
        if (response == null) {
            group.sendMessage("请求失败")
            return@withContext
        }
        Utils.writeFile(file.path, response, false)
        //上传语音
        val resource = file.toExternalResource()
        val audio = resource.use { resource ->
            group.uploadAudio(resource)
        }
        file.delete()
        group.sendMessage(audio)
    }

    /**
     * 搜索城市id
     */
    private suspend fun getCity(location: String, group: Group) = withContext(Dispatchers.IO) {
        //将location分隔
        val splitLocation = location.split(Pattern.compile(" "))
        if (splitLocation.size > 2) {
            group.sendMessage("格式错误，最多可指定一个上级行政区。例如：广东 广州")
            return@withContext null
        }
        //搜索城市
        val cityJson = if (splitLocation.size == 2)
            weather.searchCity(splitLocation[1], splitLocation[0])
        else
            weather.searchCity(location)
        if (cityJson == null) {
            group.sendMessage("未找到该城市")
            return@withContext null
        }
        val statusCode = weather.getStatusCode(cityJson.getString("code"))!!
        if (statusCode != QWeatherCode.SUCCESS) {
            group.sendMessage(statusCode.toString())
            return@withContext null
        }
        val location1 = cityJson.getJSONArray("location").getJSONObject(0)
        val country = location1.getString("country")
        val adm1 = location1.getString("adm1")
        val adm2 = location1.getString("adm2")
        val name = location1.getString("name")
        val id = location1.getString("id")

        val absoluteName = StringBuilder().apply {
            if (country != "中国") {
                append(country)
            }
            append(adm1)
            if (adm2 != adm1 && adm2 != "香港" && adm2 != "澳门") {
                append(adm2)
            }
            if (name != adm2 && name != adm1) {
                append(name)
            }
        }.toString()
        return@withContext City(id, absoluteName)
    }

    /**
     * 查询实时天气
     * @param location 位置。可模糊搜索：北京 朝阳
     */
    private suspend fun realTimeWeather(location: String, group: Group) = withContext(Dispatchers.IO) {
        val city = getCity(location, group) ?: return@withContext
        val weatherJson = weather.nowWeather(city.id)
        if (weatherJson == null) {
            group.sendMessage("查询失败")
            return@withContext
        }
        val statusCode = weather.getStatusCode(weatherJson.getString("code"))!!
        if (statusCode != QWeatherCode.SUCCESS) {
            group.sendMessage(statusCode.toString())
            return@withContext
        }
        val now = weatherJson.getJSONObject("now")

        val str = StringBuilder().apply {
            append("${city.name}实时天气\n")
            append("天气：${now.getString("text")}\n")
            append("温度：${now.getString("temp")}°\n")
            append("风力：${now.getString("windDir")} ${now.getString("windScale")}级\n")
            append("湿度：${now.getString("humidity")}%\n")
            append("能见度：${now.getString("vis")}公里")
            val precip = now.getString("precip")
            if (precip != "0.0") {
                append("\n一小时降水量：$precip 毫米")
            }
        }.toString()
        group.sendMessage(str)
    }

    /**
     * 查询未来7日天气
     */
    private suspend fun threeDayWeather(message: String, group: Group) = withContext(Dispatchers.IO) {
        val splitMsg = message.split(Pattern.compile(" "), 2)
        if (splitMsg.size != 2) {
            group.sendMessage("格式错误，正确格式：${Command.未来天气} (1~7) 城市")
            return@withContext
        }
        val dayNum = try {
            splitMsg[0].toInt()
        } catch (e: Exception) {
            group.sendMessage("时间格式错误，请输入1~7的数字")
            return@withContext
        }
        if (dayNum !in 1..7) {
            group.sendMessage("只能查询1~7天内的天气")
            return@withContext
        }
        val city = getCity(splitMsg[1], group) ?: return@withContext
        val weatherJson = weather.threeDayWeather(city.id)
        if (weatherJson == null) {
            group.sendMessage("查询失败")
            return@withContext
        }
        val statusCode = weather.getStatusCode(weatherJson.getString("code"))!!
        if (statusCode != QWeatherCode.SUCCESS) {
            group.sendMessage(statusCode.toString())
            return@withContext
        }
        val daily = weatherJson.getJSONArray("daily").getJSONObject(dayNum - 1)
        val str = StringBuilder().apply {
            append("${city.name}未来7日天气\n")
            append("${daily.getString("fxDate")}\n")
            append("温度：${daily.getString("tempMin")}° ~ ${daily.getString("tempMax")}°\n")
            append("白天天气：${daily.getString("textDay")}\n")
            append("白天风力：${daily.getString("windDirDay")} ${daily.getString("windScaleDay")}级\n")
            append("夜间天气：${daily.getString("textNight")}\n")
            append("夜间风力：${daily.getString("windDirNight")} ${daily.getString("windScaleNight")}级\n")
            append("湿度：${daily.getString("humidity")}%\n")
            append("能见度：${daily.getString("vis")}公里")
            val precip = daily.getString("precip")
            if (precip != "0.0") {
                append("\n一小时降水量：$precip 毫米")
            }
            append("\n")
        }.toString()
        group.sendMessage(str)
    }

    /**
     * 查询今日天气指数
     */
    private suspend fun weatherIndex(location: String, group: Group) = withContext(Dispatchers.IO) {
        val city = getCity(location, group) ?: return@withContext
        val weatherJson = weather.todayWeatherIndex(city.id)
        if (weatherJson == null) {
            group.sendMessage("查询失败")
            return@withContext
        }
        val statusCode = weather.getStatusCode(weatherJson.getString("code"))!!
        if (statusCode != QWeatherCode.SUCCESS) {
            group.sendMessage(statusCode.toString())
            return@withContext
        }
        val daily = weatherJson.getJSONArray("daily")
        val str = StringBuilder().apply {
            append("${city.name}今日天气指数\n")
            for (i in 0 until daily.size) {
                val index = daily.getJSONObject(i)
                val type = index.getString("type")
                if (QWeather.IndicesType.values().find { it.type == type } == null) {
                    continue
                }
                append("${index.getString("name")}: ${index.getString("category")}\n")
            }
            //删除做后一个换行符
            deleteCharAt(lastIndex)
        }.toString()
        group.sendMessage(str)
    }

    /**
     * 查询日出日落
     */
    private suspend fun sunRiseAndSunSet(message: String, group: Group) = withContext(Dispatchers.IO) {
        val splitMsg = message.split(Pattern.compile(" "), 2)
        if (splitMsg.size != 2) {
            group.sendMessage("格式错误，正确格式：${Command.日出日落} 第几天 城市")
            return@withContext
        }
        val dayNum: Int
        try {
            dayNum = splitMsg[0].toInt()
            if (dayNum > 60) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            group.sendMessage("时间格式错误，请输入数字。并且不大于60")
            return@withContext
        }
        val city = getCity(splitMsg[1], group) ?: return@withContext
        val weatherJson = weather.sunRiseSunSet(city.id, dayNum)
        if (weatherJson == null) {
            group.sendMessage("查询失败")
            return@withContext
        }
        val statusCode = weather.getStatusCode(weatherJson.getString("code"))!!
        if (statusCode != QWeatherCode.SUCCESS) {
            group.sendMessage(statusCode.toString())
            return@withContext
        }
        val str = StringBuilder().apply {
            append("${city.name}日出日落\n")
            append("日出：${weatherJson.getString("sunrise") ?: "无"}\n")
            append("日落：${weatherJson.getString("sunset") ?: "无"}")
        }.toString()
        group.sendMessage(str)
    }

    /**
     * 查询月亮
     */
    private suspend fun moonRiseAndMoonSet(message: String, group: Group) = withContext(Dispatchers.IO) {
        val splitMsg = message.split(Pattern.compile(" "), 2)
        if (splitMsg.size != 2) {
            group.sendMessage("格式错误，正确格式：${Command.月升月落} 第几天 城市")
            return@withContext
        }
        val dayNum: Int
        try {
            dayNum = splitMsg[0].toInt()
            if (dayNum > 60) throw NumberFormatException()
        } catch (e: NumberFormatException) {
            group.sendMessage("时间格式错误，请输入数字。并且不大于60")
            return@withContext
        }
        val city = getCity(splitMsg[1], group) ?: return@withContext
        val weatherJson = weather.moonRiseMoonSet(city.id, dayNum)
        if (weatherJson == null) {
            group.sendMessage("查询失败")
            return@withContext
        }
        val statusCode = weather.getStatusCode(weatherJson.getString("code"))!!
        if (statusCode != QWeatherCode.SUCCESS) {
            group.sendMessage(statusCode.toString())
            return@withContext
        }
        val str = StringBuilder().apply {
            append("${city.name}月升月落\n")
            append("月升：${weatherJson.getString("moonrise") ?: "无"}\n")
            append("月落：${weatherJson.getString("moonset") ?: "无"}")
        }.toString()
        group.sendMessage(str)
    }

}