package org.example.project

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val type: PlatformType = PlatformType.PLATFORM_WEB
    override fun isPlatform(type: PlatformType): Boolean {
        return this.type == type
    }
}

actual fun getPlatform(): Platform = WasmPlatform()