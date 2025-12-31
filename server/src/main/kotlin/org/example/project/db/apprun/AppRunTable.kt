package org.example.project.db.apprun

import org.example.project.util.DateTimeUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object AppRunTable : Table("app_run") {
    val id = long("id").autoIncrement()
    val appName = varchar("app_name", length = 15).index()
    val runDate = date("run_date").clientDefault { DateTimeUtils.nowUTC().toLocalDate() }.index()
    val startBalance = varchar("start_balance", length = 15).nullable()
    val endBalance = varchar("end_balance", length = 15).nullable()
    val checkIn = bool("check_in").nullable()
    val startupTime = datetime("startup_time").clientDefault { DateTimeUtils.nowUTC() }
    val finishTime = datetime("finish_time").clientDefault { DateTimeUtils.nowUTC() }.nullable()
    val mainTaskCount = integer("main_task_count").nullable()
    val branchTaskCount = integer("branch_task_count").nullable()
    val runDuration = integer("run_duration").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

data class AppRunInfo(
    val id: Long,
    val appName: String,
    val runDate: java.time.LocalDate,
    val startBalance: String?,
    val endBalance: String?,
    val checkIn: Boolean?,
    val startupTime: java.time.LocalDateTime,
    val finishTime: java.time.LocalDateTime?,
    val mainTaskCount: Int?,
    val branchTaskCount: Int?,
    val runDuration: Int?
)