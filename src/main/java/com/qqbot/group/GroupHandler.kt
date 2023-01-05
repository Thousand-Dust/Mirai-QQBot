package com.qqbot.group

import com.qqbot.ai.TextClassifier
import com.qqbot.database.group.GroupDatabase
import com.qqbot.group.msg.GroupMsgProc
import com.qqbot.group.msg.proc.GroupCheck
import com.qqbot.group.msg.proc.GroupManager
import com.qqbot.group.msg.proc.GroupOwner
import com.qqbot.group.msg.proc.GroupScore
import kotlinx.coroutines.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.event.events.MemberLeaveEvent
import net.mamoe.mirai.message.data.*

/**
 * 单个群的消息处理类
 * @author Thousand-Dust
 */
class GroupHandler(myGroup: Group, my: Member) : GroupEventHandler(myGroup, my) {

    private val database = GroupDatabase(myGroup.id)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val msgProcList = ArrayList<GroupMsgProc>()

    //群主系统
    private lateinit var groupOwner: GroupOwner
    //群管系统
    private lateinit var groupManager: GroupManager
    //群消息检测系统
    private lateinit var groupCheck: GroupCheck
    //积分系统
    private lateinit var groupScore: GroupScore

    override fun onCreate(): Boolean {
        //初始化群消息处理器
        groupOwner = GroupOwner(this, database)
        groupManager = GroupManager(this, database)
        groupCheck = GroupCheck(this, database)
        groupScore = GroupScore(this, database)
        //按顺序添加消息处理器
        msgProcList.add(groupOwner)
        msgProcList.add(groupManager)
        msgProcList.add(groupCheck)
        msgProcList.add(groupScore)
        return true
    }

    override fun onRemove() {
        database.close()
    }

    override fun acceptMessage(event: GroupMessageEvent) {
        addCache(event)

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
                            .append("菜单：\n")
                            .append("主人系统(还没写)")
                        for (msgProc in msgProcList) {
                            val desc = msgProc.getDesc()
                            if (desc != null) {
                                sb.append("\n").append(desc)
                            }
                        }
                        menuStr = sb.toString()
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
                event.group.sendMessage("错误：${e.message}")
            }
        }
    }

    override fun onMemberJoin(event: MemberJoinEvent) {
        runBlocking {
            event.group.sendMessage(
                MessageChainBuilder().append("欢迎新人").append(At(event.member.id)).append(" 加入本群").build()
            )
        }
    }

    override fun onMemberLeave(event: MemberLeaveEvent) {
        runBlocking {
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

}