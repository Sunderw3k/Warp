plugins {
    java
    `maven-publish`
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.3"
    id("rip.sunrise.warp") version "0.0.1"
}

group = "rip.sunrise"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}