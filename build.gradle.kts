plugins {
    idea
    `maven-publish`
    id("com.github.ben-manes.versions") version "0.45.0"
    kotlin("jvm") version "1.8.21"
}

group = "me.camdenorrb"
version = "1.1.0"

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

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
    //implementation("me.camdenorrb:KCommons:1.2.1")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:atomicfu:0.19.0")
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
        //kotlinOptions.useIR = true
        //sourceCompatibility = JavaVersion.VERSION_17.toString()
        //targetCompatibility = JavaVersion.VERSION_17.toString()
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        //kotlinOptions.apiVersion = "1.5"
        //kotlinOptions.languageVersion = "1.5"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=compatibility", "-Xmulti-platform", "-Xuse-experimental=kotlin.ExperimentalStdlibApi", "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
    }
    compileTestKotlin {
        //kotlinOptions.useIR = true
        //sourceCompatibility = JavaVersion.VERSION_17.toString()
        //targetCompatibility = JavaVersion.VERSION_17.toString()
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        //kotlinOptions.apiVersion = "1.5"
        //kotlinOptions.languageVersion = "1.5"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=compatibility", "-Xmulti-platform", "-Xuse-experimental=kotlin.ExperimentalStdlibApi", "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes")
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
