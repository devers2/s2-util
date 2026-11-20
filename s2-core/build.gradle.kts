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

/*
 * 빌드 완료 후 결과물을 테스트할 클래스 지정 (shadowJar 실행 후 testArtifact 태스크로 수행됨, 여러 개 지정 가능)
 */
extra["artifactTestClassNames"] = emptyList<String>()

// Test-only: slf4j-api for the isolated SLF4J bridge test | 시험 전용: 격리된 SLF4J 브리지 시험용 slf4j-api
val slf4jBridgeTest: Configuration = configurations.create("slf4jBridgeTest")

// 시험 전용: caffeineTest 태스크에만 넣는 Caffeine (기본 test 는 Caffeine 없이 경량 캐시로 돈다)
val caffeineTestRuntime: Configuration = configurations.create("caffeineTestRuntime")


// 선택 기능 정의 — main 소스셋을 그대로 쓰고 배포 POM 에 optional 의존성으로 표시(소비자에게 전이되지 않음)
java {
    registerFeature("caffeineSupport") {
        usingSourceSet(sourceSets["main"])
    }
}

// main 소스셋을 쓰는 선택 기능은 Gradle 이 그 의존성을 runtimeClasspath 에도 연결한다. 로컬 빌드의 Fat JAR(S2BuildUtils)가
// runtimeClasspath 를 jar 에 합치므로, 그대로 두면 선택 의존성(Spring 등) 전체가 jar 에 들어가 소비자 클래스패스에서 중복된다
// (2.1.0 까지는 compileOnly 라 들어가지 않았음). 배포 POM·모듈 메타데이터의 optional 표시는 *RuntimeElements 에서 오므로 영향 없음
// 기본 test 도 Caffeine 없이(경량 캐시로) 돌아야 한다 — Gradle 은 선택 기능을 시험 클래스패스에도 연결하므로 함께 뗀다.
// Caffeine 백엔드는 caffeineTest 태스크가 따로 시험한다
listOf("runtimeClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name) {
        setExtendsFrom(extendsFrom.filterNot { it.name.startsWith("caffeineSupport") })
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
    compileOnly(libs.caffeine) // Caffeine: 고성능 로컬 캐시 라이브러리 (선택적)
    "caffeineSupportImplementation"(libs.caffeine) // 선택 기능: 배포 POM 에 optional 로 표시(README 안내 블록용 compileOnly 도 유지)

    /**
     * testImplementation: 테스트 컴파일 및 런타임 시 모두 사용
     * - 테스트 코드에서 사용하는 라이브러리인 경우 이 방식을 사용
     * - 테스트 시에는 Caffeine을 포함하여 전체 기능을 테스트함
     */
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Caffeine 백엔드 검증: 시험 코드는 Caffeine 으로 컴파일하되(testCompileOnly), 실행은 기본 test 에서는 빼고
    // caffeineTest 태스크에서만 넣는다. S2Cache 는 클래스패스의 Caffeine 유무로 백엔드를 고르므로 두 백엔드를 각각 시험하기 위함
    testCompileOnly(libs.caffeine)
    caffeineTestRuntime(libs.caffeine)

    slf4jBridgeTest(libs.slf4j.api)
}

// Pass slf4j-api to the SLF4J bridge test as a jar path, not on the test class path, so other tests keep running without SLF4J. | SLF4J 브리지 시험에 slf4j-api 를 시험 클래스패스가 아닌 jar 경로로 넘겨, 다른 시험은 계속 SLF4J 없이 실행되도록 함
tasks.named<Test>("test") {
    val slf4jJars: FileCollection = slf4jBridgeTest
    inputs.files(slf4jJars).withPropertyName("slf4jBridgeTestJars")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Ds2.test.slf4jApiJar=" + slf4jJars.singleFile.absolutePath)
    })
}

// 같은 시험을 Caffeine 이 클래스패스에 있는 상태로 한 번 더 실행한다(S2Cache·S2Util 의 Caffeine 백엔드 경로 검증).
// 시험은 s2.test.cacheBackend=caffeine 으로 이 실행을 알아보고, Caffeine 전용 시험은 이때만 돈다.
val caffeineTest by tasks.registering(Test::class) {
    description = "Runs the s2-core tests with Caffeine on the class path (Caffeine cache backend)."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    val testSourceSet = sourceSets["test"]
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath + caffeineTestRuntime
    systemProperty("s2.test.cacheBackend", "caffeine")
    val slf4jJars: FileCollection = slf4jBridgeTest
    inputs.files(slf4jJars).withPropertyName("slf4jBridgeTestJars")
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-Ds2.test.slf4jApiJar=" + slf4jJars.singleFile.absolutePath)
    })
    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(caffeineTest)
}

// JPMS 사용자를 위한 고정 모듈 이름(없으면 jar 파일 이름에서 추론돼 파일 이름이 바뀌면 깨짐)
tasks.named<Jar>("jar") {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.devers2.s2util.core")
    }
}
