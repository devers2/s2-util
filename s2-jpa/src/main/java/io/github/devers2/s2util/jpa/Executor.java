package io.github.devers2.s2util.jpa;

import jakarta.persistence.TypedQuery;

/**
 * Step 3: Final executor with all methods
 */
public interface Executor<T> {

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>⚠️ SECURITY WARNING:</b> The {@code clause} parameter must be a hardcoded string. Never include external variables in the clause to prevent SQL injection.
     * Use {@code parameterValue} for dynamic values, which are safely bound as parameters.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * <p>
     * <b>⚠️ 보안 경고:</b> {@code clause} 파라미터는 하드코딩된 문자열만 사용해야 합니다. SQL 인젝션을 방지하기 위해 절에 외부 변수를 포함하지 마세요.
     * 동적 값은 {@code parameterValue}를 통해 안전하게 파라미터로 바인딩하세요.
     * </p>
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix         String to append to the clause | 절 뒤에 붙을 문자열
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix, String suffix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix         String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix, String suffix);

    /**
     * Adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param clause    Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix    String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix    String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String clause, String prefix, String suffix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix);

    /**
     * Adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param clause    Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix    String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String clause, String prefix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition      Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String parameterName, Object parameterValue, String clause);

    /**
     * Adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key       Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param clause    Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, boolean condition, String clause);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix         String to append to the clause | 절 뒤에 붙을 문자열
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, String parameterName, Object parameterValue, String clause, String prefix, String suffix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix         String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, String parameterName, Object parameterValue, String clause, String prefix, String suffix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, String parameterName, Object parameterValue, String clause, String prefix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix         String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, String parameterName, Object parameterValue, String clause, String prefix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, String parameterName, Object parameterValue, String clause, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key            Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameterName  Parameter name (e.g., "name") | 파라미터 이름 (예: "name")
     * @param parameterValue Parameter value | 파라미터 값
     * @param clause         Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> bindClause(String key, String parameterName, Object parameterValue, String clause);

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

    TypedQuery<T> build();

}
