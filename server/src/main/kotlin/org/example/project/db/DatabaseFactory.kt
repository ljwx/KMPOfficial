package org.example.project.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        // 切换为 MySQL 驱动
        val driverClassName = "com.mysql.cj.jdbc.Driver"
        // MySQL 连接 URL，指向本地 3306 端口的 ktor_db 数据库
        // useSSL=false 是为了本地开发方便，生产环境建议配置 SSL
        // ?serverTimezone=UTC 指定服务器时区为 UTC，避免时区问题
        val jdbcURL = "jdbc:mysql://localhost:3306/ktor_db?useSSL=false&serverTimezone=UTC"
        val user = "root" // 你的 MySQL 用户名
        val password = "your_password" // 你的 MySQL 密码

        // 连接到数据库
        val database = Database.connect(jdbcURL, driverClassName, user, password)
        // 在一个事务中执行数据库操作
        transaction(database) {
            // 使用 SchemaUtils 自动创建或更新表结构
            SchemaUtils.create(Users)
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
