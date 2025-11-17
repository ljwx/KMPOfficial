package org.example.project.shared.settings

import com.russhwolf.settings.Settings

/**
 * SettingsFactory 用于创建平台相关的 Settings 实例。
 * 在 Android 平台，我们需要一个 Context，因此使用工厂模式而不是简单的函数。
 */
expect class SettingsFactory {
    fun createSettings(): Settings
}
