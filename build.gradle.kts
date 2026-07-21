import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("jvm") version "2.4.0"
}

group = "gg.sona"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

val copyNativeLibs = tasks.register<Copy>("copyNativeLibs") {
    val nativesDir = layout.projectDirectory.dir("src/main/resources/natives")
    val nativesPath = nativesDir.asFile
    inputs.dir("EOS_SDK/SDK/Bin")
    outputs.dir(nativesPath)
    doFirst {
        nativesPath.mkdirs()
    }
    from("EOS_SDK/SDK/Bin") {
        include("EOSSDK-Win64-Shipping.dll")
        include("libEOSSDK-Linux-Shipping.so")
        include("libEOSSDK-Mac-Shipping.dylib")
    }
    into(nativesPath)
    includeEmptyDirs = false
}

tasks.named<Jar>("jar") {
    dependsOn(copyNativeLibs)
    manifest.attributes(
        "Multi-Release" to true
    )
}

tasks.named("processResources") {
    dependsOn(copyNativeLibs)
}
