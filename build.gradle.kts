plugins {
    kotlin("jvm") version "1.6.21"
}

group = "net.yakclient"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.wrapper {
    gradleVersion = "7.2"
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test"))
}

tasks.compileKotlin {
    destinationDirectory.set(tasks.compileJava.get().destinationDirectory.asFile.get())

    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    targetCompatibility = "17"
    sourceCompatibility = "17"
}