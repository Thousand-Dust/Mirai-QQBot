package com.qqbot.group

import com.qqbot.TimeMillisecond
import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.msg.GroupMsgProc
import com.qqbot.group.msg.proc.*
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.BotGroupPermissionChangeEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.message.data.*

/**
 * 单个群的消息总处理类
 * @author Thousand-Dust
 */
class GroupHandler(myGroup: Group, my: Member) : GroupEventHandler(myGroup, my) {

    //数据库
    private val database = GroupDatabase(myGroup.id)

    //协程作用域
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    //消息处理器列表
    private val msgProcList = ArrayList<GroupMsgProc>()

    //上次发送踢出群通知的时间
    private var lastKickTime = 0L

    override fun onCreate(): Boolean {
        //机器人在群里没有管理员权限，只监听群消息检测系统
        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR) {
            msgProcList.add(GroupCheck(this, database))
            return true
        }

        //按顺序添加消息处理器
        msgProcList.add(GroupMaster(this, database))
        msgProcList.add(GroupOwner(this, database))
        msgProcList.add(GroupManager(this, database))
        msgProcList.add(GroupCheck(this, database))
        msgProcList.add(GroupScore(this, database))
        msgProcList.add(GroupRecreation(this, database))
        return true
    }

    override fun onRemove() {
        //逐一调用消息处理器的onRemove方法
        msgProcList.forEach { it.onRemove() }
        database.close()
    }

    override fun acceptMessage(event: GroupMessageEvent) {
        addCache(event)
        if (event.message.size < 2) {
            return
        }

        coroutineScope.launch {
            try {
                val message = event.message

                //显示菜单
                if (message.size == 2 && message[1] is PlainText && !event.group.isBotMuted) {
                    //发送的菜单消息内容
                    var menuStr: String? = null

                    val msgStr = message[1].toString()
                    if (msgStr == "菜单") {
                        val sb = StringBuilder()
                            .append("菜单：")
                        for (msgProc in msgProcList) {
                            val desc = msgProc.getDesc()
                            if (desc != null) {
                                sb.append("\n").append(desc)
                            }
                        }
                        menuStr = sb.toString()

                        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR) {
                            //没有管理员权限，不显示菜单
                            menuStr = null
                        }
                    } else {
                        //匹配群消息处理器菜单
                        for (msgProc in msgProcList) {
                            //触发菜单的指令
                            val name = msgProc.getName() ?: continue
                            if (msgStr == name) {
                                menuStr = msgProc.getMenu(event)
                                break
                            }
                        }
                    }
                    if (menuStr != null) {
                        event.group.sendMessage(menuStr)
                        return@launch
                    }
                }

                //处理消息
                for (msgProc in msgProcList) {
                    if (msgProc.process(event)) {
                        return@launch
                    }
                }

            } catch (e: Exception) {
                event.group.sendMessage("错误：$e")
                e.printStackTrace()
            }
        }
    }

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

    @Synchronized
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
        if (!event.origin.isOperator() && event.new.isOperator()) {
            msgProcList.clear()
            onCreate()
        }
    }

}