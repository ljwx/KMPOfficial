import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return CustomHostingController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

// 自定义 UIViewController，支持动态状态栏样式
class CustomHostingController: UIViewController {
    private var currentStatusBarStyle: UIStatusBarStyle = .darkContent
    private var composeViewController: UIViewController?
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return currentStatusBarStyle
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // 创建 Compose ViewController
        composeViewController = MainViewControllerKt.MainViewController()
        
        if let composeVC = composeViewController {
            // 将 Compose ViewController 添加为子控制器
            addChild(composeVC)
            view.addSubview(composeVC.view)
            composeVC.view.frame = view.bounds
            composeVC.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            composeVC.didMove(toParent: self)
        }
        
        // 监听来自 Kotlin 的状态栏样式变化通知
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(statusBarStyleChanged),
            name: NSNotification.Name("StatusBarStyleChangedNotification"),
            object: nil
        )
        
        // 初始化时获取当前样式
        updateStatusBarStyle()
    }
    
    @objc private func statusBarStyleChanged() {
        updateStatusBarStyle()
    }
    
    private func updateStatusBarStyle() {
        // 调用 Kotlin 函数获取当前状态栏样式
        let kotlinStyle = StatusBarConfig_iosKt.getStatusBarStyle()
        
        // 将 Kotlin 的枚举转换为 iOS 的 UIStatusBarStyle
        switch kotlinStyle {
        case .darkContent:
            currentStatusBarStyle = .darkContent  // 深色图标，适合浅色背景
        case .lightContent:
            currentStatusBarStyle = .lightContent // 浅色图标，适合深色背景
        default:
            currentStatusBarStyle = .darkContent
        }
        
        // 触发状态栏更新
        setNeedsStatusBarAppearanceUpdate()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
