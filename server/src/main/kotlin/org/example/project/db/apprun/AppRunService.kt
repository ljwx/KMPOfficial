package org.example.project.db.apprun

import org.example.project.db.DatabaseFactory.dbQuery
import org.example.project.db.apprun.dto.AppRunInfoResponse
import org.jetbrains.exposed.sql.*
import java.time.LocalDate

class AppRunService {

    suspend fun getAppRunInfo(appName: String, date: LocalDate): List<AppRunInfo> {
        return dbQuery {
            AppRunTable.selectAll()
                .where { AppRunTable.appName eq appName and (AppRunTable.runDate eq date) }
                .map {
                    resultRowToInfo(it)
                }
        }
    }

    suspend fun appRun(appName: String) {
        dbQuery {
            AppRunTable.insert {
                it[AppRunTable.appName] = appName
            } get AppRunTable.id
        }
    }

    fun toInfoResponse(data: AppRunInfo?): AppRunInfoResponse? {
        if (data == null) {
            return null
        }
        return AppRunInfoResponse(
            appName = data.appName,
            runDate = data.runDate.toString(),
            startBalance = data.startBalance,
            endBalance = data.endBalance,
            checkIn = data.checkIn ?: false,
            mainTaskCount = data.mainTaskCount ?: 0
        )
    }

    private fun resultRowToInfo(row: ResultRow): AppRunInfo {
        return AppRunInfo(
            id = row[AppRunTable.id],
            appName = row[AppRunTable.appName],
            runDate = row[AppRunTable.runDate],
            startBalance = row[AppRunTable.startBalance],
            endBalance = row[AppRunTable.endBalance],
            checkIn = row[AppRunTable.checkIn],
            startupTime = row[AppRunTable.startupTime],
            finishTime = row[AppRunTable.finishTime],
            mainTaskCount = row[AppRunTable.mainTaskCount],
            branchTaskCount = row[AppRunTable.branchTaskCount],
            runDuration = row[AppRunTable.runDuration]
        )
    }

}