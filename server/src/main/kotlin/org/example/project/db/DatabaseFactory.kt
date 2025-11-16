package org.example.project.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val driverClassName = "org.h2.Driver"
        // 使用 H2 内存数据库。";DB_CLOSE_DELAY=-1" 选项可以防止在最后一个连接关闭时丢失数据。
        val jdbcURL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
        // 连接到数据库
        val database = Database.connect(jdbcURL, driverClassName)
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
