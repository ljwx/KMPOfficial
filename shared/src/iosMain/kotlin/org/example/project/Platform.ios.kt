package org.example.project

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val type: PlatformType = PlatformType.PLATFORM_IOS
    override fun isPlatform(type: PlatformType): Boolean {
        return this.type == type
    }
}

actual fun getPlatform(): Platform = IOSPlatform()