package com.qqbot.group

import com.qqbot.Info
import com.qqbot.database.group.MemberData

object GroupInfo {
    //群处理器配置路径
    val PROC_SETTING_PATH = Info.DATA_ROOT_PATH + "procSetting"
}

/**
 * QQ群成员权限
 */
object GroupPermission {
    //群员
    const val MEMBER = 0
    //群管
    const val ADMIN = 1

    /**
     * 判断是否为管理员或主人
     */
    fun MemberData.isOperator(): Boolean {
        return this.permission >= ADMIN || this.id == Info.RootManagerId
    }
}