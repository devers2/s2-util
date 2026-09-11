pluginManagement {
    // pluginManagement 블록 평가 시점에 사용자 홈의 gradle.properties 를 읽어올 수 없어 직접 읽어온다.
    val globalProperties = java.util.Properties()
    val globalFile = java.io.File(gradle.gradleUserHomeDir, "gradle.properties")
    if (globalFile.exists()) {
        globalFile.inputStream().use { stream ->
            globalProperties.load(stream)
        }
    }

    repositories {
        mavenCentral() // Maven Central
        gradlePluginPortal() // 기본 Gradle 플러그인 저장소
        mavenLocal()
    }
}

plugins {
    // Gradle이 필요한 JDK를 자동으로 찾고 다운로드하는 플러그인
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// JAR 파일의 기본 이름(BaseName, artifactId를 의미) 지정
rootProject.name = "s2-util"

include("s2-core")
include("s2-validator")
include("s2-jpa")

// ─────────────────────────────────────────────────────────────────────────────
// [배포 방법 / Publishing Guide]
//
// ▶ 일반 모듈 배포 (s2-util, s2-core, s2-validator, s2-jpa):
//     루트 프로젝트에서 실행
//     $ ./gradlew publish
//
// ▶ s2-validator-plugin 배포:
//     플러그인은 별도 독립 Gradle 빌드(Composite Build)로 분리되어 있으므로
//     플러그인 디렉터리에서 직접 실행해야 합니다.
//     $ ./gradlew -p s2-validator-plugin publish
//
//   (이유) s2-validator-plugin 은 Gradle 플러그인이라 pluginMaven 퍼블리케이션과
//          플러그인 마커(PluginMarkerMaven) 를 함께 배포해야 하며,
//          버전도 루트와 독립적으로 관리됩니다.
// ─────────────────────────────────────────────────────────────────────────────

// s2-validator-plugin 은 독립된 Gradle 플러그인 빌드로 포함 (Composite Build)
includeBuild("s2-validator-plugin")


val s2BuildSupportDir = file("../s2-build-support")
if (s2BuildSupportDir.exists()) {
    // 로컬 개발용: s2-build-support 디렉토리가 존재하는 경우에만 포함 (없어도 빌드 오류 발생 방지)
    includeBuild(s2BuildSupportDir)
}
