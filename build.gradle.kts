plugins {
    kotlin("multiplatform") version "2.0.0"
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.lkboard"
version = "1.9.1"

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
                implementation("org.apache.pdfbox:pdfbox:3.0.2")
                implementation("org.apache.poi:poi-ooxml:5.2.5")
                implementation("com.google.zxing:core:3.5.3")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.lkboard.app.MainKt"
        jvmArgs(
            "-XX:+TieredCompilation",
            "-Dskiko.renderApi=DIRECT3D",
            "-Dsun.java2d.d3d=true"
        )

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            packageName = "Lkboard"
            packageVersion = "1.9.1"
            description = "Lkboard - Infinite Canvas Collaborative Workspace"
            copyright = "© 2026 Lkboard Team"
            vendor = "Lkboard"

            modules(
                "java.base",
                "java.desktop",
                "java.instrument",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.rmi",
                "java.scripting",
                "java.security.jgss",
                "java.sql",
                "java.xml",
                "java.xml.crypto",
                "jdk.httpserver",
                "jdk.unsupported"
            )

            windows {
                menuGroup = "Lkboard"
                upgradeUuid = "b86e7a2c-e092-48a5-8dc8-fb3446927d61"
                iconFile.set(project.file("src/jvmMain/resources/logo.ico"))
                dirChooser = true
                perUserInstall = false
                shortcut = true
                menu = true
            }
        }
    }
}
