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

// 로컬 개발: 옆 폴더의 s2-build-support 소스가 있으면 배포본 대신 사용 (배포본으로만 확인: -Ps2.localBuilds=false, 상위 빌드와 같은 스위치)
val useLocalBuilds = providers.gradleProperty("s2.localBuilds").orNull != "false"
val s2BuildSupportDir = file("../../s2-build-support")
if (useLocalBuilds && s2BuildSupportDir.exists()) {
    includeBuild(s2BuildSupportDir)
}
