/**
 * S2 Support Library
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
package io.github.devers2.s2util.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * S2Template is a utility for creating dynamic strings (SQL, JPQL, messages, etc.)
 * safely and readably in Java 17+ environments.
 * </p>
 *
 * <p>
 * It uses JS-style {@code {{=key}}} syntax, provides conditional binding that includes
 * specific clauses only when values exist, and offers query-specific formatting features.
 * </p>
 *
 * <pre>{@code
 * String jpql = S2Template.of(
 *         """
 *             SELECT m FROM Member m
 *             WHERE 1=1
 *             {{=name_cond}}
 *             {{=age_in}}
 *         """
 * )
 *         .bind("name_cond", name, "AND m.name = ")
 *         .bindIn("age_in", ageList, "AND m.age IN (", ")")
 *         .render();
 * }</pre>
 *
 * <p>
 * [한국어 설명]
 * </p>
 * S2Template은 Java 17 이상의 환경에서 동적 문자열(SQL, JPQL, 메시지 등)을
 * 안전하고 가독성 있게 생성하기 위한 유틸리티입니다.
 * <p>
 * JS 스타일의 {@code {{=key}}} 문법을 사용하며, 값이 존재할 때만 특정 구문을 포함시키는
 * 조건부 바인딩과 쿼리 전용 포맷팅 기능을 제공합니다.
 * </p>
 *
 * @author devers2
 * @since 1.0.0
 */
public class S2Template {

    protected final String template;
    protected final Map<String, String> bindings = new HashMap<>();

    protected S2Template(String template) {
        this.template = Objects.requireNonNull(template, "Template must not be null");
    }

    /**
     * Creates a new S2Template instance.
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 새로운 S2Template 인스턴스를 생성합니다.
     *
     * @param template Template string containing {{=key}} format (text blocks recommended) | {{=key}} 형식을 포함한 템플릿 문자열 (텍스트 블록 사용 권장)
     * @return S2Template instance | S2Template 인스턴스
     */
    public static S2Template of(String template) {
        return new S2Template(template);
    }

    /**
     * [Value-based binding] Replaces with 'prefix + value' format only when value is valid.
     * <p>
     * Used when you want to directly include actual data values in queries or messages.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Value 기반 바인딩] 값이 유효할 때만 'prefix + value' 형태로 치환합니다.
     * <p>
     * 실제 데이터 값을 쿼리나 메시지에 직접 포함하고 싶을 때 사용합니다.
     * </p>
     *
     * @param key    Template key to be replaced (e.g., "name" -> {{=name}}) | 템플릿 내의 치환 대상 키 (예: "name" -> {{=name}})
     * @param value  Actual data value to be replaced | 치환될 실제 데이터 값
     * @param prefix Prefix to prepend when value exists (e.g., "AND name = ") | 값이 존재할 때 값 앞에 붙일 접두사 (예: "AND name = ")
     * @param suffix Suffix to append when content is injected | 내용 주입 시 뒤에 붙일 접미사
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Usage examples and results:
     *
     *          <pre>{@code
     * // When value exists
     * .bind("id", 10, "ID(", ")") → "ID(10)"
     * // When value is null
     * .bind("id", null, "ID(", ")") → ""
     * }</pre>
     */
    public S2Template bind(String key, Object value, String prefix, String suffix) {
        return doBind(key, value, prefix, null);
    }

    /**
     * [Value-based binding] Replaces with 'prefix + value' format only when value is valid.
     * <p>
     * Used when you want to directly include actual data values in queries or messages.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Value 기반 바인딩] 값이 유효할 때만 'prefix + value' 형태로 치환합니다.
     * <p>
     * 실제 데이터 값을 쿼리나 메시지에 직접 포함하고 싶을 때 사용합니다.
     * </p>
     *
     * @param key    Template key to be replaced (e.g., "name" -> {{=name}}) | 템플릿 내의 치환 대상 키 (예: "name" -> {{=name}})
     * @param value  Actual data value to be replaced | 치환될 실제 데이터 값
     * @param prefix Prefix to prepend when value exists (e.g., "AND name = ") | 값이 존재할 때 값 앞에 붙일 접두사 (예: "AND name = ")
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Usage examples and results:
     *
     *          <pre>{@code
     * // When value exists
     * .bind("id", 10, "ID:") → "ID:10"
     * // When value is null
     * .bind("id", null, "ID:") → ""
     * }</pre>
     */
    public S2Template bind(String key, Object value, String prefix) {
        return doBind(key, value, prefix, null);
    }

    /**
     * [Value-based binding] Replaces with value itself only when value is valid, without prefix.
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Value 기반 바인딩] 접두사 없이 값 자체만 치환합니다.
     *
     * @param key   Template key to be replaced | 템플릿 내의 치환 대상 키
     * @param value Actual data value to be replaced | 치환될 실제 데이터 값
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Usage examples and results:
     *
     *          <pre>{@code
     * // When value exists
     * .bind("id", 10) → "10"
     * // When value is null
     * .bind("id", null) → ""
     * }</pre>
     */
    public S2Template bind(String key, Object value) {
        return doBind(key, value, null, null);
    }

    /**
     * [Value 기반 바인딩] 값이 유효할 때만 'prefix + value' 형태로 치환합니다.
     */
    private S2Template doBind(String key, Object value, String prefix, String suffix) {
        String p = (prefix != null && !prefix.isBlank()) ? prefix : "";
        String s = (suffix != null && !suffix.isBlank()) ? suffix : "";
        String bindValue = isValid(value) ? p + value.toString() + s : "";
        putBinding(key, bindValue);
        return this;
    }

    /**
     * [Condition-based binding] Injects specified content when condition is true.
     * <p>
     * Uses the logical condition as a trigger. When valid, combines prefix and content
     * to replace the template key. Useful for conditionally inserting clauses like AND, ORDER BY
     * in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Condition 기반 바인딩] 조건(condition)이 true일 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code condition}의 논리 조건으로 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param condition Boolean condition to check | 유효성을 검사할 Boolean 조건
     * @param content   Actual content to inject when value is valid (Object.toString() is used) | 값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @param prefix    Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @param suffix    Suffix to append when content is injected | 내용 주입 시 뒤에 붙일 접미사
     * @return S2Template instance for method chaining | 메서드 체이닝을 위한 S2Template 인스턴스
     * @apiNote
     *          Mainly used for inserting sentences based on sorting conditions or true/false of specific business logic.
     *
     *          <pre>{@code
     * .bindWhen("order", pageable.isSorted(), "m.id", "ORDER BY ", " DESC")
     * // Result: "ORDER BY m.id DESC" if pageable.isSorted() is true
     * }</pre>
     */
    public S2Template bindWhen(String key, boolean condition, Object content, String prefix, String suffix) {
        return doBindWhen(key, condition, content, prefix, suffix);
    }

    /**
     * [Condition-based binding] Injects specified content when condition is true.
     * <p>
     * Uses the logical condition as a trigger. When valid, combines prefix and content
     * to replace the template key. Useful for conditionally inserting clauses like AND, ORDER BY
     * in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Condition 기반 바인딩] 조건(condition)이 true일 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code condition}의 논리 조건으로 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param condition Boolean condition to check | 유효성을 검사할 Boolean 조건
     * @param content   Actual content to inject when value is valid (Object.toString() is used) | 값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @param prefix    Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @return S2Template instance for method chaining | 메서드 체이닝을 위한 S2Template 인스턴스
     * @apiNote
     *          Mainly used for inserting sentences based on sorting conditions or true/false of specific business logic.
     *
     *          <pre>{@code
     * .bindWhen("order", pageable.isSorted(), "m.id DESC", "ORDER BY ")
     * // Result: "ORDER BY m.id DESC" if pageable.isSorted() is true
     * }</pre>
     */
    public S2Template bindWhen(String key, boolean condition, Object content, String prefix) {
        return doBindWhen(key, condition, content, prefix, null);
    }

    /**
     * [Condition-based binding] Injects specified content when condition is true.
     * <p>
     * Uses the logical condition as a trigger. When valid, replaces the template key with content.
     * Useful for conditionally inserting clauses like AND, ORDER BY in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Condition 기반 바인딩] 조건(condition)이 true일 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code condition}의 논리 조건으로 트리거로 사용합니다.
     * 유효할 경우, {@code content}로 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param condition Boolean condition to check | 유효성을 검사할 Boolean 조건
     * @param content   Actual content to inject when value is valid (Object.toString() is used) | 값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @return S2Template instance for method chaining | 메서드 체이닝을 위한 S2Template 인스턴스
     * @apiNote
     *          Mainly used for inserting sentences based on sorting conditions or true/false of specific business logic.
     *
     *          <pre>{@code
     * .bindWhen("order", pageable.isSorted(), "m.id DESC")
     * // Result: "m.id DESC" if pageable.isSorted() is true
     * }</pre>
     */
    public S2Template bindWhen(String key, boolean condition, Object content) {
        return doBindWhen(key, condition, content, null, null);
    }

    /**
     * [Presence-based binding] Injects specified content when presence value exists.
     * <p>
     * Uses the validity of presence (not null, not empty) as a trigger.
     * When valid, combines prefix and content to replace the template key.
     * Useful for conditionally inserting clauses like AND, ORDER BY in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Presence 기반 바인딩] 값(presence)이 존재할 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code presence}의 유효성(null 아님, 비어 있지 않음)을 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param presence Reference value or Boolean condition to check validity | 유효성을 검사할 기준 값 혹은 Boolean 조건
     * @param content  Actual content to inject when value is valid (Object.toString() is used) | 값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @param prefix   Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @param suffix   Suffix to append when content is injected | 내용 주입 시 뒤에 붙일 접미사
     * @return S2Template instance for method chaining | 메서드 체이닝을 위한 S2Template 인스턴스
     * @apiNote
     *          {@code presence} acts only as a trigger for existence judgment, actual replacement is done with {@code content}.
     *
     *          <pre>{@code
     * .bindWhen("stage", stage, ":stage", "AND s.stage IN (", ")")
     * // Result: "AND s.stage IN (:stage)" if stage exists
     * }</pre>
     */
    public S2Template bindWhen(String key, Object presence, Object content, String prefix, String suffix) {
        return doBindWhen(key, presence, content, prefix, suffix);
    }

    /**
     * [Presence-based binding] Injects specified content when presence value exists.
     * <p>
     * Uses the validity of presence (not null, not empty) as a trigger.
     * When valid, combines prefix and content to replace the template key.
     * Useful for conditionally inserting clauses like AND, ORDER BY in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Presence 기반 바인딩] 값(presence)이 존재할 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code presence}의 유효성(null 아님, 비어 있지 않음)을 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param presence Reference value or Boolean condition to check validity | 유효성을 검사할 기준 값 혹은 Boolean 조건
     * @param content  Actual content to inject when value is valid (Object.toString() is used) | 값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @param prefix   Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @return S2Template instance for method chaining | 메서드 체이닝을 위한 S2Template 인스턴스
     * @apiNote
     *          {@code presence} acts only as a trigger for existence judgment, actual replacement is done with {@code content}.
     *
     *          <pre>{@code
     * .bindWhen("stage", stage, "s.stage = :stage", "AND ")
     * // Result: "AND s.stage = :stage" if stage exists
     * }</pre>
     */
    public S2Template bindWhen(String key, Object presence, Object content, String prefix) {
        return doBindWhen(key, presence, content, prefix, null);
    }

    /**
     * [Presence-based binding] Injects specified content when presence value exists.
     * <p>
     * Uses the validity of presence (not null, not empty) as a trigger.
     * When valid, replaces the template key with content.
     * Useful for conditionally inserting clauses like AND, ORDER BY in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Presence 기반 바인딩] 값(presence)이 존재할 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code presence}의 유효성(null 아님, 비어 있지 않음)을 트리거로 사용합니다.
     * 유효할 경우, {@code content}로 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param presence Reference object to check validity (ignored if null/blank) | 유효성을 검사할 기준 객체 (null/blank 시 무시)
     * @param content  Actual content to inject when value is valid (Object.toString() is used) | 값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @return S2Template instance for method chaining | 메서드 체이닝을 위한 S2Template 인스턴스
     * @apiNote
     *          {@code presence} acts only as a trigger for existence judgment, actual replacement is done with {@code content}.
     *
     *          <pre>{@code
     * .bindWhen("stage", stage, "s.stage = :stage")
     * // Result: "s.stage = :stage" if stage exists
     * }</pre>
     */
    public S2Template bindWhen(String key, Object presence, Object content) {
        return doBindWhen(key, presence, content, null, null);
    }

    /**
     * [Condition/Presence 기반 바인딩] 조건이 충족되거나 값이 존재할 때 지정된 내용(content)을 주입합니다.
     * <p>
     * 이 메서드는 {@code value}의 유효성(null 아님, 비어 있지 않음, 혹은 true)을 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 문장 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key       템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param condition 유효성을 검사할 기준 값 혹은 Boolean 조건
     * @param content   값이 유효할 때 주입할 실제 내용 (Object의 toString()이 사용됨)
     * @param prefix    내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @param suffix    내용 주입 시 뒤에 붙일 접미사
     * @return 메서드 체이닝을 위한 S2Template 인스턴스
     */
    private S2Template doBindWhen(String key, Object condition, Object content, String prefix, String suffix) {
        String p = (prefix != null && !prefix.isBlank()) ? prefix : "";
        String s = (suffix != null && !suffix.isBlank()) ? suffix : "";
        String bindValue = isValid(condition) && S2Util.isNotEmpty(content) ? p + content.toString() + s : "";
        putBinding(key, bindValue);
        return this;
    }

    /**
     * [Collection-based binding] When collection is valid, joins elements
     * and wraps with specified prefix and suffix.
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Collection 기반 바인딩] 컬렉션이 유효할 때,
     * 요소들을 연결하여 지정된 접두사와 접미사로 감싸 주입합니다.
     *
     * @param key    Template key to be replaced | 템플릿 내의 치환 대상 키
     * @param values Collection to check validity (ignored if null/empty) | 유효성을 검사할 컬렉션 (null/empty 시 무시)
     * @param prefix Starting phrase for result (e.g., "AND id IN (") | 결과물 시작 문구 (예: "AND id IN (")
     * @param suffix Ending phrase for result (e.g., ")") | 결과물 종료 문구 (예: ")")
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Universally used for SQL IN clause generation and conditional list string conversion.
     *
     *          <pre>{@code
     * List<Integer> ids = List.of(1, 2, 3);
     * // Example: Generate IN clause only when active
     * .bindIn("ids", ids, "AND id IN (", ")")
     * // Result: "AND id IN (1, 2, 3)" if ids has Collection value
     * }</pre>
     */
    public S2Template bindIn(String key, Collection<?> values, String prefix, String suffix) {
        return bindIn(key, true, values, prefix, suffix);
    }

    /**
     * [Collection-based binding] When collection is valid, joins elements
     * and prepends specified prefix.
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Collection 기반 바인딩] 컬렉션이 유효할 때,
     * 요소들을 연결하여 지정된 접두사를 붙여 주입합니다.
     *
     * @param key    Template key to be replaced | 템플릿 내의 치환 대상 키
     * @param values Collection to check validity (ignored if null/empty) | 유효성을 검사할 컬렉션 (null/empty 시 무시)
     * @param prefix Starting phrase for result (e.g., "AND id IN (") | 결과물 시작 문구 (예: "AND id IN (")
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Universally used for SQL IN clause generation and conditional list string conversion.
     *
     *          <pre>{@code
     * List<String> order = List.of("name", "age", "created_at");
     * // Example: Generate IN clause only when active
     * .bindIn("order", order, "ORDER BY ")
     * // Result: "ORDER BY name, age, created_at" if order has Collection value
     * }</pre>
     */
    public S2Template bindIn(String key, Collection<?> values, String prefix) {
        return bindIn(key, true, values, prefix, null);
    }

    /**
     * [Collection-based binding] When collection is valid, joins elements.
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Collection 기반 바인딩] 컬렉션이 유효할 때,
     * 요소들을 연결하여 주입합니다.
     *
     * @param key    Template key to be replaced | 템플릿 내의 치환 대상 키
     * @param values Collection to check validity (ignored if null/empty) | 유효성을 검사할 컬렉션 (null/empty 시 무시)
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Universally used for SQL IN clause generation and conditional list string conversion.
     *
     *          <pre>{@code
     * List<String> order = List.of("name", "age", "created_at");
     * // Example: Generate IN clause only when active
     * .bindIn("order", order)
     * // Result: "name, age, created_at" if order has Collection value
     * }</pre>
     */
    public S2Template bindIn(String key, Collection<?> values) {
        return bindIn(key, true, values, null, null);
    }

    /**
     * [Collection-based binding] When condition is met and collection is valid,
     * joins elements and wraps with specified prefix and suffix.
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Collection 기반 바인딩] 조건이 충족되고 컬렉션이 유효할 때,
     * 요소들을 연결하여 지정된 접두사와 접미사로 감싸 주입합니다.
     *
     * @param key       Template key to be replaced | 템플릿 내의 치환 대상 키
     * @param condition Logical condition to determine binding | 바인딩 여부를 결정하는 논리 조건
     * @param values    Collection to check validity (ignored if null/empty) | 유효성을 검사할 컬렉션 (null/empty 시 무시)
     * @param prefix    Starting phrase for result (e.g., "AND id IN (") | 결과물 시작 문구 (예: "AND id IN (")
     * @param suffix    Ending phrase for result (e.g., ")") | 결과물 종료 문구 (예: ")")
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Universally used for SQL IN clause generation and conditional list string conversion.
     *
     *          <pre>{@code
     * List<Integer> ids = List.of(1, 2, 3);
     * // Example: Generate IN clause only when active
     * .bindIn("ids", isActive, ids, "AND id IN (", ")")
     * // Result: "AND id IN (1, 2, 3)" if isActive is true and Collection value exists
     * }</pre>
     */
    public S2Template bindIn(String key, boolean condition, Collection<?> values, String prefix, String suffix) {
        if (condition && isValid(values)) {
            String result = values.stream()
                    .map(this::formatQueryValue)
                    .collect(
                            Collectors.joining(
                                    ", ",
                                    (prefix != null ? prefix : ""),
                                    (suffix != null ? suffix : "")
                            )
                    );
            putBinding(key, result);
        }
        return this;
    }

    /**
     * Stores the binding value if it's not null or blank.
     */
    private void putBinding(String key, String bindValue) {
        if (bindValue != null && !bindValue.isBlank()) {
            bindings.put(key, bindValue);
        }
    }

    /**
     * [Functional condition control] Performs specific Action only when value is valid.
     * <p>
     * Useful for logging or additional state changes besides binding.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [함수형 조건 제어] 값이 유효할 때만 특정 작업(Action)을 수행합니다.
     * <p>
     * 바인딩 외에 로그 출력이나 추가적인 상태 변경이 필요할 때 유용합니다.
     * </p>
     *
     * @param value  Value to check validity | 유효성을 검사할 값
     * @param action Consumer to execute when value is valid (current S2Template instance is passed) | 값이 유효할 때 실행할 Consumer (현재 S2Template 인스턴스가 전달됨)
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     */
    public S2Template ifPresent(Object value, Consumer<S2Template> action) {
        if (isValid(value)) {
            action.accept(this);
        }
        return this;
    }

    /**
     * Generates the final string based on bound data.
     * <p>
     * Logic for removing unsubstituted patterns, removing blank lines,
     * cleaning trailing spaces per line, and trimming overall spaces is applied.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 바인딩된 데이터를 바탕으로 최종 문자열을 생성합니다.
     * <p>
     * 미치환된 패턴 제거, 빈 줄 제거, 줄 끝 공백 정리 및 전체 앞뒤 공백 정리 로직이 적용됩니다.
     * </p>
     *
     * @return Final result string in cleaned form | 정리된 형태의 최종 결과 문자열
     */
    public String render() {
        StringBuilder resultBuilder = new StringBuilder(template);

        // 1. 등록된 바인딩 치환
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            String target = "{{=" + entry.getKey() + "}}";
            String replacement = entry.getValue();
            int index = resultBuilder.indexOf(target);
            while (index != -1) {
                resultBuilder.replace(index, index + target.length(), replacement);
                index = resultBuilder.indexOf(target, index + replacement.length());
            }
        }

        String result = resultBuilder.toString();

        // 2. 미치환된 {{=...}} 패턴 제거 (JS 스타일 호환)
        result = S2StringUtil.replaceAll(result, "\\{\\{=.*?\\}\\}", "");

        // 3. 디테일 정리: 빈 줄 제거 및 줄 끝 공백 정리
        return result.lines()
                .filter(line -> !line.isBlank())
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .strip();
    }

    /**
     * 쿼리 문법에 맞게 값을 포맷팅합니다. (홑따옴표 처리 등)
     */
    private String formatQueryValue(Object value) {
        if (value == null)
            return "NULL";
        if (value instanceof String s) {
            return "'" + s.replace("'", "''") + "'";
        }
        return value.toString();
    }

    /**
     * 값의 유효성을 체크합니다.
     */
    private boolean isValid(Object value) {
        if (value == null)
            return false;
        if (value instanceof Boolean b)
            return b;
        return S2Util.isNotEmpty(value);
    }

}
