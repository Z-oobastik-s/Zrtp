plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "org.zoobastiks"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io") // For Vault
}

configurations {
    all {
        resolutionStrategy {
            // Указываем предпочтение для Paper API версии 1.21.4
            force("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
            // Исключаем старую версию Bukkit из Vault
            exclude(group = "org.bukkit", module = "bukkit")
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") // Vault API
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("net.kyori:adventure-api:4.15.0")
    implementation("net.kyori:adventure-text-minimessage:4.15.0")
}

tasks {
    jar {
        archiveBaseName.set("Zrtp")
    }
    
    shadowJar {
        archiveBaseName.set("Zrtp")
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "21"
        }
    }

    compileJava {
        options.release.set(21)
        options.encoding = "UTF-8"
    }
    
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version
            )
        }
    }
}