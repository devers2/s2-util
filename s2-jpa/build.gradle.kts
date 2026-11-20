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

import org.gradle.api.tasks.bundling.Jar

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
            "licenses/LICENSE-EPL-2.0",
            "licenses/LICENSE-EDL-1.0",
            "licenses/NOTICE"
        )
    )
)

/*
 * 빌드 완료 후 결과물을 테스트할 클래스 지정 (shadowJar 실행 후 testArtifact 태스크로 수행됨, 여러 개 지정 가능)
 */
extra["artifactTestClassNames"] = emptyList<String>()


// 선택 기능 정의 — main 소스셋을 그대로 쓰고 배포 POM 에 optional 의존성으로 표시(소비자에게 전이되지 않음)
java {
    registerFeature("jpaSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

// main 소스셋을 쓰는 선택 기능은 Gradle 이 그 의존성을 runtimeClasspath 에도 연결한다. 로컬 빌드의 Fat JAR(S2BuildUtils)가
// runtimeClasspath 를 jar 에 합치므로, 그대로 두면 선택 의존성(Spring 등) 전체가 jar 에 들어가 소비자 클래스패스에서 중복된다
// (2.1.0 까지는 compileOnly 라 들어가지 않았음). 배포 POM·모듈 메타데이터의 optional 표시는 *RuntimeElements 에서 오므로 영향 없음
configurations.named("runtimeClasspath") {
    val optionalFeatures = listOf("jpaSupport")
    setExtendsFrom(extendsFrom.filterNot { c -> optionalFeatures.any { c.name.startsWith(it) } })
}

dependencies {
    /**
     * api: 컴파일 및 런타임 시 모두 사용함
     * - 해당 의존성이 프로젝트의 빌드 결과물(JAR)에 직접적인 영향을 미침
     * - 이 라이브러리를 사용하는 다른 프로젝트(상위 모듈)에는 의존성이 노출됨 (API 노출)
     * - 런타임에 반드시 필요한 라이브러리인 경우 이 방식을 사용함
     */
    api(project(":s2-core"))

    /**
     * implementation: 컴파일 및 런타임 시 모두 사용함
     * - 해당 의존성이 프로젝트의 빌드 결과물(JAR)에 직접적인 영향을 미침
     * - 이 라이브러리를 사용하는 다른 프로젝트(상위 모듈)에는 의존성이 노출되지 않음 (API 캡슐화)
     * - 런타임에 반드시 필요한 라이브러리인 경우 이 방식을 사용함
     */

    /**
     * compileOnly: 컴파일 시 사용함
     * - 런타임 시 사용하지 않는 라이브러리인 경우 이 방식을 사용함
     * - Shadow JAR 생성 시 포함되지 않음
     */
    compileOnly(libs.jakarta.persistence.api) // JPQL
    "jpaSupportImplementation"(libs.jakarta.persistence.api) // 선택 기능: 배포 POM 에 optional 로 표시(README 안내 블록용 compileOnly 도 유지)

    /**
     * testImplementation: 테스트 컴파일 및 런타임 시 모두 사용
     * - 테스트 코드에서 사용하는 라이브러리인 경우 이 방식을 사용
     */
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.jakarta.persistence.api) // 테스트에서 JPA 사용
    testRuntimeOnly(libs.junit.platform.launcher)
}

/**
 * [Gradle 빌드 최적화 및 태스크 의존성 설정]
 * Gradle 9.x 이상의 엄격한 태스크 유효성 검사를 해결합니다.
 * :s2-validator의 태스크들이 :s2-core의 아티팩트를 사용하므로 명시적 의존성을 부여합니다.
 */
tasks.configureEach {
    if (name == "jar" || name == "shadowJar" || name == "downloadLicenses") {

        // s2-core:jar가 반드시 먼저 완료되도록 순서 보장
        dependsOn(":s2-core:jar")

        // jar 태스크의 경우, 버전 변화에 상관없이 생성된 결과물을 입력 파일로 자동 연결
        if (name == "jar") {
            inputs.file(project(":s2-core").tasks.named<Jar>("jar").map { it.archiveFile })
        }
    }
}

// JPMS 사용자를 위한 고정 모듈 이름(없으면 jar 파일 이름에서 추론돼 파일 이름이 바뀌면 깨짐)
tasks.named<Jar>("jar") {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.devers2.s2util.jpa")
    }
}
