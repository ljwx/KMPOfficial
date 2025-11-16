
# Ktor 服务端数据库集成指南

本指南将详细介绍如何在 KMP 项目的服务端（Ktor）中集成数据库。我们将使用 H2 内存数据库作为示例，并使用 [Exposed](https://github.com/JetBrains/Exposed) 框架作为 SQL 库。

Exposed 是一个来自 JetBrains 的开源 Kotlin SQL 框架，它提供了两种不同的数据库访问方式：领域特定语言 (DSL) 和数据访问对象 (DAO)。本示例将使用其 DSL。

---

## 步骤 1: 添加依赖

首先，你需要在你的 `server` 模块中添加必要的 Gradle 依赖。

1.  **在 `gradle/libs.versions.toml` 文件中定义版本和库：**

    在这个文件中，我们可以统一管理所有依赖的版本和名称。请确保包含 `exposed` 和 `h2` 数据库驱动。

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

    # ...
    ```

2.  **在 `server/build.gradle.kts` 文件中添加依赖：**

    现在将这些库添加到 server 模块的依赖中。

    ```kotlin
    dependencies {
        // ... 其他依赖
        implementation(libs.exposed.core)
        implementation(libs.exposed.dao)
        implementation(libs.exposed.jdbc)
        implementation(libs.h2)
    }
    ```

---

## 步骤 2: 配置数据库连接

我们需要创建一个单例对象来初始化和配置数据库连接。

1.  **创建 `DatabaseFactory.kt` 文件:**

    在 `server/src/main/kotlin/org/example/project/` 目录下创建一个 `db` 包，并在其中创建 `DatabaseFactory.kt` 文件。

    `server/src/main/kotlin/org/example/project/db/DatabaseFactory.kt`

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
            // 使用内存数据库 H2, ";DB_CLOSE_DELAY=-1" 防止内存数据丢失
            val jdbcURL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
            val database = Database.connect(jdbcURL, driverClassName)

            // 在事务中创建表
            transaction(database) {
                SchemaUtils.create(Users) // 创建 Users 表
            }
        }

        // 一个工具函数，用于在挂起函数中执行数据库查询
        suspend fun <T> dbQuery(block: suspend () -> T): T =
            newSuspendedTransaction(Dispatchers.IO) { block() }
    }
    ```

    -   `init()` 方法负责连接到数据库。这里我们使用 H2 的内存模式，数据只在程序运行时存在。
    -   `transaction { ... }` 代码块用于执行数据库操作，例如使用 `SchemaUtils.create()` 创建表。
    -   `dbQuery` 是一个辅助函数，它使用 `newSuspendedTransaction` 在 I/O 线程池中异步执行数据库操作，这对于 Ktor 协程环境非常重要。

---

## 步骤 3: 定义数据模型和表结构

接下来，我们需要定义数据模型（Data Class）和对应的数据库表结构。

1.  **创建 `User.kt` 文件:**

    在 `db` 包下创建 `User.kt` 文件。

    `server/src/main/kotlin/org/example/project/db/User.kt`

    ```kotlin
    package org.example.project.db

    import org.jetbrains.exposed.sql.Table
    import kotlinx.serialization.Serializable

    @Serializable // 使其可序列化为 JSON
    data class User(val id: Int = 0, val name: String, val age: Int)

    object Users : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val age = integer("age")

        override val primaryKey = PrimaryKey(id)
    }
    ```

    -   `User` 是一个普通的数据类，用于在代码中表示用户。添加 `@Serializable` 注解是为了让 Ktor 能够自动将其序列化/反序列化为 JSON。
    -   `Users` 是一个继承自 `Exposed.Table` 的单例对象，它定义了数据库中 `users` 表的结构。字段（`id`, `name`, `age`）映射到表的列。

---

## 步骤 4: 创建数据服务层 (Service)

创建一个服务类来封装数据库操作的业务逻辑，这是一种良好的实践，可以使你的路由处理代码更清晰。

1.  **创建 `UserService.kt` 文件:**

    在 `db` 包下创建 `UserService.kt` 文件。

    `server/src/main/kotlin/org/example/project/db/UserService.kt`

    ```kotlin
    package org.example.project.db

    import org.example.project.db.DatabaseFactory.dbQuery
    import org.jetbrains.exposed.sql.ResultRow
    import org.jetbrains.exposed.sql.insert
    import org.jetbrains.exposed.sql.selectAll

    class UserService {
        // 获取所有用户
        suspend fun getAllUsers(): List<User> = dbQuery {
            Users.selectAll().map(::resultRowToUser)
        }

        // 创建一个新用户
        suspend fun createUser(user: User): User {
            val id = dbQuery {
                Users.insert {
                    it[name] = user.name
                    it[age] = user.age
                } get Users.id
            }
            return user.copy(id = id)
        }

        // 将数据库查询结果行转换为 User 对象
        private fun resultRowToUser(row: ResultRow) = User(
            id = row[Users.id],
            name = row[Users.name],
            age = row[Users.age]
        )
    }
    ```

    -   所有数据库操作都在 `dbQuery` 辅助函数中执行，以确保它们在协程中被正确处理。
    -   `getAllUsers` 查询 `Users` 表中的所有行并将其映射为 `User` 对象列表。
    -   `createUser` 将一个新的 `User` 插入数据库，并返回带有自增 ID 的新创建的 `User` 对象。

---

## 步骤 5: 在 Ktor 中集成并创建 API 路由

最后一步是在 Ktor 的主应用模块中初始化数据库，并创建用于访问用户数据的 HTTP 端点。

1.  **修改 `Application.kt` 文件:**

    `server/src/main/kotlin/org/example/project/Application.kt`

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
    import org.example.project.network.model.BaseApiResponse // 假设你有一个标准响应包装器

    fun main() {
        embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    }

    fun Application.module() {
        // 1. 初始化数据库
        DatabaseFactory.init()
        
        // 2. 实例化服务
        val userService = UserService()

        // 配置 ContentNegotiation 和 JSON 序列化
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        
        routing {
            get("/") {
                call.respondText("Ktor: ${Greeting().greet()}")
            }

            // ... 其他路由

            // 3. 创建用户相关的 API 路由
            get("/users") {
                val users = userService.getAllUsers()
                val response = BaseApiResponse(code = 200, message = "成功", data = users)
                call.respond(response)
            }

            post("/users") {
                val user = call.receive<User>() // 自动从 JSON 请求体反序列化
                val createdUser = userService.createUser(user)
                val response = BaseApiResponse(code = 201, message = "创建成功", data = createdUser)
                call.respond(response)
            }
        }
    }
    ```
    -   在 `module` 函数的开头调用 `DatabaseFactory.init()` 来确保数据库在服务启动时被初始化。
    -   创建 `UserService` 的实例。
    -   在 `routing` 块中，添加了两个新的端点：
        -   `GET /users`: 获取所有用户列表。
        -   `POST /users`: 创建一个新用户。请求的 JSON body 会被自动解析为 `User` 对象。

---

## 步骤 6: 运行和测试

现在你可以运行 `server` 模块。服务启动后，你可以使用 `curl` 或任何 API 测试工具来测试新的端点。

**获取所有用户 (初始为空):**

```bash
curl http://localhost:8080/users
```

**创建一个新用户:**

```bash
curl -X POST http://localhost:8080/users \
-H "Content-Type: application/json" \
-d '{"name": "Alice", "age": 30}'
```

**再次获取所有用户:**

```bash
curl http://localhost:8080/users
```

这样，你就成功地为 Ktor 服务端添加了数据库功能！你可以遵循类似的步骤来集成其他数据库，如 PostgreSQL 或 MySQL，只需更改 JDBC 驱动和连接URL即可。
