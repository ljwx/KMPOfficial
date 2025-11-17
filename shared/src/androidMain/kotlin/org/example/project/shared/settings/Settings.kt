package org.example.project.shared.settings

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android 平台的 Settings 实现工厂。
 * @param context 应用程序上下文，用于访问 SharedPreferences。
 */
actual class SettingsFactory(private val context: Context) {
    actual fun createSettings(): Settings {
        val sharedPreferences = context.getSharedPreferences("kmp_settings", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(sharedPreferences)
    }
}
