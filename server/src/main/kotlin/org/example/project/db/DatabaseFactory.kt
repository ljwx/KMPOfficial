package org.example.project.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.DatabaseMetaData

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    /**
     * 检查表结构是否兼容
     * @param metaData 数据库元数据
     * @param tableName 表名
     * @return 如果表结构兼容返回 true，否则返回 false
     */
    private fun isTableStructureCompatible(metaData: DatabaseMetaData, tableName: String): Boolean {
        // 定义当前表结构的关键列（必须存在的列）
        val requiredColumns = mapOf(
            "id" to "INT",           // 主键
            "username" to "VARCHAR", // 用户名
            "password" to "VARCHAR", // 密码
            "status" to "VARCHAR",   // 状态
            "role" to "VARCHAR",     // 角色
            "created_at" to "DATETIME", // 创建时间
            "updated_at" to "DATETIME"  // 更新时间
        )
        
        // 获取表中的所有列
        val columns = metaData.getColumns(null, null, tableName, null)
        val existingColumns = mutableMapOf<String, String>()
        
        while (columns.next()) {
            val columnName = columns.getString("COLUMN_NAME").lowercase()
            val dataType = columns.getString("TYPE_NAME").uppercase()
            existingColumns[columnName] = dataType
        }
        
        // 检查所有必需列是否存在
        for ((columnName, expectedType) in requiredColumns) {
            val actualType = existingColumns[columnName]
            if (actualType == null) {
                logger.warn("缺少必需列: $columnName")
                return false
            }
            
            // 检查类型是否匹配（允许一些类型变体）
            val typeMatches = when {
                expectedType == "VARCHAR" -> actualType.contains("VARCHAR") || actualType.contains("CHAR")
                expectedType == "INT" -> actualType.contains("INT")
                expectedType == "DATETIME" -> actualType.contains("DATETIME") || actualType.contains("TIMESTAMP")
                else -> actualType.contains(expectedType)
            }
            
            if (!typeMatches) {
                logger.warn("列 $columnName 类型不匹配: 期望 $expectedType，实际 $actualType")
                return false
            }
        }
        
        return true
    }
    
    /**
     * 初始化数据库连接和表结构
     * 
     * 最佳实践：
     * 1. 开发环境：可以使用 dropExistingTable = true 快速重置
     * 2. 生产环境：必须使用 dropExistingTable = false，只进行增量更新
     * 3. 结构不兼容时：应该使用数据库迁移工具（如 Flyway）进行迁移，而不是自动删除表
     * 
     * @param dropExistingTable 是否强制删除已存在的表（仅用于开发环境）
     *                          生产环境必须设置为 false，避免数据丢失
     */
    fun init(dropExistingTable: Boolean = false) {
        // 切换为 MySQL 驱动
        val driverClassNameValue = "com.mysql.cj.jdbc.Driver"
        // MySQL 连接 URL
        val jdbcURL = System.getenv("JDBC_URL") 
            ?: "jdbc:mysql://localhost:3306/jdcr?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
        val user = System.getenv("DB_USER") ?: "root"
        // 生产环境必须通过环境变量 DB_PASSWORD 设置密码，不要硬编码
        val password = System.getenv("DB_PASSWORD") ?: "Lj13641435978"

        // 配置 HikariCP 连接池
        val config = HikariConfig().apply {
            jdbcUrl = jdbcURL
            driverClassName = driverClassNameValue
            username = user
            this.password = password
            // 连接池配置
            maximumPoolSize = System.getenv("DB_MAX_POOL_SIZE")?.toIntOrNull() ?: 10
            minimumIdle = System.getenv("DB_MIN_IDLE")?.toIntOrNull() ?: 5
            connectionTimeout = System.getenv("DB_CONNECTION_TIMEOUT")?.toLongOrNull() ?: 30000L // 30秒
            idleTimeout = System.getenv("DB_IDLE_TIMEOUT")?.toLongOrNull() ?: 600000L // 10分钟
            maxLifetime = System.getenv("DB_MAX_LIFETIME")?.toLongOrNull() ?: 1800000L // 30分钟
            poolName = "HikariCP-Pool"
            // 连接测试
            connectionTestQuery = "SELECT 1"
            // 连接泄漏检测
            leakDetectionThreshold = System.getenv("DB_LEAK_DETECTION_THRESHOLD")?.toLongOrNull() ?: 60000L // 60秒
        }
        
        val dataSource = HikariDataSource(config)
        
        // 使用 HikariCP 数据源连接到数据库
        val database = Database.connect(dataSource)
        
        // 初始化表结构（使用事务确保原子性）
        // 注意：初始化操作使用事务可以确保表创建的原子性，这是最佳实践
        transaction(database) {
            // 使用连接池获取连接进行元数据检查
            val directConnection = dataSource.connection
            try {
                val metaData = directConnection.metaData
                val tables = metaData.getTables(null, null, "users", null)
                val tableExists = tables.next()
                
                if (tableExists) {
                    // 表存在，检查是否需要删除重建
                    if (dropExistingTable) {
                        // 强制删除模式（开发环境）
                        logger.info("强制删除模式：删除旧表 users")
                        directConnection.createStatement().executeUpdate("DROP TABLE users")
                        SchemaUtils.create(Users)
                        logger.info("已创建新表 users")
                    } else {
                        // 生产环境模式：只进行增量更新，不删除表
                        // 这是最佳实践，避免数据丢失
                        val isCompatible = isTableStructureCompatible(metaData, "users")
                        
                        if (isCompatible) {
                            // 表结构兼容，只添加缺失的列（增量更新）
                            logger.info("表结构兼容，使用增量更新模式")
                            SchemaUtils.createMissingTablesAndColumns(Users)
                            logger.info("已完成表结构更新")
                        } else {
                            // 表结构不兼容
                            // 生产环境最佳实践：不自动删除表，而是抛出异常要求使用迁移工具
                            val errorMessage = """
                                |表结构不兼容，但为了数据安全，不会自动删除表。
                                |
                                |请使用以下方式之一进行迁移：
                                |1. 使用数据库迁移工具（推荐）：Flyway 或 Liquibase
                                |2. 手动迁移：备份数据 -> 删除表 -> 创建新表 -> 恢复数据
                                |3. 开发环境：设置 dropExistingTable = true（仅限开发环境）
                                |
                                |当前表缺少的必需列或类型不匹配，请检查日志了解详情。
                            """.trimMargin()
                            
                            logger.warn("表结构不兼容: $errorMessage")
                            // 尝试使用增量更新，可能会失败，但不删除表
                            try {
                                SchemaUtils.createMissingTablesAndColumns(Users)
                                logger.warn("已尝试增量更新，但可能不完整，请检查表结构")
                            } catch (e: Exception) {
                                logger.error("增量更新失败", e)
                                throw IllegalStateException(
                                    "表结构不兼容且无法自动修复。$errorMessage",
                                    e
                                )
                            }
                        }
                    }
                } else {
                    // 表不存在，直接创建
                    logger.info("表 users 不存在，创建新表")
                    SchemaUtils.create(Users)
                    logger.info("已创建新表 users")
                }
            } catch (e: Exception) {
                logger.error("数据库初始化时出现异常", e)
                // 尝试使用 Exposed 的方式创建表
                try {
                    SchemaUtils.createMissingTablesAndColumns(Users)
                    logger.info("已使用 Exposed 创建/更新表结构")
                } catch (e2: Exception) {
                    logger.error("创建表失败", e2)
                    throw e2
                }
            } finally {
                directConnection.close()
            }
        }
    }

    /**
     * 一个挂起函数，用于在非 UI 线程上执行数据库查询，防止阻塞主线程。
     * 
     * @param block 将要执行的数据库操作 lambda。
     * @return 返回数据库操作的结果。
     */
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
