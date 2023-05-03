package com.qqbot.group.standard

import com.qqbot.Info
import com.qqbot.TimeMillisecond
import com.qqbot.Utils
import com.qqbot.database.group.GroupDatabase
import com.qqbot.database.group.GroupDatabaseImpl
import com.qqbot.database.group.MemberData
import com.qqbot.group.GroupEventHandler
import com.qqbot.group.GroupPermission
import com.qqbot.group.msg.GroupMsgProc
import com.qqbot.group.other.GroupOtherProc
import com.qqbot.group.standard.proc.msg.*
import com.qqbot.group.standard.proc.other.OtherStandard
import com.qqbot.group.standard.proc.other.OtherVest
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

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

    //其他事件处理器列表
    private val otherProcList = ArrayList<GroupOtherProc>()

    //上次报错的时间
    private var lastErrorTime = 0L

    //上次报错的类型
    private var lastErrorType: Class<*>? = null

    override fun onCreate(): Boolean {

        //机器人在群里没有管理员权限，只注册群消息检测系统
        if (myGroup.botPermission < MemberPermission.ADMINISTRATOR) {
            msgProcList.add(GroupCheck(this, database))
            return true
        }

        //按顺序注册消息处理器
        msgProcList.add(GroupMaster(this, database))
        msgProcList.add(GroupOwner(this, database))
        msgProcList.add(GroupManager(this, database))
        msgProcList.add(GroupCheck(this, database))
        msgProcList.add(GroupScore(this, database))
        msgProcList.add(GroupRecreation(this, database))

        if (myGroup.id == 142155075L) {
            //注册 天际 群专属处理器
            otherProcList.add(OtherVest(myGroup))
            msgProcList.add(GroupVest(this, database))
        } else {
            //注册其他事件标准处理器
            otherProcList.add(OtherStandard(myGroup))
        }

        return true
    }

    override fun onRemove() {
        //逐一调用处理器的onRemove方法
        msgProcList.forEach { it.onRemove() }
        otherProcList.forEach { it.onRemove() }
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
                        val sendMsg = event.group.sendMessage(menuStr)
                        coroutineScope.launch {
                            // 1 minute later recall
                            delay(TimeMillisecond.MINUTE)
                            sendMsg.recall()
                        }
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
                    //将详细错误输出到文件
                    val bytesOut = ByteArrayOutputStream()
                    val printOut = PrintStream(bytesOut)
                    e.printStackTrace(printOut)
                    Utils.writeFile("logout/error.log", bytesOut.toByteArray(), true)
                    printOut.close()
                    bytesOut.close()
                }
                e.printStackTrace()
                lastErrorTime = System.currentTimeMillis()
                lastErrorType = e.javaClass
            }
        }
    }

    override fun onMemberJoinRequest(event: MemberJoinRequestEvent) {
        for (otherProc in otherProcList) {
            if (otherProc.onMemberJoinRequest(event)) {
                break
            }
        }
    }

    override fun onMemberJoin(event: MemberJoinEvent) {
        for (otherProc in otherProcList) {
            if (otherProc.onMemberJoin(event)) {
                break
            }
        }
    }

    @Synchronized
    override fun onMemberLeave(event: MemberLeaveEvent) {
        for (otherProc in otherProcList) {
            if (otherProc.onMemberLeave(event)) {
                break
            }
        }
    }

    override fun onMemberPermissionChange(event: MemberPermissionChangeEvent) {
        for (otherProc in otherProcList) {
            if (otherProc.onMemberPermissionChange(event)) {
                break
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