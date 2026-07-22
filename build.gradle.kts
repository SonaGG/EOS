plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
}

group = "gg.sona"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
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
