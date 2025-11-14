package org.example.project

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
    override val type: PlatformType = PlatformType.PLATFORM_WEB
    override fun isPlatform(type: PlatformType): Boolean {
        return this.type == type
    }
}

actual fun getPlatform(): Platform = JsPlatform()