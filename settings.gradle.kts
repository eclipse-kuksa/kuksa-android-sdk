pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    plugins {
        id("com.google.devtools.ksp") version "1.9.0"
        kotlin("jvm") version "1.9.0-1.0.11"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
include(":kuksa-sdk")
include(":samples")
include(":vss-processor")
