package helloworld.java;

import com.qqbot.contact.ContactEventHandOut;
import com.qqbot.group.GroupEventHandOut;
import com.qqbot.group.GroupHandler;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;

/**
 * @author Thousand-Dust
 */
public class HelloWorld {

    public static void main(String[] args) {
        Bot bot = BotFactory.INSTANCE.newBot(
                123456, "password",
                new BotConfiguration() {{
                    fileBasedDeviceInfo();
                    //登录协议
                    setProtocol(MiraiProtocol.ANDROID_PAD);
                }});
        bot.login();

        //好友相关事件
        ContactEventHandOut contactHandOut = new ContactEventHandOut(bot);
        contactHandOut.subScribe();

        //群事件
        GroupEventHandOut groupHandOut = new GroupEventHandOut(bot, GroupHandler::new);
        groupHandOut.subScribe();

    }

}
