/**
 * S2Util Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */

/*
 * [배포 / Publishing]
 * 이 모듈(s2-validator-plugin)은 루트 s2-util 과 독립된 별도 Gradle 빌드(Composite Build)입니다.
 * 루트의 command-palette는 includeBuild 를 모듈로 인식하지 않으므로,
 * 반드시 이 디렉터리(s2-validator-plugin/)에서 직접 배포해야 합니다.
 *
 * ▶ command-palette 사용 시:
 *     s2-validator-plugin/ 디렉터리에서 command-palette → "Maven 중앙 저장소 배포" 선택
 *
 * ▶ 직접 실행 시 (루트 s2-util 기준):
 *     $ ./gradlew -p s2-validator-plugin publish
 *
 * ※ 버전은 libs.versions.toml 의 [versions] 섹션에서 "s2-validator-plugin" 키로 관리됩니다.
 *    이 build.gradle.kts 에서 version 을 변경하면 S2BuildUtils.syncVersionToCatalog 에 의해
 *    libs.versions.toml 의 버전이 자동으로 동기화됩니다.
 */


import io.github.devers2.buildsupport.S2BuildUtils
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("io.github.devers2.buildsupport")
}

// 2. 중앙 저장소 배포 플러그인 메타데이터 설정
configure<GradlePluginDevelopmentExtension> {
    plugins {
        create("s2Validator") {
            id = "io.github.devers2.validator"
            implementationClass = "io.github.devers2.validator.plugin.S2ValidatorPlugin"
            displayName = "S2 Validator Field Checker"
            description = "Static analysis plugin for S2Validator field name validation"
        }
    }
}

group = "io.github.devers2"
version = "1.1.3"

// 빌드 시 루트 gradle/libs.versions.toml의 s2-validator-plugin 버전 자동 동기화
S2BuildUtils.syncVersionToCatalog(project, "s2-validator-plugin", version.toString())

/*
 * [추가 소스 목록]
 * dynamicSourceInfoMap에 정의된 기능 키(예: 'licensesInfo')를 추가하여 관련된 소스 파일 및 라이브러리 의존성을 빌드에 자동으로 포함시킬 수 있다.
 */
extra["activeFeatures"] = setOf("licensesInfo")

/**
 * [동적 기능 소스 정보 (Feature Toggles)]
 * - 특정 기능(Feature)에 포함될 소스 파일과 라이선스 정보 정의
 */
extra["dynamicSourceInfoMap"] = mapOf(
    "licensesInfo" to mapOf(
        "licenses" to listOf(
            "README.md",
            "README.ko.md",
            "LICENSE",
            "licenses/LICENSE-APACHE-2.0",
            "licenses/NOTICE"
        )
    )
)

/**
 * [패키징 제외 설정]
 * S2BuildUtils.configureProject()에서 패키징 설정(JAR/Shadow JAR)을 건너뛰도록 설정
 * Gradle 플러그인 프로젝트는 자체 패키징 규칙을 사용하므로 표준 패키징 로직 제외
 */
extra["skipPackaging"] = true

base {
    archivesName.set("s2-validator-plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// 툴체인(javaVersion)과 Javadoc/Sources JAR 설정. 이 프로젝트는 pluginMaven/marker라는 자체
// Publication 체계를 쓰기 때문에 S2BuildUtils.configureLibraryPublishing()(mavenJava 전용) 대신
// 필요한 두 조각만 개별 호출한다 (javaVersion/releaseCompatibility는 루트 값으로 fallback됨).
S2BuildUtils.configureJavaCompatibility(project)
S2BuildUtils.configurePublishArtifacts(project)
S2BuildUtils.configureTestDefaults(project)

// 4. Gradle Module Metadata 생성 비활성화 (Maven Central 배포 오류 방지)
tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

dependencies {
    implementation(gradleApi())

    // JavaParser for AST-based source code analysis
    implementation(libs.javaparser.core)

    // Test dependencies
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(gradleApi())
}

/**
 * [라이선스 파일 자동 포함]
 * skipPackaging = true 설정 시 S2BuildUtils의 자동 패키징 로직이 동작하지 않으므로,
 * activeFeatures와 dynamicSourceInfoMap에 정의된 라이선스 파일들을 수동으로 jar 태스크에 주입한다.
 */
project.afterEvaluate {
    @Suppress("UNCHECKED_CAST")
    val activeFeaturesLocal = extra["activeFeatures"] as Set<String>

    @Suppress("UNCHECKED_CAST")
    val dynamicSourceInfoMapLocal = extra["dynamicSourceInfoMap"] as Map<String, Map<String, Any>>

    val extraFiles = mutableSetOf<String>()

    // 1. activeFeatures에 선언된 기능들의 라이선스 정보 수집
    activeFeaturesLocal.forEach { feature ->
        val info = dynamicSourceInfoMapLocal[feature]
        @Suppress("UNCHECKED_CAST")
        val licenses = info?.get("licenses") as? List<String>
        if (licenses != null) {
            extraFiles.addAll(licenses)
        }
    }

    // 2. 수집된 파일들을 JAR에 포함 (S2BuildUtils의 검증된 로직 재사용)
    if (extraFiles.isNotEmpty()) {
        val jarTask = tasks.named<Jar>("jar").get()
        S2BuildUtils.includeExtraFiles(jarTask, project, extraFiles)
    }

    // 3. Gradle 플러그인 publication의 groupId와 artifactId 통일
    publishing.publications.forEach { pub ->
        if (pub.name in listOf("pluginMaven", "s2ValidatorPluginMarkerMaven")) {
            (pub as MavenPublication).groupId = "io.github.devers2"
            pub.artifactId = "s2-validator-plugin"
        }
    }
}

publishing {
    publications {
        // java-gradle-plugin이 'pluginMaven' publication을 (afterEvaluate 등) 지연 생성하므로
        // named(...)로 즉시 조회하지 않고, 생성되는 시점에 안전하게 구성되도록 configureEach를 사용한다.
        withType<MavenPublication>().configureEach {
            if (name == "pluginMaven") {
                groupId = "io.github.devers2"
                artifactId = "s2-validator-plugin"
                // POM의 라이선스/개발자/SCM 등 공통 메타데이터는 S2BuildUtils.applyStandardPom이 채워준다.
                S2BuildUtils.applyStandardPom(
                    this,
                    "S2 Validator Gradle Plugin",
                    "Static analysis plugin for S2Validator field name validation",
                    "https://github.com/devers2/s2-util"
                )
            }
        }
    }
}

// 배포 리포지토리 설정 (S2BuildUtils 공통 로직 재사용 - CentralPortal 등록 + 서명 필수화까지 자동 처리됨.
// .all()로 반응형 서명을 적용하므로, 아래 s2ValidatorPluginMarkerMaven처럼 나중에 등록되는 Publication도 서명 대상에 포함됨)
S2BuildUtils.configureCentralPortalRepository(project)

// Central Portal은 개별 파일 PUT 요청(404 에러 발생)을 지원하지 않고 Zip 번들 업로드만 지원하므로,
// PublishToMavenRepository 태스크를 Zip 번들 생성 및 업로드 로직으로 가로채는(Hijack) 핸들러를 등록
S2BuildUtils.configureCentralPortalPublishing(project)

// Marker Artifact에 대한 메타데이터 설정 (이미 존재하는 Publication 설정)
project.afterEvaluate {
    publishing.publications.named<MavenPublication>("s2ValidatorPluginMarkerMaven") {
        // Gradle Plugin Marker Artifact 관례에 맞게 강제 설정
        groupId = "io.github.devers2.validator"
        artifactId = "io.github.devers2.validator.gradle.plugin"

        // POM의 라이선스/개발자/SCM 등 공통 메타데이터는 S2BuildUtils.applyStandardPom이 채워준다.
        S2BuildUtils.applyStandardPom(
            this,
            "S2 Validator Gradle Plugin Marker",
            "Marker for S2 Validator Gradle Plugin",
            "https://github.com/devers2/s2-util"
        )
    }
}
