// Spring 7 호환 확인용 Gradle init 스크립트(.github/workflows/spring7-compat.yml 이 -I 로 넘김).
// :s2-validator 의 시험 "실행" 클래스패스만 환경 변수로 받은 Spring 7·Boot 4·Jakarta EE 11 계열 버전으로 바꾼다.
// main·시험 코드는 그대로 Spring 6 으로 컴파일되므로, 사용자가 Spring 6 기준 배포 jar 를 Spring 7 위에서 실행하는 상황과 같다.
// 빌드 스크립트·버전 카탈로그를 건드리지 않아 배포 산출물과 dependabot 에 영향이 없다.
val compatVersions: Map<String, String> = mapOf(
    "org.springframework" to "COMPAT_SPRING",
    "org.springframework.boot" to "COMPAT_SPRING_BOOT",
    "org.hibernate.validator" to "COMPAT_HIBERNATE_VALIDATOR",
    "org.glassfish.expressly" to "COMPAT_EXPRESSLY",
    "jakarta.validation" to "COMPAT_JAKARTA_VALIDATION",
).mapNotNull { (group, env) -> System.getenv(env)?.takeIf { it.isNotBlank() }?.let { group to it } }.toMap()

allprojects {
    if (path == ":s2-validator") {
        configurations.matching { it.name == "testRuntimeClasspath" }.configureEach {
            resolutionStrategy.eachDependency {
                compatVersions[requested.group]?.let { useVersion(it) }
            }
        }
    }
}
