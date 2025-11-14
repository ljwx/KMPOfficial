package org.example.project

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType = PlatformType.PLATFORM_ANDROID
    override fun isPlatform(type: PlatformType): Boolean {
        return this.type == type
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()