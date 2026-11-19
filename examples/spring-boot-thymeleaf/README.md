# s2-validator example — Spring Boot + Thymeleaf

🌐 **English** | [한국어](README.ko.md)

Runs the two ways to validate the server (Java) and the browser (JavaScript) with the same rules.

```bash
# from the repository root (Java 17+)
./gradlew -p examples/spring-boot-thymeleaf bootRun
```

Open http://localhost:8080. Inside the repository the example uses the **current sources** through `includeBuild("../..")`.

| Page | Approach | Code |
| :--- | :--- | :--- |
| `/join` sign-up | **1. S2 rules** — rules defined once and shared by the GET page (rules JSON) and the POST validation (`BindingResult`): conditional (business number for business members), Korean rules (mobile, business number check digit), password confirmation, number of interests | `JoinController` |
| `/profile` profile | **2. On top of Bean Validation** — `@NotBlank`, `@Size`, `@Pattern` stay as they are; `S2BeanValidation` makes the browser rules, and only the conditional rule Bean Validation lacks is added to `@Valid` with `S2SpringValidator` | `ProfileController`, `ProfileForm` |
| `POST /api/join` | REST — `validateAll(...).toMap()` answers in the same `{field: [messages]}` shape as the browser | `JoinController#api` |

```bash
curl -s -X POST localhost:8080/api/join -H 'Content-Type: application/json' -H 'Accept-Language: en' \
     -d '{"name":"Hong","email":"bad"}'
# {"email":["Invalid email format for 이메일."], ...}   (labels in this example are Korean)
```

**Things to try**
- Submit empty fields: the browser shows errors without sending the form (also live when leaving a field, `data-s2-live="blur"`).
- Remove the form's `data-s2-rules` attribute in the developer tools and submit: the server rejects it with the same rules (`th:errors`).
- The birth date on `/profile` (`@Past`) is not converted to a browser rule, so only the server checks it.

**Outside the repository**, remove `includeBuild("../..")` from `settings.gradle.kts` to use the Maven Central release in `build.gradle.kts` (`S2BeanValidation`, `S2SpringValidator`, `validateAll` and `MIN_SIZE` from 2.2.0).

Tests: `./gradlew -p examples/spring-boot-thymeleaf test` (also run in CI)
