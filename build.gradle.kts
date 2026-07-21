import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
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
    jvmToolchain(21)
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

java {
    withSourcesJar()
}

fun setting(property: String, environment: String): Provider<String> =
    providers.gradleProperty(property).orElse(providers.environmentVariable(environment))

val cloverUsername = setting("cloverUsername", "CLOVER_MAVEN_USERNAME")
val cloverPassword = setting("cloverPassword", "CLOVER_MAVEN_PASSWORD")
val cloverUrl = setting("cloverUrl", "CLOVER_MAVEN_URL")

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "eos"

            pom {
                name = "EOS for Kotlin"
                description = "Kotlin bindings for the EOS SDK, built on the Java " +
                        "Foreign Function & Memory API."
                url = "https://maven.cloverclient.com"
            }
        }
    }

    repositories {
        maven {
            name = "Clover"

            url = uri(
                cloverUrl.orNull ?: if (version.toString().endsWith("SNAPSHOT")) {
                    "https://maven.cloverclient.com/snapshots"
                } else {
                    "https://maven.cloverclient.com/releases"
                },
            )

            credentials {
                username = cloverUsername.orNull
                password = cloverPassword.orNull
            }
        }
    }
}
