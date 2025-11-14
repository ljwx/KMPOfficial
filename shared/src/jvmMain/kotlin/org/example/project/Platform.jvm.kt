package org.example.project

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val type: PlatformType = PlatformType.PLATFORM_JVM
    override fun isPlatform(type: PlatformType): Boolean {
        return this.type == type
    }
}

actual fun getPlatform(): Platform = JVMPlatform()