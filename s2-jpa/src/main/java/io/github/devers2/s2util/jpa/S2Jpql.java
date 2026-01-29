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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.devers2.s2util.core.S2Template;
import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
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
 * <li><b>SQL Injection Prevention:</b> All condition clauses ({@code bindClause}) use JPA's parameter binding (:name) method.</li>
 * <li><b>Safe LIKE Handling:</b> Through {@link LikeMode}, wildcards (%) are combined at the Java level and passed as parameters to defend against query manipulation.</li>
 * <li><b>Sort Clause Validation:</b> {@link #bindOrderBy(String, String)} parses the input string based on a whitelist (ASC/DESC check and regex separation),
 * completely blocking malicious query injection using sort clauses.</li>
 * </ul>
 *
 * <p>
 * Usage Example:
 * </p>
 *
 * <pre>{@code
 * TypedQuery<Member> query = S2Jpql.from(entityManager)
 *         .type(Member.class)
 *         .query("""
 *                SELECT member
 *                FROM Member member
 *                WHERE 1=1
 *                {{=cond_age}}
 *                {{=cond_name}}
 *                {{=cond_ids}}
 *                {{=cond_order}}
 *                """)
 *         .bindClause("cond_age", age, "AND member.age = :age")
 *         .bindParameter("age", age)
 *         .bindClause("cond_name", name, "AND member.name LIKE :name")
 *         .bindParameter("name", name, LikeMode.ANYWHERE)
 *         .bindClause("cond_ids", ids, "AND member.id IN :ids")
 *         .bindParameter("ids", ids)
 *         .bindOrderBy("cond_order", "member.id, member.age DESC")
 *         .build();
 * }</pre>
 *
 * <p>
 * <b>⚠️ CRITICAL SECURITY WARNING:</b>
 * </p>
 * <p>
 * <b>NEVER</b> include external or user-provided variables in the <code>clause</code> parameter of <code>bindClause</code> methods.
 * Only use hardcoded strings for clauses. All dynamic values must be passed through the <code>parameterValue</code> parameter,
 * which will be safely bound using JPA's parameter binding mechanism (:name).
 * </p>
 * <p>
 * <b>Example of SAFE usage:</b>
 * </p>
 *
 * <pre>{@code
 * .bindClause("cond_name", userInput, "AND m.name = :name")  // Clause is hardcoded
 * .bindParameter("name", userInput, LikeMode.ANYWHERE)  // Value bound safely here
 * }</pre>
 * <p>
 * <b>Example of DANGEROUS usage (DO NOT DO THIS):</b>
 * </p>
 *
 * <pre>{@code
 * .bindClause("cond_name", userInput, "AND m.name = " + userInput)  // DANGEROUS: SQL Injection risk!
 * }</pre>
 * <p>
 * Failure to follow this rule can result in <b>SQL Injection vulnerabilities</b>.
 * </p>
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
 * <li><b>SQL Injection 방지:</b> 모든 조건절({@code bindClause})은 JPA의 파라미터 바인딩(:name) 방식을 사용합니다.</li>
 * <li><b>안전한 LIKE 처리:</b> {@link LikeMode}를 통해 와일드카드(%)를 Java 단에서 결합하여 파라미터로 넘김으로써 쿼리 조작을 방어합니다.</li>
 * <li><b>정렬 구문 검증:</b> {@link #bindOrderBy(String, String)}는 입력 문자열을 화이트리스트 기반(ASC/DESC 체크 및 정규식 분리)으로 파싱하여,
 * 정렬 구문을 이용한 악성 쿼리 삽입을 원천 차단합니다.</li>
 * </ul>
 *
 * <p>
 * <b>⚠️ 치명적인 보안 경고:</b>
 * </p>
 * <p>
 * <b>절대</b> <code>bindClause</code> 메서드의 <code>clause</code> 파라미터에 외부 또는 사용자 제공 변수를 포함하지 마세요.
 * 절에는 하드코딩된 문자열만 사용하세요. 모든 동적 값은 <code>parameterValue</code> 파라미터를 통해 전달해야 하며,
 * JPA의 파라미터 바인딩 메커니즘(:name)을 통해 안전하게 바인딩됩니다.
 * </p>
 * <p>
 * <b>안전한 사용 예시:</b>
 * </p>
 *
 * <pre>{@code
 * .bindClause("cond_name", userInput, "AND m.name = :name")  // 절은 하드코딩됨
 * .bindParameter("name", userInput, LikeMode.ANYWHERE)  // 값은 여기서 안전하게 바인딩
 * }</pre>
 * <p>
 * <b>위험한 사용 예시 (절대 하지 마세요):</b>
 * </p>
 *
 * <pre>{@code
 * .bindClause("cond_name", userInput, "AND m.name = " + userInput)  // 위험: SQL 인젝션 가능!
 * }</pre>
 * <p>
 * 이 규칙을 따르지 않으면 <b>SQL 인젝션 취약점</b>이 발생할 수 있습니다.
 * </p>
 *
 * @author devers2
 * @version 1.0
 * @since 1.0
 * @see S2Template
 * @see LikeMode
 */

public class S2Jpql<T> extends S2Template implements Executor<T> {

    S2Logger logger = S2LogManager.getLogger(S2Jpql.class);

    /**
     * Step 1: Specify EntityManager
     */
    public interface TypeStep {
        <T> QueryStep<T> type(Class<T> entityClass);
    }

    /**
     * Step 2: Specify entity type
     */
    public interface QueryStep<T> {
        Executor<T> query(String jpql);
    }

    /**
     * Interface for binding Supplier-based parameters after bindClause.
     * Provides Supplier-based bindParameter methods that can be chained after bindClause.
     * The Supplier is executed only when the preceding bindClause condition is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * bindClause 뒤에 Supplier 기반 파라미터를 바인딩하기 위한 인터페이스입니다.
     * 직전의 bindClause 조건이 참일 때만 Supplier가 실행됩니다.
     */
    public interface SupplierBindStep<T> extends Executor<T> {
        /**
         * Binds a parameter using a Supplier, which is executed only if the preceding bindClause condition is true.
         *
         * @param parameterName          Parameter name | 파라미터 이름
         * @param parameterValueSupplier Supplier providing the parameter value | 파라미터 값을 제공하는 함수
         * @param likeMode               LIKE mode for wildcard handling | LIKE 모드
         * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
         */
        SupplierBindStep<T> bindParameter(String parameterName, Supplier<Object> parameterValueSupplier, LikeMode likeMode);

        /**
         * Binds a parameter using a Supplier, which is executed only if the preceding bindClause condition is true.
         *
         * @param parameterName          Parameter name | 파라미터 이름
         * @param parameterValueSupplier Supplier providing the parameter value | 파라미터 값을 제공하는 함수
         * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
         */
        SupplierBindStep<T> bindParameter(String parameterName, Supplier<Object> parameterValueSupplier);
    }

    private static final String ORDER_BY_PREFIX = "ORDER BY ";

    private final EntityManager entityManager;
    private final Class<T> resultClass;
    private final Map<String, Object> boundParameters;
    private boolean lastBindClauseCondition = true;
    private int offset = -1;
    private int limit = -1;

    private S2Jpql(String sql, EntityManager entityManager, Class<T> resultClass) {
        super(sql);
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager must not be null");
        this.resultClass = Objects.requireNonNull(resultClass, "Result class must not be null");
        this.boundParameters = new HashMap<>();
    }

    /**
     * Starts the fluent API chain by specifying the EntityManager.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * EntityManager를 지정하여 Fluent API 체인을 시작합니다.
     *
     * @param entityManager The JPA EntityManager | JPA 엔티티 매니저
     * @return TypeStep for specifying the entity type | 엔티티 타입을 지정하기 위한 TypeStep
     */
    public static TypeStep from(EntityManager entityManager) {
        return new TypeStepImpl(entityManager);
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause}, {@code prefix}, and {@code suffix} parameters must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause}, {@code prefix}, {@code suffix} 파라미터는 반드시 하드코딩된 문자열만 사용해야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before adding the clause | 절 추가 전 확인할 조건
     * @param clause    Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix    String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix    String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, boolean condition, String clause, String prefix, String suffix) {
        super.bindWhen(key, condition, clause, prefix, suffix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause with a prefix and suffix only if the {@code condition} is true.
     * <p>
     * The {@code clauseSupplier.get()} is executed only when the condition is true,
     * providing a safe way to wrap content with both prefix and suffix without NullPointerExceptions.
     * </p>
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The string returned by {@code clauseSupplier}, as well as the {@code prefix} and {@code suffix}, must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 조건({@code condition})이 true일 때만 접두사 및 접미사와 함께 해당 조건절을 쿼리에 추가합니다.
     * <p>
     * 조건이 true일 경우에만 {@code clauseSupplier.get()}을 실행하여 접두사({@code prefix}) 및 접미사({@code suffix})와 결합하므로,
     * 복잡한 절을 생성하는 과정에서 발생할 수 있는 NullPointerException을 안전하게 차단합니다.
     * </p>
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clauseSupplier}가 반환하는 문자열과 {@code prefix}, {@code suffix}는 반드시 하드코딩된 값이어야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 직접 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before adding the clause | 절 추가 여부를 결정하는 논리 조건
     * @param clauseSupplier Supplier providing the query clause (e.g., () -> "m.id IN (:ids)") | 추가될 쿼리 절을 제공하는 함수
     * @param prefix         String to prepend to the clause (e.g., "AND (") | 절 앞에 붙을 접두사 (예: "AND (")
     * @param suffix         String to append to the clause (e.g., ")") | 절 뒤에 붙을 접미사 (예: ")")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, boolean condition, Supplier<Object> clauseSupplier, String prefix, String suffix) {
        super.bindWhen(key, condition, clauseSupplier, prefix, suffix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause} and {@code prefix} parameters must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause}와 {@code prefix} 파라미터는 반드시 하드코딩된 문자열만 사용해야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before adding the clause | 절 추가 전 확인할 조건
     * @param clause    Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix    String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, boolean condition, String clause, String prefix) {
        super.bindWhen(key, condition, clause, prefix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause with a prefix only if the {@code condition} is true.
     * <p>
     * The {@code clauseSupplier.get()} is executed only when the condition is true,
     * allowing for safe concatenation of prefix and content without NullPointerExceptions.
     * </p>
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The string returned by {@code clauseSupplier} and the {@code prefix} must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 조건({@code condition})이 true일 때만 접두사와 함께 해당 조건절을 쿼리에 추가합니다.
     * <p>
     * 조건이 true일 경우에만 {@code clauseSupplier.get()}을 실행하여 접두사({@code prefix})와 결합하므로,
     * 절을 생성하는 과정에서 발생할 수 있는 NullPointerException을 안전하게 차단합니다.
     * </p>
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clauseSupplier}가 반환하는 문자열과 {@code prefix}는 반드시 하드코딩된 값이어야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 직접 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before adding the clause | 절 추가 여부를 결정하는 논리 조건
     * @param clauseSupplier Supplier providing the query clause (e.g., () -> "m.name = :name") | 추가될 쿼리 절을 제공하는 함수
     * @param prefix         String to prepend to the clause (e.g., "AND ") | 절 앞에 붙을 접두사 (예: "AND ")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, boolean condition, Supplier<Object> clauseSupplier, String prefix) {
        super.bindWhen(key, condition, clauseSupplier, prefix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause} parameter must be a hardcoded string.
     * Never include external variables in the clause to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause} 파라미터는 반드시 하드코딩된 문자열만 사용해야 합니다.
     * SQL 인젝션을 방지하기 위해 절에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before adding the clause | 절 추가 전 확인할 조건
     * @param clause    Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, boolean condition, String clause) {
        super.bindWhen(key, condition, clause);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the {@code condition} is true.
     * <p>
     * The {@code clauseSupplier.get()} is executed only when the condition is true,
     * ensuring safety from NullPointerExceptions when deriving the clause from potentially null objects.
     * </p>
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The string returned by {@code clauseSupplier} must be a hardcoded string.
     * Never include external variables in the clause to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 조건({@code condition})이 true일 때만 Supplier를 통해 해당 조건절을 쿼리에 추가합니다.
     * <p>
     * 조건이 true일 때만 {@code clauseSupplier.get()}이 실행되므로, 절을 생성하는 과정에서
     * 참조하는 객체가 null인 상황에서도 안전하게 동작하며 불필요한 연산을 방지합니다.
     * </p>
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clauseSupplier}가 반환하는 문자열은 반드시 하드코딩된 값이어야 합니다.
     * SQL 인젝션을 방지하기 위해 절에 외부 변수를 직접 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before adding the clause | 절 추가 여부를 결정하는 논리 조건
     * @param clauseSupplier Supplier providing the query clause (e.g., () -> "AND m.name = :name") | 추가될 쿼리 절을 제공하는 함수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, boolean condition, Supplier<Object> clauseSupplier) {
        super.bindWhen(key, condition, clauseSupplier);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition value ({@code conditionValue}) is not empty/null.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause}, {@code prefix}, and {@code suffix} parameters must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건 값({@code conditionValue})이 있을 때만 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause}, {@code prefix}, {@code suffix} 파라미터는 반드시 하드코딩된 문자열만 사용해야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param conditionValue Criteria value used to determine if the clause should be added | 절 추가 여부를 판단하는 기준 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix         String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, Object conditionValue, String clause, String prefix, String suffix) {
        boolean condition = isConditionValid(conditionValue);
        super.bindWhen(key, condition, clause, prefix, suffix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause with a prefix and suffix only if the condition value ({@code conditionValue}) is not empty/null.
     * <p>
     * The {@code clauseSupplier.get()} is executed only when {@code conditionValue} is valid,
     * providing a safe way to wrap content with both prefix and suffix without NullPointerExceptions.
     * </p>
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The string returned by {@code clauseSupplier}, as well as the {@code prefix} and {@code suffix}, must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 조건 값({@code conditionValue})이 유효할 때만 접두사 및 접미사와 함께 해당 조건절을 쿼리에 추가합니다.
     * <p>
     * {@code conditionValue}가 유효할 때만 {@code clauseSupplier.get()}을 실행하므로,
     * 불필요한 연산을 방지하고 데이터 추출 시 발생할 수 있는 NullPointerException을 안전하게 차단합니다.
     * </p>
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clauseSupplier}가 반환하는 문자열과 {@code prefix}, {@code suffix}는 반드시 하드코딩된 값이어야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 직접 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param conditionValue Criteria value used to determine if the clause should be added | 절 추가 여부를 판단하는 기준 값
     * @param clauseSupplier Supplier providing the query clause (e.g., () -> "m.id IN (:ids)") | 추가될 쿼리 절을 제공하는 함수
     * @param prefix         String to prepend to the clause (e.g., "AND (") | 절 앞에 붙을 접두사 (예: "AND (")
     * @param suffix         String to append to the clause (e.g., ")") | 절 뒤에 붙을 접미사 (예: ")")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, Object conditionValue, Supplier<Object> clauseSupplier, String prefix, String suffix) {
        boolean condition = isConditionValid(conditionValue);
        super.bindWhen(key, condition, clauseSupplier, prefix, suffix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition value ({@code conditionValue}) is not empty/null.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause} and {@code prefix} parameters must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건 값({@code conditionValue})이 있을 때만 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause}와 {@code prefix} 파라미터는 반드시 하드코딩된 문자열만 사용해야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param conditionValue Criteria value used to determine if the clause should be added | 절 추가 여부를 판단하는 기준 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, Object conditionValue, String clause, String prefix) {
        boolean condition = isConditionValid(conditionValue);
        super.bindWhen(key, condition, clause, prefix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause with a prefix only if the condition value ({@code conditionValue}) is not empty/null.
     * <p>
     * The {@code clauseSupplier.get()} is executed only when {@code conditionValue} is valid,
     * which prevents potential NullPointerExceptions and unnecessary string concatenation.
     * </p>
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The string returned by {@code clauseSupplier} and the {@code prefix} must be hardcoded strings.
     * Never include external variables in these parameters to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 조건 값({@code conditionValue})이 유효할 때만 접두사와 함께 해당 조건절을 쿼리에 추가합니다.
     * <p>
     * {@code conditionValue}가 유효할 때만 {@code clauseSupplier.get()}을 실행하므로,
     * 불필요한 연산을 방지하고 데이터 추출 시 발생할 수 있는 NullPointerException을 안전하게 차단합니다.
     * </p>
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clauseSupplier}가 반환하는 문자열과 {@code prefix}는 반드시 하드코딩된 값이어야 합니다.
     * SQL 인젝션을 방지하기 위해 이 파라미터들에 외부 변수를 직접 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param conditionValue Criteria value used to determine if the clause should be added | 절 추가 여부를 판단하는 기준 값
     * @param clauseSupplier Supplier providing the query clause (e.g., () -> "m.name = :name") | 추가될 쿼리 절을 제공하는 함수
     * @param prefix         String to prepend to the clause (e.g., "AND ") | 절 앞에 붙을 접두사 (예: "AND ")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, Object conditionValue, Supplier<Object> clauseSupplier, String prefix) {
        boolean condition = isConditionValid(conditionValue);
        super.bindWhen(key, condition, clauseSupplier, prefix);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition value ({@code conditionValue}) is not empty/null.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause} parameter must be a hardcoded string.
     * Never include external variables in the clause to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건 값({@code conditionValue})이 있을 때만 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause} 파라미터는 반드시 하드코딩된 문자열만 사용해야 합니다.
     * SQL 인젝션을 방지하기 위해 절에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param conditionValue Criteria value used to determine if the clause should be added | 절 추가 여부를 판단하는 기준 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, Object conditionValue, String clause) {
        boolean condition = isConditionValid(conditionValue);
        super.bindWhen(key, condition, clause);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Adds the corresponding condition clause to the query only if the condition value ({@code conditionValue}) is not empty/null.
     * <p>
     * The {@code clauseSupplier.get()} is executed only when {@code conditionValue} is valid,
     * preventing unnecessary string operations or NullPointerExceptions.
     * </p>
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The string returned by {@code clauseSupplier} must be a hardcoded string.
     * Never include external variables in the returned string to prevent SQL injection.
     * Use {@link #bindParameter(String, Object)} for dynamic values.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * 조건 값({@code conditionValue})이 유효할 때만 Supplier를 통해 해당 조건절을 쿼리에 추가합니다.
     * <p>
     * 조건이 유효할 때만 {@code clauseSupplier.get()}이 실행되므로, 불필요한 문자열 연산을 방지하고
     * 참조 객체가 null인 상황에서도 안전하게 동작합니다.
     * </p>
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clauseSupplier}가 반환하는 문자열은 반드시 하드코딩된 값이어야 합니다.
     * SQL 인젝션을 방지하기 위해 외부 변수를 직접 포함하지 마세요.
     * 동적 값은 {@link #bindParameter(String, Object)}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param conditionValue Criteria value used to determine if the clause should be added | 절 추가 여부를 판단하는 기준 값
     * @param clauseSupplier Supplier providing the query clause (e.g., () -> "AND m.name = :name") | 추가될 쿼리 절을 제공하는 함수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindClause(String key, Object conditionValue, Supplier<Object> clauseSupplier) {
        boolean condition = isConditionValid(conditionValue);
        super.bindWhen(key, condition, clauseSupplier);
        this.lastBindClauseCondition = condition;
        return this;
    }

    /**
     * Sets the parameter with the specified name and value, applying the LIKE mode for wildcard handling if provided.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 이름과 값으로 파라미터를 설정하며, 제공된 경우 LIKE 모드를 적용하여 와일드카드를 처리합니다.
     *
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindParameter(String parameterName, Object parameterValue, LikeMode likeMode) {
        putParameter(parameterName, parameterValue, likeMode);
        return this;
    }

    /**
     * Sets the parameter with the specified name and Supplier value, applying the LIKE mode for wildcard handling if provided.
     * The Supplier is executed only if the preceding bindClause condition is true.
     *
     * <p>
     * <b>⚠️ IMPORTANT:</b> This method must be called immediately after {@link #bindClause(String, boolean, String, String)}
     * or similar bindClause methods. The Supplier is executed only when the preceding bindClause condition was true.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 직전의 bindClause 조건이 참일 때만 Supplier를 실행하여 파라미터를 설정합니다.
     * LIKE 모드를 적용하여 와일드카드를 처리합니다.
     *
     * @param parameterName          Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValueSupplier Supplier providing the parameter value | 파라미터 값을 제공하는 함수
     * @param likeMode               LIKE mode for wildcard handling | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindParameter(String parameterName, Supplier<Object> parameterValueSupplier, LikeMode likeMode) {
        if (lastBindClauseCondition && parameterValueSupplier != null) {
            putParameter(parameterName, parameterValueSupplier.get(), likeMode);
        }
        return this;
    }

    /**
     * Sets the parameter with the specified name and value.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 이름과 값으로 파라미터를 설정합니다.
     *
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindParameter(String parameterName, Object parameterValue) {
        putParameter(parameterName, parameterValue, null);
        return this;
    }

    /**
     * Sets the parameter with the specified name and Supplier value.
     * The Supplier is executed only if the preceding bindClause condition is true.
     *
     * <p>
     * <b>⚠️ IMPORTANT:</b> This method must be called immediately after {@link #bindClause(String, boolean, String, String)}
     * or similar bindClause methods. The Supplier is executed only when the preceding bindClause condition was true.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 직전의 bindClause 조건이 참일 때만 Supplier를 실행하여 파라미터를 설정합니다.
     *
     * @param parameterName          Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValueSupplier Supplier providing the parameter value | 파라미터 값을 제공하는 함수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindParameter(String parameterName, Supplier<Object> parameterValueSupplier) {
        if (lastBindClauseCondition && parameterValueSupplier != null) {
            putParameter(parameterName, parameterValueSupplier.get(), null);
        }
        return this;
    }

    /**
     * Sets the parameter with the specified name and collection values.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 이름과 컬렉션 값으로 파라미터를 설정합니다. IN 절에서 사용하기 위한 메서드입니다.
     *
     * @param parameterName   Parameter name (e.g., "ids") | 파라미터 이름 (예: "ids")
     * @param parameterValues Collection of parameter values for IN clause | IN 절에 사용될 파라미터 값들의 컬렉션
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindParameter(String parameterName, Collection<?> parameterValues) {
        putParameter(parameterName, parameterValues, null);
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
     * @param key            Template key to be replaced (e.g., "order_clause") | 치환 대상 템플릿 키 (예: "order_clause")
     * @param condition      Whether to apply the sort condition | 정렬 조건 적용 여부
     * @param sortExpression Sort string (e.g., "m.name DESC, m.age") | 정렬 문자열 (예: "m.name DESC, m.age")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindOrderBy(String key, boolean condition, String sortExpression) {
        if (!condition || sortExpression == null || sortExpression.isBlank()) {
            return this;
        }

        var sortStr = sortExpression.trim();
        if (sortStr.regionMatches(true, 0, ORDER_BY_PREFIX, 0, ORDER_BY_PREFIX.length())) {
            sortStr = sortStr.substring(ORDER_BY_PREFIX.length()).trim();
        }
        String result = java.util.Arrays.stream(sortStr.split(","))
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
        super.bindWhen(key, !result.isBlank(), result, ORDER_BY_PREFIX);
        return this;
    }

    /**
     * Parses the sort condition string from a Supplier and dynamically adds an ORDER BY clause.
     * <p>
     * When {@code condition} is {@code true}, it executes the {@code sortExpressionSupplier} to get the sort string.
     * Supports multiple sorts separated by commas (,), and validates the sort direction (ASC/DESC).
     * This lazy evaluation prevents NullPointerExceptions when deriving sort strings from potentially null objects.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Supplier 기반 지연 정렬 바인딩] 정렬 조건 문자열을 파싱하여 동적으로 ORDER BY 절을 추가합니다.
     * <p>
     * {@code condition}이 {@code true}일 때만 {@code sortExpressionSupplier.get()}을 호출하여 정렬 문자열을 가져옵니다.
     * 콤마(,)로 구분된 다중 정렬을 지원하며, 정렬 방향(ASC/DESC)의 유효성을 검사합니다.
     * 지연 평가 방식을 통해 Pageable 등 정렬 정보를 가진 객체가 null인 상황에서도 안전하게 동작합니다.
     * </p>
     *
     * @param key                    Template key to be replaced (e.g., "order_clause") | 치환 대상 템플릿 키 (예: "order_clause")
     * @param condition              Whether to apply the sort condition | 정렬 조건 적용 여부
     * @param sortExpressionSupplier Supplier providing the sort string (e.g., "m.name: DESC") | 정렬 문자열을 제공하는 함수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     * @apiNote
     *          The supplier is executed only when the condition is met, avoiding early access to potentially null references.
     *          <p>
     *          조건이 충족될 때만 supplier가 실행되므로, null일 수 있는 참조 객체에 대한 조기 접근을 방지합니다.
     *          </p>
     *
     *          <pre>{@code
     * .bindOrderBy("orderByClause", pageable != null && pageable.getSort().isSorted(),
     * () -> pageable.getSort().toString().replace(":", ""))
     * }</pre>
     */
    @Override
    public S2Jpql<T> bindOrderBy(String key, boolean condition, Supplier<String> sortExpressionSupplier) {
        return bindOrderBy(key, condition, condition && sortExpressionSupplier != null ? sortExpressionSupplier.get() : null);
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
     * @param key            Template key to be replaced (e.g., "order_clause") | 치환 대상 템플릿 키 (예: "order_clause")
     * @param sortExpression Sort string (e.g., "m.name DESC, m.age") | 정렬 문자열 (예: "m.name DESC, m.age")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> bindOrderBy(String key, String sortExpression) {
        return bindOrderBy(key, true, sortExpression);
    }

    /**
     * Sets pagination parameters conditionally.
     *
     * <p>
     * When {@code condition} is {@code true}, applies the given {@code offset} and {@code limit}
     * to the resulting query. If {@code condition} is {@code false}, pagination is not applied.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code condition}이 {@code true}인 경우에만 주어진 {@code offset}와 {@code limit}을 쿼리에 적용합니다.
     * {@code condition}이 {@code false}이면 페이지네이션은 적용되지 않습니다.
     *
     * @param condition Whether to apply pagination | 페이지네이션 적용 여부
     * @param offset    The number of rows to skip (0-based) | 건너뛸 행의 개수 (0부터 시작)
     * @param limit     The maximum number of rows to return | 반환할 최대 행 수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> limit(boolean condition, int offset, int limit) {
        if (!condition) {
            return this;
        }
        this.offset = offset;
        this.limit = limit;
        return this;
    }

    /**
     * Sets pagination parameters conditionally using Suppliers for lazy evaluation.
     * <p>
     * When {@code condition} is {@code true}, applies the values provided by
     * {@code offsetSupplier} and {@code limitSupplier}. This is particularly useful
     * for safely calling {@code pageable.getOffset()} when {@code pageable} might be null.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Supplier 기반 지연 제한 설정] 조건이 true일 때만 Supplier로부터 값을 가져와 페이징을 설정합니다.
     * <p>
     * {@code condition}이 {@code true}인 경우에만 각 Supplier의 {@code get()}을 호출하여
     * 결과 쿼리에 적용합니다. {@code pageable} 객체가 null일 수 있는 상황에서
     * NPE(NullPointerException) 없이 안전하게 페이징 정보를 추출할 때 사용합니다.
     * </p>
     *
     * @param condition      Whether to apply pagination | 페이지네이션 적용 여부
     * @param offsetSupplier Supplier for the number of rows to skip | 건너뛸 행의 개수를 제공하는 함수
     * @param limitSupplier  Supplier for the maximum number of rows to return | 반환할 최대 행 수를 제공하는 함수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     * @apiNote
     *          The suppliers are only executed if {@code condition} is true.
     *          <p>
     *          {@code condition}이 true일 때만 각 Supplier가 실행됩니다.
     *          </p>
     *
     *          <pre>{@code
     * .limit(pageable != null && pageable.isPaged(),
     * () -> (int) pageable.getOffset(),
     * () -> pageable.getPageSize())
     * }</pre>
     */
    @Override
    public S2Jpql<T> limit(boolean condition, Supplier<Integer> offsetSupplier, Supplier<Integer> limitSupplier) {
        if (!condition) {
            return this;
        }
        if (offsetSupplier != null) {
            this.offset = offsetSupplier.get();
        }
        if (limitSupplier != null) {
            this.limit = limitSupplier.get();
        }
        return this;
    }

    /**
     * Sets pagination parameters (offset and limit) for the query.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 쿼리의 페이지네이션 파라미터(offset, limit)를 설정합니다.
     *
     * @param offset The number of rows to skip (0-based) | 건너뛸 행의 개수 (0부터 시작)
     * @param limit  The maximum number of rows to return | 반환할 최대 행 수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    @Override
    public S2Jpql<T> limit(int offset, int limit) {
        return limit(true, offset, limit);
    }

    private void putParameter(String name, Object value, LikeMode likeMode) {
        Object processedValue = value;
        if (value instanceof String && likeMode != null) {
            String stringValue = (String) value;
            processedValue = switch (likeMode) {
                case ANYWHERE -> "%" + stringValue + "%";
                case START -> stringValue + "%";
                case END -> "%" + stringValue;
            };
        }
        boundParameters.put(name, processedValue);
    }

    /**
     * Validates if the condition value is not empty/null.
     * This is a helper method used internally for Object-based bindClause methods.
     *
     * @param value The condition value to check
     * @return true if the value is not empty/null, false otherwise
     */
    private boolean isConditionValid(Object value) {
        if (value == null)
            return false;
        if (value instanceof Boolean b)
            return b;
        return S2Util.isNotEmpty(value);
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
        logger.debug("Rendered JPQL: {}", renderedSql);
        TypedQuery<T> query = entityManager.createQuery(renderedSql, resultClass);

        // 2. 자동 바인딩 (S2Template의 rawValues 활용)
        logger.debug("Parameters to bind: {}", boundParameters);
        for (Parameter<?> param : query.getParameters()) {
            String name = param.getName();
            if (name != null && boundParameters.containsKey(name)) {
                Object value = boundParameters.get(name);
                logger.debug("Binding parameter: {} = {}", name, value);
                query.setParameter(name, value);
            }
        }

        // 3. 페이지네이션 적용
        if (offset >= 0) {
            query.setFirstResult(offset);
        }
        if (limit >= 0) {
            query.setMaxResults(limit);
        }

        return query;
    }

    /**
     * Implementation of TypeStep
     */
    private static class TypeStepImpl implements TypeStep {
        private final EntityManager entityManager;

        TypeStepImpl(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public <T> QueryStep<T> type(Class<T> entityClass) {
            return new QueryStepImpl<>(entityManager, entityClass);
        }
    }

    /**
     * Implementation of QueryStep
     */
    private static class QueryStepImpl<T> implements QueryStep<T> {
        private final EntityManager entityManager;
        private final Class<T> entityClass;

        QueryStepImpl(EntityManager entityManager, Class<T> entityClass) {
            this.entityManager = entityManager;
            this.entityClass = entityClass;
        }

        @Override
        public Executor<T> query(String jpql) {
            return new S2Jpql<>(jpql, entityManager, entityClass);
        }
    }

}
