package org.example.project.db

import org.example.project.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/**
 * 用户服务类，封装了与用户相关的数据库操作。
 */
class UserService {
    /**
     * 获取所有用户。
     * @return 返回一个包含所有用户的列表。
     */
    suspend fun getAllUsers(): List<User> = dbQuery {
        // 查询 Users 表中的所有记录，并映射为 User 对象列表
        Users.selectAll().map(::resultRowToUser)
    }

    /**
     * 创建一个新用户。
     * @param user 要创建的用户对象（id 通常被忽略）。
     * @return 返回创建成功并带有新 ID 的用户对象。
     */
    suspend fun createUser(user: User): User {
        val id = dbQuery {
            // 向 Users 表中插入一条新记录
            Users.insert {
                it[name] = user.name
                it[age] = user.age
            } get Users.id // 获取插入后生成的主键 ID
        }
        return user.copy(id = id)
    }

    /**
     * 辅助函数，将数据库查询结果行（ResultRow）转换为 User 数据对象。
     * @param row 数据库查询结果的一行。
     * @return 转换后的 User 对象。
     */
    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id],
        name = row[Users.name],
        age = row[Users.age]
    )
}
