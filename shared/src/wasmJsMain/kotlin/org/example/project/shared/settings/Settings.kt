package org.example.project.shared.settings

import com.russhwolf.settings.JsSettings
import com.russhwolf.settings.Settings

/**
 * WasmJs 平台的 Settings 实现。
 * 使用浏览器的 localStorage 作为底层存储。
 */
actual class SettingsFactory {
    actual fun createSettings(): Settings {
        // JS 和 WasmJs 可以共用 JsSettings
        return JsSettings()
    }
}
