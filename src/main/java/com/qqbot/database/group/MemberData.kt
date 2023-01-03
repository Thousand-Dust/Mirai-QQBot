package com.qqbot.database.group

/**
 * qq群成员信息
 * @author Thousand-Dust
 */
class MemberData(
    val id: Long,
    var name: String,
    var score: Int = 0,
    var lastSignTime: Long = 0,
    var continueSignCount: Int = 0,
    var lastViolationTime: Long = 0,
    var violationCount: Int = 0,
    var permission: Int = 0,
) {
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