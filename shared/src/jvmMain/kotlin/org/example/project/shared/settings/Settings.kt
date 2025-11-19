package org.example.project.shared.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

/**
 * JVM 平台的 Settings 实现。
 * 使用 java.util.prefs.Preferences 作为底层存储。
 */
actual class SettingsFactory {
    actual fun createSettings(): Settings {
        val delegate = Preferences.userRoot()
        return PreferencesSettings(delegate)
    }
}
