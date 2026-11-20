# s2-util AI 에이전트 원칙

## 필수: 수정 후 전체 검증
Java·`s2.validator.js`·Gradle 설정·문서를 수정했으면 완료 보고·커밋·push 전에 `./gradlew check` 전수 통과를 확인한다.
개별 테스트만으로 끝내지 않는다(`s2-core`, `s2-jpa`, `s2-validator`, `s2-validator-plugin` 전부).

## 1. 서버·클라이언트 규칙 일치 (s2-validator)
- 새 `S2RuleType` 은 `s2-validator/src/main/resources/META-INF/resources/s2-util/js/s2.validator.js` 의 `validateCheck()` 구현과
  `ServerClientParityTest.testCases()` 의 성공·실패·빈값 케이스를 함께 추가한다(누락 시 `testAllRuleTypesHaveParityTests()` 가 빌드 실패).
- 정규식: Java `matcher.matches()` 와 JS `RegExp.test()` 모두 전체 일치. JS 정규식 오류는 `try/catch` 로 격리해 실패(fail-closed) 처리하고 submit 리스너가 멈추지 않게 한다.
- `MIN_VALUE`·`MAX_VALUE` 등 숫자 규칙은 `Number` 뿐 아니라 숫자 문자열 값(`"25"`)·문자열 기준값(`"19"`)도 비교되어야 한다.
- `EQUALS_FIELD`·`DATE_AFTER`·`DATE_BEFORE` 메시지는 대상 필드의 영문 이름이 아닌 라벨(`비밀번호`, `시작일`)로 치환한다.
- `s2.validator.js` 의 DOM 처리(오류 표시, 히든 필드 앵커, 렌더러, `reindex`)를 바꿨으면 `s2-validator/browser-test/run.sh`(Playwright)도 통과시킨다.
  - 시스템 라이브러리 설치에 sudo 가 필요하다. 에이전트는 sudo 를 실행하지 말고, 라이브러리가 없다고 나오면 사용자에게 `! s2-validator/browser-test/run.sh` 실행을 요청한다.
  - 이 시험은 `./gradlew check` 에 없고, GitHub Actions `Browser tests (s2.validator.js)` 가 JS·`browser-test/` 변경 때만 실행한다.
  - `BrowserDemoRules.java` 를 바꾸면 `./gradlew :s2-validator:writeBrowserDemoRules` 로 `demo/rules.json` 을 다시 만든다.

## 2. 커스텀 람다 규칙
- 값이 비면(`S2Util.isEmpty`) 실행하지 않고 통과한다. 빈 값도 검사해야 하면(예: 둘 중 하나 필수) `.includeEmpty()` 를 붙인다.
- 비밀번호 확인 같은 연관 필드는 `.rule(REQUIRED)` 를 먼저 두고 람다를 잇는다(빈 확인 필드 통과 방지).
- 람다 안의 런타임 예외는 삼키지 말고 필드명을 담은 `S2RuleExecutionException` 으로 감싸 전파한다.
- 람다 규칙은 JSON 으로 직렬화되지 않아 서버에서만 동작한다. 양쪽에서 검증할 규칙은 `S2RuleType.REGEX` 나 내장 규칙을 쓴다.

## 3. 전역 상태
- 전역 검증기 등록부(`S2ValidatorFactory`, `S2BindValidator.context`)는 2.0.0 에서 삭제됐다. Spring 은 `S2BindValidator.bind(validator)`,
  Spring 이 없으면 `validator.getRulesJson(locale)` 을 쓴다.
- 문자열 키 전역 캐시를 다시 도입하지 않는다(키가 첫 규칙에 고정돼 역할별 규칙이 경고 없이 공유되던 문제).
- 테스트 간 격리가 필요하면 `S2Validator.resetAll()`(기본 로케일, 검증 번들, 리소스 번들 기본 이름).

## 4. 버전·배포
- 버전은 `build.gradle.kts` 만 바꾼다. `./gradlew test` 때 `S2BuildUtils` 가 `README`·`GUIDE`·`MANUAL` 의 버전·의존성 블록을 동기화한다.
- 사용자가 요청하지 않으면 배포 태스크(`publish`, `publishToCentralPortal` 등)를 실행하지 않는다.
- 평소에는 `develop` 브랜치에서 commit·push 한다. `main` 은 Maven Central 배포본과 같아야 하므로, 사용자가 배포를 요청할 때만 `develop` 을 `main` 에 fast-forward 하고 버전 태그를 단다.

## 5. Java·JS 버전
- 기준은 Java 17(`--release 17`). Java 21+ 전용 문법·API(`Thread.ofVirtual()`, `getFirst()` 등)를 직접 쓰지 않는다.
- 예외는 `s2-core` 의 `S2ThreadUtil` 가상 스레드뿐이며, MethodHandle/리플렉션으로 런타임에 감지하고 Java 17 용 대체 로직(플랫폼 스레드 풀)을 반드시 둔다.
- `s2.validator.js` 는 **ES2020 까지**(Chrome·Edge 80, Firefox 74, Safari 13.1, s2-kit 과 같은 기준). 숫자 구분자(`5_000` 대신 `5 * 1000`),
  `??=`·`||=`, `.at()`, `replaceAll`, 클래스 필드 선언(`x = 1;` — 생성자에서 초기화), `#private` 등 ES2021+ 문법·API 금지.
  자동 검사가 없으므로 고친 뒤 ES2020 파서로 확인한다(예: acorn `ecmaVersion: 2020` 으로 parse).

## 6. 인코딩
- 모든 소스·설정·문서는 UTF-8 NoBOM.

## 7. 언어 (문서·주석·커밋)
| 대상 | 언어 |
|---|---|
| `README`·`GUIDE`·`MANUAL`·`CHANGELOG` 등 가이드 문서 | 영문(`*.md`)·한글(`*.ko.md`) 쌍으로 함께 갱신 |
| Javadoc·JSDoc | 영문 설명 → `<p><b>[한국어 설명]</b></p>` → 한글 설명 (기존 형식) |
| 그 밖의 주석(`//`, 메서드 내부 블록 주석, 빌드 스크립트) | 한국어 |
| 커밋 메시지 | 한국어. 접두어(`feat:`, `fix:`, `docs:`, `build:` …)·식별자는 영어 — 예: `fix: 날짜 검증에서 null 처리 누락 수정` |
| 예외·로그 등 사용자에게 노출되는 문자열 | 영어 (다국어 메시지는 기존 i18n 구조). 예외: `s2-validator-plugin` 의 빌드 출력은 한국어 유지(사용자 결정) |
| 이슈·PR 답변 | 작성자의 언어 |

- 기존 주석·커밋 이력은 번역·정리 목적으로 바꾸지 않는다. 주석은 설명하는 코드가 바뀌었거나 틀렸을 때만 그 부분을 고치고, 고친 주석은 위 원칙의 언어로 쓴다.
