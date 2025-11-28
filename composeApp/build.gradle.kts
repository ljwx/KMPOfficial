import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Kotlin & Compose Multiplatform
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    
    // Android
    alias(libs.plugins.androidApplication)
    
    // Serialization
    alias(libs.plugins.kotlinSerialization)
    
    // Room & KSP
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    
    // Dev Tools
    alias(libs.plugins.composeHotReload)
}

kotlin {
    // --- Android Target ---
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // --- iOS Targets ---
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            
            // Export shared modules to iOS Framework
            export(projects.shared)
            export(projects.kmplog)
            export(libs.kotlinx.datetime) // Transitive export
        }
    }

    // --- Desktop (JVM) Target ---
    jvm()

    // --- Web Targets (JS & Wasm) ---
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    // --- Source Sets ---
    sourceSets {
        
        // 1. Common Main (Shared Logic & UI)
        commonMain.dependencies {
            // Compose Core
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Navigation
            implementation(libs.androidx.navigation.compose)

            // Lifecycle & ViewModel
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // Dependency Injection (Koin)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Project Modules
            api(projects.shared)
            api(projects.kmplog)

            implementation(libs.paging.common)
            implementation(libs.paging.compose)
        }

        // 2. Android Main
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            
            // Android-specific Data (Room/SQLite)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            
            // Koin for Android
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        // 3. iOS Main
        val iosMain by creating {
            dependencies {
                // iOS-specific dependencies if needed
            }
        }
        
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        // 4. Desktop (JVM) Main
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            
            // JavaFX (WebView)
            implementation("org.openjfx:javafx-base:17.0.2")
            implementation("org.openjfx:javafx-controls:17.0.2")
            implementation("org.openjfx:javafx-web:17.0.2")
            implementation("org.openjfx:javafx-graphics:17.0.2")
            
            // Desktop Data (Room/SQLite)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            
            // Koin for Desktop
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        // 5. Common Test
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Room Schema Configuration
room {
    schemaDirectory("$projectDir/schemas")
}

// Android Configuration
android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Extra Dependencies (Debug)
dependencies {
    debugImplementation(compose.uiTooling)
    // KSP for Room
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
}

// Compose Desktop Configuration
compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }
    }
}
