pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "2.1.20"
    }
}

rootProject.name = "Warp"
include("plugin")