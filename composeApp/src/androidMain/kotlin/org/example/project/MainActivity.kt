package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.navigation.AndroidLifecycleOwner
import org.example.project.navigation.AppRoot
import org.example.project.shared.settings.SettingsFactory
import org.example.project.shared.settings.SettingsManager

/**
 * MainActivity - Android 应用入口
 * 
 * 职责：
 * 1. 初始化应用级别的设置管理器
 * 2. 创建和管理 RootComponent 的生命周期
 * 3. 处理配置变更时的状态保存和恢复
 * 
 * 生命周期管理：
 * - onCreate: 创建 RootComponent，传入 savedInstanceState（如果有）
 * - onSaveInstanceState: 保存 RootComponent 的状态到 Bundle
 * - onDestroy: 销毁 RootComponent（如果需要）
 * 
 * 状态保存：
 * - 配置变更（屏幕旋转等）时，Activity 会被重建，但状态会通过 Bundle 恢复
 * - 进程被杀死后重启时，状态会从 Bundle 恢复
 */
class MainActivity : ComponentActivity() {
    
    /**
     * Android 生命周期管理器
     * 负责将 Activity 生命周期绑定到 Decompose 生命周期
     */
    private val lifecycleOwner = AndroidLifecycleOwner(this)
    
    /**
     * RootComponent 实例
     * 在 Activity 生命周期内保持存活，确保导航状态不会丢失
     */
    private var rootComponent: org.example.project.navigation.RootComponent? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 SettingsManager
        SettingsManager.init(SettingsFactory(applicationContext))

        // 创建 RootComponent，传入 savedInstanceState 以恢复之前的状态
        rootComponent = lifecycleOwner.createRootComponent(savedInstanceState)

        setContent {
            // 传入 RootComponent，确保导航状态在配置变更时能够恢复
            AppRoot(rootComponent = rootComponent)
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存 RootComponent 的状态到 Bundle
        // 这样在配置变更或进程重启时，导航栈状态可以恢复
        lifecycleOwner.saveState(outState)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 如果 Activity 真的被销毁（不是配置变更），清理资源
        if (isFinishing) {
            lifecycleOwner.destroyRootComponent()
            rootComponent = null
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
    AppRoot()
}