package com.qqbot.group.other.proc

import com.qqbot.TimeMillisecond
import com.qqbot.group.other.GroupOtherProc
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.BotGroupPermissionChangeEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChainBuilder

/**
 * 群其他事件标准处理器类
 * @author Thousand-Dust
 */
class OtherStandard(myGroup: Group) : GroupOtherProc(myGroup) {

    //上次发送踢出群通知的时间
    private var lastKickTime = 0L

    /**
     * 收到入群申请时调用
     */
    override fun onMemberJoinRequest(event: MemberJoinRequestEvent) {
        runBlocking {
            event.bot.getStranger(event.fromId)?.let {
                val userProfile = it.queryProfile()
                //等级大于等于16级的用户可以直接通过
                if (userProfile.qLevel >= 16) {
                    event.accept()
                    return@runBlocking
                }
            }
        }
    }

    /**
     * 新群员加入后调用
     */
    override fun onMemberJoin(event: MemberJoinEvent) {
        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR) {
            return
        }
        runBlocking {
            event.group.sendMessage(
                MessageChainBuilder().append("欢迎新人").append(At(event.member.id)).append(" 加入本群").build()
            )
        }
    }

    /**
     * 群员退出群后调用
     */
    override fun onMemberLeave(event: MemberLeaveEvent) {
        val currentTime = System.currentTimeMillis()
        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR || currentTime - lastKickTime < TimeMillisecond.SECOND * 20) {
            return
        }
        runBlocking {
            lastKickTime = currentTime
            if (event is MemberLeaveEvent.Quit) {
                event.group.sendMessage(At(event.member.id) + " 退出了本群")
                return@runBlocking
            }
            if (event is MemberLeaveEvent.Kick && event.operator != null) {
                event.group.sendMessage(At(event.member.id) + "被" + event.operator!!.nameCardOrNick + " 踢出了群聊")
                return@runBlocking
            }
        }
    }

    override fun onMyPermissionChange(event: BotGroupPermissionChangeEvent) {
        //TODO("Not yet implemented")
    }

}