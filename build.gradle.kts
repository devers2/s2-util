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
 * [Version Catalog]
 * 의존성 및 플러그인 버전은 'gradle/libs.versions.toml' 파일에서 통합 관리
 * 별도 설정 없이 Gradle이 기본 경로(gradle/libs.versions.toml)를 자동으로 인식하여 'libs' 접근자로 제공
 */
plugins {
    alias(libs.plugins.s2.build.support)
    id("signing")
    id("java-library")
    id("maven-publish")

    /*
     * ⭐ [Shadow / Relocation 스위치 — 이 alias와 아래 65번째 줄의 shadedPackagePrefix는 반드시 함께 켜고 꺼야 한다]
     * 이 alias만 단독으로 켜면 S2BuildUtils는 이를 사용하지 않는다 (shadowJar 태스크는 등록되지만
     * assemble/publish 파이프라인과 무관하게 방치됨). shadedPackagePrefix까지 함께 설정해야만
     * S2BuildUtils가 shadowJar를 실제 패키징에 편입시킨다.
     *
     * - 둘 다 꺼짐(기본값, 지금 상태): 배포 시 Standard JAR(의존성 미포함, POM으로 전이),
     *   로컬 빌드 시 Fat JAR(runtimeClasspath 전체 병합, relocation 없음) — Shadow 플러그인 미관여.
     * - 둘 다 켜짐: 배포 시 Shaded JAR(implementation/runtimeOnly는 relocate되어 JAR에 포함,
     *   api는 JAR에서 빠지고 POM에만 compile scope로 추가됨),
     *   로컬 빌드 시 Relocated Fat JAR(api 포함 전체를 담되 api를 제외한 나머지만 relocate) — shadowJar 사용.
     *
     * 자세한 4가지 조합 표는 S2BuildUtils 클래스 최상단 Javadoc(Packaging Strategies) 참고.
     */
    // alias(libs.plugins.shadow)
}

import io.github.devers2.buildsupport.S2BuildUtils

/*
 * Maven 좌표 설정 (Root)
 * - group: 프로젝트의 그룹 ID (예: io.github.devers2, com.example)
 * - version: 프로젝트 버전
 *
 * ❗중요: 동일한 소스를 다른 조직/목적으로 배포하는 경우 group을 다르게 설정해야 한다.
 *   예시) 원본: io.github.devers2, 포크: com.company
 *   이렇게 하면 의존성 관리 도구가 서로 다른 아티팩트로 인식하여 같은 리포지토리라도 별도 아티팩트로 취급된다.
 */
group = "io.github.devers2"
version = "1.1.7"

repositories {
    mavenCentral()
    mavenLocal()
}

// Shadow Plugin - Relocation 패키지 설정
// ⚠️ 이 값을 설정해도 위 plugins{} 블록의 shadow alias가 함께 켜져 있지 않으면 아무 효과가 없다 (둘 다 켜야 함).
// extra["shadedPackagePrefix"] = "io.github.devers2.s2util.shaded"

// ========================================================================
// ⭐ [사용자 설정 (User Configuration)]
// 개발자가 프로젝트 상황에 맞춰 자주 변경하거나 확인해야 하는 설정
// ========================================================================

/**
 * 기본 Java 버전 설정: JavaVersion.current() 또는 JavaVersion.VERSION_17, JavaVersion.VERSION_25 등 설정 가능
 * (JavaVersion.current(): 현재 실행 중인 JVM 버전)
 *
 * Java 버전에 따른 의존성 설정
 * JavaVersion.VERSION_10 이하: javax.servlet
 * JavaVersion.VERSION_11 이상: jakarta.servlet
 *
 * Jakarta EE 버전   서블릿 버전     패키지 명칭        일반적으로 사용되는 Java 버전
 * -----------------------------------------------------------------------------
 * Jakarta EE 8      Servlet 4.0    javax.servlet      Java 8, Java 11 등
 * Jakarta EE 9      Servlet 5.0    jakarta.servlet    Java 11, Java 17 등
 * Jakarta EE 10     Servlet 6.0    jakarta.servlet    Java 17, Java 21 등
 */
extra["javaVersion"] = JavaVersion.VERSION_21

/**
 * [배포 바이트코드 타겟 (Release Compatibility)]
 * javaVersion(툴체인 JDK)과 다르게 설정하면, 최신 JDK로 컴파일하면서도 이전 Java 버전과
 * 호환되는 바이트코드를 생성한다 (S2BuildUtils.configureJavaCompatibility 참고).
 * 현재는 Java 21로 컴파일하되 Java 17에서도 실행 가능하도록 17로 고정한다.
 */
extra["releaseCompatibility"] = JavaVersion.VERSION_17

/*
 * [추가 소스 목록]
 * dynamicSourceInfoMap에 정의된 기능 키(예: 'licensesInfo')를 추가하여 관련된 소스 파일 및 라이브러리 의존성을 빌드에 자동으로 포함시킬 수 있다.
 */
extra["activeFeatures"] = setOf("licensesInfo")

/**
 * [동적 기능 소스 정보 (Feature Toggles)]
 * - 특정 기능(Feature)에 포함될 소스 파일과 라이선스 정보 정의
 *
 * 💡 이 설정은 각 서브프로젝트의 build.gradle.kts 에서 모듈별로 관리된다.
 * [사용 예시 - subproject/build.gradle.kts]
 * extra["activeFeatures"] = setOf("S2PdfUtil") // 활성화할 기능 키
 * extra["dynamicSourceInfoMap"] = mapOf(
 *     "S2PdfUtil" to mapOf(
 *         "variantId" to "pdf",
 *         "sources" to listOf("io/github/devers2/s2util/support/S2PdfUtil.java"),
 *         "dependencies" to listOf(
 *             mapOf("configuration" to "compileOnly", "group" to "org.jsoup", "name" to "jsoup", "version" to "1.18.3")
 *         ),
 *         "licenses" to listOf("README-LGPL-2.1-PDF.md")
 *     )
 * )
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

/*
 * 기본 제외 소스 목록 (항상 제외되는 파일들)
 */
extra["excludedSources"] = emptySet<String>()

/**
 * [기준 Java 버전: Artifact ID 생성 기준]
 * javaVersion이 baselineJavaVersion과 다를 경우 아티팩트 ID에 접미사 추가
 */
extra["baselineJavaVersion"] = JavaVersion.VERSION_21



// ========================================================================
// ⭐ [상수 및 환경 설정 (Constants & Environment)]
// 프로젝트 구조나 외부 환경과 관련된 설정 (변경 빈도 낮음)
// ========================================================================

// JAVA_SRC_ROOT는 S2BuildUtils가 .java <-> .java.txt 소스 토글에 읽어가므로 extra로 노출한다.
extra["JAVA_SRC_ROOT"] = "src/main/java"
// RESOURCES_SRC_ROOT는 플러그인이 읽지 않는다 — 아래 subprojects { }가 rootProject.extra로 다시 읽어가는
// 루트→서브프로젝트 값 전달용일 뿐이다 (JAVA_SRC_ROOT와 이름은 짝을 맞췄지만 용도가 다름).
extra["RESOURCES_SRC_ROOT"] = "src/main/resources"

/*
 * [Java 버전 오버라이드]
 * -PtargetJavaVersion=21 옵션으로 덮어쓰기 가능
 */
if (project.hasProperty("targetJavaVersion")) {
    extra["javaVersion"] = JavaVersion.toVersion(project.findProperty("targetJavaVersion")!!)
}

sourceSets {
    main {
        java.setSrcDirs(
            listOf(
                "s2-core/src/main/java",
                "s2-validator/src/main/java",
                "s2-jpa/src/main/java"
            )
        )
        resources.setSrcDirs(
            listOf(
                "s2-core/src/main/resources",
                "s2-validator/src/main/resources",
                "s2-jpa/src/main/resources"
            )
        )
    }
}

dependencies {
    /**
     * api: 컴파일 및 런타임 시 모두 사용함
     * - 해당 의존성이 프로젝트의 빌드 결과물(JAR)에 직접적인 영향을 미침
     * - 이 라이브러리를 사용하는 다른 프로젝트(상위 모듈)에는 의존성이 노출됨 (API 노출)
     * - 런타임에 반드시 필요한 라이브러리인 경우 이 방식을 사용함
     */

    /**
     * implementation: 컴파일 및 런타임 시 모두 사용함
     * - 해당 의존성이 프로젝트의 빌드 결과물(JAR)에 직접적인 영향을 미침
     * - 이 라이브러리를 사용하는 다른 프로젝트(상위 모듈)에는 의존성이 노출되지 않음 (API 캡슐화)
     * - 런타임에 반드시 필요한 라이브러리인 경우 이 방식을 사용함
     */

    /**
     * compileOnly: 컴파일 타임에만 필요하며 런타임에는 선택적
     * - Caffeine Cache는 사용 가능하면 고성능 캐싱을, 없으면 경량 캐시를 사용
     * - 소비자 프로젝트에서 Caffeine을 사용하지 않아도 S2Cache가 정상 동작함
     */
    // s2-core 의존성
    compileOnly(libs.caffeine)
    // s2-validator 의존성
    compileOnly(libs.spring6.context)
    // s2-jpa 의존성
    compileOnly(libs.jakarta.persistence.api)
}

/*
 * [표준 라이브러리 배포 설정 (원콜)]
 * 아래를 한 번에 처리한다: 아티팩트 ID 접미사, 툴체인/source-target 호환성(javaVersion → releaseCompatibility),
 * Javadoc/Sources JAR, "mavenJava" Publication(POM 라이선스/개발자/SCM 포함), CentralPortal 리포지토리 등록(+서명).
 * s2-packages 등 다른 곳에도 배포하려면 별도로 S2BuildUtils.configureGitHubPackagesRepository(...)를 함께 호출한다.
 */
S2BuildUtils.configureLibraryPublishing(
    project,
    project.name,
    "S2Util Library - Unified utility library (Core, Validator, JPA)",
    "https://github.com/devers2/s2-util"
)

// 프로젝트 통합 설정 호출 (루트 프로젝트를 통합 모듈로 취급하여 표준 빌드/배포 규칙 적용)
S2BuildUtils.configureProject(project)

/*
 * [동적 기능 모듈 오버라이드]
 * -PtargetSources=S2PdfUtil,OtherFeature 옵션으로 덮어쓰기 가능
 */
if (project.hasProperty("targetSources")) {
    val targetSourcesVal = project.findProperty("targetSources")
    val sources = targetSourcesVal.toString().split(",")
    extra["activeFeatures"] = sources.map { it.trim() }.toSet()
}

// 저작권 연도 업데이트 (수정이 필요한 경우에만 파일 IO 발생)
S2BuildUtils.updateCopyright(project, arrayOf("README.md"))


// ========================================================================
// ⭐ [서브 프로젝트 구성] ※ subprojects { ... } 안에서의 project 는 서브 프로젝트를 가리킨다.
// ========================================================================
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    // 인코딩(UTF-8), Gradle 버전 정합성 검사, -parameters 컴파일러 옵션 등
    // 범용 컨벤션을 서브 프로젝트에도 적용하기 위해 s2-build-support 플러그인을 직접 적용한다.
    // (plugins{} 블록은 루트 프로젝트에만 적용되므로, 서브 프로젝트는 별도로 apply해야 한다.)
    apply(plugin = rootProject.libs.plugins.s2.build.support.get().pluginId)
    if (project.name != "s2-validator-plugin") {
        apply(plugin = "signing")
    }

    if (rootProject.plugins.hasPlugin("com.github.johnrengelman.shadow")) {
        // 💡 루트 프로젝트에 shadow 플러그인이 실제로 apply 되어 있을 때만
        // 서브 프로젝트 별 Shadow 플러그인을 적용한다. (루트의 버전 카탈로그에서 확인한 ID 사용).
        // s2-validator-plugin 은 Gradle 플러그인으로 배포되므로 제외한다.
        if (project.name != "s2-validator-plugin") {
            apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)
        }
    }

    group = rootProject.group
    if (version == "unspecified") {
        // 서브 프로젝트에 버전이 정의되어 있지 않은 경우 루트 버전을 사용
        version = rootProject.version
    }

    repositories {
        mavenCentral()
    }

    val subJavaSrcRoot = rootProject.extra["JAVA_SRC_ROOT"] as String
    val subResourcesSrcRoot = rootProject.extra["RESOURCES_SRC_ROOT"] as String

    configure<SourceSetContainer> {
        named("main") {
            // 서브 프로젝트 디렉터리를 기준으로 'src/main/java'를 정의한다.
            java.setSrcDirs(listOf(subJavaSrcRoot))
            resources.setSrcDirs(listOf(subResourcesSrcRoot))
        }
    }

    // 프로젝트 통합 설정
    S2BuildUtils.configureProject(project)

    // 저작권 연도 업데이트 (수정이 필요한 경우에만 파일 IO 발생)
    S2BuildUtils.updateCopyright(
        project,
        arrayOf(
            subJavaSrcRoot,
            "src/main/kotlin",
            "src/main/python",
            subResourcesSrcRoot,
            "src/main/webapp",
            "README.md",
            "build.gradle.kts"
        )
    )

    if (project.name != "s2-validator-plugin") {
        /*
         * [표준 라이브러리 배포 설정 (원콜)]
         * 아티팩트 ID 접미사, 툴체인/호환성, Javadoc/Sources JAR, "mavenJava" Publication(POM 포함),
         * CentralPortal 리포지토리 등록(+서명)을 한 번에 처리한다.
         * s2-validator-plugin은 자체 pluginMaven/marker Publication 체계를 쓰므로 제외하고,
         * 자신의 build.gradle.kts에서 필요한 것만 개별 호출한다.
         */
        S2BuildUtils.configureLibraryPublishing(
            project,
            project.name,
            "S2Util Library - ${project.name} module",
            "https://github.com/devers2/s2-util"
        )
    }

    // JUnit 5(Jupiter) 플랫폼 사용 + 테스트 JVM 인코딩 강화 (S2BuildUtils.configureTestDefaults)
    S2BuildUtils.configureTestDefaults(project)

    // 'Tasks → other → copyDependencies' 실행 시 지정 디렉토리로 의존성 복사
    S2BuildUtils.registerCopyDependenciesTask(project)
}

// --------------------------------------------------------------------------------------
// 루트프로젝트 README 파일 버전 & 의존성 가이드 업데이트
// --------------------------------------------------------------------------------------
S2BuildUtils.updateReadmeWithVersionAndDependencies(project, file("README.md"))
