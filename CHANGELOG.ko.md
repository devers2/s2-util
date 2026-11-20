# 변경 이력

[English](./CHANGELOG.md) | **한국어**

`s2-util`(`s2-core`, `s2-validator`, `s2-jpa`)과 `s2-validator-plugin`의 주요 변경 사항을 기록합니다.

## [Unreleased]

### ⚠️ 호환성

- **`S2Validator`·`S2Field`·`S2Rule`·`S2Condition` 은 더 이상 `Serializable` 이 아닙니다.** 람다 규칙이 하나라도 있으면 내부 람다 때문에 항상 `NotSerializableException` 이 나서 지킬 수 없는 계약이었습니다.
  검증기는 설정 객체이므로 세션 등에 직렬화하지 말고 `static final` 상수(또는 Spring 빈)로 공유하십시오.
  세션에 저장되는 객체나 JSF·Vaadin 등의 view/session scope 빈이 검증기를 인스턴스 필드로 가지면 세션 저장·복제 때 `NotSerializableException` 이 나므로, 그 필드는 `static final` 로 바꾸거나 `transient` 로 표시하십시오.

### ⚠️ 동작 변경

- **`S2Util.cast`(와 `getValue(…, Class)`) 형변환을 정확하게 바꿨습니다.** 정수 타입은 정확히 맞는 값만 받습니다 — 이전에는 `3.7` → `3`, `10000000000` → `1410065408` 처럼 조용히 잘리거나 넘쳤고, 이제 변환 실패(기본값, 없으면 `null`)입니다.
  숫자 문자열의 앞뒤 공백을 허용하고(`"12 "` → 12), `BigDecimal`·`BigInteger`·`Short`·`Byte`·열거형(상수 이름)을 지원합니다(이전에는 `ClassCastException`). Boolean 은 `true/1/on/yes/y`·`false/0/off/no/n` 만 인식하고 그 밖은 변환 실패입니다(이전에는 모두 `false`).
- **setter·생성자·레코드 접근자 자체가 던진 예외를 삼키지 않습니다.** `setValue` 는 `S2PropertyAccessException`(쓰기), `createInstance` 는 `S2RuntimeException` 을 던집니다. 쓸 곳이 없거나 값 타입이 맞지 않으면 이전처럼 `false`/`null` 입니다. `S2Copier` 도 setter 실패를 알립니다.
- `S2Util.isEmpty` 는 `Optional.empty()` 와 빈 값을 담은 `Optional` 을 빈 값으로 봅니다(검증기에서 `Optional` 필드의 `REQUIRED` 가 통과하던 문제).
- **`S2Util.isEmpty` 의 공백 정의를 JavaScript `trim()` 과 같게 바꿨습니다.** NBSP(U+00A0)·U+2007·U+202F·BOM(U+FEFF)만 있는 문자열은 빈 값이고, 제어 문자 U+001C~U+001F 는 공백이 아닙니다(이전에는 `Character.isWhitespace` 기준으로 반대).
  이전에는 U+001F 만 든 값을 서버는 빈 값으로 보고(`EMAIL` 통과·`REQUIRED` 실패) 브라우저는 값으로 봐서 판정이 갈렸습니다.
- **공백만 있는 값도 상한 규칙(`LENGTH`·`MAX_LENGTH`·`MAX_BYTE`)은 원래 길이로 판정합니다.** 빈 값으로 보아 건너뛰던 탓에 공백 300자가 `MAX_LENGTH 10` 을 통과해 그대로 저장될 수 있었습니다.
  `LENGTH` 는 기준을 넘을 때만 실패하고, 나머지 규칙·`REQUIRED`·람다 규칙에서는 이전처럼 빈 값입니다. 서버와 `s2.validator.js` 모두 바뀌었습니다.
- **기본값 없는 `S2Util.getValue(target, field)`·`getValue(target, field, Class)`·`cast(value)`·`cast(value, Class)` 와 `getValueAll` 은 저장된 값을 그대로 돌려줍니다.** 이전에는 빈 문자열·공백 문자열·빈 컬렉션을 `null` 로 바꿨습니다.
  빈 값 여부는 `S2Util.isEmpty`(공백만 있는 문자열도 빈 값)로 판단하십시오 — `getValue(...) == null` 로 빈 값을 검사하던 코드는 바꿔야 합니다. 기본값을 주는 `getValue(target, field, default)`·`cast(value, default)` 는 이전처럼 빈 값·없음·변환 실패면 기본값입니다.
- **사용자 정규식(`REGEX`·`@Pattern` 변환)의 서버·브라우저 판정을 더 맞췄습니다.** 브라우저는 가능하면 `u` 플래그로 평가해 이모지 등을 Java 처럼 한 글자로 보고, `.` 은 Java 처럼 U+0085 와 일치하지 않습니다.
  두 쪽 뜻이 달라 규칙 JSON 으로 내보내지 않는(`IllegalStateException`, BV 변환은 서버 전용) 문법이 늘었습니다: `\x{…}`·`\v`·`\N{…}`·`\b{g}`, `[` 바로 뒤의 `]`(`[]a]`·`[^]a]`), 8진수 `\0…`, 여러 자리·없는 그룹 역참조(`\12`), Java 가 컴파일하지 못하는 패턴(`a{,3}`·`\y`).
- **`S2Util.getValue` 는 getter 자체가 던진 예외를 삼키지 않고 `S2PropertyAccessException`(속성 경로·원인 포함)으로 던집니다.** 없는 속성·경로 중간의 `null` 은 이전처럼 `null`(또는 기본값)입니다.
  이전에는 getter 예외(지연 로딩 실패, 계산 getter 의 NPE 등)가 빈 값이 되어, 검증기에서 `REQUIRED` 는 엉뚱한 메시지로 실패하고 나머지 규칙(`EMAIL`·`MAX_LENGTH` 등)은 **조용히 통과**했습니다.
  같은 이유로 `S2Copier`·`setValue`(중첩 경로)·`getValueAll` 도 getter 실패를 알립니다. `OutOfMemoryError` 같은 JVM 오류도 더 이상 삼키지 않습니다.
- **상한 규칙(`LENGTH`·`MAX_LENGTH`·`MAX_BYTE`)은 앞뒤 공백을 포함한 원래 값으로 판정합니다.** 이전에는 공백을 뺀 값으로 판정해 공백을 덧붙인 값이
  길이 제한을 통과한 뒤 그대로 저장될 수 있었습니다. 서버와 `s2.validator.js` 모두 바뀌었고, 브라우저는 폼 값을 공백을 빼지 않고 읽습니다.
- **여러 값(컬렉션·배열·체크박스 그룹·다중 선택)은 요소별로 판정합니다.** 이전에는 서버가 `toString()`(`"[a, b]"`, `"[Ljava.lang.String;@…"`),
  브라우저가 `"a,b"` 문자열로 판정해 `String[]` 파라미터 맵의 길이·형식 규칙이 오작동했습니다. `REQUIRED` 는 빈 요소만 있으면(`["", " "]`) 실패합니다.
- **메시지 우선순위: 규칙에 단 메시지(`.ko()`·`.en()`·`.message()`)가 내장 키(`valid.err.*`)의 번들·`MessageSource` 메시지보다 우선합니다.**
  이전에는 `messages.properties` 에 `valid.err.required` 같은 공통 키가 있으면 규칙별 메시지가 모두 무시됐습니다. `.rule(type, value, key)`로 명시한 키는 지금처럼 가장 먼저 찾습니다.
- `S2Rule.required()` 공유 인스턴스에 `storeMessage()` 를 호출하면 `UnsupportedOperationException` 이 납니다. 이전에는 JVM 전체의 암묵 필수 메시지가 바뀌었습니다.
- **`DATE_AFTER`·`DATE_BEFORE`**: 날짜로 읽을 수 없는 값은 브라우저에서도 통과합니다(서버와 동일). 이전에는 브라우저만 실패시켜 서버가 받는 값을 막았습니다.
- **`S2ThreadUtil.newExecutor(maxThreads)`: 양수 `maxThreads` 는 가상 스레드(Java 21+)에서도 동시 실행 수를 제한합니다.**
  이전에는 Java 17(플랫폼 풀)에서만 제한되고 Java 21+ 에서는 무시되어, Java 버전만 올려도 DB 연결 풀·외부 API 한도를 넘을 수 있었습니다.
  작업마다 가상 스레드를 만들고 세마포어로 동시에 `maxThreads` 개만 실행합니다. 0 이하는 지금처럼 가상 스레드에서 제한이 없습니다.
- **`S2DateUtil` 의 파싱·검증 함수(`isValidDate`·`parseToLocalDate`·`parseToLocalDateTime`·`compareDates` 등)는 없는 날짜를 거부합니다.**
  이전에는 `2024-02-30`·`2023-02-29` 를 그 달의 마지막 날로 고쳐 받아들여 `isValidDate` 가 `true` 를 돌려줬습니다(JDK 기본 SMART 해석). 이제 STRICT 로 해석합니다.
- **SLF4J 가 없을 때 쓰는 기본 콘솔 로거는 INFO 부터 출력합니다.** 이전에는 DEBUG 를 항상 켜 두어 운영에서도 디버그 로그가 모두 찍혔습니다.
  `-Ds2.log.level=DEBUG` 로 바꿀 수 있고, WARN·ERROR 는 표준 오류(`System.err`)로 나갑니다.
- **`S2Copier`: 상속받은 필드도 복사하고, 빈 문자열·빈 컬렉션을 `null` 로 바꾸지 않고 그대로 복사합니다.** static 필드는 복사하지 않습니다.
- **`S2Template.render()`: 바인딩 값 안의 `{{=…}}` 를 다시 치환하지 않습니다.** 이전에는 다른 바인딩으로 바뀌거나 지워졌고, 결과가 바인딩 순서에 따라 달랐습니다.
- `S2StringUtil.clearNewline` 은 역슬래시도 이스케이프합니다. 이전에는 입력의 `\'` 가 JS 문자열을 닫아 스크립트가 주입될 수 있었습니다.
- `S2JsonUtil.toJson` 은 문자열 안의 `<`·`>`·`&` 를 `\u003c`·`\u003e`·`\u0026` 으로 씁니다. 출력을 HTML `<script>` 안에 그대로 넣어도 `</script>` 로 닫히지 않습니다(읽은 값은 같음).
- **`build()` 한 검증기는 바꿀 수 없습니다.** 같은 빌더 체인으로 `build()` 뒤에 `field()`·`rule()` 등을 부르면 `IllegalStateException` 이 납니다.
  이전에는 이미 공유 중인 검증기에 필드가 조용히 추가될 수 있었고, 필드 목록이 여러 스레드에서 안전하지 않았습니다. 다른 검증기는 새 `builder()` 로 만드십시오.
- 목록 검증(`EACH`)에서 요소가 검증 경로의 조상과 같은 객체(순환 참조)면 순환 참조 오류를 냅니다(`NESTED` 와 같음). 이전에는 오류 없이 통과했습니다.
- `S2JsonUtil.toJson` 은 `java.sql.Date` 를 날짜(`2024-02-01`), `java.sql.Time` 을 시각(`09:30`)으로 씁니다. 이전에는 UTC 시각으로 써서 한국 시간에서 날짜가 하루 밀렸습니다(`2024-01-31T15:00:00Z`). `fromJson` 으로 다시 읽을 수 있습니다.
- `S2Jpql` 의 DEBUG 로그는 바인딩 파라미터의 값을 남기지 않고 이름과 타입만 남깁니다(개인정보 보호).
- **`S2Jpql` 의 Supplier `bindParameter` 는 `build()` 때, 렌더링된 JPQL 이 그 파라미터(`:name`)를 쓸 때만 실행합니다.** 이전에는 "직전 `bindClause` 의 조건"으로 정해,
  다른 절 뒤에 부르면 포함된 절의 파라미터가 바인딩되지 않거나(실행 때 오류) 빠진 절의 Supplier 가 실행됐습니다. 이제 `bindClause` 와의 호출 순서에 상관없습니다.
- **`S2Template.bindIn` 은 숫자·논리값만 그대로 쓰고, 그 밖의 값(날짜·enum 등)은 문자열 리터럴로 감쌉니다. 역슬래시가 든 문자열은 `IllegalArgumentException` 으로 거부합니다.**
  이전에는 MySQL·MariaDB(기본 설정)에서 `a\'` 같은 값이 따옴표 이스케이프를 깨고 SQL 을 주입할 수 있었고, 숫자가 아닌 객체는 `toString()` 그대로 들어갔습니다. 사용자 입력은 쿼리 파라미터로 바인딩하십시오.
- `S2Jpql` 렌더링은 JPQL 문자열 리터럴(`'…'`) 안의 빈 줄·줄 끝 공백·줄바꿈 문자를 지우지 않습니다. 이전에는 여러 줄 리터럴의 값이 바뀌었습니다. 기본 `S2Template` 은 이전처럼 모든 줄을 정리합니다(메시지의 아포스트로피는 리터럴이 아님).
- **`MIN_VALUE`·`MAX_VALUE` 와 숫자 조건(`GT`·`GTE`·`LT`·`LTE`)은 서버와 브라우저 모두 정확한 십진 비교를 합니다.**
  이전에는 double 로 비교해 유효숫자 15~16자리를 넘는 값(예: `9007199254740993` 과 `9007199254740992`, `0.1000000000000000001` 과 `0.1`)을 같게 봤습니다.
  서버는 `BigDecimal`, `s2.validator.js` 는 십진 문자열을 직접 비교하므로 두 쪽 판정은 계속 같습니다.
- `s2.validator.js` 가 화면에 내는 시스템 메시지(폼·규칙 JSON 오류)는 페이지 언어(`<html lang>` → 브라우저 언어)를 따라 한국어 또는 영어로 나옵니다. 언어를 알 수 없으면(브라우저 밖) 이전처럼 한국어입니다.

### 추가

- **`S2BeanValidation`: Bean Validation 어노테이션(`@NotBlank`·`@Size`·`@Pattern`·`@Min`·`@Valid` 등)으로 S2 규칙을 만듭니다.** 기존 DTO 를 고치지 않고 브라우저 검증을 얻고, `toBuilder()`로 S2 규칙을 더할 수 있습니다. Bean Validation 보다 엄격하지 않은 제약만 변환하고 나머지는 `serverOnly()`·로그로 알립니다(`practical()` 로 범위 확장). Hibernate Validator 와 무작위 입력으로 비교하는 시험 포함. MANUAL 5-5.
- 배포 POM 에 Spring·`jakarta.validation-api` 를 선택 의존성(`<optional>true</optional>`)으로 표시합니다. 프로젝트로 전이되지 않으며 어떤 기능에 무엇이 필요한지 보입니다.
- 규칙 `MIN_SIZE`·`MAX_SIZE`: 값의 개수(컬렉션·배열·체크박스 그룹·다중 선택, 단일 값은 1개)를 검사합니다. 서버·브라우저 동일, 빌드 플러그인 기준값 검사 포함.
- `S2SpringValidator`: `S2Validator`를 Spring `SmartValidator`로 감싸, `@InitBinder` 로 등록하면 `@Valid`/`@Validated` 때 Bean Validation 과 함께 실행되고 같은 `BindingResult`에 기록됩니다(MANUAL 2-C). 추가 의존성 없음.
- `S2Validator.validateAll(target[, locale])` → `S2ValidationResult`: 예외 없이 모든 오류를 모으며 `toMap()`은 `s2.validator.js`와 같은 `{필드: [메시지]}` 형식입니다(REST 응답용).
- 검증 필드명이 대상 클래스에 없으면(오타 등) 클래스·필드당 한 번 WARN 로그를 남깁니다. 그런 필드는 항상 빈 값이라 필수 외 규칙이 실행되지 않습니다. 판정은 바뀌지 않습니다.
- `S2ThreadUtil.newExecutor(maxThreads, maxPending)`: 대기 작업 수까지 제한하는 실행기. 실행 중 + 대기가 한도에 이르면 제출하는 스레드가 기다려(역압),
  처리보다 빠른 제출이 이어져도 메모리가 바닥나지 않습니다. 가상 스레드·플랫폼 모두 같게 동작합니다(MANUAL 9-3).
- 시스템 속성 `s2.thread.virtual.enabled=false`(가상 스레드 끄기, Java 21~23 pinning 회피용)와 `s2.thread.virtual.max_concurrency`(가상 스레드 공용 실행기 동시 실행 상한).
- `S2Jpql.escapeLike()`: 검색어의 LIKE 와일드카드(`%`, `_`)를 이스케이프하는 도우미. JPQL 의 `ESCAPE '\'` 와 함께 씁니다(GUIDE_JPQL 3-2).
- `S2Jpql.bindOrderBy(key, sortExpression, allowedFields)`: 요청에서 온 정렬 문자열(`name desc`)을 허용 목록의 JPQL 경로(`m.name DESC`)로 바꾸고, 목록에 없는 키는 버립니다. 숨은 컬럼(비밀번호 해시 등)으로 정렬해 값을 추측하는 것을 막습니다.
- `S2Validator.getValidationBundle()`·`getDefaultLocale()`·`getMessageResolver()` 를 공개합니다(현재 전역 설정 확인용).

### 성능

- s2-core 경량 캐시(`S2OptimisticCache`): 조회 때 공유 카운터를 올리던 방식을 세대(epoch) 기반 근사 LRU 로 바꿔, 조회가 공유 쓰기 없이 동작합니다.
  여러 스레드가 동시에 조회해도 처리량이 코어 수만큼 늘어납니다. 축출은 지금처럼 전용 플랫폼 스레드에서 비동기로 하며, 절반이 아닌 최대 크기의 75%까지만 지웁니다.
- `S2Cache` 의 메서드 핸들·필드(`getFields`)·프록시 실제 클래스 캐시를 클래스별 `ClassValue` 로 옮겼습니다. 재배포·DevTools 재시작 때 이전 ClassLoader 가
  정적 캐시에 붙잡혀 메모리에 남던 문제가 없어집니다. JDK·상위 로더 클래스는 상한이 있는 공용 캐시를 쓰고, 메서드 핸들은 클래스당 최대
  `s2.cache.method.max_per_class`(기본 512)개까지만 캐싱합니다(외부 입력 이름으로 인한 메모리 증가 방지).
- Caffeine 백엔드의 유지보수 작업은 Caffeine 기본 실행기를 씁니다. 이전에는 `S2ThreadUtil` 공용 실행기를 써서,
  `S2ThreadUtil.shutdownCommonExecutor()` 이후 캐시에 쓸 때마다 `RejectedExecutionException` 경고가 찍혔습니다.
- `S2StringUtil.replaceChars`·`removeChars` 가 호출마다 64KB 배열을 만들던 것을 대상 문자 크기만큼만 만듭니다(검증기 `DATE`·주민번호 규칙이 매번 호출).
- `S2DateUtil` 파싱 포맷터를 패턴별로 캐시합니다.
- `s2.validator.js` 가 정규식 규칙을 검사할 때마다 패턴 변환·`RegExp` 컴파일을 반복하던 것을 캐시합니다.

### 제거 (보안)

- **`S2Util.chkSuPw` 를 삭제했습니다.** 현재 시각으로 고정 규칙의 비밀번호를 만드는 메서드로, 규칙이 소스에 공개되어 있어 누구나 계산할 수 있습니다.
  인증 우회에 쓰면 뒷문이 되므로 대체 없이 제거합니다. 이 메서드로 로그인 등을 허용하던 코드가 있다면 함께 없애십시오.

### 문서·예제

- 공개 데모 사이트(GitHub Pages, `s2-validator/browser-test/site`): 서버 검증기로 만든 규칙으로 회원가입·주문 행·Bean Validation 변환·`check()` 플레이그라운드를 한국어/영어로 시연. Playwright 시험 포함.
- 예제 앱 `examples/spring-boot-thymeleaf`: `./gradlew -p examples/spring-boot-thymeleaf bootRun` 한 번으로 실행. S2 규칙 방식과 Bean Validation 에 얹는 방식, REST 오류 응답. CI 에서 시험.

### 수정

- `S2Util.getValue` 가 Lombok 이 `Boolean isXxx` 필드에 만드는 `getIsXxx()` 와, getter 모양 이름(`is3d`)의 필드를 읽지 못하고 항상 `null` 을 돌려주던 문제. `getValueAll` 은 static getter 를 속성으로 넣지 않습니다.
- `S2Util.checkDependency` 의 예외 메시지가 한국어로만 나오던 문제(영어로), 확인 대상 클래스를 초기화하지 않습니다.
- `S2Jpql.limit(condition, offsetSupplier, limitSupplier)` 에서 Supplier 가 `null` 을 돌려주면 `NullPointerException` 이 나던 문제 — 그 값은 설정하지 않습니다.
- `S2Jpql` 의 `LikeMode` 가 `StringBuilder` 등 `String` 이 아닌 `CharSequence` 값에는 적용되지 않던 문제.
- `S2Template` 클래스 Javadoc 예제가 값을 그대로 넣는 `bind("name_cond", name, "AND m.name = ")` 를 안내하던 문제(SQL 주입 패턴) — 쿼리 파라미터를 쓰는 예로 바꾸고 보안 주의를 추가.
- 사용자 정규식(`REGEX`, `@Pattern` 변환 포함)의 `\s`·`\S` 를 브라우저가 JS 의미(NBSP 등 유니코드 공백 포함)로 해석해 서버와 판정이 다르던 문제 — 브라우저도 Java 의미로 평가합니다. 문자 클래스 안의 `\S` 는 같은 의미로 바꿀 수 없어 규칙 JSON 으로 내보내지 않습니다(`IllegalStateException`).
- GUIDE_JPQL 이 없는 `LikeMode.EXACT` 를 안내하던 문서 오류.
- `DATE_AFTER`·`DATE_BEFORE` 가 `java.util.Date`(및 `java.sql.Date`·`Timestamp`·`Instant`) 필드를 비교하지 않고 항상 통과하던 문제.
- `DATE_AFTER`·`DATE_BEFORE` 에서 타입이 다른 값(예: `LocalDate` 필드와 문자열)을 비교하면 `ClassCastException` 이 나던 문제.
- `<input type="datetime-local">` 값(`2024-01-01T09:00`)을 서버는 비교하지 않고, 브라우저는 항상 실패시키던 문제 — 이제 양쪽 모두 시간까지 비교.
- 와일드카드 행(`items[].qty`)의 `BiPredicate` 가 두 번째 인자로 루트 객체 대신 행 객체를 받아 항상 실패하던 문제.
- `S2BindValidator` 가 대상 객체에서 읽을 수 없는 경로(없는 속성, `null` 중간 경로)의 오류를 기록할 때 `NotReadablePropertyException` 으로 실패하던 문제 — `FieldError` 로 기록.
- 브라우저 판정 불일치: 아무것도 고르지 않은 다중 선택(빈 배열)을 빈 값으로 보지 않아 필수 외 규칙이 실패하던 문제, `DATE` 가 `"1a240101"` 같은 값을 통과시키던 문제.
- `TEXT_COMBINE`·`PASSWORD_ANSWR` 의 공백 문자를 명시해, 브라우저가 NBSP 등 유니코드 공백을 서버와 달리 허용하던 문제.
- `S2Util.getValue` 가 평면 Map 의 점(.)·대괄호가 든 키(`"address.zip"`)를 중첩 경로로만 풀어 찾지 못하던 문제 — 경로의 첫 마디가 Map 에 없으면 리터럴 키를 사용.
- 규칙 JSON(`getRulesJson`)이 `<`·`>`·`&` 를 이스케이프하지 않아, JSP 처럼 `<script>` 안에 그대로 넣으면 라벨·메시지의 `</script>` 가 스크립트를 끝낼 수 있던 문제 — `\u003c` 등으로 이스케이프(JSON 값은 같음).
- `s2.validator.js` 가 name 에 따옴표·역슬래시가 있는 필드에서 CSS 선택자 오류로 검증을 중단하던 문제, 규칙 JSON 이 없거나 깨져 제출을 막을 때 아무 표시가 없던 문제(콘솔 오류 추가).
- 서버 `DATE` 가 `"+2020101"` 처럼 부호가 섞인 값을 날짜로 받던 문제.
- 서버가 NBSP(U+00A0)·전각 공백(U+3000) 등 유니코드 공백을 공백으로 보지 않아(Java `String.trim()` 기준) 브라우저와 판정이 다르던 문제 — 서버도 자바스크립트 `trim()` 과 같은 공백 정의를 씀. Java·JS 에 같은 무작위 입력을 넣어 비교하는 차분 퍼즈 시험으로 발견.
- s2-core 경량 캐시(Caffeine 이 없을 때 쓰는 `S2OptimisticCache`): 상한에 이른 뒤 축출과 조회가 겹치면 정렬 중 값이 바뀌어
  `IllegalArgumentException: Comparison method violates its general contract!` 가 나고, 이 예외가 `get()` 호출자에게까지 전파되던 문제.
- 같은 캐시에서 서로 다른 키가 몰리면(전부 miss) 축출이 따라가지 못해 크기가 상한을 크게 넘어 계속 늘던 문제. 이제 상한의 2배에 이르면 새 값은 캐싱하지 않고 반환만 합니다.
- `S2Cache.getMethodHandle` 이 조회 종류(`LookupType`)를 캐시 키에 넣지 않아, 먼저 조회한 종류의 결과가 굳던 문제
  (`BOTH` 를 먼저 조회하면 public 만 찾는 `METHOD` 조회가 private 메서드를 돌려주고, 반대면 `BOTH` 가 private 멤버를 못 찾음).
- s2-core 로그 메시지(캐시 엔진 안내, 가상 스레드 안내)에 ANSI 색 코드가 들어 있어 로그 파일·로그 수집기에 제어 문자가 남던 문제.
- 경량 캐시 통계(`-Ds2.cache.stats.enabled=true`)가 조회마다 캐시를 두 번 찾고 경합이 큰 카운터를 쓰던 문제. 이제 한 번만 찾고 `LongAdder` 로 셉니다.
- `S2Copier.deep()`: `LocalDate`·`BigDecimal` 등 JDK 값 타입 필드가 복사되지 않고 빠지던 문제, 배열이 원본과 공유되던 문제. `Date` 는 복제하고,
  `TreeSet`·`TreeMap` 은 같은 정렬 기준으로, `LinkedHashMap` 은 순서를 유지해 복사하며, JPA 엔티티처럼 생성자가 `protected` 여도 깊은 복사합니다.
- `S2Util.setValue`: setter 가 없을 때 필드에 직접 쓰는 폴백이 항상 실패하던 문제(필드 읽기 핸들로 쓰려 했음). static·final 필드는 쓰지 않습니다.
- `S2Util.getPercentage`: 나누어떨어지지 않는 비율(1/3 등)에서 `ArithmeticException` 이 나던 문제. 자리수는 반올림(HALF_UP), 분모가 0 이면 빈 문자열.
- `S2Util.getValueAll`: `issue()`·`getaway()` 같은 메서드를 `sue`·`away` 속성으로 잘못 읽던 문제.
- `S2StringUtil.substringByBytes`: 이모지 등 4바이트 문자를 6바이트로 세고 반으로 잘라 깨진 문자열을 돌려주던 문제.
- `S2StringUtil` 의 대소문자 변환(`toCamelCase`·`toSnakeCase` 등)이 시스템 로케일을 따라 터키어 환경에서 `userİd` 처럼 바뀌던 문제.
- JSON 파서가 `\u` 이스케이프에 아라비아 숫자·전각 문자 등 비ASCII 문자를 16진수로 받아들이던 문제.
- `S2DateUtil` 파싱에서 오전/오후(`a`)·월 이름(`MMM`)·요일(`E`) 같은 문자형 필드가 기본 로케일로만 읽혀, 한국어 JVM 에서 `11:30 PM` 을 읽지 못하던 문제. 기본 로케일 다음에 영어로도 시도하고 대소문자를 구분하지 않습니다.
- `S2JsonUtil.toJson`: 하위 클래스가 상위 클래스와 같은 이름의 필드를 다시 선언하면 같은 키가 두 번 나오던 문제(이제 하위 클래스 값만), `java.sql.Timestamp` 의 나노초가 잘리던 문제.
- Spring Boot 자동 설정: 한 JVM 에 컨텍스트가 여럿일 때(시험 컨텍스트 캐시·부모/자식) 먼저 만든 컨텍스트가 닫히면 다른 컨텍스트가 설정한 번들·로케일·`MessageSource` 연결까지 지우던 문제.
  이제 항목마다 그 값을 설정한 가장 최근의 살아 있는 컨텍스트 값을 쓰고, 없으면 기본값으로 되돌립니다. 컨텍스트가 설정하지 않은 항목은 건드리지 않습니다.

## [2.1.0]

- **한국어 조사 자동 완성 (Smart Postposition Auto-Completion)**:
  - `{0|은}`, `{0|이}`, `{0|을}`, `{0|로}` 등 하나의 조사만 지정하더라도 앞 단어의 받침 유무(및 'ㄹ' 받침 예외)에 따라 알맞은 조사를 자동으로 완성하도록 개선.
  - 슬래시 쌍 표기(`{0|은/는}`)로 인한 오타(예: `이/로`) 방지 및 문법 단순화.

## [2.0.0] - 2026-10-02

1.1.8 대비 변경입니다. 하위 호환을 유지하지 않는 **메이저 버전**이며 동작 변경이 있으므로 올리기 전에 [업그레이드 안내](#업그레이드-안내)를 확인하십시오.

### ⚠️ 호환성

- **`s2-support` 1.x 는 `s2-core` 2.0.0 과 호환되지 않습니다.** `s2-support` 1.1.3 의 `S2AutoConfiguration`이 삭제된
  `S2LogManager.touch()`를 호출하므로, Spring Boot 애플리케이션이 시작 시
  `NoSuchMethodError: 'void io.github.devers2.s2util.log.S2LogManager.touch()'`로 실패합니다. `s2-support`의 새 이름인 `s2-kit`
  1.0.0 으로 옮기십시오. `s2-support`의 `S2JsonUtil`은 삭제되었으며 `s2-core`의 `io.github.devers2.s2util.json.S2JsonUtil`을 사용합니다.
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
  - `ASSERT_TRUE`: "{0|을} 선택(동의)해야 합니다." / "{0} must be checked." (이전 "반드시 true여야 합니다");
    `ASSERT_FALSE`: "{0|은} 선택할 수 없습니다." / "{0} must not be checked."
  - 와일드카드 행의 상대 표기 비교 대상(`items[].end` → `"start"`)도 `start`가 아니라 라벨(`시작일`)로 보여 줍니다.
  - 순환 참조 오류는 번들 키 `valid.err.circular`(이전 `ERR_CIRCULAR_REFERENCE`)를 쓰고 로케일에 맞춰 표시합니다
    ("{0}에서 순환 참조가 감지되었습니다."). 이전에는 항상 한국어였습니다.
- **NESTED/EACH 와 바깥 필드**: NESTED/EACH 하위 검증기와 와일드카드 행 안의 필드 간 비교 대상과 `when` 조건을 현재 객체 → 바깥 객체들 →
  루트 순서로 찾습니다(브라우저도 같은 순서). 이전에는 하위 검증기가 바깥 필드를 볼 수 없어, 예를 들어 EACH 안의
  `items[].end DATE_AFTER "globalStart"`나 NESTED 안의 `when("globalType", "X")`가 조용히 건너뛰어졌습니다. 대상 라벨도 같은 방식으로 찾습니다
  ("…globalStart보다" 대신 "…전체 시작일보다").
- **인덱스 빈칸**: 와일드카드/EACH 컬렉션의 `null` 요소(예: 0·2번 행만 전송되고 1번 행 삭제)는 행으로 검증하지 않습니다(브라우저와 같음).
- **조건**: 빈 문자열은 브라우저의 빈 폼 필드처럼 빈 값으로 봅니다 (`when(field, null)`).
- **서버 전용 안내**: 필드에 커스텀 람다 규칙이 있으면 `getRulesJson()`이 정의 위치마다 한 번 `INFO` 로그로 알립니다. 이 규칙은 브라우저로
  내보내지지 않습니다(이전에는 조용히 빠짐).
- **중첩 깊이**: `NESTED`/`EACH`는 64단계에서 멈추고 `valid.err.maxdepth`를 보고합니다 (`StackOverflowError` 방지).
- **`check(value, label)`**: 예외의 필드 이름이 내부 키 `"value"` 대신 라벨입니다.
- **`EMAIL`**: 최상위 도메인 2~63자를 허용합니다 (이전 2~6자라 `.technology` 등을 거부). 빈 레이블(`b..com`)과 `-`로 시작·끝나는 레이블은
  거부합니다.
- **브라우저 히든 필드**: 1px 앵커가 와일드카드 행에도 적용되고, `display:none` 컨테이너(닫힌 탭·아코디언) 안의 필드는 가장 바깥 숨은
  컨테이너 뒤에 앵커를 둡니다. 라디오·체크박스 그룹은 앵커 하나, 메시지는 `aria-hidden` 대신 `aria-label`로 제공하며, 렌더링된
  `position:fixed` 필드를 숨은 것으로 오판하지 않습니다.
- **규칙 JSON 형식**: `getRulesJson()`이 배열 대신 `{"schemaVersion":1,"fields":[…]}`를 반환합니다. `s2.validator.js`는 두 형식을 모두
  받으며, 스크립트보다 높은 `schemaVersion`(서버와 브라우저 스크립트의 릴리스 불일치)이면 콘솔에 한 번 경고합니다. JSON 을 직접
  파싱한다면 `fields`를 읽으십시오.
- **클라이언트 내보내기**: `REGEX` 규칙이 Java 전용 문법(`(?i)`, 소유 한정자, 원자 그룹, `\p{…}`, `\A`/`\z`, `\Q…\E`, 문자 클래스 교집합
  등)을 쓰면 `getRulesJson()`이 `IllegalStateException`을 던집니다. 서버 전용 검증기는 영향이 없습니다.
- **로케일**: 검증기가 생성 시점의 기본 로케일을 복사해 두지 않습니다. `S2Validator.setDefaultLocale(null)`은 JVM 기본값으로 되돌립니다
  (이전에는 무시).
- **로그**: `System.out`/`System.err`를 교체하지 않고 배너 스레드도 띄우지 않습니다. SLF4J 브리지는 `org.slf4j.Logger`
  인터페이스에서 메서드를 한 번 찾아 묶은 `MethodHandle`로 호출합니다(로그 호출마다 `Method.invoke`를 쓰지 않음). SLF4J 가 클래스패스에
  없으면 이전과 같이 내장 로거를 사용합니다.

### 추가

- 브라우저: 폼 없는 검증 `S2Validator.check(rules, data)`(중첩·펼친 데이터, 서버와 같은 판정)와 실시간 검증(`data-s2-live="blur"|"input"`,
  `S2Validator.setLiveMode`). 실시간 검증은 초점을 옮기지 않으며 렌더러 훅 `showField`(`classRenderer` 구현 포함)를 씁니다. 폼 검증도 같은
  판정 엔진을 사용합니다.
- Spring Boot 자동 설정: s2-validator 를 추가하면 메시지 키를 Spring `MessageSource`에서 먼저 찾고(`messages.properties`로
  검증 메시지 변경), `s2.validator.bundle`, `s2.validator.default-locale`, `s2.validator.use-message-source`,
  `s2.validator.enabled`를 지원합니다. Spring Boot 는 컴파일 전용 의존성이라 Spring 이 없는 환경에는 영향이 없습니다.
- `S2Validator.setMessageResolver(S2MessageResolver)`: 검증 번들보다 먼저 조회할 메시지 출처(예: DB)를 연결합니다.
- `s2-core`: 의존성 없는 경량 JSON 유틸 `io.github.devers2.s2util.json.S2JsonUtil`(`toJson`, `parse`, `parseObject`,
  `parseArray`, `fromJson`, `fromJsonList`, `convert`)과 `S2JsonException`. 기본은 엄격한 표준 JSON 이며 느슨한 문법은 `Feature`로
  켭니다. 뒤따르는 문자·깊은 중첩(512)·잘못되거나 너무 긴 숫자(1,000자)를 위치와 함께 거부하고, 지원하지 않는 타입·순환 참조·NaN·손실되는 숫자 변환은
  예외입니다(`null`이나 깨진 JSON 을 돌려주지 않음). 사람이 쓰는 파일용으로 공개 규격 JSON5 를 받는 `Feature.JSON5`가 있습니다. 지원 타입은 클래스 Javadoc 에 명시되어 있으며, 그 밖의 기능은 Jackson 을
  사용하십시오. `s2-support`의 같은 이름 클래스를 대체합니다. 인자 없는 생성자가 없는 불변 VO 는 생성자 파라미터 이름으로 만들고,
  Hibernate·Spring AOP 프록시는 실제 객체로 씁니다(빈 프록시 필드를 쓰지 않음).
- `S2Validator.fromJson(json)`: 규칙 JSON(`getRulesJson()` 형식 + `messages`, `key` 정의용 키)으로 검증기를 만듭니다. 규칙을 DB·설정에
  두고 재배포 없이 바꿀 수 있습니다. 모르는 키·규칙 타입·연산자, 잘못된 기준값, 지원하지 않는 `schemaVersion`은 JSON 경로와 함께
  `IllegalArgumentException`으로 거부합니다. 사람이 쓰는 규칙 파일은 `fromJson(json, Feature.JSON5)`로 JSON5 를 받습니다.
- 조건 비교 연산자 `S2Operator`: `when(field, 연산자, 값)` / `and(...)`로 `NE`, `GT`/`GTE`/`LT`/`LTE`(숫자), `IN`/`NOT_IN`,
  `EMPTY`/`NOT_EMPTY` 조건을 씁니다(서버·브라우저 동일). 기존 `when(field, 값)`은 그대로 "같음"(`EQ`)입니다. 규칙 JSON 의 조건에는
  `EQ`가 아닐 때만 `"op"`가 들어갑니다.
- `BIZRNO` 검증번호 검사: `.rule(S2RuleType.BIZRNO, true)`는 형식에 더해 10번째 자리 검증번호를 확인합니다(서버·브라우저 동일). 기본값
  `.rule(S2RuleType.BIZRNO)`는 이전처럼 형식만 검사합니다.
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

- 규칙 JSON 이 열거형 조건 값·기준값을 `toString()` 대신 `name()`으로 내보냅니다. 서버는 `name()`으로 비교하므로 `toString()`을
  재정의한 열거형은 브라우저 판정이 달랐습니다. 규칙 JSON 생성은 이제 `S2JsonUtil`을 사용합니다.
- 브라우저: 체크박스 그룹에서 여러 개를 체크하면 `when("hobbies", "a")` 조건이 충족되지 않던 문제(값을 `"a,b"`로 합쳐 비교함)를
  고쳤습니다. 서버처럼 "포함"으로 판정합니다. 서버는 빈 컬렉션을 빈 값으로 봅니다(브라우저의 체크 안 한 그룹과 같음).
- 와일드카드 행의 규칙 실행 오류가 실제 경로(`items[1].qty`)를 담습니다.
- 라벨 없는 `field(Object)`가 문자열이 아닌 키에서 `ClassCastException`을 내지 않습니다.
- 공용 실행기가 종료된 뒤 다시 만들어집니다.
- `S2DateUtil.dateFormat`이 날짜로 해석하지 못한 값을 표준 오류(`System.err`)에 출력하지 않고 디버그 로그로 남깁니다(원래 값을 돌려주는 동작은 같음).
- `S2ValidationError`가 `errorArgs` 배열을 참조가 아닌 내용으로 비교합니다(`equals`/`hashCode`/`toString`). 같은 오류가 서로 다르다고
  판정되거나 `Set`에서 중복 제거되지 않던 문제를 고쳤습니다.
- jar 의 라이선스 파일: `s2-jpa`가 Jakarta Persistence 용 `LICENSE-EPL-2.0`, `LICENSE-EDL-1.0`을 함께 담으며(NOTICE 에 두 라이선스 명시),
  `s2-validator`는 없는 라이선스 파일을 목록에서 뺐고 NOTICE 에 Spring Boot 를 추가했습니다.

### 업그레이드 안내

1. `s2-support`를 `s2-kit` 1.0.0 으로 바꿔 `s2-core`와 함께 올리십시오 (호환성 참고).
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
9. 규칙 JSON 을 서버나 자체 스크립트에서 직접 파싱한다면 최상위 배열 대신 `fields` 배열을 읽으십시오.

## s2-validator-plugin [2.1.0]

- **중첩 DTO 검사**: 다른 타입 안에 선언한 DTO(컨트롤러 안의 `record Form`, static 중첩 클래스)도 검사합니다. 같은 파일의 `Form`,
  `Join.Form`, `import ...Join.Form`, `S2Validator.of(form)` 모두 찾으며, 이전에는 "DTO 소스를 찾을 수 없어 검증을 건너뜁니다" 경고만
  남겼습니다. DTO 필드는 그 타입 자신의 필드·레코드 컴포넌트·상위 클래스 필드만 모읍니다(이전에는 같은 파일의 모든 타입 필드를 섞어 바깥
  클래스 필드명이 통과했습니다).
- **경로 검사**: `address.city`, `items[0].name`, `products[].price`, `byCode[A1].qty`를 부분마다 검사합니다. 선언 타입을 따라 중첩
  객체와 배열·컬렉션·맵 값의 요소로 들어가며, `Map`·타입 변수·JDK 타입처럼 소스로 알 수 없는 지점에서는 멈춥니다. 이전에는 첫 부분
  (`address`, `items`)만 확인해 `address.ctiy` 같은 뒷부분 오타를 놓쳤습니다. 오류는 없는 부분과 그 부분을 찾은 클래스를 함께 보여 줍니다.
- **오류 줄 번호**: 여러 줄로 이어 쓴 체인에서 필드·기준값 오류가 체인 시작 줄이 아니라 해당 `.field(...)`·`.rule(...)` 줄을 가리킵니다.

## s2-validator-plugin [2.0.0] - 2026-10-02

- **Configuration cache**: `checkS2Validators`가 실행 시점에 `getProject()`를 호출하지 않아 `--configuration-cache`에서 동작합니다.
- **바인딩 검사**: `S2BindValidator.bind(...)` 결과에서 `validate`/`getRulesJson`을 호출하지 않으면 경고합니다. 삭제된
  `context`/`getOrRegister`/`getValidator` 검사는 없앴습니다.
- **record DTO**: record 컴포넌트를 필드로 인식하며, 소스를 Java 17 언어 수준으로 파싱합니다.
- **대상 DTO 추론**: 타입 인자 없는 `S2Validator.of(dto)`도 인자의 선언 타입(파라미터, 지역 변수, 필드, `var`, `new`, 캐스트)으로
  DTO 를 찾아 필드명을 검사합니다(이전에는 조용히 건너뜀). JDK 타입(`Map` 등)은 건너뛰고, 소스가 없는 클래스는 한 번만 알립니다.
- **증분 빌드·빌드 캐시**: `checkS2Validators`가 모든 프로젝트의 `src/main/java`를 입력으로, `build/s2-validator/checkS2Validators.txt`를
  출력으로 선언한 `@CacheableTask`입니다. 소스가 바뀌지 않으면 매 컴파일마다 다시 파싱하지 않습니다.
- **규칙 기준값·조건 값 검사**: 리터럴로 적힌 기준값이 규칙에 맞지 않으면(`.rule(MAX_LENGTH, "abc")`, 기준값 누락, `NESTED`에 문자열,
  `when(f, GT, "abc")` 등) 빌드를 실패시킵니다. 런타임이 조용히 틀리는 문법 오류 `REGEX`, 기준값을 받지 않는 규칙에 준 값, `JUMIN`/`BIZRNO`의
  잘못된 스위치 값도 막습니다. 비교 대상 필드가 DTO 에 없으면 경고합니다. 규칙별 전용 메서드 대신 이 검사로 컴파일 시점 안전성을 제공합니다.
