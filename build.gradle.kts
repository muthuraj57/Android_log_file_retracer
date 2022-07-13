import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.muthura.logretrace"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Log_Retrace"
            packageVersion = "1.0.0"
        }
    }
}

dependencies {
    commonMainImplementation("com.guardsquare:proguard-base:7.2.2")
    commonMainImplementation("com.guardsquare:proguard-retrace:7.2.2")

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")
}