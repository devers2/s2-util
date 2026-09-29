# 변경 이력

[English](./CHANGELOG.md) | **한국어**

`s2-util`(`s2-core`, `s2-validator`, `s2-jpa`)과 `s2-validator-plugin`의 주요 변경 사항을 기록합니다.

## [1.2.0] - 미배포

1.1.8 대비 변경입니다. 이번 버전에는 **동작 변경**이 있으므로 올리기 전에 [업그레이드 안내](#업그레이드-안내)를 확인하십시오.

### ⚠️ 호환성

- **`s2-support` 1.1.x 는 `s2-core` 1.2.0 과 호환되지 않습니다.** `s2-support` 1.1.3 의 `S2AutoConfiguration`이 삭제된
  `S2LogManager.touch()`를 호출하므로, Spring Boot 애플리케이션이 시작 시
  `NoSuchMethodError: 'void io.github.devers2.s2util.log.S2LogManager.touch()'`로 실패합니다. `s2-core` 1.2.0 과 함께 배포되는
  `s2-support` 버전으로 올리십시오.
- **삭제된 공개 API** (1.1.8 배포본 대비):
  - `S2LogManager.touch()`
  - `DefaultS2Logger.printWarningBannerOnce()`, `DefaultS2Logger.markAdapterConfigured()` (경고 배너와 배너 스레드 제거)
  - `io.github.devers2.s2util.validation.annotation.CheckReturnValue` (Error Prone 의 `@CheckReturnValue`로 대체)
  - `message(Locale, String)` → `message(String, Locale)`로 대체 (`message(String)`처럼 문구가 먼저). `.ko()`, `.en()`은 그대로
- **전역 검증기 등록부와 `S2ValidatorFactory`를 삭제했습니다.** `S2BindValidator.context(key, supplier)`와 `S2ValidatorFactory` 클래스
  전체(`getOrRegister`, `getValidator`, `getRulesJson(key, locale)`, `getRulesJson(validator, locale)`)가 없어졌습니다. 키가 처음 만든
  검증기에 고정되어 역할별로 다른 규칙이 경고 없이 공유됐고, 캐시로 아끼는 시간은 요청당 약 0.5µs 뿐이었습니다(GET 폼의 규칙 JSON 생성
  약 15µs 는 캐시되지 않았음). Spring 에서는 `S2BindValidator.bind(validator)`, Spring 없이 규칙을 내보낼 때는
  `validator.getRulesJson(locale)`을 쓰십시오. 업그레이드 안내 참고.

### 변경 (동작)

- **공백**: 내장 규칙은 문자열 값(과 필드 간 비교 대상 값)의 앞뒤 공백을 제거한 뒤 판정합니다. 브라우저와 같아졌습니다. 판정만 바뀌며
  저장되는 값은 바뀌지 않습니다. 커스텀 람다에는 원래 값이 그대로 전달됩니다.
- **숫자**: `MIN_VALUE`/`MAX_VALUE`는 서버와 브라우저 모두 일반 십진 표기만 숫자로 인정합니다. `"1,000"`, `"25abc"`, `"25d"`,
  `"NaN"`은 무효입니다 (이전에는 브라우저가 `"1,000"`을 `1`로 읽었습니다).
- **정규식**: 브라우저가 정규식 규칙을 항상 전체 일치(`^(?:…)$`)로 평가합니다. Java `matcher.matches()`와 같습니다. 한쪽 앵커(`^\d+`)나
  대체 한 갈래에만 걸린 앵커(`^a|b$`)도 더 이상 부분 일치하지 않습니다.
- **`ASSERT_TRUE` / `ASSERT_FALSE`**: 빈 값을 건너뛰지 않고 판정하므로, 체크하지 않은 필수 동의 체크박스가 거부됩니다.
  `"true"`/`"on"`, `"false"`/`"off"`/`""` 문자열을 인식합니다.
- **커스텀 람다**: 값이 비어 있으면 실행하지 않습니다(`NullPointerException` 없음). 빈 값에서도 실행하려면 `.includeEmpty()`를 붙입니다.
  람다 내부 예외는 `S2RuleExecutionException`으로 감쌉니다 (아래 참고).
- **`PASSWORD`**: 8~64자, 영문·숫자·ASCII 특수문자 각 1개 이상 (이전: 9~32자, 특수문자는 `!@#$%^&*`만).
- **`JUMIN`**: **기본값으로 검증번호를 검사하지 않습니다.** 2020년 10월부터 신규 부여·변경된 번호는 출생일과 무관하게 임의번호라서
  검증번호 검사가 정상 번호를 거부했습니다. 2020년 10월 이전 출생자의 기존 검증번호를 검사하려면 `.rule(S2RuleType.JUMIN, true)`를
  사용합니다.
- **`DATE`**: "현재 연도 − 100" 이전 날짜(예: 100세 이상 생년월일)를 더 이상 거부하지 않습니다. `java.util.Date` / `java.sql.Date`를
  허용합니다 (이전에는 항상 무효).
- **`MIN_BYTE` / `MAX_BYTE`**: 플랫폼 문자셋과 무관하게 UTF-8 바이트 수로 셉니다 (Java 17 + 한국어 Windows 는 MS949 로 셌습니다).
- **규칙 기준값을 생성 시점에 검사**: `MIN_VALUE`/`MAX_VALUE`는 유한한 숫자가 필요합니다(NaN/Infinity/`"abc"`는
  `IllegalArgumentException`). 길이·바이트 규칙은 정수가 필요합니다
  (브라우저처럼 생성·판정 시점 모두 앞뒤 공백은 무시).
- **필드 간 메시지** (`EQUALS_FIELD`, `DATE_AFTER`, `DATE_BEFORE`): 대상 필드가 같은 검증기에 선언되어 있으면 내부 이름 대신 라벨을
  보여 줍니다.
- **예외 모드**: 검증 실패는 `S2ValidationException`(`getFieldName()` / `getErrorCode()` 제공), 커스텀 규칙 내부 오류는
  `S2RuleExecutionException`(원인 예외는 메시지가 아니라 `getCause()`로만 제공)을 던집니다. 둘 다 `S2RuntimeException`의 하위형이라
  기존 `catch (S2RuntimeException e)` 코드는 그대로 동작합니다.
- **메시지**:
  - 받침을 판별할 수 없는 단어(영문·기호)에도 조사를 빠뜨리지 않습니다:
    `[documentId] 필수 입력 항목입니다.` 대신 `[documentId]은(는) 필수 입력 항목입니다.`
  - `ASSERT_TRUE`: "{0|을/를} 선택(동의)해야 합니다." / "{0} must be checked." (이전 "반드시 true여야 합니다");
    `ASSERT_FALSE`: "{0|은/는} 선택할 수 없습니다." / "{0} must not be checked."
  - 와일드카드 행의 상대 표기 비교 대상(`items[].end` → `"start"`)도 `start`가 아니라 라벨(`시작일`)로 보여 줍니다.
  - 순환 참조 오류는 번들 키 `valid.err.circular`(이전 `ERR_CIRCULAR_REFERENCE`)를 쓰고 로케일에 맞춰 표시합니다
    ("{0}에서 순환 참조가 감지되었습니다."). 이전에는 항상 한국어였습니다.
- **NESTED/EACH 와 바깥 필드**: NESTED/EACH 하위 검증기와 와일드카드 행 안의 필드 간 비교 대상과 `when` 조건을 현재 객체 → 바깥 객체들 →
  루트 순서로 찾습니다(브라우저도 같은 순서). 이전에는 하위 검증기가 바깥 필드를 볼 수 없어, 예를 들어 EACH 안의
  `items[].end DATE_AFTER "globalStart"`나 NESTED 안의 `when("globalType", "X")`가 조용히 건너뛰어졌습니다. 대상 라벨도 같은 방식으로 찾습니다
  ("…globalStart보다" 대신 "…전체 시작일보다").
- **인덱스 빈칸**: 와일드카드/EACH 컬렉션의 `null` 요소(예: 0·2번 행만 전송되고 1번 행 삭제)는 행으로 검증하지 않습니다(브라우저와 같음).
- **조건**: 빈 문자열은 브라우저의 빈 폼 필드처럼 빈 값으로 봅니다 (`when(field, null)`).
- **중첩 깊이**: `NESTED`/`EACH`는 64단계에서 멈추고 `valid.err.maxdepth`를 보고합니다 (`StackOverflowError` 방지).
- **`check(value, label)`**: 예외의 필드 이름이 내부 키 `"value"` 대신 라벨입니다.
- **`EMAIL`**: 최상위 도메인 2~63자를 허용합니다 (이전 2~6자라 `.technology` 등을 거부). 빈 레이블(`b..com`)과 `-`로 시작·끝나는 레이블은
  거부합니다.
- **브라우저 히든 필드**: 1px 앵커가 와일드카드 행에도 적용되고, `display:none` 컨테이너(닫힌 탭·아코디언) 안의 필드는 가장 바깥 숨은
  컨테이너 뒤에 앵커를 둡니다. 라디오·체크박스 그룹은 앵커 하나, 메시지는 `aria-hidden` 대신 `aria-label`로 제공하며, 렌더링된
  `position:fixed` 필드를 숨은 것으로 오판하지 않습니다.
- **클라이언트 내보내기**: `REGEX` 규칙이 Java 전용 문법(`(?i)`, 소유 한정자, 원자 그룹, `\p{…}`, `\A`/`\z`, `\Q…\E`, 문자 클래스 교집합
  등)을 쓰면 `getRulesJson()`이 `IllegalStateException`을 던집니다. 서버 전용 검증기는 영향이 없습니다.
- **로케일**: 검증기가 생성 시점의 기본 로케일을 복사해 두지 않습니다. `S2Validator.setDefaultLocale(null)`은 JVM 기본값으로 되돌립니다
  (이전에는 무시).
- **로그**: `System.out`/`System.err`를 교체하지 않고 배너 스레드도 띄우지 않습니다. SLF4J 브리지는 `org.slf4j.Logger`
  인터페이스에서 메서드를 한 번 찾아 묶은 `MethodHandle`로 호출합니다(로그 호출마다 `Method.invoke`를 쓰지 않음). SLF4J 가 클래스패스에
  없으면 이전과 같이 내장 로거를 사용합니다.

### 추가

- 전역 상태 초기화(예: 시험): `S2Validator.resetAll()`, `resetDefaultLocale()`, `resetValidationBundle()`,
  `S2ResourceBundle.resetDefaultBasename()`.
- `S2BindValidator.bind(validator)`: 검증기 인스턴스를 연결해 `validate(target, bindingResult)`와 `getRulesJson()` 제공.
- `S2Validator.getRulesJson()` / `getRulesJson(locale)`: 검증기 규칙을 브라우저용 JSON 으로 내보냄
  (`S2ValidatorFactory.getRulesJson(validator, locale)` 대체).
- 커스텀 람다 규칙용 `.includeEmpty()` 수식어.
- `.message(문구)`: 언어별 문구가 없는 모든 언어에 쓰는 기본 메시지. 조회 순서: 번들 키 → 요청 언어 → 기본 메시지 → 기본 로케일 언어 → 내장 메시지.
- `S2ValidationException`, `S2RuleExecutionException`.
- 브라우저: 기본 말풍선 대신 애플리케이션 방식으로 오류를 그리는 `S2Validator.setRenderer({ show, clear, clearField })`와
  `S2Validator.classRenderer()`(`is-invalid` 추가, `[data-s2-error-for]`에 메시지 표시, 첫 오류로 초점 이동).
- 브라우저: 행 삭제 후 행 인덱스를 `0..n-1`로 다시 매기는 `S2Validator.reindex(form, collection)` (명시적 호출 전용, 자동 적용 없음).
- 브라우저: `{필드명}_error` 대리 요소가 없는 히든·비표시 필드 옆에 1px 앵커를 만들어 기본 오류 말풍선을 표시.

### 수정

- 와일드카드 행의 규칙 실행 오류가 실제 경로(`items[1].qty`)를 담습니다.
- 라벨 없는 `field(Object)`가 문자열이 아닌 키에서 `ClassCastException`을 내지 않습니다.
- 공용 실행기가 종료된 뒤 다시 만들어집니다.

### 업그레이드 안내

1. `s2-support`를 `s2-core`와 함께 올리십시오 (호환성 참고).
2. 등록부를 `S2BindValidator.bind(...)`로 바꾸십시오. GET 폼과 POST 처리가 계속 같은 규칙 정의를 쓰므로 동일한 규칙이 적용됩니다:
   ```java
   // 이전
   S2BindValidator.context("signup", this::signupRules).getRulesJson();    // GET
   S2BindValidator.context("signup", this::signupRules).validate(cmd, r);  // POST
   // 이후
   S2BindValidator.bind(signupRules()).getRulesJson();    // GET
   S2BindValidator.bind(signupRules()).validate(cmd, r);  // POST
   ```
   규칙 생성 자체가 무거운 경우에만 필드나 Spring 빈에 보관하십시오(`S2BindValidator.bind(signupValidator)`).
   Spring 없이 쓰던 `S2ValidatorFactory.getRulesJson(validator, locale)`은 `validator.getRulesJson(locale)`로 바꾸십시오.
3. `s2.validator.js`를 애플리케이션에 복사해 쓰고 있다면 사본을 교체하거나, jar 의 `/s2-util/js/s2.validator.js` 경로로 서빙하십시오.
   위의 브라우저 동작 변경은 이 파일에 들어 있습니다.
4. 이전 `PASSWORD` 정책을 유지하려면 `.rule(S2RuleType.REGEX, "^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{9,32}$")`를 사용합니다.
5. 이전 `JUMIN` 검증번호 검사를 유지하려면 `.rule(S2RuleType.JUMIN, true)`를 사용합니다.
6. 빈 값에서도 실행해야 하는 커스텀 람다(예: "두 필드 중 하나 필수")는 `.includeEmpty()`가 필요합니다.
7. 예외 모드 실패를 `S2RuntimeException`으로 처리하고 있다면, `S2ValidationException`(입력 오류 → 400)과
   `S2RuleExecutionException`(버그 → 500)을 나눠 처리하는 것을 검토하십시오.
8. `.message(Locale, "…")`의 인자 순서를 `.message("…", Locale)`로 바꾸십시오.

## s2-validator-plugin [1.2.0] - 미배포

- **Configuration cache**: `checkS2Validators`가 실행 시점에 `getProject()`를 호출하지 않아 `--configuration-cache`에서 동작합니다.
- **바인딩 검사**: `S2BindValidator.bind(...)` 결과에서 `validate`/`getRulesJson`을 호출하지 않으면 경고합니다. 삭제된
  `context`/`getOrRegister`/`getValidator` 검사는 없앴습니다.
- **record DTO**: record 컴포넌트를 필드로 인식하며, 소스를 Java 17 언어 수준으로 파싱합니다.
