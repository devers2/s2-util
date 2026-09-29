# Changelog

**English** | [한국어](./CHANGELOG.ko.md)

All notable changes to `s2-util` (`s2-core`, `s2-validator`, `s2-jpa`) and `s2-validator-plugin` are recorded here.

## [1.2.0] - Unreleased

Compared with 1.1.8. This release contains **behavior changes**; read [Upgrade notes](#upgrade-notes) before upgrading.

### ⚠️ Compatibility

- **`s2-support` 1.1.x is not compatible with `s2-core` 1.2.0.** `S2AutoConfiguration` in `s2-support` 1.1.3 calls the removed
  `S2LogManager.touch()`, so a Spring Boot application fails at startup with
  `NoSuchMethodError: 'void io.github.devers2.s2util.log.S2LogManager.touch()'`. Upgrade `s2-support` to the version released
  together with `s2-core` 1.2.0.
- **Removed public API** (compared with the 1.1.8 artifacts):
  - `S2LogManager.touch()`
  - `DefaultS2Logger.printWarningBannerOnce()`, `DefaultS2Logger.markAdapterConfigured()` (the warning banner and its thread were removed)
  - `io.github.devers2.s2util.validation.annotation.CheckReturnValue` (replaced by Error Prone's `@CheckReturnValue`)

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
  `IllegalArgumentException`); length and byte rules require an integer.
- **Cross-field messages** (`EQUALS_FIELD`, `DATE_AFTER`, `DATE_BEFORE`) show the target field's label instead of its
  internal name when the target is declared in the same validator.
- **Exception mode**: validation failures throw `S2ValidationException` (with `getFieldName()` / `getErrorCode()`), and
  errors inside custom rules throw `S2RuleExecutionException` (the original exception only via `getCause()`, not in the
  message). Both extend `S2RuntimeException`, so existing `catch (S2RuntimeException e)` code keeps working.
- **Registry**: a supplier-class collision on the same context key is logged at `DEBUG` (was a false-positive `WARN` for the
  documented GET/POST usage). `S2BindValidator.of(validator)` is the recommended path.
- **Client export**: `getRulesJson()` throws `IllegalStateException` if a `REGEX` rule uses Java-only syntax (`(?i)`,
  possessive quantifiers, atomic groups, `\p{…}`, `\A`/`\z`, `\Q…\E`, class intersection, …). Server-only validators are
  not affected.
- **Locale**: validators no longer snapshot the default locale at creation. `S2Validator.setDefaultLocale(null)` resets to
  the JVM default (was ignored).
- **Logging**: `System.out`/`System.err` are no longer replaced and no banner thread is started.

### Added

- `S2Validator.resetAll()`, `resetDefaultLocale()`, `resetValidationBundle()`, `S2ValidatorFactory.clear()`,
  `S2ResourceBundle.resetDefaultBasename()` for resetting global state (e.g. in tests).
- `S2BindValidator.of(validator)`: bind a validator instance directly without the global registry.
- `.includeEmpty()` modifier for custom lambda rules.
- `S2ValidationException`, `S2RuleExecutionException`.
- Browser: a 1px anchor next to hidden/non-rendered fields without a `{field}_error` proxy, so the native message is shown.

### Fixed

- Wildcard rows report rule execution errors with the concrete path (`items[1].qty`).
- `field(Object)` without a label no longer throws `ClassCastException` for non-String keys.
- The common executor is recreated after shutdown.

### Upgrade notes

1. Upgrade `s2-support` together with `s2-core` (see Compatibility).
2. If you copied `s2.validator.js` into your application, replace the copy (or serve it from the jar at
   `/s2-util/js/s2.validator.js`); the browser behavior above lives in that file.
3. To keep the previous `PASSWORD` policy, use
   `.rule(S2RuleType.REGEX, "^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{9,32}$")`.
4. To keep the previous `JUMIN` check digit verification, use `.rule(S2RuleType.JUMIN, true)`.
5. Custom lambdas that must run on empty values (e.g. "one of two fields is required") need `.includeEmpty()`.
6. If exception-mode failures are handled as `S2RuntimeException`, consider handling `S2ValidationException`
   (input error → 400) and `S2RuleExecutionException` (bug → 500) separately.

## s2-validator-plugin [1.2.0] - Unreleased

- **Configuration cache**: `checkS2Validators` no longer calls `getProject()` at execution time and works with
  `--configuration-cache`.
- **Record DTOs**: record components are recognized as fields; sources are parsed at the Java 17 language level.
