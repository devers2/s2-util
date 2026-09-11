rootProject.name = "s2-validator-plugin"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

val s2BuildSupportDir = file("../../s2-build-support")
if (s2BuildSupportDir.exists()) {
    includeBuild(s2BuildSupportDir)
}
