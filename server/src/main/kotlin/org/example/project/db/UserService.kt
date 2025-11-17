package org.example.project.db

import org.example.project.db.DatabaseFactory.dbQuery
import org.example.project.db.dto.CreateUserRequest
import org.example.project.db.dto.UserResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

/**
 * 用户服务类，封装了与用户相关的数据库操作。
 */
class UserService {
    /**
     * 获取所有用户。
     * @return 返回一个包含所有用户的列表。
     */
    suspend fun getAllUsers(): List<User> = dbQuery {
        Users.selectAll().map(::resultRowToUser)
    }

    /**
     * 根据ID获取用户。
     * @param id 用户ID。
     * @return 返回用户对象，如果不存在则返回null。
     */
    suspend fun getUserById(id: Int): User? = dbQuery {
        Users.select { Users.id eq id }
            .map(::resultRowToUser)
            .singleOrNull()
    }

    /**
     * 根据用户名获取用户。
     * @param username 用户名。
     * @return 返回用户对象，如果不存在则返回null。
     */
    suspend fun getUserByUsername(username: String): User? = dbQuery {
        Users.select { Users.username eq username }
            .map(::resultRowToUser)
            .singleOrNull()
    }

    /**
     * 根据邮箱获取用户。
     * @param email 邮箱地址。
     * @return 返回用户对象，如果不存在则返回null。
     */
    suspend fun getUserByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }
            .map(::resultRowToUser)
            .singleOrNull()
    }

    /**
     * 创建一个新用户（使用 CreateUserRequest）。
     * @param request 创建用户请求 DTO。
     * @param passwordHash 加密后的密码哈希值。
     * @return 返回创建成功并带有新 ID 的用户对象。
     */
    suspend fun createUser(request: CreateUserRequest, passwordHash: String): User {
        val now = LocalDateTime.now()
        val id = dbQuery {
            Users.insert {
                it[username] = request.username
                it[email] = request.email
                it[password] = passwordHash
                it[nickname] = request.nickname
                it[avatar] = request.avatar
                it[phone] = request.phone
                it[status] = UserStatus.INACTIVE.name
                it[role] = UserRole.USER.name
                it[createdAt] = now
                it[updatedAt] = now
                it[lastLoginAt] = null
            } get Users.id
        }
        return User(
            id = id,
            username = request.username,
            email = request.email,
            password = passwordHash,
            nickname = request.nickname,
            avatar = request.avatar,
            phone = request.phone,
            status = UserStatus.INACTIVE.name,
            role = UserRole.USER.name,
            createdAt = now.toString(),
            updatedAt = now.toString(),
            lastLoginAt = null
        )
    }

    /**
     * 创建一个新用户（兼容旧版本，直接使用 User 对象）。
     * @param user 要创建的用户对象（id 通常被忽略）。
     * @return 返回创建成功并带有新 ID 的用户对象。
     */
    suspend fun createUser(user: User): User {
        val now = LocalDateTime.now()
        val id = dbQuery {
            Users.insert {
                it[username] = user.username
                it[email] = user.email
                it[password] = user.password
                it[nickname] = user.nickname
                it[avatar] = user.avatar
                it[phone] = user.phone
                it[status] = user.status
                it[role] = user.role
                it[createdAt] = now
                it[updatedAt] = now
                it[lastLoginAt] = null
            } get Users.id
        }
        return user.copy(
            id = id,
            createdAt = now.toString(),
            updatedAt = now.toString()
        )
    }
    
    /**
     * 将 User 对象转换为 UserResponse DTO（不包含密码）。
     * @param user 用户对象。
     * @return 用户响应 DTO。
     */
    fun toUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            nickname = user.nickname,
            avatar = user.avatar,
            phone = user.phone,
            status = user.status,
            role = user.role,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            lastLoginAt = user.lastLoginAt
        )
    }

    /**
     * 更新用户信息。
     * @param id 用户ID。
     * @param user 要更新的用户对象（只更新提供的字段）。
     * @return 返回更新后的用户对象，如果用户不存在则返回null。
     */
    suspend fun updateUser(id: Int, user: User): User? = dbQuery {
        val now = LocalDateTime.now()
        Users.update({ Users.id eq id }) {
            user.email?.let { email -> it[Users.email] = email }
            user.password.takeIf { it.isNotBlank() }?.let { password -> it[Users.password] = password }
            user.nickname?.let { nickname -> it[Users.nickname] = nickname }
            user.avatar?.let { avatar -> it[Users.avatar] = avatar }
            user.phone?.let { phone -> it[Users.phone] = phone }
            it[Users.status] = user.status
            it[Users.role] = user.role
            it[Users.updatedAt] = now
        }
        getUserById(id)?.copy(updatedAt = now.toString())
    }

    /**
     * 更新用户最后登录时间。
     * @param id 用户ID。
     */
    suspend fun updateLastLogin(id: Int) = dbQuery {
        val now = LocalDateTime.now()
        Users.update({ Users.id eq id }) {
            it[lastLoginAt] = now
            it[updatedAt] = now
        }
    }

    /**
     * 删除用户（软删除：将状态设置为BANNED，或硬删除）。
     * @param id 用户ID。
     * @param hardDelete 是否硬删除，默认为false（软删除）。
     */
    suspend fun deleteUser(id: Int, hardDelete: Boolean = false) = dbQuery {
        if (hardDelete) {
            Users.deleteWhere { Users.id eq id }
        } else {
            Users.update({ Users.id eq id }) {
                it[status] = UserStatus.BANNED.name
                it[updatedAt] = LocalDateTime.now()
            }
        }
    }

    /**
     * 辅助函数，将数据库查询结果行（ResultRow）转换为 User 数据对象。
     * @param row 数据库查询结果的一行。
     * @return 转换后的 User 对象。
     */
    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id],
        username = row[Users.username],
        email = row[Users.email],
        password = row[Users.password],
        nickname = row[Users.nickname],
        avatar = row[Users.avatar],
        phone = row[Users.phone],
        status = row[Users.status],
        role = row[Users.role],
        createdAt = row[Users.createdAt].toString(),
        updatedAt = row[Users.updatedAt].toString(),
        lastLoginAt = row[Users.lastLoginAt]?.toString()
    )
}
