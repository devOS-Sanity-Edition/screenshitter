import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "one.devos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}


dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("dev.kord:kord-core:0.12.0")

    implementation("org.seleniumhq.selenium:selenium-java:4.16.1")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.16.1")
    implementation("org.seleniumhq.selenium:selenium-devtools-v120:4.16.1")

}

tasks.test {
    useJUnitPlatform()
}


kotlin {
    jvmToolchain(21)
}

tasks.withType<ShadowJar> {
    archiveFileName.set("Screenshitter.jar")
    manifest {
        attributes(
            mapOf(
                "Main-class" to "one.devos.MainKt",
                "Implementation-Version" to project.version
            )
        )
    }
}