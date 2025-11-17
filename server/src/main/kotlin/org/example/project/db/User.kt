package org.example.project.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * 用户状态枚举
 */
enum class UserStatus {
    ACTIVE,      // 激活状态
    INACTIVE,    // 未激活
    BANNED       // 已封禁
}

/**
 * 用户角色枚举
 */
enum class UserRole {
    USER,        // 普通用户
    ADMIN        // 管理员
}

/**
 * 用户数据模型类。
 * @param id 用户 ID，数据库中自增，因此提供默认值。
 * @param username 用户名，唯一标识，用于登录。
 * @param email 邮箱地址，可选，用于登录和找回密码。
 * @param password 密码哈希值，存储加密后的密码。
 * @param nickname 昵称，显示名称。
 * @param avatar 头像URL，可选。
 * @param phone 手机号，可选。
 * @param status 用户状态，默认为未激活。
 * @param role 用户角色，默认为普通用户。
 * @param createdAt 创建时间。
 * @param updatedAt 更新时间。
 * @param lastLoginAt 最后登录时间，可选。
 * @property Serializable 使 Ktor 能够自动将其与 JSON 格式相互转换。
 */
@Serializable
data class User(
    val id: Int = 0,
    val username: String,
    val email: String? = null,
    val password: String,  // 存储加密后的密码
    val nickname: String? = null,
    val avatar: String? = null,
    val phone: String? = null,
    val status: String = UserStatus.INACTIVE.name,  // 序列化为字符串
    val role: String = UserRole.USER.name,          // 序列化为字符串
    val createdAt: String = LocalDateTime.now().toString(),  // 序列化为字符串
    val updatedAt: String = LocalDateTime.now().toString(),  // 序列化为字符串
    val lastLoginAt: String? = null  // 序列化为字符串
)

/**
 * 定义了 `Users` 数据库表的结构。
 * 继承自 Exposed 的 `Table` 类。
 * 
 * 包含商业项目常用的用户字段：
 * - 基础信息：用户名、邮箱、密码、昵称
 * - 扩展信息：头像、手机号
 * - 状态管理：用户状态、角色
 * - 时间戳：创建时间、更新时间、最后登录时间
 */
object Users : Table("users") {
    // 主键：用户ID，自增
    val id = integer("id").autoIncrement()
    
    // 登录凭证：用户名（唯一，必填）
    val username = varchar("username", length = 50).uniqueIndex()
    
    // 登录凭证：邮箱（唯一，可选）
    val email = varchar("email", length = 100).uniqueIndex().nullable()
    
    // 密码：存储加密后的密码哈希值（必填）
    val password = varchar("password", length = 255)
    
    // 显示信息：昵称（可选）
    val nickname = varchar("nickname", length = 50).nullable()
    
    // 头像：头像URL（可选）
    val avatar = text("avatar").nullable()
    
    // 联系方式：手机号（可选）
    val phone = varchar("phone", length = 20).nullable()
    
    // 状态：用户状态，默认为未激活
    val status = varchar("status", length = 20).default(UserStatus.INACTIVE.name)
    
    // 角色：用户角色，默认为普通用户
    val role = varchar("role", length = 20).default(UserRole.USER.name)
    
    // 时间戳：创建时间
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    // 时间戳：更新时间
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    // 时间戳：最后登录时间（可选）
    val lastLoginAt = datetime("last_login_at").nullable()

    // 指定主键
    override val primaryKey = PrimaryKey(id)
    
    // 注意：username 和 email 已经通过 uniqueIndex() 创建了唯一索引
    // 如果需要为 status 和 role 添加索引以提高查询性能，可以在数据库层面手动添加
    // 或者在需要时使用 SchemaUtils.createIndex() 创建
}
