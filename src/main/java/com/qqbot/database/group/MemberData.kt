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
)
