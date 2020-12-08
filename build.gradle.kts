import java.io.FileInputStream
import java.util.*

plugins {
    java
    idea
    maven
    `maven-publish`
    kotlin("jvm") version "1.4.10"
}

group = "me.camdenorrb"
version = "1.0.4"

repositories {

    mavenCentral()

    maven("https://maven.pkg.jetbrains.space/camdenorrb/p/twelveoclock-dev/maven") {
        name = "Camdenorrb"
    }

    maven("https://dl.bintray.com/kotlin/kotlinx/") {
        name = "KotlinX"
    }
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("me.camdenorrb:KCommons:1.2.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:atomicfu:0.14.4")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}


tasks {

    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }
    val javadocJar by creating(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(getByName("javadoc"))
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    wrapper {
        gradleVersion = "6.7.1"
    }
    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
    repositories {
        maven {

            url = uri("https://maven.pkg.jetbrains.space/camdenorrb/p/twelveoclock-dev/maven")

            credentials {

                val secretProperties = Properties().apply {
                    load(FileInputStream("secret-gradle.properties"))
                }

                username = secretProperties["username"].toString()
                password = secretProperties["password"].toString()
            }

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
