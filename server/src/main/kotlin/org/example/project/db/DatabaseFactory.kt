package org.example.project.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

object DatabaseFactory {
    /**
     * 初始化数据库连接和表结构
     * @param dropExistingTable 是否删除已存在的旧表（仅用于开发环境，生产环境请设置为 false）
     */
    fun init(dropExistingTable: Boolean = false) {
        // 切换为 MySQL 驱动
        val driverClassName = "com.mysql.cj.jdbc.Driver"
        // MySQL 连接 URL，指向本地 3306 端口的 ktor_db 数据库
        // useSSL=false 是为了本地开发方便，生产环境建议配置 SSL
        // ?serverTimezone=UTC 指定服务器时区为 UTC，避免时区问题
        val jdbcURL = "jdbc:mysql://localhost:3306/jdcr?useSSL=false&serverTimezone=UTC"
        val user = "root" // 你的 MySQL 用户名
        val password = "Lj13641435978" // 你的 MySQL 密码

        // 连接到数据库
        val database = Database.connect(jdbcURL, driverClassName, user, password)
        
        // 如果需要在开发环境删除旧表，先直接使用 JDBC 连接删除
        if (dropExistingTable) {
            try {
                // 直接使用 JDBC 连接删除表，避免 Exposed 的类型问题
                val directConnection = DriverManager.getConnection(jdbcURL, user, password)
                directConnection.createStatement().executeUpdate("DROP TABLE IF EXISTS users")
                directConnection.close()
                println("成功删除旧表")
            } catch (e: Exception) {
                // 表可能不存在或删除失败，忽略错误
                println("删除旧表时出现异常（可能表不存在）: ${e.message}")
            }
        }
        
        // 在一个事务中执行数据库操作
        transaction(database) {
            if (dropExistingTable) {
                // 开发环境：创建新表
                SchemaUtils.create(Users)
            } else {
                // 生产环境：只添加缺失的列，保留现有数据
                // 使用 createMissingTablesAndColumns 来创建缺失的表和列
                SchemaUtils.createMissingTablesAndColumns(Users)
            }
        }
    }

    /**
     * 一个挂起函数，用于在非 UI 线程上执行数据库查询，防止阻塞主线程。
     * @param block 将要执行的数据库操作 lambda。
     * @return 返回数据库操作的结果。
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
