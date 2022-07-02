import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "org.hildan.pictionary"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-java:1.6.7")
    implementation("com.tfowl.ktor:ktor-jsoup:1.6.4")

    implementation("net.sourceforge.tess4j:tess4j:5.1.1")
    implementation("org.hildan.ocr:simple-ocr:1.0.0")

    implementation(platform("com.google.cloud:libraries-bom:24.2.0"))
    implementation("com.google.cloud:google-cloud-vision")

    implementation("com.github.ajalt.mordant:mordant:2.0.0-beta4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("MainKt")
}
