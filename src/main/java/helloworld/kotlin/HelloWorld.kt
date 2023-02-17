package helloworld.kotlin

import com.qqbot.contact.ContactEventHandOut
import com.qqbot.group.GroupEventHandOut
import com.qqbot.group.GroupHandler
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.BotFactory.INSTANCE.newBot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.utils.BotConfiguration

/**
 * @author Thousand-Dust
 */
fun main() {
    val bot = newBot(
        123456, "password",
        object : BotConfiguration() {
            init {
                fileBasedDeviceInfo()
                //登录协议
                protocol = MiraiProtocol.ANDROID_PAD
            }
        })
    runBlocking {
        bot.login()
    }

    //好友相关事件

    //好友相关事件
    val contactHandOut = ContactEventHandOut(bot)
    contactHandOut.subScribe()

    //群事件
    val groupHandOut = GroupEventHandOut(bot) { myGroup: Group ->
        GroupHandler(myGroup)
    }
    groupHandOut.subScribe()
}