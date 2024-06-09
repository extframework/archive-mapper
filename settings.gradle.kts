pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.extframework.dev/releases")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "archive-mapper"
include("transform")
include("tiny")
include("proguard")
