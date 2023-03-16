package com.qqbot.group.standard.proc.other

import com.qqbot.TimeMillisecond
import com.qqbot.group.other.GroupOtherProc
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
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
    override fun onMemberJoinRequest(event: MemberJoinRequestEvent): Boolean {
//        return runBlocking {
//            event.bot.getStranger(event.fromId)?.let {
//                val userProfile = it.queryProfile()
//                //等级大于等于16级的用户可以直接通过
//                if (userProfile.qLevel >= 16) {
//                    event.accept()
//                    return@runBlocking true
//                }
//            }
//            false
//        }
        return false
    }

    /**
     * 新群员加入后调用
     */
    override fun onMemberJoin(event: MemberJoinEvent): Boolean {
        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR) {
            return false
        }
        runBlocking {
            event.group.sendMessage(
                MessageChainBuilder().append("欢迎新人").append(At(event.member.id)).append(" 加入本群").build()
            )
        }
        return true
    }

    /**
     * 群员退出群后调用
     */
    override fun onMemberLeave(event: MemberLeaveEvent): Boolean {
        val currentTime = System.currentTimeMillis()
        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR || currentTime - lastKickTime < TimeMillisecond.SECOND * 20) {
            return false
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
        return true
    }

    override fun onMemberPermissionChange(event: MemberPermissionChangeEvent): Boolean {
        //TODO("Not yet implemented")
        return false
    }

}