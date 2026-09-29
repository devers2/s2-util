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
  `IllegalArgumentException`). 길이·바이트 규칙은 정수가 필요합니다.
- **필드 간 메시지** (`EQUALS_FIELD`, `DATE_AFTER`, `DATE_BEFORE`): 대상 필드가 같은 검증기에 선언되어 있으면 내부 이름 대신 라벨을
  보여 줍니다.
- **예외 모드**: 검증 실패는 `S2ValidationException`(`getFieldName()` / `getErrorCode()` 제공), 커스텀 규칙 내부 오류는
  `S2RuleExecutionException`(원인 예외는 메시지가 아니라 `getCause()`로만 제공)을 던집니다. 둘 다 `S2RuntimeException`의 하위형이라
  기존 `catch (S2RuntimeException e)` 코드는 그대로 동작합니다.
- **등록부**: 같은 컨텍스트 키의 공급자 클래스 충돌은 `DEBUG`로 기록합니다 (문서의 GET/POST 사용 형태에서 거짓 `WARN`이 떴습니다).
  `S2BindValidator.of(validator)`를 권장합니다.
- **클라이언트 내보내기**: `REGEX` 규칙이 Java 전용 문법(`(?i)`, 소유 한정자, 원자 그룹, `\p{…}`, `\A`/`\z`, `\Q…\E`, 문자 클래스 교집합
  등)을 쓰면 `getRulesJson()`이 `IllegalStateException`을 던집니다. 서버 전용 검증기는 영향이 없습니다.
- **로케일**: 검증기가 생성 시점의 기본 로케일을 복사해 두지 않습니다. `S2Validator.setDefaultLocale(null)`은 JVM 기본값으로 되돌립니다
  (이전에는 무시).
- **로그**: `System.out`/`System.err`를 교체하지 않고 배너 스레드도 띄우지 않습니다.

### 추가

- 전역 상태 초기화(예: 시험): `S2Validator.resetAll()`, `resetDefaultLocale()`, `resetValidationBundle()`, `S2ValidatorFactory.clear()`,
  `S2ResourceBundle.resetDefaultBasename()`.
- `S2BindValidator.of(validator)`: 전역 등록부 없이 검증기 인스턴스를 직접 연결.
- 커스텀 람다 규칙용 `.includeEmpty()` 수식어.
- `S2ValidationException`, `S2RuleExecutionException`.
- 브라우저: `{필드명}_error` 대리 요소가 없는 히든·비표시 필드 옆에 1px 앵커를 만들어 기본 오류 말풍선을 표시.

### 수정

- 와일드카드 행의 규칙 실행 오류가 실제 경로(`items[1].qty`)를 담습니다.
- 라벨 없는 `field(Object)`가 문자열이 아닌 키에서 `ClassCastException`을 내지 않습니다.
- 공용 실행기가 종료된 뒤 다시 만들어집니다.

### 업그레이드 안내

1. `s2-support`를 `s2-core`와 함께 올리십시오 (호환성 참고).
2. `s2.validator.js`를 애플리케이션에 복사해 쓰고 있다면 사본을 교체하거나, jar 의 `/s2-util/js/s2.validator.js` 경로로 서빙하십시오.
   위의 브라우저 동작 변경은 이 파일에 들어 있습니다.
3. 이전 `PASSWORD` 정책을 유지하려면 `.rule(S2RuleType.REGEX, "^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{9,32}$")`를 사용합니다.
4. 이전 `JUMIN` 검증번호 검사를 유지하려면 `.rule(S2RuleType.JUMIN, true)`를 사용합니다.
5. 빈 값에서도 실행해야 하는 커스텀 람다(예: "두 필드 중 하나 필수")는 `.includeEmpty()`가 필요합니다.
6. 예외 모드 실패를 `S2RuntimeException`으로 처리하고 있다면, `S2ValidationException`(입력 오류 → 400)과
   `S2RuleExecutionException`(버그 → 500)을 나눠 처리하는 것을 검토하십시오.

## s2-validator-plugin [1.2.0] - 미배포

- **Configuration cache**: `checkS2Validators`가 실행 시점에 `getProject()`를 호출하지 않아 `--configuration-cache`에서 동작합니다.
- **record DTO**: record 컴포넌트를 필드로 인식하며, 소스를 Java 17 언어 수준으로 파싱합니다.
