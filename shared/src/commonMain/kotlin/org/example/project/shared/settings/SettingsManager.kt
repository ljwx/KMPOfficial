package org.example.project.shared.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlin.native.concurrent.ThreadLocal

/**
 * SettingsManager 是一个单例，用于在整个应用中方便地访问 Settings。
 * 它封装了 Settings 的实例化过程，并提供了类型安全的存取方法。
 *
 * **重要**: 在使用前，必须在各个平台的应用入口处调用 `SettingsManager.init(settingsFactory)` 进行初始化。
 */
@ThreadLocal
object SettingsManager {

    private lateinit var settings: Settings

    /**
     * 初始化 SettingsManager。
     * @param factory 各个平台提供的 SettingsFactory 实例。
     */
    fun init(factory: SettingsFactory) {
        settings = factory.createSettings()
    }

    // --- 示例：存储和读取一个字符串 ---

    private const val KEY_USER_TOKEN = "user_token"

    /**
     * 保存用户 Token。
     * @param token 要保存的 Token 字符串。
     */
    fun saveUserToken(token: String) {
        settings[KEY_USER_TOKEN] = token
    }

    /**
     * 获取用户 Token。
     * @return 返回保存的 Token，如果不存在则返回 null。
     */
    fun getUserToken(): String? {
        return settings[KEY_USER_TOKEN]
    }

    // --- 示例：存储和读取一个布尔值 ---

    private const val KEY_IS_DARK_MODE = "is_dark_mode"

    /**
     * 保存深色模式开关状态。
     * @param isEnabled 是否启用深色模式。
     */
    fun saveDarkMode(isEnabled: Boolean) {
        settings[KEY_IS_DARK_MODE] = isEnabled
    }

    /**
     * 获取深色模式开关状态。
     * @return 返回保存的状态，如果不存在则默认为 false。
     */
    fun isDarkMode(): Boolean {
        return settings.getBoolean(KEY_IS_DARK_MODE, false)
    }
}
