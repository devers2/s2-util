/*
 * [S2 프로젝트 패키징 규칙 검증 스크립트]
 *
 * 이 스크립트는 S2BuildUtils를 통해 구현된 4가지 패키징 전략이
 * 의도한 대로 동작하는지 자동으로 테스트하기 위해 작성되었습니다.
 *
 * [검증 항목]
 * 1. Standard JAR: 배포 모드, Prefix 없음 -> 외부 의존성 포함 안 됨 (POM 전이)
 * 2. Shaded JAR: 배포 모드, Prefix 설정 -> 외부 의존성 Shaded(Relocate) 포함
 * 3. Fat JAR: 빌드 모드, Prefix 없음 -> 모든 의존성 포함 (Relocation 없음)
 *
 * [사용 방법]
 * ※ 루트 프로젝트 build.gradle.kts 의 'shadow' 플러그인을 적용한 후 아래 명령어를 실행합니다.
 * ./gradlew -I verification.gradle.kts verifyPackaging
 */
import java.util.zip.ZipFile

data class VerificationResult(val name: String, val success: Boolean, val msg: String?)

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
                    val argList = (if (isWindows) listOf("cmd", "/c", gradlewPath) else listOf(gradlewPath)) +
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
                fun record(name: String, success: Boolean, msg: String?) {
                    results.add(VerificationResult(name, success, msg))
                    if (success) logger.lifecycle("✅ SUCCESS: $name")
                    else logger.lifecycle("❌ FAILURE: $name - $msg")
                }

                // Temporary variables for checks
                var checkName: String
                var failedMsg: String?

                // 1. Verify Publishing -> Standard JAR (No Prefix)
                checkName = "Standard JAR (Publishing, No Prefix)"
                logger.lifecycle("\n--- 1. Testing $checkName ---")
                failedMsg = null
                try {
                    val exitCode1 = runGradle(
                        listOf(":$artifactId:clean", ":$artifactId:publishToMavenLocal", "-PshadedPackagePrefix=", "--no-daemon")
                    )
                    if (exitCode1 != 0) {
                        failedMsg = "Build failed"
                    } else {
                        val standardJar = project.file("$buildDirPath/$artifactId-$version.jar")
                        if (!standardJar.exists()) {
                            failedMsg = "JAR not found"
                        } else {
                            ZipFile(standardJar).use { zip ->
                                val entries = zip.entries().toList()
                                val hasDeps = entries.any { it.name.contains("com/google/common") || it.name.contains("org/apache/commons") }
                                val hasReloc = entries.any { it.name.contains("io/github/devers2/s2util/shaded") }
                                val hasReadme = entries.any { it.name == "README.md" }

                                logger.lifecycle("   - Has Dependencies: $hasDeps (Should be FALSE)")
                                logger.lifecycle("   - Has Relocation: $hasReloc (Should be FALSE)")
                                logger.lifecycle("   - Has README.md: $hasReadme (Should be TRUE)")

                                failedMsg = when {
                                    hasDeps -> "Contains dependencies"
                                    hasReloc -> "Has unexpected relocation"
                                    !hasReadme -> "Missing README.md"
                                    else -> null
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    failedMsg = "Exception: ${e.message}"
                }
                record(checkName, failedMsg == null, failedMsg)


                // 2. Verify Publishing -> Shaded JAR (With Prefix)
                checkName = "Shaded JAR (Publishing, With Prefix)"
                logger.lifecycle("\n--- 2. Testing $checkName ---")
                failedMsg = null
                try {
                    val exitCode2 = runGradle(
                        listOf(":$artifactId:clean", ":$artifactId:publishToMavenLocal", "-PshadedPackagePrefix=io.github.devers2.s2util.shaded", "--no-daemon")
                    )
                    if (exitCode2 != 0) {
                        failedMsg = "Build failed"
                    } else {
                        val shadedJar = project.file("$buildDirPath/$artifactId-$version.jar")
                        if (!shadedJar.exists()) {
                            failedMsg = "JAR not found"
                        } else {
                            ZipFile(shadedJar).use { zip ->
                                val entries = zip.entries().toList()
                                val hasReloc = entries.any {
                                    it.name.contains("io/github/devers2/s2util/shaded/com") || it.name.contains("io/github/devers2/s2util/shaded/org")
                                }

                                logger.lifecycle("   - Has Relocated Dependencies: $hasReloc (Should be TRUE)")

                                if (!hasReloc) failedMsg = "Missing relocated dependencies"
                            }
                        }
                    }
                } catch (e: Exception) {
                    failedMsg = "Exception: ${e.message}"
                }
                record(checkName, failedMsg == null, failedMsg)


                // 3. Verify Build -> Fat JAR (No Prefix)
                checkName = "Fat JAR (Build, No Prefix)"
                logger.lifecycle("\n--- 3. Testing $checkName ---")
                failedMsg = null
                try {
                    val exitCode3 = runGradle(
                        listOf(":$artifactId:clean", ":$artifactId:shadowJar", "-PshadedPackagePrefix=", "--no-daemon")
                    )
                    if (exitCode3 != 0) {
                        failedMsg = "Build failed"
                    } else {
                        var fatJar = project.file("$buildDirPath/$artifactId-$version-all.jar")
                        if (!fatJar.exists()) fatJar = project.file("$buildDirPath/$artifactId-$version.jar")

                        ZipFile(fatJar).use { zip ->
                            val entries = zip.entries().toList()
                            val hasDeps = entries.any { it.name.contains("com/google/common") || it.name.contains("org/apache/commons") }
                            val hasReloc = entries.any { it.name.contains("io/github/devers2/s2util/shaded") }

                            logger.lifecycle("   - Has Dependencies: $hasDeps (Should be TRUE)")
                            logger.lifecycle("   - Has Relocation: $hasReloc (Should be FALSE)")

                            failedMsg = when {
                                !hasDeps -> "Missing dependencies"
                                hasReloc -> "Unexpected relocation"
                                else -> null
                            }
                        }
                    }
                } catch (e: Exception) {
                    failedMsg = "Exception: ${e.message}"
                }
                record(checkName, failedMsg == null, failedMsg)


                // 4. Verify Build -> Shaded Fat JAR (With Prefix)
                checkName = "Shaded Fat JAR (Build, With Prefix)"
                logger.lifecycle("\n--- 4. Testing $checkName ---")
                failedMsg = null
                try {
                    val exitCode4 = runGradle(
                        listOf(":$artifactId:clean", ":$artifactId:shadowJar", "-PshadedPackagePrefix=io.github.devers2.s2util.shaded", "--no-daemon")
                    )
                    if (exitCode4 != 0) {
                        failedMsg = "Build failed"
                    } else {
                        var fatShadedJar = project.file("$buildDirPath/$artifactId-$version-all.jar")
                        if (!fatShadedJar.exists()) fatShadedJar = project.file("$buildDirPath/$artifactId-$version.jar")

                        ZipFile(fatShadedJar).use { zip ->
                            val entries = zip.entries().toList()
                            val hasReloc = entries.any {
                                it.name.contains("io/github/devers2/s2util/shaded/com") || it.name.contains("io/github/devers2/s2util/shaded/org")
                            }

                            logger.lifecycle("   - Has Relocated Dependencies: $hasReloc (Should be TRUE)")

                            if (!hasReloc) failedMsg = "Missing relocated dependencies"
                        }
                    }
                } catch (e: Exception) {
                    failedMsg = "Exception: ${e.message}"
                }
                record(checkName, failedMsg == null, failedMsg)


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
