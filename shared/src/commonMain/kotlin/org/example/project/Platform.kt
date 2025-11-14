package org.example.project

enum class PlatformType(val type: Int) {
    PLATFORM_ANDROID(1),
    PLATFORM_IOS(2),
    PLATFORM_WEB(3),
    PLATFORM_JVM(4),
}

interface Platform {
    val type: PlatformType
    val name: String
    fun isPlatform(type: PlatformType): Boolean
}

expect fun getPlatform(): Platform