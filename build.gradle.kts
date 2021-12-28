import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.0.1-rc2"
}

group = "be.bluexin"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val scalaVersion = "2.13"

dependencies {
    testImplementation(kotlin("test"))
    implementation(compose.desktop.currentOs)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("br.com.devsrsouza.compose.icons.jetbrains:eva-icons:1.0.0")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")

    implementation(platform("com.typesafe.akka:akka-bom_$scalaVersion:2.6.18"))
    implementation("com.typesafe.akka:akka-actor-typed_$scalaVersion")
    implementation("com.typesafe.akka:akka-stream_$scalaVersion")

    runtimeOnly("ch.qos.logback:logback-classic:1.2.9")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

compose.desktop {
    application {
        mainClass = "be.bluexin.ghe.view.GlorifiedHexEditorKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Rpm)
            packageName = "GlorifiedHexEditor"
            packageVersion = "1.0.0"

            windows {
                menuGroup = "Blade & Soul Tools"
                upgradeUuid = "E7B8F0DB-C47A-42DA-BF33-1129A5B4125D"
            }
        }
    }
}