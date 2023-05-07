import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    id("org.jetbrains.compose") version "1.4.0"
}

group = "be.bluexin"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project("HexDataLayoutLoader"))
    testImplementation(kotlin("test"))
    implementation(compose.desktop.currentOs)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("br.com.devsrsouza.compose.icons.jetbrains:eva-icons:1.0.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.3")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

compose.desktop {
    application {
        mainClass = "be.bluexin.ghe.view.GlorifiedHexEditorKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Rpm)
            packageName = "GlorifiedHexEditor"
            packageVersion = "1.0.0"
            modules += "java.naming"

            windows {
                menuGroup = "Blade & Soul Tools"
                upgradeUuid = "E7B8F0DB-C47A-42DA-BF33-1129A5B4125D"
            }
        }
    }
}