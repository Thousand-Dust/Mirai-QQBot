package com.qqbot.group.msg.proc

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.qqbot.HttpUrl
import com.qqbot.HttpUtils
import com.qqbot.Utils
import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.GroupHandler
import com.qqbot.group.msg.GroupMsgProc
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.sourceOrNull
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import java.io.IOException

class GroupRecreation(groupHandler: GroupHandler, database: GroupDatabase) : GroupMsgProc(groupHandler, database) {

    companion object {
        //摸鱼人日历图片储存路径
        private const val CalendarImagePath = "data/calendar.png"
        private const val CalendarDataPath = "data/calendar.txt"

        //摸鱼人日历资源文件
        private var resource: ExternalResource? = null
        //摸鱼人日历图片id
        private var calendarId: String? = null

        /**
         * 获取摸鱼人日历图片消息
         */
        private suspend fun getCalendarMsg(url: String, bot: Bot, group: Group): Image {
            val json: JSONObject =
                if (File(CalendarDataPath).exists()) {
                    JSON.parseObject(String(Utils.readFile(CalendarDataPath)))
                } else {
                    JSONObject()
                }
            val calendarUrl: String? = json.getString("url")
            calendarId = json.getString("id")

            val calendarImageFile = File(CalendarImagePath)
            if (calendarImageFile.exists()) {
                resource = calendarImageFile.toExternalResource()
            }
            if (url != calendarUrl || calendarId == null || !Image.isUploaded(bot, resource!!.md5, resource!!.size)) {
                //下载图片
                HttpUtils.download(url, CalendarImagePath)
                //上传图片
                resource?.close()
                resource = File(CalendarImagePath).toExternalResource()
                val image = group.uploadImage(resource!!)
                //更新图片id
                calendarId = image.imageId
                json["url"] = url
                json["id"] = calendarId
                Utils.writeFile(CalendarDataPath, json.toJSONString().toByteArray(), false)
                return image
            }
            return Image(calendarId!!)
        }
    }

    private enum class Command {
        摸鱼人日历,
    }

    override suspend fun process(event: GroupMessageEvent): Boolean {
        return command(event)
    }

    override fun getName(): String {
        return "娱乐系统"
    }

    override fun getDesc(): String {
        return "娱乐系统(公开可用)"
    }

    override fun getMenu(event: GroupMessageEvent): String {
        return "娱乐系统：\n" +
                Command.摸鱼人日历
    }

    private suspend fun command(event: GroupMessageEvent): Boolean {
        val message = event.message
        message.sourceOrNull
        //命令消息（消息头）
        val commandMessage = message[1]

        when (commandMessage.toString()) {
            Command.摸鱼人日历.name -> {
                fishCalendar(event.group)
                return true
            }
        }
        return false
    }

    /**
     * 摸鱼人日历
     */
    private suspend fun fishCalendar(group: Group) {

        //发送http请求摸鱼人日历
        try {
            val response = HttpUtils.get(HttpUrl.FishCalendar)
            val json = JSON.parseObject(response)
            val code = json.getIntValue("code")
            if (code != 200) {
                val message = json.getString("message")
                group.sendMessage("请求失败：$message")
                return
            }
            val data = json.getJSONObject("data")
            val url = data.getString("moyu_url")
            group.sendMessage(getCalendarMsg(url, my.bot, group))
        } catch (e: IOException) {
            group.sendMessage("请求失败: $e")
        }
    }

}