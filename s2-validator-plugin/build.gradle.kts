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

import io.github.devers2.buildsupport.S2BuildUtils
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

// 1. 플러그인 개발 기능 로드
apply(plugin = "java-gradle-plugin")
apply(plugin = "signing")

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

version = "1.1.2"

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

group = project.property("group")!!
version = project.property("version")!!

base {
    archivesName.set("s2-validator-plugin")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
    }
    withSourcesJar()
    withJavadocJar()
}

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
                pom {
                    name = "S2 Validator Gradle Plugin"
                    description = "Static analysis plugin for S2Validator field name validation"
                    url = "https://github.com/devers2/s2-util"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "devers2"
                            name = "이승수"
                            email = "eseungsu.dev@gmail.com"
                            organization = "devers2"
                            organizationUrl = "https://github.com/devers2"
                        }
                    }
                    scm {
                        connection = "scm:git:git://github.com/devers2/s2-util.git"
                        developerConnection = "scm:git:ssh://github.com/devers2/s2-util.git"
                        url = "https://github.com/devers2/s2-util"
                    }
                }
            }
        }
    }
}

// 배포 리포지토리 설정 (S2BuildUtils 공통 로직 재사용 - CentralPortal 등록 + 서명 필수화까지 자동 처리됨.
// .all()로 반응형 서명을 적용하므로, 아래 s2ValidatorPluginMarkerMaven처럼 나중에 등록되는 Publication도 서명 대상에 포함됨)
S2BuildUtils.configureCentralPortalRepository(project)

// Marker Artifact에 대한 메타데이터 설정 (이미 존재하는 Publication 설정)
project.afterEvaluate {
    publishing.publications.named<MavenPublication>("s2ValidatorPluginMarkerMaven") {
        // Gradle Plugin Marker Artifact 관례에 맞게 강제 설정
        groupId = "io.github.devers2.validator"
        artifactId = "io.github.devers2.validator.gradle.plugin"

        pom {
            name = "S2 Validator Gradle Plugin Marker"
            description = "Marker for S2 Validator Gradle Plugin"
            url = "https://github.com/devers2/s2-util"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "devers2"
                    name = "이승수"
                    email = "eseungsu.dev@gmail.com"
                    organization = "devers2"
                    organizationUrl = "https://github.com/devers2"
                }
            }
            scm {
                connection = "scm:git:git://github.com/devers2/s2-util.git"
                developerConnection = "scm:git:ssh://github.com/devers2/s2-util.git"
                url = "https://github.com/devers2/s2-util.git"
            }
        }
    }
}
