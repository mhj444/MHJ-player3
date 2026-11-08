pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MHJ-player3"

include(":app")
include(":core:model")
include(":core:data")
include(":core:playback")
include(":core:ui")
include(":feature:music")
include(":feature:video")
