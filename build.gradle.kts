plugins {
    idea
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.53.0"
    kotlin("jvm") version "2.2.21"
}

group = "me.camdenorrb"
version = "1.6.1" // x-release-please-version

repositories {

    mavenCentral()

    /*
    maven("https://maven.pkg.jetbrains.space/camdenorrb/p/twelveoclock-dev/maven") {
        name = "Camdenorrb"
    }
    */
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    //implementation("me.camdenorrb:KCommons:1.2.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:atomicfu:0.29.0")
}

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}


tasks {

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlin.ExperimentalUnsignedTypes"
            )
        }
    }

    wrapper {
        gradleVersion = "9.2.1"
        distributionType = Wrapper.DistributionType.BIN
        distributionSha256Sum = "72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f"
    }
}

/*
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
*/

publishing {

    val twelveOClockUsername = (findProperty("twelveOClockUsername") as String?)
        ?: System.getenv("REPOSILITE_USERNAME")

    val twelveOClockPassword = (findProperty("twelveOClockPassword") as String?)
        ?: System.getenv("REPOSILITE_TOKEN")

    repositories {
        maven {
            name = "12oclockDev"
            url = uri("https://maven.12oclock.dev/releases")
            credentials(PasswordCredentials::class) {
                username = twelveOClockUsername
                password = twelveOClockPassword
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.twelveoclock"
            artifactId = "netlius"
            from(components["java"])
        }
    }
}

/*
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/camdenorrb/Netlius")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String?  ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenKotlin") {
            from(components["kotlin"])
            artifactId = tasks.jar.get().archiveBaseName.get()
        }
    }
}
*/
