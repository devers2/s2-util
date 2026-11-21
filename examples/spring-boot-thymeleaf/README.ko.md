# s2-validator 예제 — Spring Boot + Thymeleaf

🌐 [English](./README.md) | **한국어**

서버(Java)와 브라우저(JavaScript)를 같은 규칙으로 검증하는 두 가지 방식을 실행해 볼 수 있는 예제입니다.

```bash
# 저장소 루트에서 (Java 17+)
./gradlew -p examples/spring-boot-thymeleaf bootRun
```

http://localhost:8080 을 엽니다. 저장소 안에서는 `includeBuild("../..")`로 **현재 소스**를 씁니다.

| 화면 | 방식 | 코드 |
| :--- | :--- | :--- |
| `/join` 회원가입 | **1. S2 규칙** — 규칙을 한 번 정의해 GET 화면(규칙 JSON)과 POST 서버 검증(`BindingResult`)이 함께 씀. 조건부(사업자일 때 사업자번호), 한국 규칙(휴대폰·사업자번호 검증번호), 비밀번호 확인, 관심사 개수 | `JoinController` |
| `/profile` 프로필 | **2. Bean Validation 에 얹기** — `@NotBlank`·`@Size`·`@Pattern` 은 그대로, `S2BeanValidation`이 브라우저 규칙을 만들고 Bean Validation 에 없는 조건부 규칙만 `S2SpringValidator`로 `@Valid`에 추가 | `ProfileController`, `ProfileForm` |
| `POST /api/join` | REST — `validateAll(...).toMap()`이 브라우저와 같은 `{필드: [메시지]}` 형식으로 응답 | `JoinController#api` |

```bash
curl -s -X POST localhost:8080/api/join -H 'Content-Type: application/json' -H 'Accept-Language: ko' \
     -d '{"name":"홍길동","email":"bad"}'
# {"email":["이메일 형식이 올바르지 않습니다."],"mobile":["휴대폰 번호는 필수 입력 항목입니다."], ...}
```

**확인해 볼 것**
- 빈 칸으로 제출하면 브라우저가 서버에 보내지 않고 오류를 표시합니다(칸을 벗어날 때도 실시간 검사, `data-s2-live="blur"`).
- 개발자 도구에서 폼의 `data-s2-rules` 속성을 지우고 제출하면 서버가 같은 규칙으로 막습니다(`th:errors`).
- `/profile` 의 생년월일(`@Past`)은 브라우저 규칙으로 바꾸지 않는 제약이라 서버에서만 검사합니다.

**저장소 밖에서 쓰려면** `settings.gradle.kts`의 `includeBuild("../..")`를 지우면 `build.gradle.kts`에 적은 Maven Central 배포본을 받습니다(`S2BeanValidation`·`S2SpringValidator`·`validateAll`·`MIN_SIZE`는 2.2.0 부터).

시험: `./gradlew -p examples/spring-boot-thymeleaf test` (CI 에서도 실행)
