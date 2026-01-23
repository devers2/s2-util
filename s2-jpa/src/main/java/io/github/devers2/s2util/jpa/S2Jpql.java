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
package io.github.devers2.s2util.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.devers2.s2util.core.S2Template;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;

/**
 * A builder class that integrates S2Template's dynamic query generation functionality with JPA TypedQuery creation.
 * Through method chaining, parameters are set and finally an executable TypedQuery is returned.
 *
 * <p>
 * <b>Security and Design Features:</b>
 * </p>
 *
 * <ul>
 * <li><b>SQL Injection Prevention:</b> All condition clauses ({@code setParameter}) use JPA's parameter binding (:name) method.</li>
 * <li><b>Safe LIKE Handling:</b> Through {@link LikeMode}, wildcards (%) are combined at the Java level and passed as parameters to defend against query manipulation.</li>
 * <li><b>Sort Clause Validation:</b> {@link #setOrder(String, String)} parses the input string based on a whitelist (ASC/DESC check and regex separation),
 * completely blocking malicious query injection using sort clauses.</li>
 * </ul>
 *
 * <p>
 * Usage Example:
 * </p>
 *
 * <pre>{@code
 * TypedQuery<Member> query = S2Jpql.of(
 *         """
 *         SELECT member
 *         FROM Member member
 *         WHERE 1=1
 *         {{=cond_age}}
 *         {{=cond_name}}
 *         {{=cond_ids}}
 *         {{=cond_order}}
 *         """,
 *         entityManager,
 *         Member.class
 * )
 *         .setParameter("cond_age", "age", age, "AND member.age = :age") // Replace {{=cond_age}} with AND member.age = :age, then handle TypedQuery.setParameter
 *         .setParameter("cond_name", "name", name, "AND member.name LIKE :name", LikeMode.ANYWHERE) // Replace {{=cond_name}} with AND member.name LIKE :name, then handle TypedQuery.setParameter
 *         .setParameter("cond_ids", "ids", ids, "AND member.id IN :ids") // Replace {{=cond_ids}} with AND member.id IN :ids, then handle TypedQuery.setParameter
 *         .setOrder("cond_order", "member.id, member.age DESC") // Replace {{=cond_order}} with ORDER BY member.id, member.age DESC
 *         .build();
 * }</pre>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Template의 동적 쿼리 생성 기능과 JPA TypedQuery 생성을 통합한 빌더 클래스입니다.
 * 메서드 체이닝을 통해 파라미터를 설정하고 최종적으로 실행 가능한 TypedQuery를 반환합니다.
 *
 * <p>
 * <b>보안 및 설계 특징:</b>
 * </p>
 *
 * <ul>
 * <li><b>SQL Injection 방지:</b> 모든 조건절({@code setParameter})은 JPA의 파라미터 바인딩(:name) 방식을 사용합니다.</li>
 * <li><b>안전한 LIKE 처리:</b> {@link LikeMode}를 통해 와일드카드(%)를 Java 단에서 결합하여 파라미터로 넘김으로써 쿼리 조작을 방어합니다.</li>
 * <li><b>정렬 구문 검증:</b> {@link #setOrder(String, String)}는 입력 문자열을 화이트리스트 기반(ASC/DESC 체크 및 정규식 분리)으로 파싱하여,
 * 정렬 구문을 이용한 악성 쿼리 삽입을 원천 차단합니다.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.0
 * @since 1.0
 * @see S2Template
 * @see LikeMode
 */
public class S2Jpql<T> extends S2Template {

    private final EntityManager entityManager;
    private final Class<T> resultClass;
    private final Map<String, Object> parameters;

    private S2Jpql(String sql, EntityManager entityManager, Class<T> resultClass) {
        super(sql);
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager must not be null");
        this.resultClass = Objects.requireNonNull(resultClass, "Result class must not be null");
        this.parameters = new HashMap<>();
    }

    /**
     * Creates an S2Jpql builder instance.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2Jpql 빌더 인스턴스를 생성합니다.
     *
     * @param em          JPA EntityManager | JPA 엔티티 매니저
     * @param sql         Base JPQL string containing placeholders | 치환자가 포함된 기본 JPQL 문자열
     * @param resultClass Result entity class | 결과 엔티티 클래스
     * @return S2Jpql builder object | S2Jpql 빌더 객체
     */
    public static <T> S2Jpql<T> of(EntityManager em, String sql, Class<T> resultClass) {
        return new S2Jpql<>(sql, em, resultClass);
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix   String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix   String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, String prefix, String suffix) {
        super.bindWhen(key, condition, clause, prefix, suffix);
        putParameters(name, value, null);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix   String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix   String to append to the clause | 절 뒤에 붙을 문자열
     * @param likeMode Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, String prefix, String suffix, LikeMode likeMode) {
        super.bindWhen(key, condition, clause, prefix, suffix);
        putParameters(name, value, likeMode);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix   String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, String prefix) {
        super.bindWhen(key, condition, clause, prefix);
        putParameters(name, value, null);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix   String to prepend to the clause | 절 앞에 붙을 문자열
     * @param likeMode Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, String prefix, LikeMode likeMode) {
        super.bindWhen(key, condition, clause, prefix);
        putParameters(name, value, likeMode);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause) {
        super.bindWhen(key, condition, clause);
        putParameters(name, value, null);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param likeMode Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, boolean condition, String name, Object value, Object clause, LikeMode likeMode) {
        super.bindWhen(key, condition, clause);
        putParameters(name, value, likeMode);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param name   Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value  Parameter value | 파라미터 값
     * @param clause Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, String prefix, String suffix) {
        super.bindWhen(key, value, clause, prefix, suffix);
        putParameters(name, value, null);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix   String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix   String to append to the clause | 절 뒤에 붙을 문자열
     * @param likeMode Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, String prefix, String suffix, LikeMode likeMode) {
        super.bindWhen(key, value, clause, prefix, suffix);
        putParameters(name, value, likeMode);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param name   Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value  Parameter value | 파라미터 값
     * @param clause Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, String prefix) {
        super.bindWhen(key, value, clause, prefix);
        putParameters(name, value, null);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix   String to prepend to the clause | 절 앞에 붙을 문자열
     * @param likeMode Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, String prefix, LikeMode likeMode) {
        super.bindWhen(key, value, clause, prefix);
        putParameters(name, value, likeMode);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key    Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param name   Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value  Parameter value | 파라미터 값
     * @param clause Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause) {
        super.bindWhen(key, value, clause);
        putParameters(name, value, null);
        return this;
    }

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key      Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param name     Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param value    Parameter value | 파라미터 값
     * @param clause   Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param likeMode Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setParameter(String key, String name, Object value, Object clause, LikeMode likeMode) {
        super.bindWhen(key, value, clause);
        putParameters(name, value, likeMode);
        return this;
    }

    /**
     * Parses the sort condition string and dynamically adds an ORDER BY clause.
     * Supports multiple sorts separated by commas (,), and validates the sort direction (ASC/DESC).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 정렬 조건 문자열을 파싱하여 동적으로 ORDER BY 절을 추가합니다.
     * 콤마(,)로 구분된 다중 정렬을 지원하며, 정렬 방향(ASC/DESC)의 유효성을 검사합니다.
     *
     * @param key  Template key to be replaced (e.g., "order_clause") | 치환 대상 템플릿 키 (예: "order_clause")
     * @param sort Sort string (e.g., "m.name DESC, m.age") | 정렬 문자열 (예: "m.name DESC, m.age")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    public S2Jpql<T> setOrder(String key, String sort) {
        if (sort == null || sort.isBlank()) {
            return this;
        }

        String result = java.util.Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(sortInfo -> {
                    String[] parts = sortInfo.split("\\s+");
                    if (parts.length == 1)
                        return parts[0];
                    if (parts.length == 2) {
                        String direction = parts[1].toUpperCase();
                        if (direction.equals("ASC") || direction.equals("DESC")) {
                            return parts[0] + " " + direction;
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.joining(", "));

        // 결과가 있을 때만 부모 템플릿에 바인딩
        super.bindWhen(key, !result.isBlank(), result, "ORDER BY ");
        return this;
    }

    private void putParameters(String name, Object value, LikeMode likeMode) {
        Object processedValue = value;
        if (value instanceof String && likeMode != null) {
            String stringValue = (String) value;
            processedValue = switch (likeMode) {
                case ANYWHERE -> "%" + stringValue + "%";
                case START -> stringValue + "%";
                case END -> "%" + stringValue;
            };
        }
        parameters.put(name, processedValue);
    }

    /**
     * Creates a TypedQuery with parameter binding completed based on the settings so far.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지금까지 설정된 내용을 바탕으로 파라미터 바인딩이 완료된 TypedQuery를 생성합니다.
     *
     * @return Executable TypedQuery object | 실행 가능한 TypedQuery 객체
     */
    public TypedQuery<T> build() {
        // 1. 템플릿 렌더링
        String renderedSql = super.render();
        TypedQuery<T> query = entityManager.createQuery(renderedSql, resultClass);

        // 2. 자동 바인딩 (S2Template의 rawValues 활용)
        for (Parameter<?> param : query.getParameters()) {
            String name = param.getName();
            if (name != null && parameters.containsKey(name)) {
                query.setParameter(name, parameters.get(name));
            }
        }

        return query;
    }

    /**
     * Enumeration that determines the position of wildcards (%) in LIKE searches.
     * This enum provides the basis for automatically adding % before, after, or on both sides of the search term in S2Jpql.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * LIKE 검색 시 와일드카드(%)의 위치를 결정하는 모드입니다.
     * 이 열거형은 S2Jpql에서 검색어의 앞, 뒤 또는 양옆에 %를 자동으로 붙여주는 기준이 됩니다.
     */
    public enum LikeMode {
        /**
         * Adds % before and after the search term. (e.g., %searchterm%)
         * Searches for strings that contain the word anywhere within them.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 검색어의 앞뒤에 %를 붙입니다. (예: %검색어%)
         * 문자열 내의 어느 위치에든 해당 단어가 포함되어 있으면 검색됩니다.
         */
        ANYWHERE,
        /**
         * Adds % after the search term. (e.g., searchterm%)
         * Searches for strings that start with the word.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 검색어의 뒤에 %를 붙입니다. (예: 검색어%)
         * 해당 단어로 시작하는 문자열을 검색합니다.
         */
        START,
        /**
         * Adds % before the search term. (e.g., %searchterm)
         * Searches for strings that end with the word.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 검색어의 앞에 %를 붙입니다. (예: %검색어)
         * 해당 단어로 끝나는 문자열을 검색합니다.
         */
        END
    }

}
