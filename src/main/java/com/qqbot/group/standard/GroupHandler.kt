package com.qqbot.group.standard

import com.qqbot.Info
import com.qqbot.TimeMillisecond
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.GroupDatabaseImpl
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.GroupPermission
import com.qqbot.group.msg.GroupMsgProc
import com.qqbot.group.standard.proc.msg.*
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*

/**
 * 单个群的事件普通总处理类
 * @author Thousand-Dust
 */
class GroupHandler(myGroup: Group) : GroupEventHandler(myGroup) {

    //数据库
    private val database: GroupDatabaseImpl = GroupDatabase(myGroup.id)

    //协程作用域
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    //消息处理器列表
    private val msgProcList = ArrayList<GroupMsgProc>()

    //上次报错的时间
    private var lastErrorTime = 0L

    //上次报错的类型
    private var lastErrorType: Class<*>? = null

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
                val sender = event.sender
                var senderData = database.getMember(event.sender.id)
                if (senderData == null) {
                    senderData = MemberData(sender.id, sender.nameCardOrNick, 0)
                    if (sender.id == Info.RootManagerId) {
                        senderData.permission = GroupPermission.SUPER_OWNER.level
                    }
                    database.addMember(senderData)
                } else if (senderData.name != sender.nameCardOrNick) {
                    senderData.name = sender.nameCardOrNick
                    if (sender.id == Info.RootManagerId) {
                        senderData.permission = GroupPermission.SUPER_OWNER.level
                    }
                    database.setMember(senderData)
                }
                for (msgProc in msgProcList) {
                    //逐一调用消息处理器，直到有一个处理器处理了消息
                    if (msgProc.process(event)) {
                        break
                    }
                }

            } catch (e: Exception) {
                if (!myGroup.isBotMuted && System.currentTimeMillis() - lastErrorTime > TimeMillisecond.MINUTE && lastErrorType != e.javaClass) {
                    event.group.sendMessage("错误：$e")
                }
                e.printStackTrace()
                lastErrorTime = System.currentTimeMillis()
                lastErrorType = e.javaClass
            }
        }
    }

    override fun onMemberJoinRequest(event: MemberJoinRequestEvent) {

    }

    override fun onMemberJoin(event: MemberJoinEvent) {

    }

    @Synchronized
    override fun onMemberLeave(event: MemberLeaveEvent) {

    }

    override fun onMyPermissionChange(event: BotGroupPermissionChangeEvent) {
        if (!event.origin.isOperator() && event.new.isOperator()) {
            msgProcList.clear()
            onCreate()
        }
    }

}