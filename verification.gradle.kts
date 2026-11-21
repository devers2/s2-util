/*
 * [S2 프로젝트 패키징 규칙 검증 스크립트]
 *
 * 이 스크립트는 S2BuildUtils를 통해 구현된 4가지 패키징 전략이
 * 의도한 대로 동작하는지 자동으로 테스트하기 위해 작성되었습니다.
 *
 * [검증 항목]
 * 1. Standard JAR: 배포 모드, Prefix 없음 -> 의존성 클래스 포함 안 됨 (POM 전이)
 * 2. Shaded JAR: 배포 모드, Prefix 설정 -> implementation 의존성은 재배치되어 포함, api(s2-core)는 빠지고 POM 에 compile 로 남음
 * 3. Fat JAR: 빌드 모드, Prefix 없음 -> 모든 의존성 포함 (Relocation 없음)
 * 4. Shaded Fat JAR: 빌드 모드, Prefix 설정 -> 모든 의존성 포함, api(s2-core)를 뺀 나머지만 재배치
 *
 * [검증 전용 의존성]
 * s2-validator 의 외부 의존성은 모두 compileOnly 라 재배치·Fat JAR 를 확인할 대상이 없다. 그래서 이 스크립트가 띄우는
 * 하위 실행에서만 VERIFY_DEPENDENCY 를 s2-validator 의 implementation 으로 넣는다 (일반 빌드에는 영향 없음).
 *
 * [사용 방법]
 * ※ 루트 프로젝트 build.gradle.kts 의 'shadow' 플러그인을 적용한 후 아래 명령어를 실행합니다.
 * ./gradlew clean -I verification.gradle.kts verifyPackaging
 * (끝나면 ~/.m2/repository/io/github/devers2/s2-validator 의 검증용 산출물을 지운다)
 */
import java.util.zip.ZipFile

val verifyDependency = "org.apache.commons:commons-lang3:3.17.0"

// 하위 실행(-Ps2.verify.dependency=...)에서만 s2-validator 에 검증 전용 implementation 의존성을 넣는다
gradle.startParameter.projectProperties["s2.verify.dependency"]?.let { dependency ->
    gradle.allprojects {
        if (name == "s2-validator") {
            pluginManager.withPlugin("java") { dependencies.add("implementation", dependency) }
        }
    }
}

data class VerificationResult(
    val name: String,
    val success: Boolean,
    val msg: String?,
)

gradle.projectsEvaluated {
    gradle.rootProject {
        tasks.register("verifyPackaging") {
            doLast {
                logger.lifecycle("Starting Packaging Verification (ProcessBuilder mode)...")

                val artifactId = "s2-validator"
                val version = project.version.toString()
                val buildDirPath = project.file("s2-validator/build/libs")

                val isWindows = System.getProperty("os.name").lowercase().contains("windows")
                val gradlewName = if (isWindows) "gradlew.bat" else "./gradlew"
                val gradlewPath = File(project.projectDir, gradlewName).absolutePath

                val scriptPath = project.file("verification.gradle.kts").absolutePath
                val runGradle: (List<String>) -> Int = { args ->
                    // 내부 실행 시에도 이 스크립트를 적용하여 shadow 플러그인 주입을 유지합니다.
                    val argList =
                        (if (isWindows) listOf("cmd", "/c", gradlewPath) else listOf(gradlewPath)) +
                            listOf("-I", scriptPath) +
                            args
                    logger.lifecycle("   🚀 [Exec] ${argList.joinToString(" ")}")

                    val pb = ProcessBuilder(argList)
                    pb.directory(project.projectDir)
                    pb.redirectErrorStream(true)
                    val process = pb.start()

                    process.inputStream.bufferedReader().forEachLine { line ->
                        logger.lifecycle("      > $line")
                    }

                    process.waitFor()
                }

                val results = mutableListOf<VerificationResult>()

                // Helper to record result
                fun record(
                    name: String,
                    success: Boolean,
                    msg: String?,
                ) {
                    results.add(VerificationResult(name, success, msg))
                    if (success) {
                        logger.lifecycle("✅ SUCCESS: $name")
                    } else {
                        logger.lifecycle("❌ FAILURE: $name - $msg")
                    }
                }

                // jar 안에서 찾을 경로: s2-core(api), 검증 의존성(implementation), 재배치된 검증 의존성
                val corePath = "io/github/devers2/s2util/core/"
                val dependencyPath = "org/apache/commons/lang3/"
                val shadedDependencyPath = "io/github/devers2/s2util/shaded/org/apache/commons/lang3/"
                val shadedPrefix = "io/github/devers2/s2util/shaded/"

                // 기대값: 이름 → (jar 항목 중 이 경로로 시작하는 것이 있어야 하는가)
                fun check(
                    number: Int,
                    name: String,
                    task: String,
                    prefix: String,
                    expected: Map<String, Boolean>,
                    extra: (() -> String?)? = null,
                ) {
                    // 배포(publishToMavenLocal)는 배포되는 기본 jar 를, 빌드(shadowJar)는 -all.jar 가 있으면 그것을 본다
                    val preferAll = task == "shadowJar"
                    logger.lifecycle("\n--- $number. Testing $name ---")
                    var failedMsg: String? = null
                    try {
                        val exitCode =
                            runGradle(
                                listOf(
                                    ":$artifactId:clean",
                                    ":$artifactId:$task",
                                    "-PshadedPackagePrefix=$prefix",
                                    "-Ps2.verify.dependency=$verifyDependency",
                                    "--no-daemon",
                                ),
                            )
                        if (exitCode != 0) {
                            failedMsg = "Build failed"
                        } else {
                            var jar = project.file("$buildDirPath/$artifactId-$version.jar")
                            val allJar = project.file("$buildDirPath/$artifactId-$version-all.jar")
                            if (preferAll && allJar.exists()) jar = allJar
                            if (!jar.exists()) {
                                failedMsg = "JAR not found"
                            } else {
                                val names = ZipFile(jar).use { zip -> zip.entries().toList().map { it.name } }
                                logger.lifecycle("   - JAR: ${jar.name}")
                                for ((path, shouldHave) in expected) {
                                    val has = names.any { it.startsWith(path) }
                                    logger.lifecycle("   - $path: $has (Should be ${shouldHave.toString().uppercase()})")
                                    if (has != shouldHave && failedMsg == null) {
                                        failedMsg = (if (shouldHave) "Missing " else "Unexpected ") + path
                                    }
                                }
                                if (failedMsg == null) failedMsg = extra?.invoke()
                            }
                        }
                    } catch (e: Exception) {
                        failedMsg = "Exception: ${e.message}"
                    }
                    record(name, failedMsg == null, failedMsg)
                }

                val prefix = "io.github.devers2.s2util.shaded"
                val pomFile = project.file("s2-validator/build/publications/mavenJava/pom-default.xml")

                // 1. Verify Publishing -> Standard JAR (No Prefix)
                check(
                    1, "Standard JAR (Publishing, No Prefix)", "publishToMavenLocal", "",
                    mapOf(corePath to false, dependencyPath to false, shadedPrefix to false),
                ) {
                    val hasReadme = project.file("$buildDirPath/$artifactId-$version.jar").let { jar ->
                        ZipFile(jar).use { zip -> zip.getEntry("README.md") != null }
                    }
                    logger.lifecycle("   - Has README.md: $hasReadme (Should be TRUE)")
                    if (hasReadme) null else "Missing README.md"
                }

                // 2. Verify Publishing -> Shaded JAR (With Prefix)
                check(
                    2, "Shaded JAR (Publishing, With Prefix)", "publishToMavenLocal", prefix,
                    mapOf(shadedDependencyPath to true, dependencyPath to false, corePath to false),
                ) {
                    // POM: api(s2-core)만 compile 로 남고, 재배치되어 들어간 implementation 의존성은 없어야 한다
                    val pom = if (pomFile.exists()) pomFile.readText() else ""
                    val hasCore = pom.contains("<artifactId>s2-core</artifactId>")
                    val hasDependency = pom.contains("<artifactId>commons-lang3</artifactId>")
                    logger.lifecycle("   - POM has s2-core: $hasCore (Should be TRUE)")
                    logger.lifecycle("   - POM has commons-lang3: $hasDependency (Should be FALSE)")
                    when {
                        !hasCore -> "POM is missing s2-core"
                        hasDependency -> "POM still has the relocated dependency"
                        else -> null
                    }
                }

                // 3. Verify Build -> Fat JAR (No Prefix)
                check(
                    3, "Fat JAR (Build, No Prefix)", "shadowJar", "",
                    mapOf(corePath to true, dependencyPath to true, shadedPrefix to false),
                )

                // 4. Verify Build -> Shaded Fat JAR (With Prefix)
                check(
                    4, "Shaded Fat JAR (Build, With Prefix)", "shadowJar", prefix,
                    mapOf(shadedDependencyPath to true, dependencyPath to false, corePath to true),
                )

                // Summary
                logger.lifecycle("\n========================================")
                logger.lifecycle("           TEST SUMMARY")
                logger.lifecycle("========================================")

                val successCount = results.count { it.success }
                val totalCount = results.size

                logger.lifecycle("Total Tests: $totalCount")
                logger.lifecycle("Success: $successCount")
                logger.lifecycle("Failed: ${totalCount - successCount}")

                if (successCount != totalCount) {
                    logger.lifecycle("\nFAILED TESTS:")
                    results.filter { !it.success }.forEach {
                        // Using simple indicators since raw ANSI might not always render depending on console
                        logger.error("❌ [FAILED] ${it.name}")
                        logger.error("   Reason: ${it.msg}")
                    }
                    throw GradleException("Verification failed! ${totalCount - successCount} tests failed.")
                } else {
                    logger.lifecycle("\n🎉 All verification tests passed!")
                }
            }
        }
    }
}
