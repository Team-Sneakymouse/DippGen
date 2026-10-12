import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "2.4.0"
    `maven-publish`
}

group = "io.github.team-sneakymouse"

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

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

tasks.processResources {
    inputs.property("version", project.version.toString())
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version.toString())
    }
}

tasks.jar {
    archiveBaseName.set("DippGen")
    archiveFileName.set("DippGen-${project.version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
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
