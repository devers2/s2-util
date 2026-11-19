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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.devers2.s2util.validation.S2BeanValidation.Conversion;
import io.github.devers2.s2util.validation.S2BeanValidation.FieldSpec;
import io.github.devers2.s2util.validation.S2BeanValidation.RuleSpec;
import jakarta.validation.Constraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

/**
 * Reads Bean Validation constraints with reflection and converts them for {@link S2BeanValidation}. This is the only
 * class that references {@code jakarta.validation}, so the rest of the library works without it.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 리플렉션으로 Bean Validation 제약을 읽어 {@link S2BeanValidation}용으로 변환합니다. {@code jakarta.validation}을 참조하는 유일한
 * 클래스이므로 나머지 기능은 그것 없이도 동작합니다.
 */
final class S2BeanValidationReader {

    /** 이 클래스가 S2 규칙으로 바꾸거나 판단하는 제약 (그 밖의 @Constraint 는 서버 전용) */
    private static final Set<Class<? extends Annotation>> HANDLED = Set.of(NotBlank.class, NotEmpty.class,
            NotNull.class, Size.class, Min.class, Max.class, DecimalMin.class, DecimalMax.class, PositiveOrZero.class,
            NegativeOrZero.class, Pattern.class, Email.class, AssertTrue.class, AssertFalse.class);

    /** 메시지에서 실제 값으로 바꾸는 어노테이션 속성 */
    private static final List<String> MESSAGE_ATTRIBUTES = List.of("min", "max", "value", "regexp");

    private S2BeanValidationReader() {
    }

    /**
     * Converts the constraints of {@code type}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code type}의 제약을 변환합니다.
     *
     * @param type      The class | 클래스
     * @param practical Whether to convert the practical mappings too | 실용 매핑도 변환할지
     * @return The conversion | 변환 결과
     */
    static Conversion read(Class<?> type, boolean practical) {
        Conversion conversion = S2BeanValidation.newConversion();
        Deque<Class<?>> visiting = new ArrayDeque<>();
        conversion.fields().addAll(readClass(type, "", practical, visiting, conversion.serverOnly()));
        return conversion;
    }

    private static List<FieldSpec> readClass(Class<?> type, String path, boolean practical, Deque<Class<?>> visiting,
            List<String> serverOnly) {
        visiting.push(type);
        try {
            for (Annotation a : type.getAnnotations()) {
                if (isConstraint(a)) {
                    // 클래스 단위 제약(필드 간 비교 등)
                    serverOnly.add((path.isEmpty() ? type.getSimpleName() : path) + " @" + a.annotationType().getSimpleName());
                }
            }
            List<FieldSpec> fields = new ArrayList<>();
            for (Field field : declaredFields(type)) {
                String fieldPath = S2BeanValidation.join(path, field.getName());
                List<RuleSpec> rules = readField(field, fieldPath, practical, visiting, serverOnly);
                if (!rules.isEmpty()) {
                    fields.add(new FieldSpec(field.getName(), fieldPath, rules));
                }
            }
            Set<String> accessors = new HashSet<>();
            if (type.isRecord()) {
                // record 구성요소의 어노테이션은 접근자 메서드에도 복사되므로 getter 제약으로 보지 않음
                for (java.lang.reflect.RecordComponent component : type.getRecordComponents()) {
                    accessors.add(component.getName());
                }
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && accessors.contains(method.getName())) {
                    continue;
                }
                for (Annotation a : method.getAnnotations()) {
                    if (isConstraint(a) || a instanceof Valid) {
                        // getter 에 단 제약은 1차 범위 밖
                        serverOnly.add(S2BeanValidation.join(path, method.getName() + "()") + " @"
                                + a.annotationType().getSimpleName());
                    }
                }
            }
            return fields;
        } finally {
            visiting.pop();
        }
    }

    /** 상위 클래스부터 선언 순서대로 인스턴스 필드 */
    private static List<Field> declaredFields(Class<?> type) {
        Deque<Class<?>> hierarchy = new ArrayDeque<>();
        for (Class<?> c = type; c != null && c != Object.class && !c.getName().startsWith("java."); c = c.getSuperclass()) {
            hierarchy.push(c);
        }
        List<Field> fields = new ArrayList<>();
        for (Class<?> c : hierarchy) {
            for (Field field : c.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    private static List<RuleSpec> readField(Field field, String path, boolean practical, Deque<Class<?>> visiting,
            List<String> serverOnly) {
        Class<?> type = field.getType();
        boolean text = CharSequence.class.isAssignableFrom(type);
        boolean multi = Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) || type.isArray();
        boolean notNull = defaultGroup(field.getAnnotationsByType(NotNull.class)).length > 0;
        List<RuleSpec> rules = new ArrayList<>();
        Set<S2RuleType> added = new HashSet<>();

        for (Annotation a : field.getAnnotations()) {
            for (Annotation c : unwrap(a)) {
                if (!isConstraint(c) || !inDefaultGroup(c)) {
                    continue;
                }
                if (!HANDLED.contains(c.annotationType())) {
                    serverOnly.add(path + " @" + c.annotationType().getSimpleName());
                    continue;
                }
                boolean converted = convert(c, type, text, multi, notNull, practical, rules, added);
                if (!converted) {
                    serverOnly.add(path + " @" + c.annotationType().getSimpleName());
                }
            }
        }

        if (field.isAnnotationPresent(Valid.class) || elementValid(field.getAnnotatedType())) {
            readValid(field, path, practical, visiting, serverOnly, rules);
        }
        if (hasContainerElementConstraints(field.getAnnotatedType())) {
            serverOnly.add(path + " <element constraints>");
        }
        return rules;
    }

    /** 제약 하나를 변환. 변환하지 못하면 false */
    private static boolean convert(Annotation c, Class<?> type, boolean text, boolean multi, boolean notNull,
            boolean practical, List<RuleSpec> rules, Set<S2RuleType> added) {
        String message = messageOf(c);
        if (c instanceof NotBlank) {
            return add(rules, added, S2RuleType.REQUIRED, null, message);
        }
        if (c instanceof NotEmpty) {
            // 공백만 있는 문자열·빈 요소만 있는 목록을 BV 는 받지만 REQUIRED 는 거부
            return practical && add(rules, added, S2RuleType.REQUIRED, null, message);
        }
        if (c instanceof NotNull) {
            // 문자열·컬렉션은 ""·[] 를 BV 는 받지만 REQUIRED 는 거부. 그 밖의 타입은 빈 입력이 null 로 바인딩되어 같음
            return (practical || (!text && !multi)) && add(rules, added, S2RuleType.REQUIRED, null, message);
        }
        if (c instanceof Size size) {
            if (!text && !multi) {
                return false;
            }
            S2RuleType min = text ? S2RuleType.MIN_LENGTH : S2RuleType.MIN_SIZE;
            S2RuleType max = text ? S2RuleType.MAX_LENGTH : S2RuleType.MAX_SIZE;
            if (size.min() > 0) {
                add(rules, added, min, size.min(), message);
            }
            if (size.max() < Integer.MAX_VALUE) {
                add(rules, added, max, size.max(), message);
            }
            return true;
        }
        if (c instanceof Min min) {
            return add(rules, added, S2RuleType.MIN_VALUE, min.value(), message);
        }
        if (c instanceof Max max) {
            return add(rules, added, S2RuleType.MAX_VALUE, max.value(), message);
        }
        if (c instanceof DecimalMin min) {
            return min.inclusive() && add(rules, added, S2RuleType.MIN_VALUE, min.value(), message);
        }
        if (c instanceof DecimalMax max) {
            return max.inclusive() && add(rules, added, S2RuleType.MAX_VALUE, max.value(), message);
        }
        if (c instanceof PositiveOrZero) {
            return add(rules, added, S2RuleType.MIN_VALUE, 0, message);
        }
        if (c instanceof NegativeOrZero) {
            return add(rules, added, S2RuleType.MAX_VALUE, 0, message);
        }
        if (c instanceof Pattern pattern) {
            return pattern.flags().length == 0 && clientCompatible(pattern.regexp())
                    && add(rules, added, S2RuleType.REGEX, pattern.regexp(), message);
        }
        if (c instanceof Email email) {
            boolean converted = false;
            if (email.flags().length == 0 && !".*".equals(email.regexp()) && clientCompatible(email.regexp())) {
                // BV @Email 은 이메일 형식과 regexp 를 모두 검사하므로 regexp 만 쓰면 더 느슨함
                converted = add(rules, added, S2RuleType.REGEX, email.regexp(), message);
            }
            if (practical) {
                converted |= add(rules, added, S2RuleType.EMAIL, null, message);
            }
            return converted;
        }
        if (c instanceof AssertTrue) {
            // BV 는 null 을 통과시키므로 null 이 될 수 없을 때만 (primitive boolean 또는 @NotNull)
            return (type == boolean.class || notNull) && add(rules, added, S2RuleType.ASSERT_TRUE, null, message);
        }
        if (c instanceof AssertFalse) {
            return add(rules, added, S2RuleType.ASSERT_FALSE, null, message);
        }
        return false;
    }

    /** 같은 규칙 타입은 한 번만 (예: @NotBlank 와 @NotNull 이 함께 있을 때) */
    private static boolean add(List<RuleSpec> rules, Set<S2RuleType> added, S2RuleType type, Object criterion,
            String message) {
        if (added.add(type)) {
            rules.add(new RuleSpec(type, criterion, message));
        }
        return true;
    }

    private static void readValid(Field field, String path, boolean practical, Deque<Class<?>> visiting,
            List<String> serverOnly, List<RuleSpec> rules) {
        Class<?> type = field.getType();
        boolean each = Collection.class.isAssignableFrom(type) || type.isArray();
        Class<?> target = each ? elementType(field) : type;
        if (Map.class.isAssignableFrom(type) || target == null || target.isPrimitive()
                || target.getName().startsWith("java.")) {
            serverOnly.add(path + " @Valid");
            return;
        }
        if (visiting.contains(target)) {
            // 자기 자신을 다시 참조하는 구조(트리 등)는 브라우저 규칙으로 펼칠 수 없음
            serverOnly.add(path + " @Valid (recursive)");
            return;
        }
        List<FieldSpec> nested = readClass(target, path, practical, visiting, serverOnly);
        if (!nested.isEmpty()) {
            rules.add(new RuleSpec(each ? S2RuleType.EACH : S2RuleType.NESTED, nested, null));
        }
    }

    private static Class<?> elementType(Field field) {
        Class<?> type = field.getType();
        if (type.isArray()) {
            return type.getComponentType();
        }
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length == 1
                && parameterized.getActualTypeArguments()[0] instanceof Class<?> element) {
            return element;
        }
        return null;
    }

    /** List<@Valid Item> 처럼 컬렉션 타입 인자·배열 요소에 @Valid 가 있는지 */
    private static boolean elementValid(AnnotatedType annotated) {
        if (annotated instanceof AnnotatedParameterizedType parameterized
                && parameterized.getAnnotatedActualTypeArguments().length == 1) {
            return parameterized.getAnnotatedActualTypeArguments()[0].isAnnotationPresent(Valid.class);
        }
        return annotated instanceof AnnotatedArrayType array
                && array.getAnnotatedGenericComponentType().isAnnotationPresent(Valid.class);
    }

    /** List<@NotBlank String> 처럼 타입 인자·배열 요소에 단 제약이 있는지 (@Valid 는 EACH 로 처리하므로 제외) */
    private static boolean hasContainerElementConstraints(AnnotatedType annotated) {
        List<AnnotatedType> nested = new ArrayList<>();
        if (annotated instanceof AnnotatedParameterizedType parameterized) {
            nested.addAll(List.of(parameterized.getAnnotatedActualTypeArguments()));
        } else if (annotated instanceof AnnotatedArrayType array) {
            nested.add(array.getAnnotatedGenericComponentType());
        }
        for (AnnotatedType element : nested) {
            for (Annotation a : element.getAnnotations()) {
                if (isConstraint(a)) {
                    return true;
                }
            }
            if (hasContainerElementConstraints(element)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConstraint(Annotation a) {
        return a.annotationType().isAnnotationPresent(Constraint.class);
    }

    /** @Size.List 같은 반복 컨테이너를 풀어 개별 제약으로 */
    private static List<Annotation> unwrap(Annotation a) {
        if (isConstraint(a)) {
            return List.of(a);
        }
        try {
            Method value = a.annotationType().getMethod("value");
            if (value.getReturnType().isArray() && value.getReturnType().getComponentType().isAnnotation()) {
                Object[] items = (Object[]) value.invoke(a);
                List<Annotation> list = new ArrayList<>();
                for (Object item : items) {
                    list.add((Annotation) item);
                }
                return list;
            }
        } catch (ReflectiveOperationException e) {
            // value 속성이 없는 일반 어노테이션
        }
        return List.of();
    }

    private static <A extends Annotation> A[] defaultGroup(A[] annotations) {
        return java.util.Arrays.stream(annotations).filter(S2BeanValidationReader::inDefaultGroup).toArray(
                n -> java.util.Arrays.copyOf(annotations, n));
    }

    /** groups 가 비었거나 Default 를 포함하면 기본 그룹 */
    private static boolean inDefaultGroup(Annotation c) {
        Object groups = attribute(c, "groups");
        if (!(groups instanceof Class<?>[] classes) || classes.length == 0) {
            return true;
        }
        for (Class<?> group : classes) {
            if (group == Default.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * 직접 쓴 메시지면 {min}·{max}·{value}·{regexp} 를 실제 값으로 바꿔 반환. BV 기본 메시지·메시지 키({...})·EL(${...})이면 null (S2 내장
     * 메시지 사용)
     */
    private static String messageOf(Annotation c) {
        Object raw = attribute(c, "message");
        if (!(raw instanceof String message) || message.isBlank() || message.contains("${")) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.indexOf('{', 1) < 0 && !trimmed.contains(" ")) {
            return null;
        }
        for (String name : MESSAGE_ATTRIBUTES) {
            Object value = attribute(c, name);
            if (value != null && !value.getClass().isArray()) {
                message = message.replace("{" + name + "}", String.valueOf(value));
            }
        }
        return message;
    }

    private static Object attribute(Annotation c, String name) {
        try {
            return c.annotationType().getMethod(name).invoke(c);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** 브라우저(JS)에서 같은 의미로 평가할 수 있는 정규식인지 */
    private static boolean clientCompatible(String regex) {
        if (S2RegexCompatibility.findIncompatibility(regex) != null) {
            return false;
        }
        try {
            java.util.regex.Pattern.compile(regex);
            return true;
        } catch (java.util.regex.PatternSyntaxException e) {
            return false;
        }
    }
}
