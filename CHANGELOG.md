# Changelog

**English** | [한국어](./CHANGELOG.ko.md)

All notable changes to `s2-util` (`s2-core`, `s2-validator`, `s2-jpa`) and `s2-validator-plugin` are recorded here.

## [2.0.0] - Unreleased

Compared with 1.1.8. This is a **major release** that does not keep backward compatibility and contains behavior changes; read [Upgrade notes](#upgrade-notes) before upgrading.

### ⚠️ Compatibility

- **`s2-support` 1.x is not compatible with `s2-core` 2.0.0.** `S2AutoConfiguration` in `s2-support` 1.1.3 calls the removed
  `S2LogManager.touch()`, so a Spring Boot application fails at startup with
  `NoSuchMethodError: 'void io.github.devers2.s2util.log.S2LogManager.touch()'`. Upgrade `s2-support` to 2.0.0. `S2JsonUtil`
  was removed from `s2-support`; use `io.github.devers2.s2util.json.S2JsonUtil` in `s2-core`.
- **Removed public API** (compared with the 1.1.8 artifacts):
  - `S2LogManager.touch()`
  - `DefaultS2Logger.printWarningBannerOnce()`, `DefaultS2Logger.markAdapterConfigured()` (the warning banner and its thread were removed)
  - `io.github.devers2.s2util.validation.annotation.CheckReturnValue` (replaced by Error Prone's `@CheckReturnValue`)
  - `message(Locale, String)` → replaced by `message(String, Locale)` (template first, like `message(String)`); `.ko()`
    and `.en()` are unchanged
- **The global validator registry and `S2ValidatorFactory` are removed.** `S2BindValidator.context(key, supplier)` and
  the whole `S2ValidatorFactory` class (`getOrRegister`, `getValidator`, `getRulesJson(key, locale)`,
  `getRulesJson(validator, locale)`) no longer exist. A key was bound to the first validator built for it, so rule sets
  that differed by role were silently shared, and caching saved only ~0.5µs per request (the rules JSON for the GET
  form, ~15µs, was never cached). Use `S2BindValidator.bind(validator)` with Spring, and `validator.getRulesJson(locale)`
  to export rules without Spring; see Upgrade notes.

### Changed (behavior)

- **Whitespace**: built-in rules judge string values (and cross-field target values) after trimming leading/trailing
  whitespace, matching the browser. Only the judgment changes; the stored value is not modified. Custom lambdas still
  receive the raw value.
- **Numbers**: `MIN_VALUE`/`MAX_VALUE` accept only plain decimal notation on both server and client. `"1,000"`, `"25abc"`,
  `"25d"`, `"NaN"` are invalid (the browser previously read `"1,000"` as `1`).
- **Regex**: the browser always evaluates regex rules as a full match (`^(?:…)$`), like Java `matcher.matches()`. Patterns
  with a one-sided anchor (`^\d+`) or an anchor on a single alternative (`^a|b$`) no longer partially match.
- **`ASSERT_TRUE` / `ASSERT_FALSE`**: an empty value is now judged instead of skipped, so an unchecked required consent
  checkbox is rejected. `"true"`/`"on"` and `"false"`/`"off"`/`""` strings are recognized.
- **Custom lambdas**: skipped when the value is empty (no `NullPointerException`); add `.includeEmpty()` to run them on
  empty values. Exceptions thrown inside a lambda are wrapped in `S2RuleExecutionException` (see below).
- **`PASSWORD`**: 8–64 characters with at least one letter, one digit, and one ASCII special character (was 9–32
  characters with the special characters `!@#$%^&*` only).
- **`JUMIN`**: the check digit is **not verified by default**. Since October 2020, any newly issued or changed number gets
  random digits regardless of birth date, so the check digit rejected valid numbers. Use `.rule(S2RuleType.JUMIN, true)`
  to verify the legacy check digit for people born before October 2020.
- **`DATE`**: no longer rejects dates older than "current year − 100" (e.g. birth dates of people over 100). Accepts
  `java.util.Date` / `java.sql.Date` (previously always invalid).
- **`MIN_BYTE` / `MAX_BYTE`**: count UTF-8 bytes regardless of the platform charset (Java 17 on Korean Windows used MS949).
- **Rule criteria are validated at creation**: `MIN_VALUE`/`MAX_VALUE` require a finite number (NaN/Infinity/`"abc"` throw
  `IllegalArgumentException`); length and byte rules require an integer
  (surrounding whitespace is ignored at both creation and validation, like the browser).
- **Cross-field messages** (`EQUALS_FIELD`, `DATE_AFTER`, `DATE_BEFORE`) show the target field's label instead of its
  internal name when the target is declared in the same validator.
- **Exception mode**: validation failures throw `S2ValidationException` (with `getFieldName()` / `getErrorCode()`), and
  errors inside custom rules throw `S2RuleExecutionException` (the original exception only via `getCause()`, not in the
  message). Both extend `S2RuntimeException`, so existing `catch (S2RuntimeException e)` code keeps working.
- **Messages**:
  - Korean particles are no longer dropped for words whose final consonant cannot be determined (Latin letters, symbols):
    `[documentId]은(는) 필수 입력 항목입니다.` instead of `[documentId] 필수 입력 항목입니다.`
  - `ASSERT_TRUE`: "{0|을/를} 선택(동의)해야 합니다." / "{0} must be checked." (was "must be true");
    `ASSERT_FALSE`: "{0|은/는} 선택할 수 없습니다." / "{0} must not be checked."
  - A relative cross-field target in wildcard rows (`items[].end` → `"start"`) shows the label (`시작일`), not `start`.
  - The circular reference error uses the bundle key `valid.err.circular` (was `ERR_CIRCULAR_REFERENCE`) and is
    localized ("Circular reference detected at {0}."); it was Korean-only.
- **NESTED/EACH and outer fields**: cross-field targets and `when` conditions inside NESTED/EACH sub-validators and
  wildcard rows are looked up in the current object, then in each outer object up to the root (the browser uses the same
  order). Previously a sub-validator could not see outer fields, so e.g. `items[].end DATE_AFTER "globalStart"` inside
  EACH or `when("globalType", "X")` inside NESTED was silently skipped. Target labels are resolved the same way
  ("…전체 시작일보다" instead of "…globalStart보다").
- **Index gaps**: `null` elements in wildcard/EACH collections (e.g. rows 0 and 2 submitted, row 1 deleted) are not
  validated as rows, matching the browser.
- **Conditions**: a blank string counts as empty, like an empty form field in the browser (`when(field, null)`).
- **Server-only notice**: `getRulesJson()` logs an `INFO` notice once per definition site when a field has custom
  lambda rules, which are not exported to the browser (previously they were dropped silently).
- **Nesting depth**: `NESTED`/`EACH` stop at depth 64 and report `valid.err.maxdepth` instead of risking a
  `StackOverflowError`.
- **`check(value, label)`**: exceptions report the label as the field name (was the internal key `"value"`).
- **`EMAIL`**: top-level domains of 2–63 letters are accepted (was 2–6, rejecting e.g. `.technology`); empty labels
  (`b..com`) and labels starting/ending with `-` are rejected.
- **Browser, hidden fields**: the 1px anchor also works for wildcard rows, for fields inside `display:none` containers
  (closed tabs/accordions; the anchor is placed after the outermost hidden container), uses one anchor per radio/checkbox
  group, exposes the message via `aria-label` instead of `aria-hidden`, and no longer treats rendered `position:fixed`
  fields as hidden.
- **Rules JSON format**: `getRulesJson()` returns `{"schemaVersion":1,"fields":[…]}` instead of a bare array.
  `s2.validator.js` accepts both, and warns once in the console when `schemaVersion` is newer than the script (server and
  browser script from different releases). If you parse the JSON yourself, read `fields`.
- **Client export**: `getRulesJson()` throws `IllegalStateException` if a `REGEX` rule uses Java-only syntax (`(?i)`,
  possessive quantifiers, atomic groups, `\p{…}`, `\A`/`\z`, `\Q…\E`, class intersection, …). Server-only validators are
  not affected.
- **Locale**: validators no longer snapshot the default locale at creation. `S2Validator.setDefaultLocale(null)` resets to
  the JVM default (was ignored).
- **Logging**: `System.out`/`System.err` are no longer replaced and no banner thread is started. The SLF4J bridge
  resolves methods once on the `org.slf4j.Logger` interface and calls them through bound `MethodHandle`s instead of
  `Method.invoke` on every log call; without SLF4J on the class path the built-in logger is used as before.

### Added

- `s2-core`: dependency-free lightweight JSON utility `io.github.devers2.s2util.json.S2JsonUtil` (`toJson`, `parse`,
  `parseObject`, `parseArray`, `fromJson`, `fromJsonList`, `convert`) and `S2JsonException`. Strict standard JSON by default,
  with relaxed syntax through `Feature`. Trailing content, deep nesting (512) and malformed or overlong (1,000 characters) numbers are rejected with the
  position; unsupported types, circular references, NaN and lossy number conversions throw (never `null` or broken JSON).
  `Feature.JSON5` accepts the published JSON5 standard for hand-written files.
  The supported types are listed in the class Javadoc; use Jackson for anything beyond them. Replaces the class of the same
  name in `s2-support`. Immutable value objects without a no-arg constructor are created through their constructor by
  parameter name, and Hibernate/Spring AOP proxies are written from their real object (never from their empty proxy fields).
- `S2Validator.fromJson(json)`: builds a validator from rules JSON (the `getRulesJson()` shape plus the definition keys
  `messages` and `key`), so rules can live in a database or configuration and change without a redeploy. Unknown keys,
  rule types or operators, bad criteria and unsupported `schemaVersion` fail with `IllegalArgumentException` naming the
  JSON path.
- Condition operators `S2Operator`: `when(field, operator, value)` / `and(...)` support `NE`, `GT`/`GTE`/`LT`/`LTE` (numeric),
  `IN`/`NOT_IN` and `EMPTY`/`NOT_EMPTY`, judged the same on the server and in the browser. `when(field, value)` stays
  equality (`EQ`). Conditions in the rules JSON carry `"op"` only when it is not `EQ`.
- `BIZRNO` check digit: `.rule(S2RuleType.BIZRNO, true)` also verifies the 10th (check) digit, on the server and in the
  browser. The default `.rule(S2RuleType.BIZRNO)` still checks the format only.
- `S2Validator.resetAll()`, `resetDefaultLocale()`, `resetValidationBundle()`,
  `S2ResourceBundle.resetDefaultBasename()` for resetting global state (e.g. in tests).
- `S2BindValidator.bind(validator)`: bind a validator instance for `validate(target, bindingResult)` and `getRulesJson()`.
- `S2Validator.getRulesJson()` / `getRulesJson(locale)`: export the validator's rules as JSON for the browser (replaces
  `S2ValidatorFactory.getRulesJson(validator, locale)`).
- `.includeEmpty()` modifier for custom lambda rules.
- `.message(template)`: a default message for every language without a language-specific one. Lookup order: bundle key →
  request language → default message → default locale's language → built-in.
- `S2ValidationException`, `S2RuleExecutionException`.
- Browser: `S2Validator.setRenderer({ show, clear, clearField })` to draw errors the application's way instead of native
  bubbles, and `S2Validator.classRenderer()` (adds `is-invalid`, writes messages into `[data-s2-error-for]`, focuses the
  first error).
- Browser: `S2Validator.reindex(form, collection)` renumbers row indices to `0..n-1` after a row is deleted (explicit
  call only; never automatic).
- Browser: a 1px anchor next to hidden/non-rendered fields without a `{field}_error` proxy, so the native message is shown.

### Fixed

- The rules JSON exports enum condition values and criteria by `name()` instead of `toString()`. The server compares by
  `name()`, so an enum overriding `toString()` was judged differently in the browser. Rules JSON is now written with
  `S2JsonUtil`.
- Browser: a condition such as `when("hobbies", "a")` was not satisfied when several boxes of a checkbox group were
  checked (the values were joined into `"a,b"`); it now means "contains", as on the server. The server treats an empty
  collection as empty, like an unchecked group in the browser.
- Wildcard rows report rule execution errors with the concrete path (`items[1].qty`).
- `field(Object)` without a label no longer throws `ClassCastException` for non-String keys.
- The common executor is recreated after shutdown.
- `S2ValidationError` compares its `errorArgs` array by content, not by reference (`equals`/`hashCode`/`toString`), so
  equal errors are equal and deduplicate in a `Set`.

### Upgrade notes

1. Upgrade `s2-support` together with `s2-core` (see Compatibility).
2. Replace the registry with `S2BindValidator.bind(...)`. Both the GET form and the POST handler keep using the same rule
   definition, so they still apply identical rules:
   ```java
   // Before
   S2BindValidator.context("signup", this::signupRules).getRulesJson();    // GET
   S2BindValidator.context("signup", this::signupRules).validate(cmd, r);  // POST
   // After
   S2BindValidator.bind(signupRules()).getRulesJson();    // GET
   S2BindValidator.bind(signupRules()).validate(cmd, r);  // POST
   ```
   Keep the validator in a field or Spring bean (`S2BindValidator.bind(signupValidator)`) only if building the rules is
   itself expensive. Without Spring, replace `S2ValidatorFactory.getRulesJson(validator, locale)` with
   `validator.getRulesJson(locale)`.
3. If you copied `s2.validator.js` into your application, replace the copy (or serve it from the jar at
   `/s2-util/js/s2.validator.js`); the browser behavior above lives in that file.
4. To keep the previous `PASSWORD` policy, use
   `.rule(S2RuleType.REGEX, "^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{9,32}$")`.
5. To keep the previous `JUMIN` check digit verification, use `.rule(S2RuleType.JUMIN, true)`.
6. Custom lambdas that must run on empty values (e.g. "one of two fields is required") need `.includeEmpty()`.
7. If exception-mode failures are handled as `S2RuntimeException`, consider handling `S2ValidationException`
   (input error → 400) and `S2RuleExecutionException` (bug → 500) separately.
8. Swap the arguments of `.message(Locale, "…")` to `.message("…", Locale)`.
9. If your server code or scripts parse the rules JSON directly, read the `fields` array instead of a top-level array.

## s2-validator-plugin [2.0.0] - Unreleased

- **Configuration cache**: `checkS2Validators` no longer calls `getProject()` at execution time and works with
  `--configuration-cache`.
- **Binding check**: warns when `S2BindValidator.bind(...)` is neither validated nor exported (`validate`/`getRulesJson`);
  the checks for the removed `context`/`getOrRegister`/`getValidator` are gone.
- **Record DTOs**: record components are recognized as fields; sources are parsed at the Java 17 language level.
- **Target DTO inference**: `S2Validator.of(dto)` without a type argument is now checked, using the argument's declared
  type (parameter, local variable, field, `var`, `new`, cast); previously it was silently skipped. JDK types (`Map`, ...)
  are skipped, and a class without source is reported once.
- **Incremental build & build cache**: `checkS2Validators` is a `@CacheableTask` with the `src/main/java` trees of all
  projects as inputs and `build/s2-validator/checkS2Validators.txt` as output, so unchanged sources are not re-parsed on
  every compile.
