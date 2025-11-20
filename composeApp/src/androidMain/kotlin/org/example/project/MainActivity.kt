package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.shared.settings.SettingsFactory
import org.example.project.shared.settings.SettingsManager

class MainActivity : ComponentActivity() {
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SettingsManager.init(SettingsFactory(applicationContext))

        setContent {
            AppRootPage()
        }
    }
    
}

/**
 * Android 预览函数
 * 预览时使用默认的 RootComponent（不绑定到 Activity 生命周期）
 */
@Preview
@Composable
fun AppAndroidPreview() {
    AppRootPage()
}