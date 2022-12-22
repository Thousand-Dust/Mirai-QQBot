package com.qqbot;

import com.qqbot.ai.ChatGPTManager;
import com.qqbot.contact.ContactEventHandOut;
import com.qqbot.group.GroupEventHandOut;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;

/**
 * @author Thousand-Dust
 * 机器人启动入口
 */
public class BotMain {

    public static void main(String[] args) {
        Bot bot = BotFactory.INSTANCE.newBot(551753263, "zwb9446426", new BotConfiguration() {{
            fileBasedDeviceInfo();
            setProtocol(MiraiProtocol.ANDROID_PAD);
        }});
        bot.login();
        /*Bot bot = BotFactory.INSTANCE.newBot(1280715626, "zwb9446426", new BotConfiguration() {{
            fileBasedDeviceInfo();
            setProtocol(MiraiProtocol.ANDROID_PAD);
        }});
        bot.login();*/

        ChatGPTManager chatGPTManager = new ChatGPTManager();
        //好友相关事件
        ContactEventHandOut contactHandOut = new ContactEventHandOut(bot, chatGPTManager);
        contactHandOut.subScribe();

        //群事件
        GroupEventHandOut groupHandOut = new GroupEventHandOut(bot, chatGPTManager);
        groupHandOut.subScribe();

        /*ChatGPTLogic logic = new ChatGPTLogic();
        logic.refreshSession();
        System.out.println(logic.getChatResponse("hello"));*/

    }

}