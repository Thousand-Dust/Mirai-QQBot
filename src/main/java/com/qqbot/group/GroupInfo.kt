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
enum class GroupPermission : Comparable<GroupPermission> {
    //群员 0
    MEMBER,
    //群管 1
    ADMIN,
    //主人 2
    SUPER_OWNER;

    val level: Int
    get() = ordinal
}

/**
 * 判断是否为管理员或主人
 */
fun MemberData.isOperator(): Boolean {
    return this.permission >= GroupPermission.ADMIN.level || this.id == Info.RootManagerId
}
