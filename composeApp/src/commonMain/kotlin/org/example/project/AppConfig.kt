package org.example.project

import org.example.project.feature.user.UserAuthRepositoryImpl
import org.example.project.feature.user.UserAuthViewModel
import org.example.project.feature.work.WorkDetailRepositoryImpl
import org.example.project.feature.work.WorkDetailViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    // ===================================================================
    // Koin 依赖注入的完整方式列表
    // ===================================================================
    
    // ========== 1. single - 单例模式 ==========
    // 整个应用生命周期内只有一个实例
    // 适用场景：Repository、Database、Network Client 等
    single { UserAuthRepositoryImpl() }
    
    // ========== 2. factory - 工厂模式 ==========
    // 每次获取时都创建新实例
    // 适用场景：需要动态参数的对象、临时对象
    factory { (workId: String) -> WorkDetailRepositoryImpl(workId) }
    
    // ========== 3. scoped - 作用域单例 ==========
    // 在特定作用域（scope）内是单例，作用域销毁后实例也会销毁
    // 适用场景：用户会话、页面级别的单例
    // 示例：
    // scope<MyScope> {
    //     scoped { SessionManager() }
    // }
    
    // ========== 4. viewModel - ViewModel 传统创建方式 ==========
    // 支持动态参数，需要手动创建
    // 适用场景：ViewModel 需要动态参数时
    viewModel { (workId: String) -> WorkDetailViewModel(workId) }
    
    // ========== 5. viewModelOf - ViewModel 类型安全创建方式 ==========
    // 自动解析构造函数参数，类型安全
    // 适用场景：ViewModel 的依赖都是 single 或 factory（无参数）时
    // 限制：不支持动态参数传递
    viewModelOf(::UserAuthViewModel)
    
    // ========== 6. get() - 在模块内部获取依赖 ==========
    // 在定义依赖时获取其他依赖
    // 示例：
    // single { Database(get()) }  // get() 会自动查找 Database 的依赖
    // factory { (userId: String) -> 
    //     UserService(get { parametersOf(userId) })
    // }
    
    // ========== 7. bind() - 绑定接口实现 ==========
    // 将实现类绑定到接口，可以通过接口类型获取
    // 示例：
    // single<UserRepository> { UserRepositoryImpl() } bind UserRepository::class
    // 或者：
    // single { UserRepositoryImpl() } bind UserRepository::class
    
    // ========== 8. named() - 命名限定符 ==========
    // 为同类型的多个实例命名区分
    // 示例：
    // single(named("api")) { HttpClient() }
    // single(named("cache")) { HttpClient() }
    // 使用：get<HttpClient>(named("api"))
    
    // ========== 9. parametersOf() - 传递动态参数 ==========
    // 在获取依赖时传递参数
    // 示例：
    // factory { (id: String) -> MyClass(id) }
    // 使用：get<MyClass> { parametersOf("123") }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule)
}