package io.github.devers2.s2util.jpa;

import java.util.Collection;
import java.util.function.Supplier;

import jakarta.persistence.TypedQuery;

/**
 * Step 3: Final executor with all methods
 */
public interface Executor<T> {

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
    Executor<T> bindClause(String key, boolean condition, String clause, String prefix, String suffix);

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
    Executor<T> bindClause(String key, boolean condition, Supplier<Object> clauseSupplier, String prefix, String suffix);

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
    Executor<T> bindClause(String key, boolean condition, String clause, String prefix);

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
    Executor<T> bindClause(String key, boolean condition, Supplier<Object> clauseSupplier, String prefix);

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
    Executor<T> bindClause(String key, boolean condition, String clause);

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
    Executor<T> bindClause(String key, boolean condition, Supplier<Object> clauseSupplier);

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
    Executor<T> bindClause(String key, Object conditionValue, String clause, String prefix, String suffix);

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
    Executor<T> bindClause(String key, Object conditionValue, Supplier<Object> clauseSupplier, String prefix, String suffix);

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
    Executor<T> bindClause(String key, Object conditionValue, String clause, String prefix);

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
    Executor<T> bindClause(String key, Object conditionValue, Supplier<Object> clauseSupplier, String prefix);

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
    Executor<T> bindClause(String key, Object conditionValue, String clause);

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
    Executor<T> bindClause(String key, Object conditionValue, Supplier<Object> clauseSupplier);

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
    Executor<T> bindParameter(String parameterName, Object parameterValue, LikeMode likeMode);

    Executor<T> bindParameter(String parameterName, Supplier<Object> parameterValueSupplier, LikeMode likeMode);

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
    Executor<T> bindParameter(String parameterName, Object parameterValue);

    Executor<T> bindParameter(String parameterName, Supplier<Object> parameterValueSupplier);

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
    Executor<T> bindParameter(String parameterName, Collection<?> parameterValues);

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
    Executor<T> bindOrderBy(String key, boolean condition, String sortExpression);

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
    Executor<T> bindOrderBy(String key, boolean condition, Supplier<String> sortExpressionSupplier);

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
    Executor<T> bindOrderBy(String key, String sortExpression);

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
    Executor<T> limit(boolean condition, int offset, int limit);

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
    Executor<T> limit(boolean condition, Supplier<Integer> offsetSupplier, Supplier<Integer> limitSupplier);

    /**
     * Sets pagination parameters for the query.
     *
     * <p>
     * Applies OFFSET and LIMIT to the resulting query. The offset parameter determines the starting row,
     * and the limit parameter determines the maximum number of rows to return.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 쿼리에 페이지네이션 파라미터를 설정합니다.
     * offset 파라미터는 시작 행을 결정하고, limit 파라미터는 반환할 최대 행 수를 결정합니다.
     *
     * @param offset The number of rows to skip (0-based) | 건너뛸 행의 개수 (0부터 시작)
     * @param limit  The maximum number of rows to return | 반환할 최대 행 수
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> limit(int offset, int limit);

    TypedQuery<T> build();

}
