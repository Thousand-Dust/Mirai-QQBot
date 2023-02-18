package com.qqbot.group.msg

import com.qqbot.Info



/**
 * 群消息处理器系统配置的封装
 * TODO: 未完成
 */
class ProcConfig(val groupId: Long) {

    //配置文件路径
    private val configPath = Info.DATA_ROOT_PATH+"/Group/${groupId}/procConfig.json"

}