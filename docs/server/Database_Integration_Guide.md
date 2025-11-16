# Ktor Server 数据库集成指南

本指南将引导您如何为 Ktor 服务器项目添加数据库功能。我们将使用 H2 内存数据库作为示例，并使用 Exposed 框架作为 SQL 库。

## 步骤 1: 添加依赖

首先，我们需要在项目中添加 `Exposed` 和 `H2` 的依赖。

### 1.1 更新 `gradle/libs.versions.toml`

在 `[versions]` 部分添加 `exposed` 和 `h2` 的版本号，然后在 `[libraries]` 部分添加相应的库定义。

```toml
[versions]
# ... 其他版本
exposed = "0.52.0"
h2 = "2.2.224"

[libraries]
# ... 其他库
exposed-core = { module = "org.jetbrains.exposed:exposed-core", version.ref = "exposed" }
exposed-dao = { module = "org.jetbrains.exposed:exposed-dao", version.ref = "exposed" }
exposed-jdbc = { module = "org.jetbrains.exposed:exposed-jdbc", version.ref = "exposed" }
h2 = { module = "com.h2database:h2", version.ref = "h2" }
```

### 1.2 更新 `server/build.gradle.kts`

在 `dependencies` 代码块中，添加 `Exposed` 和 `H2` 的实现。

```kotlin
dependencies {
    // ... 其他依赖
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.h2)
}
```

添加完毕后，请同步您的 Gradle 项目。

## 步骤 2: 创建数据库连接

我们将创建一个 `DatabaseFactory` 单例来管理数据库连接和初始化。

在 `server/src/main/kotlin/org/example/project/` 目录下创建一个 `db` 包，然后在此包中创建 `DatabaseFactory.kt` 文件：

```kotlin
package org.example.project.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
        val database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
```

这里的 `init` 方法配置了 H2 内存数据库的连接，并创建了 `Users` 表（我们将在下一步定义它）。`dbQuery` 是一个辅助函数，用于在 I/O 线程池中执行数据库操作。

## 步骤 3: 定义数据模型

现在，我们来定义数据模型和数据库表结构。

在 `db` 包中创建 `User.kt` 文件：

```kotlin
package org.example.project.db

import org.jetbrains.exposed.sql.Table

data class User(val id: Int, val name: String, val age: Int)

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 50)
    val age = integer("age")

    override val primaryKey = PrimaryKey(id)
}
```

- `User` 是一个数据类，代表我们的模型。
- `Users` 是一个继承自 `Table` 的对象，它定义了数据库表的结构。

## 步骤 4: 创建服务层

为了更好地组织代码，我们将数据库逻辑封装在一个 `UserService` 中。

在 `db` 包中创建 `UserService.kt` 文件：

```kotlin
package org.example.project.db

import org.example.project.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class UserService {
    suspend fun getAllUsers(): List<User> = dbQuery {
        Users.selectAll().map(::resultRowToUser)
    }

    suspend fun createUser(user: User): User {
        val id = dbQuery {
            Users.insert {
                it[name] = user.name
                it[age] = user.age
            } get Users.id
        }
        return user.copy(id = id)
    }

    private fun resultRowToUser(row: ResultRow) = User(
        id = row[Users.id],
        name = row[Users.name],
        age = row[Users.age]
    )
}
```

`UserService` 提供了获取所有用户和创建新用户的方法。

## 步骤 5: 集成到 Ktor 应用

最后一步，我们将所有东西集成到 `Application.kt` 中。

```kotlin
package org.example.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.db.DatabaseFactory
import org.example.project.db.User
import org.example.project.db.UserService
import org.example.project.network.model.BaseApiResponse
import org.example.project.network.model.ProductSummaryData

// ... main function ...

fun Application.module() {
    // 1. 初始化数据库
    DatabaseFactory.init()
    // 2. 创建服务实例
    val userService = UserService()

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
        })
    }
    
    routing {
        // ... 其他路由 ...

        // 3. 添加新的 API 路由
        get("/users") {
            val users = userService.getAllUsers()
            val response = BaseApiResponse(code = 200, message = "成功", data = users)
            call.respond(response)
        }

        post("/users") {
            val user = call.receive<User>()
            val createdUser = userService.createUser(user)
            val response = BaseApiResponse(code = 201, message = "创建成功", data = createdUser)
            call.respond(response)
        }
    }
}
```

### 如何测试

1.  运行 `server` 模块。
2.  使用 `curl` 或 Postman 等工具测试 API：

    - **创建用户**:
      ```bash
      curl -X POST http://0.0.0.0:8081/users \
      -H "Content-Type: application/json" \
      -d '{"id":0, "name":"Alice", "age":30}'
      ```

    - **获取所有用户**:
      ```bash
      curl http://0.0.0.0:8081/users
      ```

现在，您的 Ktor 服务器已经成功集成了数据库功能！
