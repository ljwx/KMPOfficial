# Exposed 索引创建指南

在 Exposed 中创建索引有多种方式，本指南将详细介绍各种方法及其使用场景。

## 索引类型

### 1. 唯一索引（Unique Index）
确保列的值唯一，通常用于主键、用户名、邮箱等需要唯一性的字段。

### 2. 普通索引（Non-Unique Index）
提高查询性能，不限制值的唯一性，通常用于经常用于 WHERE、ORDER BY、JOIN 的字段。

### 3. 复合索引（Composite Index）
包含多个列的索引，用于多列组合查询的场景。

## 创建索引的方法

### 方式1: 在列定义时使用 `.uniqueIndex()` 或 `.index()`

**适用场景**：单列唯一索引或单列普通索引

```kotlin
object Users : Table("users") {
    val username = varchar("username", length = 50).uniqueIndex()  // 唯一索引
    val email = varchar("email", length = 100).uniqueIndex().nullable()  // 唯一索引
    val status = varchar("status", length = 20).index()  // 普通索引（如果支持）
}
```

**优点**：
- 简洁直观
- 与列定义在一起，易于理解

**缺点**：
- 某些版本的 Exposed 可能不支持在列定义时直接使用 `.index()`
- 无法创建复合索引

### 方式2: 使用 `index()` 函数创建单列索引

**适用场景**：单列普通索引

```kotlin
object Users : Table("users") {
    val status = varchar("status", length = 20).default(UserStatus.INACTIVE.name)
    
    // 创建单列索引
    val statusIndex = index("idx_users_status", status, isUnique = false)
}
```

**参数说明**：
- 第一个参数：索引名称（建议使用 `idx_表名_列名` 的命名规范）
- 第二个参数：要索引的列
- `isUnique`: 是否为唯一索引（false = 普通索引，true = 唯一索引）

### 方式3: 使用 `index()` 函数创建复合索引

**适用场景**：多列组合查询

```kotlin
object Users : Table("users") {
    val status = varchar("status", length = 20)
    val role = varchar("role", length = 20)
    
    // 创建复合索引
    val statusRoleIndex = index("idx_users_status_role", status, role, isUnique = false)
}
```

**使用场景示例**：
```kotlin
// 这个查询会使用复合索引
Users.select { (Users.status eq "ACTIVE") and (Users.role eq "USER") }
```

**注意事项**：
- 复合索引的顺序很重要，应该将最常用的列放在前面
- MySQL 的复合索引遵循"最左前缀原则"

### 方式4: 使用 `SchemaUtils.createIndex()` 在运行时创建索引

**适用场景**：需要动态创建索引，或者在表创建后添加索引

```kotlin
transaction(database) {
    // 创建表
    SchemaUtils.create(Users)
    
    // 在运行时创建索引
    try {
        SchemaUtils.createIndex(
            index = Users.statusIndex,
            isUnique = false
        )
    } catch (e: Exception) {
        // 索引可能已存在，忽略错误
        println("创建索引时出现异常: ${e.message}")
    }
}
```

**优点**：
- 可以在表创建后动态添加索引
- 适合生产环境的索引迁移

**缺点**：
- 需要手动处理索引已存在的情况

## 索引命名规范

建议使用以下命名规范：

- **唯一索引**：`uk_表名_列名`（如：`uk_users_username`）
- **普通索引**：`idx_表名_列名`（如：`idx_users_status`）
- **复合索引**：`idx_表名_列1_列2`（如：`idx_users_status_role`）

## 索引选择建议

### 应该创建索引的字段：

1. **主键和外键**：自动创建索引
2. **经常用于 WHERE 条件的字段**：如 `status`、`role`
3. **经常用于 JOIN 的字段**：外键字段
4. **经常用于 ORDER BY 的字段**：如 `createdAt`、`updatedAt`
5. **经常用于 GROUP BY 的字段**
6. **唯一性约束字段**：如 `username`、`email`

### 不应该创建索引的字段：

1. **很少用于查询的字段**
2. **数据量很小的表**（索引的开销可能大于收益）
3. **经常更新的字段**（索引会影响更新性能）
4. **包含 NULL 值很多的字段**（索引效果不佳）

## 实际示例

### 完整的表定义示例

```kotlin
object Users : Table("users") {
    // 主键（自动创建索引）
    val id = integer("id").autoIncrement()
    
    // 唯一索引 - 方式1
    val username = varchar("username", length = 50).uniqueIndex()
    val email = varchar("email", length = 100).uniqueIndex().nullable()
    
    // 普通字段
    val status = varchar("status", length = 20).default(UserStatus.INACTIVE.name)
    val role = varchar("role", length = 20).default(UserRole.USER.name)
    val phone = varchar("phone", length = 20).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    // 单列索引 - 方式2
    val statusIndex = index("idx_users_status", status, isUnique = false)
    val phoneIndex = index("idx_users_phone", phone, isUnique = false)
    val createdAtIndex = index("idx_users_created_at", createdAt, isUnique = false)
    
    // 复合索引 - 方式3
    val statusRoleIndex = index("idx_users_status_role", status, role, isUnique = false)
    
    override val primaryKey = PrimaryKey(id)
}
```

## 注意事项

1. **索引会占用存储空间**：每个索引都需要额外的存储空间
2. **索引会影响写入性能**：INSERT、UPDATE、DELETE 操作需要更新索引
3. **不要过度索引**：过多的索引会影响性能
4. **定期分析查询性能**：使用 `EXPLAIN` 语句分析查询是否使用了索引
5. **生产环境谨慎操作**：在生产环境创建索引时，大表可能需要较长时间

## 检查索引是否创建成功

在 MySQL 中可以使用以下 SQL 查询：

```sql
-- 查看表的所有索引
SHOW INDEX FROM users;

-- 或者
SELECT * FROM information_schema.statistics 
WHERE table_schema = 'your_database_name' 
AND table_name = 'users';
```

