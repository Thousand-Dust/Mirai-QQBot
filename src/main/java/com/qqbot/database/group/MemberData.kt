package com.qqbot.database.group

/**
 * qq群成员信息
 * @author Thousand-Dust
 */
class MemberData(
    val id: Long,
    name: String,
    var score: Int = 0,
    var lastSignTime: Long = 0,
    var continueSignCount: Int = 0,
    var lastViolationTime: Long = 0,
    var violationCount: Int = 0,
    var permission: Int = 0,
) {

    var name: String = name.toMysqlString()
        set(value) {
            field = value.toMysqlString()
        }

    private fun String.toMysqlString(): String {
        return this.replace("\'", "\\'").let {
            if (it.length > 32) it.substring(0, 32) else it
        }
    }

    override fun toString(): String {
        return "{id=$id, name='$name', score=$score, lastSignTime=$lastSignTime, continueSignCount=$continueSignCount, lastViolationTime=$lastViolationTime, violationCount=$violationCount, permission=$permission}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemberData

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
