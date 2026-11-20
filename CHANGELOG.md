# Changelog

**English** | [한국어](./CHANGELOG.ko.md)

All notable changes to `s2-util` (`s2-core`, `s2-validator`, `s2-jpa`) and `s2-validator-plugin` are recorded here.

## [Unreleased]

### ⚠️ Compatibility

- **`S2Validator`, `S2Field`, `S2Rule` and `S2Condition` are no longer `Serializable`.** With any lambda rule, serialization always failed with `NotSerializableException` because of an internal lambda, so the contract could not be kept.
  A validator is configuration: share it as a `static final` constant (or a Spring bean) instead of serializing it into a session.
  If an object stored in a session, or a view/session-scoped bean (JSF, Vaadin, …), holds a validator in an instance field, session persistence or replication fails with `NotSerializableException`; make that field `static final` or mark it `transient`.

### ⚠️ Behavior changes

- **Validation errors come in field declaration order on both the server and the browser.** Wildcard fields (`items[].name`) used to be processed before regular fields, so fail-fast `validate()` reported a row error instead of an earlier-declared field, and the order of `validateAll().toMap()` and JS `validate()` differed from the declarations.
- **`S2StringUtil.substringByBytes` begins at the right byte.** It used to start one character early (`("abc", 1, 3)` → `"abc"`, `("한글", 3, 6)` → `"한글"`); it now starts at the first character beginning at or after the start byte and skips a straddling character (`"bc"`, `"글"`).
  `toPascalCase`/`toCamelCase`/`toSnakeCase`/`toKebabCase` split at the end of an uppercase acronym (`HTTPServer` → `HttpServer`, `getHTTPResponseCode` → `get_http_response_code`). `clearNewline` writes `<` as `\u003C` (so it cannot close an inline `<script>`) and removes U+2028/U+2029.
- **`S2Copier` leaves properties that the source does not have untouched.** It used to read a source value for every target field and overwrote missing ones (`id`, `createdBy`, ...) with `null`, wiping values when updating an entity from a partial DTO.
  Values of a different type are converted with `S2Util.cast` (`"30"` → `int`; previously skipped). Exceeding the deep-copy depth (`MAX_DEPTH`) now throws instead of silently skipping the field (as GUIDE 3-5 describes).
- **`S2Util.cast` (and `getValue(…, Class)`) converts exactly.** Integer types accept only values that fit exactly — previously `3.7` became `3` and `10000000000` became `1410065408`; now it is a conversion failure (the default, or `null`).
  Numeric strings may have surrounding spaces (`"12 "` → 12), and `BigDecimal`, `BigInteger`, `Short`, `Byte` and enums (by constant name) are supported (previously `ClassCastException`). Booleans recognize only `true/1/on/yes/y` and `false/0/off/no/n`; anything else is a conversion failure (previously always `false`).
- **Exceptions thrown by a setter, constructor or record accessor itself are no longer swallowed.** `setValue` throws `S2PropertyAccessException` (write) and `createInstance` throws `S2RuntimeException`; a missing target or a value of the wrong type still yields `false`/`null`. `S2Copier` reports setter failures too.
- `S2Util.isEmpty` treats `Optional.empty()` and an `Optional` holding an empty value as empty (a `REQUIRED` rule on an `Optional` field used to pass).
- **`S2Util.isEmpty` now uses the same whitespace as JavaScript `trim()`.** Strings of only NBSP (U+00A0), U+2007, U+202F or BOM (U+FEFF) are empty, and the control characters U+001C–U+001F are not whitespace (the opposite of the previous `Character.isWhitespace` rule).
  Previously a value of only U+001F was empty on the server (`EMAIL` passed, `REQUIRED` failed) but a value in the browser, so verdicts differed.
- **Upper-limit rules (`LENGTH`, `MAX_LENGTH`, `MAX_BYTE`) also judge whitespace-only values by their original length.** Such values were skipped as empty, so 300 spaces passed `MAX_LENGTH 10` and could be stored as is.
  `LENGTH` fails only when exceeded; other rules, `REQUIRED` and lambda rules still treat the value as empty. Both the server and `s2.validator.js` changed.
- **`S2Util.getValue(target, field)`, `getValue(target, field, Class)`, `cast(value)`, `cast(value, Class)` (no default) and `getValueAll` return the stored value as is.** Previously empty and blank strings and empty collections became `null`.
  Check emptiness with `S2Util.isEmpty` (a blank string is empty); code that tested `getValue(...) == null` for emptiness must change. `getValue(target, field, default)` and `cast(value, default)` still return the default for empty, missing or unconvertible values.
- **Server/browser verdicts for user regexes (`REGEX`, converted `@Pattern`) agree more closely.** The browser evaluates with the `u` flag when possible, so emoji and other astral characters count as one character as in Java, and `.` no longer matches U+0085 (as in Java).
  More syntax that means different things in Java and JS is now refused for rules JSON (`IllegalStateException`; BV conversion keeps it server-only): `\x{…}`, `\v`, `\N{…}`, `\b{g}`, `]` right after `[` (`[]a]`, `[^]a]`), octal `\0…`, multi-digit or missing-group backreferences (`\12`), and patterns Java cannot compile (`a{,3}`, `\y`).
- **`S2Util.getValue` no longer swallows exceptions thrown by the getter itself; it throws `S2PropertyAccessException` (with the property path and cause).** A missing property or a `null` along the path still yields `null` (or the default value).
  Previously a getter failure (lazy-loading failure, NPE in a computed getter, …) became an empty value, so in the validator `REQUIRED` failed with a misleading message and the other rules (`EMAIL`, `MAX_LENGTH`, …) **silently passed**.
  For the same reason `S2Copier`, `setValue` (nested paths) and `getValueAll` report getter failures too. JVM errors such as `OutOfMemoryError` are no longer swallowed.
- **Upper-limit rules (`LENGTH`, `MAX_LENGTH`, `MAX_BYTE`) judge the original value including surrounding spaces.** Previously the
  trimmed value was judged, so a value padded with spaces could pass the limit and be stored as is. Both the server and
  `s2.validator.js` changed; the browser now reads form values without trimming.
- **Multiple values (collections, arrays, checkbox groups, multi-selects) are judged per element.** Previously the server judged
  `toString()` (`"[a, b]"`, `"[Ljava.lang.String;@…"`) and the browser `"a,b"`, so length and format rules misbehaved for `String[]`
  parameter maps. `REQUIRED` fails when every element is empty (`["", " "]`).
- **Message priority: a message set on the rule (`.ko()`, `.en()`, `.message()`) now wins over the bundle/`MessageSource` message for the
  built-in key (`valid.err.*`).** Previously an application-wide key such as `valid.err.required` in `messages.properties` overrode every
  per-rule message. A key given with `.rule(type, value, key)` is still looked up first.
- Calling `storeMessage()` on the shared `S2Rule.required()` instance throws `UnsupportedOperationException`. Previously it changed the
  implicit required message for the whole JVM.
- **`DATE_AFTER`/`DATE_BEFORE`**: a value that is not a readable date now passes in the browser too (same as the server).
  Previously only the browser failed it, blocking values the server accepts.
- **`S2ThreadUtil.newExecutor(maxThreads)`: a positive `maxThreads` now limits concurrency on virtual threads (Java 21+) too.**
  It was honored only on Java 17 (platform pool) and ignored on Java 21+, so upgrading Java alone could exceed a database pool or external API limit.
  Each task still gets its own virtual thread, and a semaphore lets at most `maxThreads` run at once. Values of 0 or less stay unbounded on virtual threads.
- **`S2DateUtil` parsing and validation (`isValidDate`, `parseToLocalDate`, `parseToLocalDateTime`, `compareDates`, ...) reject nonexistent dates.**
  They used to adjust `2024-02-30` or `2023-02-29` to the last day of the month, so `isValidDate` returned `true` (the JDK default SMART resolution). They now resolve STRICTly.
- **The fallback console logger (used without SLF4J) logs from INFO.** DEBUG was always on, so every debug log was printed in production too.
  Change it with `-Ds2.log.level=DEBUG`; WARN and ERROR go to standard error (`System.err`).
- **`S2Copier` copies inherited fields and keeps empty strings and empty collections instead of turning them into `null`.** Static fields are not copied.
- **`S2Template.render()` no longer re-expands `{{=…}}` inside binding values.** They used to be replaced by other bindings or removed, with results depending on binding order.
- `S2StringUtil.clearNewline` escapes backslashes too. Before, a `\'` in the input closed the JS string and allowed script injection.
- `S2JsonUtil.toJson` writes `<`, `>` and `&` in strings as `\u003c`, `\u003e` and `\u0026`, so the output can be placed inside an HTML `<script>` block as is without being closed by `</script>` (the parsed value is the same).
- **A built validator can no longer be changed.** Calling `field()`, `rule()`, ... on the same builder chain after `build()` throws `IllegalStateException`.
  Fields could silently be added to a validator already in use, and the field list was not safe across threads. Start a new `builder()` for another validator.
- List validation (`EACH`) reports a circular reference when an element is the same object as an ancestor on the validation path (as `NESTED` does). It used to pass silently.
- `S2JsonUtil.toJson` writes `java.sql.Date` as a date (`2024-02-01`) and `java.sql.Time` as a time (`09:30`). They were written as UTC instants, shifting the date by a day in Korea (`2024-01-31T15:00:00Z`). `fromJson` reads them back.
- `S2Jpql` DEBUG logs no longer include bound parameter values, only names and types (personal data).
- **`S2Jpql` Supplier `bindParameter` runs in `build()`, only when the rendered JPQL references the parameter (`:name`).** It used to depend on the condition of the preceding `bindClause`,
  so calling it after another clause could leave an included clause's parameter unbound (error at execution) or run the Supplier of an excluded clause. Call order relative to `bindClause` no longer matters.
- **`S2Template.bindIn` writes only numbers and booleans as-is and quotes other values (dates, enums, …) as string literals. Strings containing a backslash are rejected with `IllegalArgumentException`.**
  Previously a value such as `a\'` could break the quote escaping and inject SQL on MySQL/MariaDB (default settings), and non-numeric objects were inserted via `toString()`. Bind user input as query parameters.
- `S2Jpql` rendering keeps blank lines, trailing spaces and line terminators inside JPQL string literals (`'…'`); multi-line literal values used to change. Plain `S2Template` still cleans every line (an apostrophe in a message is not a literal).
- **`MIN_VALUE`/`MAX_VALUE` and numeric conditions (`GT`, `GTE`, `LT`, `LTE`) compare decimals exactly on both the server and the browser.**
  They compared doubles, so values beyond 15-16 significant digits (e.g. `9007199254740993` vs `9007199254740992`, `0.1000000000000000001` vs `0.1`) were treated as equal.
  The server uses `BigDecimal` and `s2.validator.js` compares decimal strings directly, so both sides still judge the same.
- System messages shown by `s2.validator.js` (form or rules JSON errors) follow the page language (`<html lang>`, then the browser language): Korean or English. When it cannot be determined (outside a browser) they stay Korean as before.

### Added

- A fixed `Automatic-Module-Name` in the jar manifests (`io.github.devers2.s2util`, `.core`, `.jpa`, `.validation`), so JPMS users can `requires` the same name regardless of the jar file name.
- The published POMs list Caffeine (s2-core) and the Jakarta Persistence API (s2-jpa) as `optional` dependencies, like Spring and Bean Validation (not transitive; shows what each feature needs).
- `S2Util.setValue` writes literal keys of a flat `Map` (`"address.zip"`, `"items[0].name"`) by the same rule as `getValue` (the whole path is the key when its first segment is not in the map), so written values read back as is. It used to return `false` without writing.
- **`S2BeanValidation`: builds S2 rules from Bean Validation annotations (`@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Valid`, ...).** Existing DTOs get browser validation unchanged, and `toBuilder()` adds S2 rules. Only constraints never stricter than Bean Validation are converted; the rest are reported by `serverOnly()` and a log line (`practical()` widens the range). Includes a test comparing against Hibernate Validator with random input. MANUAL 5-5.
- The published POM lists Spring and `jakarta.validation-api` as optional dependencies (`<optional>true</optional>`): not transitive, but visible as what each feature needs.
- Rules `MIN_SIZE`/`MAX_SIZE`: check the number of values (collections, arrays, checkbox groups, multi-selects; a single value counts as one). Same on server and browser; the build plugin checks their criteria.
- `S2SpringValidator`: wraps an `S2Validator` as a Spring `SmartValidator`; registered with `@InitBinder`, it runs with Bean Validation on `@Valid`/`@Validated` and writes to the same `BindingResult` (MANUAL 2-C). No extra dependency.
- `S2Validator.validateAll(target[, locale])` → `S2ValidationResult`: collects every error without throwing; `toMap()` has the same `{field: [messages]}` shape as `s2.validator.js` (for REST responses).
- A WARN log (once per class and field) when a validated field name does not exist on the target class (e.g. a typo). Such a field always reads as empty, so its non-required rules never run. Results are unchanged.
- `S2ThreadUtil.newExecutor(maxThreads, maxPending)`: an executor that also bounds waiting tasks. Once running + waiting tasks reach the limit, the submitting thread waits (backpressure),
  so sustained over-submission cannot exhaust memory. Behaves the same on virtual and platform threads (MANUAL 9-3).
- System properties `s2.thread.virtual.enabled=false` (turn off virtual threads, e.g. to avoid pinning on Java 21-23) and `s2.thread.virtual.max_concurrency` (concurrency cap of the virtual-thread common executor).
- `S2Jpql.escapeLike()`: escapes LIKE wildcards (`%`, `_`) in a keyword, used with `ESCAPE '\'` in the JPQL (GUIDE_JPQL 3-2).
- `S2Jpql.bindOrderBy(key, sortExpression, allowedFields)`: translates a sort string from the request (`name desc`) to JPQL paths in an allow list (`m.name DESC`) and drops unknown keys, so clients cannot sort by hidden columns (such as a password hash) to infer their values.
- `S2Validator.getValidationBundle()`, `getDefaultLocale()` and `getMessageResolver()` are public (to check the current global settings).

### Performance

- `s2.validator.js` validation cost grew with the square of the form's input count; it is now linear — instead of rescanning the form with `querySelectorAll` per field and error proxy, it uses a name index built once per validation (measured in Chromium: 1,000 inputs 42 ms → 2 ms, 2,500 inputs 222 ms → 5 ms). Noticeable on large grid forms with input-mode live validation.
- The Caffeine backend now runs loaders outside any cache lock, like the lightweight cache. Caffeine's `get(key, loader)` (an internal `computeIfAbsent`) let a slow loader block other keys in the same bin and pinned virtual threads on Java 21-23 (2,000 loads of a 20 ms loader: Java 21 2.2 s → 0.19 s, Java 25 1.4 s → 0.23 s). Concurrent misses on the same key may compute twice; the first stored value wins.
- s2-core lightweight cache (`S2OptimisticCache`): reads no longer bump a shared counter; recency is tracked as an epoch-based approximate LRU, so reads perform no shared writes
  and throughput scales with the number of cores under concurrent reads. Eviction still runs asynchronously on its dedicated platform thread and now trims to 75% of the maximum size instead of half.
- `S2Cache` method handle, field (`getFields`) and proxy real-class caches moved to a per-class `ClassValue`. Previous class loaders are no longer pinned by a static cache
  after a redeploy or DevTools restart. JDK and parent-loader classes use a bounded shared cache, and at most `s2.cache.method.max_per_class` (default 512)
  method handles are cached per class (guards against memory growth from member names taken from external input).
- The Caffeine backend uses Caffeine's default executor for maintenance. It used the `S2ThreadUtil` common executor before,
  so every cache write logged a `RejectedExecutionException` warning after `S2ThreadUtil.shutdownCommonExecutor()`.
- `S2StringUtil.replaceChars` / `removeChars` no longer allocate a 64 KB array per call, only up to the largest target character (the validator's `DATE` and resident number rules call them every time).
- `S2DateUtil` caches parsing formatters per pattern.
- `s2.validator.js` caches compiled regular expressions instead of converting and compiling the pattern on every check.

### Removed (security)

- **`S2Util.chkSuPw` has been removed.** It built a password from the current time with a fixed rule that is public in the source, so anyone can compute it.
  Used to bypass authentication it is a backdoor, so it is removed without a replacement. Remove any code that grants access (e.g. login) through it.

### Docs & examples

- Public demo site (GitHub Pages, `s2-validator/browser-test/site`): sign-up, order rows, Bean Validation conversion and a `check()` playground in Korean/English, with rules made by the server validators. Covered by Playwright tests.
- Example app `examples/spring-boot-thymeleaf`: runs with one `./gradlew -p examples/spring-boot-thymeleaf bootRun`; S2 rules, Bean Validation on top, REST error responses. Tested in CI.

### Fixed

- Cases where the build plugin (`checkS2Validators`) failed the build on valid code:
  - Properties with only a getter (`getFullName()`, `isAdult()`, `code()`) were reported as missing fields — they now count as fields, as at runtime.
  - A builder kept in a variable and finished later (`b.field(..).build()`) or handed on (`return b`) was reported as an incomplete chain.
  - A same-named variable in another method could be used to infer the validator's target class.
  - `when`/`and` in a sub-validator (a DTO that can be a NESTED/EACH target) referring to an outer-object field was an error — it is now a warning (condition-field typos on top-level DTOs are still errors).
  - Hexadecimal, binary and octal integer criteria (`0x10`) were reported as invalid.
- The build plugin could not parse files using syntax newer than Java 17 (switch and record patterns) and silently skipped them — it now reads the newest syntax its JavaParser supports (Java 25) and warns when a file that uses S2Validator cannot be parsed.
- `EQUALS_FIELD` on multi-selects and checkbox groups compared order on the server but not in the browser — the server now compares the values regardless of order.
- When a cross-field target was empty in the nearest object, the server kept looking in outer objects while the browser stopped — the browser now continues outward, like the server and condition lookups.
- For cross-field rules (`DATE_AFTER`, `EQUALS_FIELD`, ...) inside a nested object of a row or list, the browser looked up a same-named target in the row before the object itself, so its verdict differed from the server — it now looks in the object, then outer objects, then the root, as the server does (forms and `check()`).
- An unexpected error during browser validation escaped the submit listener and **the form was submitted without validation** — the error is logged and the submission blocked (fail-closed).
- A built validator could still be changed through `getFields().get(i).rule(…)`/`when(…)` or `getRules().clear()` (bypassing the shared-validator lock) — now rejected, and a field's rule and condition lists are returned read-only. `S2Field.build()` seals as well.
- `NESTED`/`EACH` on a wildcard field (`field("items[].address").rule(NESTED, …)`) was silently ignored on both the server and the browser — each row is now validated with the sub-validator (which looks up outer fields row first, then root).
- The default logger (without SLF4J) always prefixed levels with ANSI color codes, leaving control characters in log files, Docker and CI logs — colors are now used only on an interactive terminal (`-Ds2.log.color=true/false` and the `NO_COLOR` environment variable are honored).
- Resource bundles were looked up only with the library's class loader, so deployments sharing the library jar across web apps (Tomcat `lib/`, ...) could miss a web app's bundle or cache another app's — bundles are now looked up with the thread context class loader (still cached when it is the library's loader).
- The lightweight cache (without Caffeine) ignored the `expiryMs` of `S2Cache.resolve`, so values never expired — entries now expire after the last access like the Caffeine backend (`S2OptimisticCache(int, long)` and `SimpleCacheAdapter(int, long, boolean)` added). Caches without expiry perform as before.
- `S2Template`: a key with surrounding spaces (`{{= name }}`) was silently removed even when bound (now the same key). Javadoc examples corrected — `bindIn` quotes string lists as literals (the `ORDER BY` example was wrong), and the `bindWhen` examples that joined values into the query with `String.join` (an SQL injection pattern) or inserted the request's sort as is now use a query parameter or an allow-list.
- `S2StringUtil`: `replaceChars(…, " ", …)` removed instead of replacing with a space; `replaceChars`, `replaceAll` and `substringByBytes` ignored whitespace-only input (including the regex cache rejecting the pattern `" "`); `decodeUnicode` doubled the backslash of an invalid `\uZZZZ` and accepted signs such as `\u-001`; `formatMessage` indexes beyond the int range.
- `S2DateUtil`: `compareDates` threw for date-only patterns (`yyyy-MM-dd`; now compared at 00:00); `dateFormat(Instant)` printed the UTC digits instead of the system time zone; `parseToZonedDateTime` missed the `x`/`O` offset letters (shifting the instant) and mistook quoted literals (`'z'`) for a zone; `toDateTimeString(OffsetDateTime, …)` dropped the offset and threw `UnsupportedTemporalTypeException` for `XXX` (now formats with the offset; unsupported fields return null); `parseToLocalDate`/`parseToLocalDateTime` threw on blank input even with `isThrowException=false` (now null). Exception messages are in English and output formatters are cached.
- `S2Copier` deep copy dropped `ConcurrentHashMap`, `CopyOnWriteArrayList`, `EnumMap` and `EnumSet` fields entirely (it built a `LinkedHashMap`/`ArrayList` that did not fit the field type) and shared `ArrayDeque` instead of copying it — collections are now copied into the same kind (`PriorityQueue`, `TreeSet`, `TreeMap` keep their comparator).
- `S2Copier.to(Record.class)` failed with "Failed to instantiate" — records are now created with the canonical constructor. Passing a record instance as the target, which silently did nothing, now throws `IllegalArgumentException`.
- `S2ThreadUtil`: logged an initialization ERROR on Java 19/20 (virtual threads in preview) — now treated as unsupported. Platform threads are named `s2-util-N`.
- `S2Util.getValue` could not read public methods of JDK classes (`String.length`, `BigDecimal.scale`, ...), returning `null` with an ERROR log — public members of public classes are now read without changing access. Inaccessible members are reported once at WARN.
- `S2Cache.getFields` threw `InaccessibleObjectException` for JDK classes (modules not open). `S2Cache.resolve` checked the value type only when DEBUG logging was on (now always); exception messages are in English.
- `S2Util.getValue` could not read Lombok's `getIsXxx()` for a `Boolean isXxx` field or a field whose name looks like a getter (`is3d`), always returning `null`. `getValueAll` no longer lists static getters as properties.
- `S2Util.checkDependency` threw a Korean-only message (now English) and no longer initializes the checked class.
- `S2Jpql.limit(condition, offsetSupplier, limitSupplier)` threw `NullPointerException` when a Supplier returned `null` — that value is now left unset.
- `S2Jpql` `LikeMode` was not applied to `CharSequence` values other than `String` (e.g. `StringBuilder`).
- The `S2Template` class Javadoc example inserted the value as-is (`bind("name_cond", name, "AND m.name = ")`, an SQL injection pattern) — now uses a query parameter, with a security note.
- In user regular expressions (`REGEX`, including converted `@Pattern`), the browser read `\s`/`\S` with JS meaning (including Unicode spaces such as NBSP), judging differently from the server; the browser now evaluates them with Java meaning. `\S` inside a character class cannot be translated and is not exported to the rules JSON (`IllegalStateException`).
- GUIDE_JPQL referred to a nonexistent `LikeMode.EXACT`.
- `DATE_AFTER`/`DATE_BEFORE` always passed for `java.util.Date` (and `java.sql.Date`, `Timestamp`, `Instant`) fields.
- `DATE_AFTER`/`DATE_BEFORE` threw `ClassCastException` when comparing values of different types (e.g. a `LocalDate` field and a string).
- `<input type="datetime-local">` values (`2024-01-01T09:00`) were not compared on the server and always failed in the browser; both now compare including the time.
- A `BiPredicate` on a wildcard row (`items[].qty`) received the row instead of the root object as its second argument and always failed.
- `S2BindValidator` failed with `NotReadablePropertyException` for errors on paths the target cannot read (missing property, `null` intermediate path); they are now recorded as a `FieldError`.
- Browser mismatches: a multi-select with nothing selected (empty array) was not treated as empty, failing non-required rules; `DATE` accepted values such as `"1a240101"`.
- `TEXT_COMBINE` and `PASSWORD_ANSWR` now list their whitespace characters explicitly, so the browser no longer accepts Unicode spaces such as NBSP that the server rejects.
- `S2Util.getValue` resolved keys with dots or brackets (`"address.zip"`) in a flat Map only as nested paths and missed them; the literal key is now used when the first path segment is not in the Map.
- The rules JSON (`getRulesJson`) did not escape `<`, `>` and `&`, so a `</script>` in a label or message could end the script when the JSON was placed inside `<script>` as is (e.g. JSP); they are now escaped as `\u003c` etc. (same JSON value).
- `s2.validator.js` stopped validating on fields whose name contains a quote or backslash (CSS selector error), and blocked submission without any sign when the rules JSON was missing or invalid (a console error is now logged).
- The server `DATE` rule accepted signed values such as `"+2020101"`.
- The server did not treat Unicode spaces such as NBSP (U+00A0) and the ideographic space (U+3000) as whitespace (Java `String.trim()`), judging padded values differently from the browser; the server now uses the JavaScript `trim()` definition. Found by a differential fuzz test that feeds the same random inputs to Java and JS.
- s2-core lightweight cache (`S2OptimisticCache`, used when Caffeine is absent): when eviction overlapped with reads at capacity,
  values changed during sorting and `IllegalArgumentException: Comparison method violates its general contract!` was thrown, reaching callers of `get()`.
- A burst of distinct keys (all misses) outran eviction and the cache kept growing far past its bound. New values are now returned without caching once the cache holds twice its maximum size.
- `S2Cache.getMethodHandle` did not include the lookup type (`LookupType`) in its cache key, so whichever type was looked up first stuck
  (looking up `BOTH` first made the public-only `METHOD` lookup return a private method; the reverse made `BOTH` miss private members).
- s2-core log messages (cache engine notice, virtual thread notice) contained ANSI color codes, leaving control characters in log files and log collectors.
- Lightweight cache statistics (`-Ds2.cache.stats.enabled=true`) looked up the cache twice per call and used contended counters. They now look up once and count with `LongAdder`.
- `S2Copier.deep()`: fields of JDK value types such as `LocalDate` and `BigDecimal` were dropped instead of copied, and arrays were shared with the source. `Date` is now cloned,
  `TreeSet`/`TreeMap` keep their ordering, `LinkedHashMap` keeps insertion order, and objects with a `protected` constructor (e.g. JPA entities) are deep-copied.
- `S2Util.setValue`: the fallback that writes a field directly when there is no setter always failed (it tried to write through a field read handle). Static and final fields are not written.
- `S2Util.getPercentage`: non-terminating ratios (1/3, ...) threw `ArithmeticException`. Digits are rounded HALF_UP, and a zero denominator returns an empty string.
- `S2Util.getValueAll`: methods such as `issue()` and `getaway()` were read as properties `sue` and `away`.
- `S2StringUtil.substringByBytes`: 4-byte characters such as emoji were counted as 6 bytes and could be cut in half, returning a broken string.
- `S2StringUtil` case conversion (`toCamelCase`, `toSnakeCase`, ...) followed the system locale and produced `userİd` under Turkish.
- The JSON parser accepted non-ASCII characters such as Arabic-Indic digits and fullwidth letters as hex digits in `\u` escapes.
- `S2DateUtil` parsing read text fields such as AM/PM (`a`), month names (`MMM`) and day names (`E`) only in the default locale, so `11:30 PM` failed on a Korean JVM. English is now tried after the default locale, case-insensitively.
- `S2JsonUtil.toJson`: a field redeclared in a subclass with the same name as a superclass field produced the key twice (now only the subclass value), and `java.sql.Timestamp` lost its nanoseconds.
- Spring Boot auto-configuration: with several contexts in one JVM (test context cache, parent/child), closing the first context cleared the bundle, locale and `MessageSource` link set by another context.
  Each setting now takes the value of the most recent live context that set it, or the default when none does. Settings a context does not configure are left untouched.

## [2.1.0]

- **Korean Postposition Auto-Completion**:
  - Automatically completes Korean particles from single postposition notation (`{0|은}`, `{0|이}`, `{0|을}`, `{0|로}`) by analyzing the final consonant (batchim) of the preceding word.
  - Replaced paired slash notation (`{0|은/는}`) to eliminate pairing typos (e.g., `이/로`).

## [2.0.0] - 2026-10-02

Compared with 1.1.8. This is a **major release** that does not keep backward compatibility and contains behavior changes; read [Upgrade notes](#upgrade-notes) before upgrading.

### ⚠️ Compatibility

- **`s2-support` 1.x is not compatible with `s2-core` 2.0.0.** `S2AutoConfiguration` in `s2-support` 1.1.3 calls the removed
  `S2LogManager.touch()`, so a Spring Boot application fails at startup with
  `NoSuchMethodError: 'void io.github.devers2.s2util.log.S2LogManager.touch()'`. Move to `s2-kit` 1.0.0, the new name of `s2-support`. `S2JsonUtil`
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
  - `ASSERT_TRUE`: "{0|을} 선택(동의)해야 합니다." / "{0} must be checked." (was "must be true");
    `ASSERT_FALSE`: "{0|은} 선택할 수 없습니다." / "{0} must not be checked."
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

- Browser: form-free validation `S2Validator.check(rules, data)` (nested or flat data, judged like the server) and live
  validation (`data-s2-live="blur"|"input"`, `S2Validator.setLiveMode`). Live validation never moves focus and uses the
  renderer hook `showField` (implemented by `classRenderer`). Form validation shares the same judgment engine.
- Spring Boot auto-configuration: with s2-validator on the classpath, message keys are looked up in Spring's
  `MessageSource` first (override validation messages in `messages.properties`), and `s2.validator.bundle`,
  `s2.validator.default-locale`, `s2.validator.use-message-source` and `s2.validator.enabled` are supported. Spring Boot
  is a compile-only dependency, so applications without Spring are unaffected.
- `S2Validator.setMessageResolver(S2MessageResolver)`: plug in a message source (a database, for example) consulted
  before the validation bundle.
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
  JSON path. Hand-written rule files can use JSON5 with `fromJson(json, Feature.JSON5)`.
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
- `S2DateUtil.dateFormat` logs a value it cannot read as a date at debug level instead of printing it to `System.err`
  (it still returns the original value).
- `S2ValidationError` compares its `errorArgs` array by content, not by reference (`equals`/`hashCode`/`toString`), so
  equal errors are equal and deduplicate in a `Set`.
- License files in the jars: `s2-jpa` now ships `LICENSE-EPL-2.0` and `LICENSE-EDL-1.0` for Jakarta Persistence (its
  NOTICE names both licenses); `s2-validator` no longer lists license files it does not have, and its NOTICE names Spring
  Boot.

### Upgrade notes

1. Replace `s2-support` with `s2-kit` 1.0.0 and upgrade it together with `s2-core` (see Compatibility).
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

## s2-validator-plugin [2.1.0]

- **Nested DTOs**: DTOs declared inside another type (a `record Form` in a controller, a static nested class) are
  checked. `Form` in the same file, `Join.Form`, `import ...Join.Form` and `S2Validator.of(form)` are all found;
  before, the check was skipped with "DTO 소스를 찾을 수 없어 검증을 건너뜁니다". A DTO's fields are its own fields,
  record components and superclass fields only (before, the fields of every type in the file were mixed, so an outer
  class's field name passed).
- **Paths**: `address.city`, `items[0].name`, `products[].price` and `byCode[A1].qty` are checked part by part,
  following the declared types into nested objects and the elements of arrays, collections and map values, and
  stopping where the sources cannot tell the type (`Map`, type variables, JDK types). Before, only the first part
  (`address`, `items`) was checked, so a typo later in the path (`address.ctiy`) passed. An error names the missing
  part and the class it was looked up in.
- **Error lines**: in a chain written over several lines, field and criterion errors point at the line of the
  `.field(...)` or `.rule(...)` call instead of the start of the chain.

## s2-validator-plugin [2.0.0] - 2026-10-02

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
- **Rule criterion and condition value check**: literal criteria that do not fit the rule (`.rule(MAX_LENGTH, "abc")`, a
  missing criterion, a string for `NESTED`, `when(f, GT, "abc")`, ...) fail the build, as do mistakes the runtime gets
  silently wrong: an invalid `REGEX`, a value for a rule that takes none, and a bad `JUMIN`/`BIZRNO` switch. A comparison
  field missing from the DTO is a warning. This provides compile-time safety instead of per-rule methods.
