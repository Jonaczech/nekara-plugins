pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://repo.purpurmc.org/snapshots")
        maven("https://mvn.lumine.io/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "NekaraRPG"
