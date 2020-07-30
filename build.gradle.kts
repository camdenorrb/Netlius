plugins {
    java
    kotlin("jvm") version "1.4.0-rc"
}

group = "me.camdenorrb"
version = "1.0-SNAPSHOT"

repositories {

    mavenCentral()

    maven("https://dl.bintray.com/kotlin/kotlinx/") {
        name = "KotlinX"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-io-jvm:0.1.16")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8-1.4.0-rc")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}