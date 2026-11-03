import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "2.4.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    `maven-publish`
}

group = "io.github.team-sneakymouse"

// Keep build outputs on a native FS when the checkout sits on FUSE/NTFS (e.g. /mnt/files).
layout.buildDirectory.set(
    file("${System.getProperty("user.home")}/.cache/${rootProject.name}-gradle-build"),
)

// Requires a git checkout with at least one commit (CI and local clones).
version =
    providers.exec {
        workingDir(rootDir)
        commandLine("git", "show", "-s", "--format=%ct:%h", "--abbrev=12", "HEAD")
    }.standardOutput.asText.map { commit ->
        val (timestamp, hash) = commit.trim().split(":", limit = 2)
        val date =
            DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochSecond(timestamp.toLong()))
        "$date-$hash"
    }.get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

val minecraftVersion = "26.2"
val paperApi = "26.2.build.+"
val worldGuardVersion = "7.0.19"

/**
 * On Windows, a bare `bash` resolves to WSL's System32\bash.exe before PATH is searched.
 * Use Git for Windows' bash (located via `git --exec-path`) when it exists.
 */
fun bashExecutable(): String {
    if (!System.getProperty("os.name").startsWith("Windows")) return "bash"
    val gitExecPath =
        providers.exec { commandLine("git", "--exec-path") }.standardOutput.asText.get().trim()
    val gitBash = file(gitExecPath).resolve("../../../bin/bash.exe").normalize()
    return if (gitBash.isFile) gitBash.absolutePath else "bash"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:$worldGuardVersion") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation("io.papermc.paper:paper-api:$paperApi")
    testImplementation("com.sk89q.worldguard:worldguard-bukkit:$worldGuardVersion") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

java {
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks {
    processResources {
        inputs.property("version", project.version.toString())
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to project.version.toString())
        }
    }

    jar {
        archiveBaseName.set("DippGen")
        archiveFileName.set("DippGen-${project.version}.jar")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }

    runServer {
        minecraftVersion(minecraftVersion)
        // DippGen hard-depends on WorldGuard (which needs WorldEdit); PlaceholderAPI is optional.
        downloadPlugins {
            // Version id of the Bukkit build of 7.4.6; the version number alone resolves to a non-Bukkit jar.
            modrinth("worldedit", "YFnqiUlL")
            modrinth("worldguard", worldGuardVersion)
        }
    }

    test {
        useJUnitPlatform()
    }

    val publishPreflight by registering(Exec::class) {
        group = "verification"
        description = "Catch sneakyrp publish footguns (gradlew mode, wrapper jar, workflow_dispatch)."
        workingDir = rootDir
        commandLine(bashExecutable(), "scripts/publish-preflight.sh", rootDir.absolutePath)
    }

    check {
        dependsOn(publishPreflight)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "dippgen"
            from(components["java"])
            pom {
                name.set("DippGen")
                description.set("Paper plugin for plot management and server utilities.")
                url.set("https://github.com/Team-Sneakymouse/DippGen")
                properties.put("sneakyrp.deployment.type", "paper-plugin")
                developers {
                    developer {
                        id.set("team-sneakymouse")
                        name.set("Team Sneakymouse")
                        url.set("https://github.com/Team-Sneakymouse")
                    }
                }
                scm {
                    url.set("https://github.com/Team-Sneakymouse/DippGen")
                    connection.set("scm:git:https://github.com/Team-Sneakymouse/DippGen.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "sneakyrp"
            url = uri("https://maven.sneakyrp.com/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<org.gradle.authentication.http.BasicAuthentication>("basic")
            }
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.check)
}
