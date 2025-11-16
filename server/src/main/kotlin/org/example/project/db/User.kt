package org.example.project.db

import org.jetbrains.exposed.sql.Table
import kotlinx.serialization.Serializable

/**
 * 用户数据模型类。
 * @param id 用户 ID，数据库中自增，因此提供默认值。
 * @param name 用户名。
 * @param age 用户年龄。
 * @property Serializable 使 Ktor 能够自动将其与 JSON 格式相互转换。
 */
@Serializable
data class User(val id: Int = 0, val name: String, val age: Int)

/**
 * 定义了 `Users` 数据库表的结构。
 * 继承自 Exposed 的 `Table` 类。
 */
object Users : Table() {
    // 定义 "id" 列，类型为整数，自动增长，作为主键。
    val id = integer("id").autoIncrement()
    // 定义 "name" 列，类型为字符串，最大长度为 50。
    val name = varchar("name", length = 50)
    // 定义 "age" 列，类型为整数。
    val age = integer("age")

    // 指定主键
    override val primaryKey = PrimaryKey(id)
}
