plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm")
}

group = "rip.sunrise"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))

    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-tree:9.8")
}

gradlePlugin {
    plugins {
        create("warp") {
            id = "rip.sunrise.warp"
            implementationClass = "rip.sunrise.warp.Plugin"
        }
    }
}