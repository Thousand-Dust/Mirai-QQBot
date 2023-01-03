package com.qqbot.database.group

import java.sql.Connection
import java.sql.DriverManager

/**
 * qq群成员 MySql数据库操作
 * @author Thousand-Dust
 */
class GroupDatabase(private val groupId: Long) {

    companion object {
        private const val USER_NAME = "qqbot"
        private const val PASSWORD = "QQBot-tdzn123"
        private const val DATABASE_NAME = "qqbot_5517"

        private val connection: Connection

        private const val TABLE_VERSION = 1000

        //群成员id 即qq号
        private const val COLUMN_GROUP_ID = "id"
        //群成员昵称
        private const val COLUMN_GROUP_NAME = "name"
        //群成员积分
        private const val COLUMN_SCORE = "score"
        //群成员上一次签到时间
        private const val COLUMN_LAST_SIGN_TIME = "lastSignTime"
        //群成员连续签到次数
        private const val COLUMN_CONTINUE_SIGN_COUNT = "continueSignCount"
        //群成员上一次违规被禁言时间
        private const val COLUMN_LAST_VIOLATION_TIME = "lastViolationTime"
        //群成员违规被禁言次数
        private const val COLUMN_VIOLATION_COUNT = "violationCount"
        //群成员权限
        private const val COLUMN_PERMISSION = "permission"

        init {
            //加载驱动
            try {
                Class.forName("com.mysql.jdbc.Driver")
            } catch (e: Exception) {
                println("加载驱动失败")
                throw e
            }
            try {//连接数据库
                val url =
                    "jdbc:mysql://localhost:3306/$DATABASE_NAME?useUnicode=true&characterEncoding=utf-8&useSSL=false"
                connection = DriverManager.getConnection(url, USER_NAME, PASSWORD)
            } catch (e: Exception) {
                println("连接数据库失败")
                throw e
            }
        }
    }

    private val TABLE_NAME = "group_$groupId"

    init {
        //数据表不存在则创建
        val sql = "CREATE TABLE IF NOT EXISTS $TABLE_NAME (" +
                "$COLUMN_GROUP_ID BIGINT NOT NULL COMMENT 'QQ号', " +
                "$COLUMN_GROUP_NAME varchar(32) NOT NULL COMMENT '名字', " +
                "$COLUMN_SCORE INT NOT NULL COMMENT '积分', " +
                "$COLUMN_LAST_SIGN_TIME BIGINT NOT NULL COMMENT '上一次签到时间', " +
                "$COLUMN_CONTINUE_SIGN_COUNT INT NOT NULL COMMENT '连续签到时间', " +
                "$COLUMN_LAST_VIOLATION_TIME BIGINT NOT NULL COMMENT '上一次违规时间', " +
                "$COLUMN_VIOLATION_COUNT INT NOT NULL COMMENT '连续违规次数', " +
                "$COLUMN_PERMISSION INT NOT NULL COMMENT '自定义权限', " +
                "PRIMARY KEY ($COLUMN_GROUP_ID)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;"
        connection.createStatement().execute(sql)
    }

    /**
     * 关闭数据库连接，释放资源
     */
    fun close() {
        connection.close()
    }

    /**
     * 插入群成员信息
     */
    fun addMember(groupMember: MemberData) {
        val sql = "INSERT INTO $TABLE_NAME VALUES (" +
                "${groupMember.id}, " +
                "'${groupMember.name}', " +
                "${groupMember.score}, " +
                "${groupMember.lastSignTime}, " +
                "${groupMember.continueSignCount}, " +
                "${groupMember.lastViolationTime}, " +
                "${groupMember.violationCount}, " +
                "${groupMember.permission}" +
                ");"
        connection.createStatement().execute(sql)
    }

    /**
     * 删除群成员信息
     */
    fun delete(id: Long) {
        val sql = "DELETE FROM $TABLE_NAME WHERE $COLUMN_GROUP_ID = ${id};"
        connection.createStatement().execute(sql)
    }

    /**
     * 更新群员信息
     */
    fun setMember(member: MemberData) {
        val sql = "UPDATE $TABLE_NAME SET " +
                "$COLUMN_GROUP_NAME = '${member.name}', " +
                "$COLUMN_SCORE = ${member.score}, " +
                "$COLUMN_LAST_SIGN_TIME = ${member.lastSignTime}, " +
                "$COLUMN_CONTINUE_SIGN_COUNT = ${member.continueSignCount}, " +
                "$COLUMN_LAST_VIOLATION_TIME = ${member.lastViolationTime}, " +
                "$COLUMN_VIOLATION_COUNT = ${member.violationCount}, " +
                "$COLUMN_PERMISSION = ${member.permission} " +
                "WHERE $COLUMN_GROUP_ID = ${member.id}"
        connection.createStatement().execute(sql)
    }

    /**
     * 查询群员信息
     */
    fun getMember(id: Long): MemberData? {
        val sql = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_GROUP_ID = $id"
        val resultSet = connection.createStatement().executeQuery(sql)
        if (resultSet.next()) {
            return MemberData(
                resultSet.getLong(COLUMN_GROUP_ID),
                resultSet.getString(COLUMN_GROUP_NAME),
                resultSet.getInt(COLUMN_SCORE),
                resultSet.getLong(COLUMN_LAST_SIGN_TIME),
                resultSet.getInt(COLUMN_CONTINUE_SIGN_COUNT),
                resultSet.getLong(COLUMN_LAST_VIOLATION_TIME),
                resultSet.getInt(COLUMN_VIOLATION_COUNT),
                resultSet.getInt(COLUMN_PERMISSION)
            )
        }
        return null
    }

    /**
     * 查询群员积分前十名列表
     */
    fun getTopTen(): List<MemberData> {
        val sql = "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_SCORE DESC LIMIT 10"
        val resultSet = connection.createStatement().executeQuery(sql)
        val list = ArrayList<MemberData>(10)
        while (resultSet.next()) {
            list.add(
                MemberData(
                    resultSet.getLong(COLUMN_GROUP_ID),
                    resultSet.getString(COLUMN_GROUP_NAME),
                    resultSet.getInt(COLUMN_SCORE),
                    resultSet.getLong(COLUMN_LAST_SIGN_TIME),
                    resultSet.getInt(COLUMN_CONTINUE_SIGN_COUNT),
                    resultSet.getLong(COLUMN_LAST_VIOLATION_TIME),
                    resultSet.getInt(COLUMN_VIOLATION_COUNT),
                    resultSet.getInt(COLUMN_PERMISSION)
                )
            )
        }
        return list
    }

    /**
     * 查询权限为某个值的群员列表
     */
    fun getPermissions(permission: Int): List<MemberData> {
        val sql = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_PERMISSION=$permission"
        val resultSet = connection.createStatement().executeQuery(sql)
        val list = ArrayList<MemberData>()
        while (resultSet.next()) {
            list.add(
                MemberData(
                    resultSet.getLong(COLUMN_GROUP_ID),
                    resultSet.getString(COLUMN_GROUP_NAME),
                    resultSet.getInt(COLUMN_SCORE),
                    resultSet.getLong(COLUMN_LAST_SIGN_TIME),
                    resultSet.getInt(COLUMN_CONTINUE_SIGN_COUNT),
                    resultSet.getLong(COLUMN_LAST_VIOLATION_TIME),
                    resultSet.getInt(COLUMN_VIOLATION_COUNT),
                    resultSet.getInt(COLUMN_PERMISSION)
                )
            )
        }
        return list
    }

}