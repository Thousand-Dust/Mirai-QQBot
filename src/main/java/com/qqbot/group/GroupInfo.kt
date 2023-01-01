package com.qqbot.group

import com.qqbot.Info
import com.qqbot.database.group.MemberData

/**
 * QQ群成员权限
 */
object GroupPermission {
    const val MEMBER = 0
    const val ADMIN = 1
    const val MASTER = 2

    /**
     * 判断是否为管理员或主人
     */
    fun MemberData.isOperator(): Boolean {
        return this.permission >= ADMIN || this.id == Info.RootManagerId
    }
}