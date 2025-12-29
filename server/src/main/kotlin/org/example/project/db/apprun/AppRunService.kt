package org.example.project.db.apprun

import org.example.project.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*

class AppRunService {

    suspend fun getAppRunInfo(appName: String): List<AppRunInfo> {
        return AppRunTable.selectAll()
            .where { AppRunTable.appName eq appName }
            .map { row ->
                resultRowToInfo(row)
            }
    }

    suspend fun appLunch(appName: String) {
        dbQuery {
            AppRunTable.insert {
                it[AppRunTable.appName] = appName
            } get AppRunTable.id
        }
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