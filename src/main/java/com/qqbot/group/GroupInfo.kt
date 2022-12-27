package com.qqbot.group

/**
 * 群成员权限
 * @author Thousand-Dust
 */
enum class Permission(val value: Int) {
    /**
     * 机器人主人
     */
    MASTER(0),
    /**
     * 群主
     */
    OWNER(1),
    /**
     * 管理员
     */
    ADMIN(2),
    /**
     * 群成员
     */
    MEMBER(3),
}