// s2-validator 예제 (Spring Boot + Thymeleaf). 저장소 안에서는 현재 소스를 composite build 로 사용
rootProject.name = "s2-validator-example"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// 저장소 밖으로 복사해 쓸 때는 이 줄을 지우면 Maven Central 의 배포본(build.gradle.kts 의 버전)을 받음
includeBuild("../..")
