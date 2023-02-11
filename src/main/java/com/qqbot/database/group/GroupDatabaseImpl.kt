package com.qqbot.database.group

/**
 * @author Thousand-Dust
 */
interface GroupDatabaseImpl {

    /**
     * 关闭数据库连接，释放资源
     */
    fun close()

    /**
     * 插入群成员信息
     */
    fun addMember(groupMember: MemberData)

    /**
     * 删除群成员信息
     */
    fun delete(id: Long)

    /**
     * 更新群员信息
     */
    fun setMember(member: MemberData)

    /**
     * 查询群员信息
     */
    fun getMember(id: Long): MemberData?

    /**
     * 查询群员积分前十名列表
     */
    fun getTopTen(): List<MemberData>

    /**
     * 查询权限为某个值的群员列表
     */
    fun getPermissions(permission: Int): List<MemberData>

}