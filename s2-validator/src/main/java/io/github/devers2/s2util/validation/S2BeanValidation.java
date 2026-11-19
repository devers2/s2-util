/**
 * S2Util Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */
package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Builds S2 rules from the Bean Validation (Jakarta Validation) constraints of a class, so forms that already use
 * {@code @NotBlank}, {@code @Size}, {@code @Pattern} and so on get the same checks in the browser without writing the
 * rules again. The server keeps validating with Bean Validation ({@code @Valid}); the result is mainly for
 * {@link S2Validator#getRulesJson(java.util.Locale)}, and S2 rules can be added on top.
 *
 * <pre>{@code
 * S2Validator<JoinForm> rules = S2BeanValidation.of(JoinForm.class)
 *         .labels(Map.of("name", "이름", "email", "이메일"))
 *         .toBuilder()                                               // continue with S2 rules
 *         .field("vipCode", "VIP 코드").when("type", "VIP").rule(S2RuleType.REQUIRED)
 *         .build();
 * model.addAttribute("rules", rules.getRulesJson(locale));         // the server still uses @Valid
 * }</pre>
 * <p>
 * <b>Never stricter than Bean Validation.</b> A constraint is converted only when the S2 rule rejects nothing that
 * Bean Validation accepts, so the browser never blocks a value the server would take. Constraints that cannot be
 * converted that way (time-based ones, {@code @Digits}, exclusive bounds, class-level and custom constraints, container
 * element constraints, getters) stay server-only; {@link #serverOnly()} lists them and they are logged once per
 * class. {@link #practical()} also converts {@code @NotEmpty}, {@code @NotNull} on text/collections and {@code @Email},
 * which differ from Bean Validation only for unusual input (spaces only, rare e-mail forms).
 * </p>
 * <p>
 * Requires {@code jakarta.validation:jakarta.validation-api} (3.x) on the classpath; only this feature needs it. Only
 * the {@code Default} group is converted. Labels come from {@link #labels(Map)}, then {@link #labelResolver(Function)},
 * then the field name; nested fields use dotted paths ({@code "address.zip"}).
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 클래스의 Bean Validation(Jakarta Validation) 제약으로 S2 규칙을 만듭니다. 이미 {@code @NotBlank}, {@code @Size}, {@code @Pattern} 등을
 * 쓰는 폼이 규칙을 다시 쓰지 않고 브라우저에서도 같은 검사를 하게 됩니다. 서버는 그대로 Bean Validation({@code @Valid})으로 검증하고, 결과는
 * 주로 {@link S2Validator#getRulesJson(java.util.Locale)}에 쓰며 S2 규칙을 덧붙일 수 있습니다.
 * <p>
 * <b>Bean Validation 보다 엄격하지 않습니다.</b> Bean Validation 이 받는 값을 S2 규칙이 거부하지 않는 경우에만 변환하므로, 서버가 받을 값을
 * 브라우저가 막지 않습니다. 그렇게 변환할 수 없는 제약(시간 기준, {@code @Digits}, 경계 미포함, 클래스 단위·커스텀 제약, 컨테이너 요소 제약,
 * getter)은 서버 전용으로 남기며 {@link #serverOnly()}로 확인하고 클래스당 한 번 로그를 남깁니다. {@link #practical()}을 켜면
 * {@code @NotEmpty}, 문자열·컬렉션의 {@code @NotNull}, {@code @Email}도 변환합니다(공백만 입력, 드문 이메일 형식처럼 특이한 입력에서만 다름).
 * </p>
 * <p>
 * 클래스패스에 {@code jakarta.validation:jakarta.validation-api}(3.x)가 필요하며 이 기능만 필요합니다. {@code Default} 그룹만 변환합니다. 라벨은
 * {@link #labels(Map)} → {@link #labelResolver(Function)} → 필드명 순으로 정하고, 중첩 필드는 점 경로({@code "address.zip"})를 씁니다.
 * </p>
 *
 * @param <T> The target type | 대상 타입
 * @since 2.2.0
 */
public final class S2BeanValidation<T> {

    private static final S2Logger logger = S2LogManager.getLogger(S2BeanValidation.class);

    /** 서버 전용 제약을 이미 알린 클래스 (클래스가 언로드되면 함께 정리) */
    private static final ClassValue<java.util.concurrent.atomic.AtomicBoolean> NOTICED = new ClassValue<>() {
        @Override
        protected java.util.concurrent.atomic.AtomicBoolean computeValue(Class<?> type) {
            return new java.util.concurrent.atomic.AtomicBoolean();
        }
    };

    /** A rule converted from a constraint | 제약에서 변환한 규칙 */
    record RuleSpec(S2RuleType type, Object criterion, String message) {
    }

    /** A field and its converted rules; NESTED/EACH carry the nested fields as criterion | 필드와 변환한 규칙 */
    record FieldSpec(String name, String path, List<RuleSpec> rules) {
    }

    /** The conversion result | 변환 결과 */
    record Conversion(List<FieldSpec> fields, List<String> serverOnly) {
    }

    private final Class<T> type;
    private final Map<String, String> labels = new LinkedHashMap<>();
    private Function<String, String> labelResolver;
    private boolean practical;

    private S2BeanValidation(Class<T> type) {
        this.type = type;
    }

    /**
     * Starts converting the Bean Validation constraints of {@code type}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code type}의 Bean Validation 제약 변환을 시작합니다.
     *
     * @param <T>  The target type | 대상 타입
     * @param type The class with Bean Validation constraints | Bean Validation 제약이 있는 클래스
     * @return A new converter | 새 변환기
     * @throws IllegalStateException If {@code jakarta.validation-api} is not on the classpath | 클래스패스에
     *                               {@code jakarta.validation-api}가 없는 경우
     */
    public static <T> S2BeanValidation<T> of(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        try {
            Class.forName("jakarta.validation.constraints.NotNull", false, S2BeanValidation.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException("S2BeanValidation needs Bean Validation 3.x on the classpath: add "
                    + "'jakarta.validation:jakarta.validation-api' (Spring Boot: spring-boot-starter-validation).", e);
        }
        return new S2BeanValidation<>(type);
    }

    /**
     * Sets labels for error messages by field path ({@code "name"}, {@code "address.zip"}).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 오류 메시지에 쓸 라벨을 필드 경로({@code "name"}, {@code "address.zip"})별로 정합니다.
     *
     * @param labels Field path → label | 필드 경로 → 라벨
     * @return This converter | 이 변환기
     */
    public S2BeanValidation<T> labels(Map<String, String> labels) {
        if (labels != null) {
            this.labels.putAll(labels);
        }
        return this;
    }

    /**
     * Sets a function that returns the label of a field path (e.g. from a {@code MessageSource}); used when
     * {@link #labels(Map)} has none. A null result falls back to the field name.
     *
     * <pre>{@code
     * .labelResolver(path -> messageSource.getMessage("joinForm." + path, null, path, locale))
     * }</pre>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드 경로의 라벨을 돌려주는 함수(예: {@code MessageSource} 조회)를 정합니다. {@link #labels(Map)}에 없을 때 쓰며, null 이면 필드명을
     * 씁니다.
     *
     * @param labelResolver Field path → label (or null) | 필드 경로 → 라벨 (또는 null)
     * @return This converter | 이 변환기
     */
    public S2BeanValidation<T> labelResolver(Function<String, String> labelResolver) {
        this.labelResolver = labelResolver;
        return this;
    }

    /**
     * Also converts {@code @NotEmpty}, {@code @NotNull} on text and collections (as {@code REQUIRED}) and {@code @Email}
     * (as {@code EMAIL}). They differ from Bean Validation only for unusual input: a value of only spaces, or an e-mail
     * address Bean Validation accepts but {@code EMAIL} does not (e.g. without a dot in the domain).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code @NotEmpty}, 문자열·컬렉션의 {@code @NotNull}({@code REQUIRED}로)과 {@code @Email}({@code EMAIL}로)도 변환합니다. 공백만
     * 입력한 값이나, Bean Validation 은 받지만 {@code EMAIL}은 받지 않는 이메일(예: 도메인에 점이 없음)처럼 특이한 입력에서만 Bean Validation 과
     * 다릅니다.
     *
     * @return This converter | 이 변환기
     */
    public S2BeanValidation<T> practical() {
        this.practical = true;
        return this;
    }

    /**
     * Returns the constraints that are not converted and are validated on the server only, as
     * {@code "path @Annotation"}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 변환하지 않아 서버에서만 검증되는 제약을 {@code "경로 @어노테이션"} 형식으로 반환합니다.
     *
     * @return The server-only constraints | 서버 전용 제약
     */
    public List<String> serverOnly() {
        return List.copyOf(convert().serverOnly());
    }

    /**
     * Returns an S2 builder that already has the converted fields, to add S2 rules before {@code build()}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 변환한 필드가 들어 있는 S2 빌더를 반환합니다. {@code build()} 전에 S2 규칙을 더할 수 있습니다.
     *
     * @return The builder | 빌더
     */
    @SuppressWarnings("unchecked")
    public S2FieldStep.BuilderFieldStep<T> toBuilder() {
        Conversion conversion = convert();
        notice(conversion);
        S2FieldStep.BuilderStartStep<T> start = S2Validator.builder();
        // 빌더 체인 객체는 모든 단계 인터페이스를 구현하므로 필드가 없어도 이어 쓸 수 있음
        S2FieldStep.BuilderFieldStep<T> step = (S2FieldStep.BuilderFieldStep<T>) (Object) start;
        return addFields(step, conversion.fields());
    }

    /**
     * Builds the validator with the converted rules.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 변환한 규칙으로 검증기를 만듭니다.
     *
     * @return The validator | 검증기
     */
    public S2Validator<T> build() {
        return toBuilder().build();
    }

    private Conversion convert() {
        return S2BeanValidationReader.read(type, practical);
    }

    private <V> S2FieldStep.BuilderFieldStep<V> addFields(S2FieldStep.BuilderFieldStep<V> step, List<FieldSpec> fields) {
        for (FieldSpec field : fields) {
            step = step.field(field.name(), labelOf(field.path(), field.name()));
            for (RuleSpec rule : field.rules()) {
                Object criterion = rule.criterion();
                if (rule.type() == S2RuleType.NESTED || rule.type() == S2RuleType.EACH) {
                    @SuppressWarnings("unchecked")
                    List<FieldSpec> nested = (List<FieldSpec>) criterion;
                    criterion = nestedValidator(nested);
                }
                S2RuleStep.BuilderRuleStep<V> ruleStep = criterion == null ? step.rule(rule.type())
                        : step.rule(rule.type(), criterion);
                if (rule.message() != null) {
                    ruleStep = ruleStep.message(rule.message());
                }
                step = ruleStep;
            }
        }
        return step;
    }

    @SuppressWarnings("unchecked")
    private S2Validator<Object> nestedValidator(List<FieldSpec> fields) {
        S2FieldStep.BuilderStartStep<Object> start = S2Validator.builder();
        return addFields((S2FieldStep.BuilderFieldStep<Object>) (Object) start, fields).build();
    }

    private String labelOf(String path, String name) {
        String label = labels.get(path);
        if (label == null && labelResolver != null) {
            label = labelResolver.apply(path);
        }
        return label != null && !label.isBlank() ? label : name;
    }

    private void notice(Conversion conversion) {
        if (conversion.serverOnly().isEmpty() || !NOTICED.get(type).compareAndSet(false, true)) {
            return;
        }
        if (S2Util.isKorean()) {
            logger.info("{} 의 Bean Validation 제약 중 브라우저 규칙으로 바꾸지 않은 것(서버에서만 검증): {}", type.getName(),
                    conversion.serverOnly());
        } else {
            logger.info("Bean Validation constraints of {} not converted to browser rules (validated on the server only): {}",
                    type.getName(), conversion.serverOnly());
        }
    }

    /** 경로를 이어 붙임 (최상위는 이름 그대로) */
    static String join(String parent, String name) {
        return parent.isEmpty() ? name : parent + "." + name;
    }

    /** 변환 결과를 담는 가변 목록 (Reader 가 채움) */
    static Conversion newConversion() {
        return new Conversion(new ArrayList<>(), new ArrayList<>());
    }
}
