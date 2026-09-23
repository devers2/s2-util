# S2Util AI 에이전트 개발 및 검증 절대 원칙 (AGENTS.md)

이 문서는 `s2-util` 저장소의 코드를 분석, 수정, 생성하는 모든 AI 에이전트가 준수해야 할 **절대 원칙 및 검증 가이드라인**입니다.

---

## 🚨 [CRITICAL RULE] 코드 수정 후 전체 빌드 및 검증 필수 실행

Java 소스 코드, JavaScript 검증기(`s2.validator.js`), Gradle 설정, 문서를 수정한 후에는 **사용자에게 완료를 보고하거나 커밋/푸시하기 전에 반드시 아래 검증 명령어를 실행하여 전수 통과(100% PASS)를 확인**해야 합니다.

### 필수 실행 명령어
```bash
./gradlew check
```

> ⚠️ 개별 임의 테스트만 실행하고 끝내지 마십시오. 반드시 `./gradlew check`를 통해 전체 서브프로젝트(`s2-core`, `s2-jpa`, `s2-validator`, `s2-validator-plugin`)의 모든 단위/통합/스모크 테스트가 100% 통과함을 검증해야 합니다.

---

## 1. 서버-클라이언트 규칙 일관성 절대 원칙 (Write Once, Validate Anywhere)

`s2-validator`는 서버(Java)와 클라이언트(JavaScript)가 동일한 규칙으로 일관되게 검증되는 것을 핵심 가치로 합니다.

1. **신규 룰(`S2RuleType`) 추가 시**:
   - `s2-validator/src/main/resources/META-INF/resources/s2-util/js/s2.validator.js`의 `validateCheck()`에 해당 룰에 대한 검증 로직을 반드시 구현해야 합니다.
   - `ServerClientParityTest.testCases()`에 신규 룰의 성공/실패/빈값 판정 테스트 케이스를 반드시 추가해야 합니다.
   - `ServerClientParityTest.testAllRuleTypesHaveParityTests()`가 누락을 자동 감지하므로, 테스트 누락 시 빌드가 실패합니다.
2. **정규식 일치성 및 예외 격리**:
   - Java의 `matcher.matches()`와 JavaScript의 `RegExp.test()`가 동일하게 전체 일치(full match)로 동작해야 합니다.
   - JavaScript 검증기에서 정규식 평가 시 구문 오류(`SyntaxError`)가 발생하더라도 submit 리스너가 중단되지 않도록 `try/catch`로 예외를 안전하게 격리(fail-closed)해야 합니다.
3. **숫자 형 변환 호환성**:
   - `MIN_VALUE`, `MAX_VALUE` 등 숫자 규칙은 DTO의 `Number`뿐 아니라 폼 및 `Map` 바인딩에서 들어오는 숫자 문자열(`"25"`)과 문자열 기준값(`"19"`)도 정상적으로 비교되어야 합니다.
4. **교차 필드 메시지 가독성**:
   - `EQUALS_FIELD`, `DATE_AFTER`, `DATE_BEFORE`의 에러 메시지는 내부 영문 필드명(`pw`, `startDate`)이 아닌 대상 필드의 논리 라벨(`비밀번호`, `시작일`)로 치환되어야 합니다.

---

## 2. 커스텀 람다 규칙 빈 값 및 예외 처리 원칙

1. **빈 값(null/empty) 단락(Short-circuit)**:
   - 커스텀 람다 규칙은 기본적으로 대상 값이 비어있으면(`S2Util.isEmpty(value)`) 실행되지 않고 통과합니다 (NPE 방지).
   - 빈 값 상태에서도 검증이 필요한 특수 규칙(예: 두 필드 중 하나 필수)은 반드시 `.includeEmpty()`를 명시해야 합니다.
2. **비밀번호 확인 등 연관 필드 검증**:
   - 확인 필드가 비었을 때 통과되는 것을 방지하기 위해 반드시 `.rule(REQUIRED)`를 선행하고 커스텀 람다를 연결하십시오.
3. **람다 예외 전파**:
   - 커스텀 람다 내부에서 발생한 런타임 예외는 삼키지 말고 필드명을 포함한 `S2RuntimeException`으로 감싸서 전파해야 합니다.
4. **서버 전용 검증 원칙 (Server-Only Rule)**:
   - 커스텀 람다 규칙(`Predicate`, `BiPredicate`)은 JVM 메모리 상의 바이트코드 객체이므로 클라이언트(`s2.validator.js`)로 JSON 직렬화되지 않으며, **오직 서버 사이드 검증 시에만 동작**합니다.
   - 클라이언트와 서버 양쪽에서 동일(교차)하게 검증되어야 하는 규칙은 람다 대신 `S2RuleType.REGEX` 또는 내장 규칙을 사용해야 합니다.

---

## 3. 전역 상태 격리 및 인스턴스 패턴 우선

1. **검증기 생성 권장 경로**:
   - 전역 레지스트리(`S2ValidatorFactory`) 캐시 충돌을 방지하기 위해 `S2BindValidator.of(validator)` 인스턴스 직접 전달 방식을 기본 권장합니다.
2. **전역 상태 초기화**:
   - 단위 테스트 간 상태 격리를 위해 필요 시 `S2Validator.resetAll()`을 호출하십시오.

---

## 4. 버전 관리 및 문서 동기화 원칙

1. **버전 변경**:
   - `build.gradle.kts`의 버전만 변경하십시오.
   - 빌드 플러그인(`S2BuildUtils`)이 `./gradlew test` 실행 시 모든 `README`, `GUIDE`, `MANUAL`의 버전 및 의존성 코드 블록을 자동으로 동기화합니다.
2. **배포 안전성**:
   - 사용자가 명시적으로 배포를 요청하지 않는 한 임의로 배포 태스크(`publish`, `publishToCentralPortal` 등)를 실행하지 마십시오.
   - 평상시에는 `git commit` 및 `git push`만 수행합니다.

---

## 5. Java 버전 호환성 원칙 (Java 17 Baseline & Java 21+ Dynamic Feature)

1. **기본 호환성 (Java 17 Baseline)**:
   - 모든 기본 소스코드는 Java 17 바이트코드 타깃(`--release 17`) 호환을 기준으로 작성해야 합니다.
   - Java 21+ 전용 문법이나 신규 API(예: `Thread.ofVirtual()`, `Thread.startVirtualThread()`, Sequenced Collections `getFirst()` 등)를 소스에 직접 하드코딩해서는 안 됩니다.
2. **Java 21+ 기능 사용 원칙 (가상 스레드 독점 예외)**:
   - 가상 스레드(Virtual Thread)는 `s2-core`의 `S2ThreadUtil`에서만 유일하게 Java 21+ 동적 기능으로 지원합니다.
   - Java 21+ 신규 기능을 활용할 때는 반드시 `S2ThreadUtil`과 같이 **MethodHandle/리플렉션을 통한 런타임 동적 감지** 방식을 취해야 합니다.
   - 실행 환경이 Java 17인 경우를 대비하여 **안전한 대체 로직(Fallback: 플랫폼 스레드 풀 등)**을 필수적으로 함께 제공해야 합니다.

---

## 6. 인코딩 절대 원칙: UTF-8 NoBOM

- 저장소의 모든 소스 파일, 설정 파일, 마크다운 문서는 순수 UTF-8 NoBOM(Byte Order Mark 없음)이어야 합니다.

