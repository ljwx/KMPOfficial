# 数据库迁移最佳实践

本文档说明数据库迁移的最佳实践，确保代码可商用且符合生产环境标准。

## ❌ 当前实现的问题

### 1. **自动删除表的风险**

```kotlin
// 不推荐：生产环境中自动删除表会导致数据丢失
directConnection.createStatement().executeUpdate("DROP TABLE users")
```

**问题**：
- 生产环境删除表会导致**数据永久丢失**
- 没有备份机制
- 没有回滚能力
- 无法追踪变更历史

### 2. **结构检查不够完善**

当前只检查了：
- 列是否存在
- 列类型是否匹配

**缺失的检查**：
- 约束（主键、外键、唯一约束）
- 索引
- 默认值
- 是否允许 NULL

### 3. **没有版本管理**

- 无法追踪数据库变更历史
- 无法回滚到之前的版本
- 无法在不同环境间同步

## ✅ 生产环境最佳实践

### 1. **使用数据库迁移工具**

推荐使用专业的数据库迁移工具：

#### Flyway
- 版本化的 SQL 脚本
- 自动执行迁移
- 支持回滚
- 追踪迁移历史

#### Liquibase
- XML/YAML/SQL 格式
- 更灵活的变更管理
- 支持多种数据库

### 2. **迁移策略**

#### 原则1: 只增不减
- ✅ 添加新列
- ✅ 添加新表
- ✅ 添加索引
- ❌ 不删除列（标记为废弃）
- ❌ 不删除表（使用软删除）

#### 原则2: 向后兼容
- 新列使用默认值或允许 NULL
- 保持旧列可用
- 逐步迁移数据

#### 原则3: 可回滚
- 每个迁移都有对应的回滚脚本
- 测试回滚流程

### 3. **迁移脚本示例**

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V2__add_email_to_users.sql
ALTER TABLE users 
ADD COLUMN email VARCHAR(100) NULL UNIQUE AFTER username;

-- V3__add_status_to_users.sql
ALTER TABLE users 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE' AFTER password;
```

### 4. **数据迁移**

```sql
-- 迁移现有数据
UPDATE users 
SET status = 'ACTIVE' 
WHERE status IS NULL;
```

## 推荐的实现方案

### 方案1: 使用 Flyway（推荐）

#### 1. 添加依赖

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.flywaydb:flyway-core:9.22.0")
    implementation("org.flywaydb:flyway-mysql:9.22.0")
}
```

#### 2. 配置 Flyway

```kotlin
object DatabaseFactory {
    fun init() {
        val database = Database.connect(...)
        
        // 配置 Flyway
        val flyway = Flyway.configure()
            .dataSource(jdbcURL, user, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
        
        // 执行迁移
        flyway.migrate()
    }
}
```

#### 3. 创建迁移脚本

在 `src/main/resources/db/migration/` 目录下：

```
V1__create_users_table.sql
V2__add_email_to_users.sql
V3__add_status_to_users.sql
```

### 方案2: 简化版迁移（适合小型项目）

如果项目较小，可以使用简化版迁移：

```kotlin
object DatabaseFactory {
    /**
     * 数据库版本管理
     */
    private const val CURRENT_SCHEMA_VERSION = 1
    
    fun init() {
        val database = Database.connect(...)
        
        transaction(database) {
            // 创建版本表
            createVersionTableIfNotExists()
            
            // 获取当前版本
            val currentVersion = getCurrentSchemaVersion()
            
            // 执行迁移
            if (currentVersion < CURRENT_SCHEMA_VERSION) {
                migrate(currentVersion, CURRENT_SCHEMA_VERSION)
            }
        }
    }
    
    private fun migrate(fromVersion: Int, toVersion: Int) {
        // 按版本顺序执行迁移
        for (version in (fromVersion + 1)..toVersion) {
            executeMigration(version)
        }
    }
    
    private fun executeMigration(version: Int) {
        when (version) {
            1 -> {
                SchemaUtils.create(Users)
                updateSchemaVersion(1)
            }
            // 后续版本只添加列，不删除表
        }
    }
}
```

## 当前实现的改进建议

### 1. **移除自动删除表的逻辑**

```kotlin
// ❌ 删除这部分
if (!isCompatible) {
    directConnection.createStatement().executeUpdate("DROP TABLE users")
}

// ✅ 改为抛出异常，要求手动迁移
if (!isCompatible) {
    throw IllegalStateException(
        "表结构不兼容，请使用数据库迁移工具进行迁移。当前版本不支持自动删除表。"
    )
}
```

### 2. **只使用增量更新**

```kotlin
// ✅ 只使用增量更新，不删除表
SchemaUtils.createMissingTablesAndColumns(Users)
```

### 3. **添加迁移检查**

```kotlin
fun init() {
    val database = Database.connect(...)
    
    transaction(database) {
        // 检查是否需要迁移
        if (needsMigration()) {
            logger.warn("检测到数据库结构需要迁移，请使用迁移工具")
            // 只添加缺失的列，不删除表
            SchemaUtils.createMissingTablesAndColumns(Users)
        } else {
            // 创建表（如果不存在）
            SchemaUtils.createMissingTablesAndColumns(Users)
        }
    }
}
```

## 总结

### ❌ 不推荐的做法

1. **自动删除表**：生产环境会导致数据丢失
2. **结构不兼容就删除**：应该使用迁移脚本
3. **没有版本管理**：无法追踪和回滚

### ✅ 推荐的做法

1. **使用 Flyway/Liquibase**：专业的迁移工具
2. **只增不减**：添加列，不删除列
3. **版本化管理**：每个变更都有版本号
4. **可回滚**：每个迁移都有回滚脚本
5. **测试迁移**：在测试环境先测试

### 开发环境 vs 生产环境

- **开发环境**：可以使用 `dropExistingTable = true` 快速重置
- **生产环境**：必须使用迁移工具，禁止自动删除表

