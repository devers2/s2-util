package io.github.devers2.s2util.jpa;

import java.util.Collection;
import java.util.Map;

import jakarta.persistence.TypedQuery;

/**
 * Step 3: Final executor with all methods
 */
public interface Executor<T> {

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
     * @param likeMode       Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix, String suffix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition  Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix     String to append to the clause | 절 뒤에 붙을 문자열
     * @param likeMode   Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, Map<String, Object> parameters, String clause, String prefix, String suffix, LikeMode likeMode);

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
    Executor<T> applyClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix, String suffix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition  Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix     String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, Map<String, Object> parameters, String clause, String prefix, String suffix);

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
    Executor<T> applyClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition  Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @param likeMode   Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, Map<String, Object> parameters, String clause, String prefix, LikeMode likeMode);

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
    Executor<T> applyClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, String prefix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition  Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, Map<String, Object> parameters, String clause, String prefix);

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
    Executor<T> applyClause(String key, boolean condition, String parameterName, Object parameterValue, String clause, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition  Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param likeMode   Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, Map<String, Object> parameters, String clause, LikeMode likeMode);

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
    Executor<T> applyClause(String key, boolean condition, String parameterName, Object parameterValue, String clause);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the condition ({@code condition}) is true.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조건({@code condition})이 true 일때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param condition  Condition to check before setting the parameter | 파라미터 설정 전 확인할 조건
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, boolean condition, Map<String, Object> parameters, String clause);

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
    Executor<T> applyClause(String key, String parameterName, Object parameterValue, String clause, String prefix, String suffix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix     String to append to the clause | 절 뒤에 붙을 문자열
     * @param likeMode   Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, Map<String, Object> parameters, String clause, String prefix, String suffix, LikeMode likeMode);

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
    Executor<T> applyClause(String key, String parameterName, Object parameterValue, String clause, String prefix, String suffix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @param suffix     String to append to the clause | 절 뒤에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, Map<String, Object> parameters, String clause, String prefix, String suffix);

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
    Executor<T> applyClause(String key, String parameterName, Object parameterValue, String clause, String prefix, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @param likeMode   Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, Map<String, Object> parameters, String clause, String prefix, LikeMode likeMode);

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
    Executor<T> applyClause(String key, String parameterName, Object parameterValue, String clause, String prefix);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param prefix     String to prepend to the clause | 절 앞에 붙을 문자열
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, Map<String, Object> parameters, String clause, String prefix);

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
    Executor<T> applyClause(String key, String parameterName, Object parameterValue, String clause, LikeMode likeMode);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @param likeMode   Mode determining wildcard (%) placement for LIKE searches | LIKE 검색 시 와일드카드(%) 위치 결정 모드
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, Map<String, Object> parameters, String clause, LikeMode likeMode);

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
    Executor<T> applyClause(String key, String parameterName, Object parameterValue, String clause);

    /**
     * Sets the parameter and adds the corresponding condition clause to the query only if the parameter value ({@code value}) is present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파라미터 값({@code value})이 있을때만 파라미터를 설정하고 해당 조건절을 쿼리에 추가합니다.
     *
     * @param key        Template key to be replaced (e.g., "where_clause") | 치환 대상 템플릿 키 (예: "where_clause")
     * @param parameters Map of parameter names and their values | 파라미터 이름과 값의 쌍을 담은 맵
     * @param clause     Query clause to be added (e.g., "AND m.name = :name") | 추가될 쿼리 절 (예: "AND m.name = :name")
     * @return Current object for method chaining | 메서드 체이닝을 위한 현재 객체
     */
    Executor<T> applyClause(String key, Map<String, Object> parameters, String clause);

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
    Executor<T> applyOrderBy(String key, boolean condition, String sortExpression);

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
    Executor<T> applyOrderBy(String key, String sortExpression);

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
    Executor<T> bind(String key, Object value, String prefix, String suffix);

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
    Executor<T> bind(String key, Object value, String prefix);

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
    Executor<T> bind(String key, Object value);

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
     * @param content   Actual content to inject when value is valid | 값이 유효할 때 주입할 실제 내용
     * @param prefix    Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @param suffix    Suffix to append when content is injected | 내용 주입 시 뒤에 붙일 접미사
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Mainly used for inserting sentences based on sorting conditions or true/false of specific business logic.
     *
     *          <pre>{@code
     * .bindWhen("order", pageable.isSorted(), "m.id", "ORDER BY ", " DESC")
     * // Result: "ORDER BY m.id DESC" if pageable.isSorted() is true
     * }</pre>
     */
    Executor<T> bindWhen(String key, boolean condition, String content, String prefix, String suffix);

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
     * @param content   Actual content to inject when value is valid | 값이 유효할 때 주입할 실제 내용
     * @param prefix    Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Mainly used for inserting sentences based on sorting conditions or true/false of specific business logic.
     *
     *          <pre>{@code
     * .bindWhen("order", pageable.isSorted(), "m.id DESC", "ORDER BY ")
     * // Result: "ORDER BY m.id DESC" if pageable.isSorted() is true
     * }</pre>
     */
    Executor<T> bindWhen(String key, boolean condition, String content, String prefix);

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
     * @param content   Actual content to inject when value is valid | 값이 유효할 때 주입할 실제 내용
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          Mainly used for inserting sentences based on sorting conditions or true/false of specific business logic.
     *
     *          <pre>{@code
     * .bindWhen("order", pageable.isSorted(), "m.id DESC")
     * // Result: "m.id DESC" if pageable.isSorted() is true
     * }</pre>
     */
    Executor<T> bindWhen(String key, boolean condition, String content);

    /**
     * [Value-based binding] Injects specified content when the value exists and is valid.
     * <p>
     * Uses the validity of the value (not null, not empty) as a trigger.
     * When valid, combines prefix and content to replace the template key.
     * Useful for conditionally inserting JPQL clauses like 'AND' or 'ORDER BY' in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Value 기반 바인딩] 값({@code value})이 유효할 때 지정된 내용({@code content})을 주입합니다.
     * <p>
     * 이 메서드는 {@code value}의 유효성(null 아님, 비어 있지 않음)을 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 구문 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key     Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param value   Reference value or Boolean condition to check validity | 유효성을 검사할 기준 값 혹은 Boolean 조건
     * @param content Actual content to inject when value is valid | 값이 유효할 때 주입할 실제 내용
     * @param prefix  Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @param suffix  Suffix to append when content is injected | 내용 주입 시 뒤에 붙일 접미사
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          {@code value} acts only as a trigger for existence judgment, actual replacement is done with {@code content}.
     *
     *          <pre>{@code
     * .bindWhen("stage", stage, ":stage", "AND s.stage IN (", ")")
     * // Result: "AND s.stage IN (:stage)" if stage exists
     * }</pre>
     */
    Executor<T> bindWhen(String key, Object value, String content, String prefix, String suffix);

    /**
     * [Value-based binding] Injects specified content when the reference value exists.
     * <p>
     * Uses the validity of the value (not null, not empty) as a trigger.
     * When valid, combines the prefix and content to replace the template key.
     * Useful for conditionally inserting JPQL clauses like 'AND' or 'ORDER BY' in dynamic queries.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Value 기반 바인딩] 값({@code value})이 유효할 때 지정된 내용({@code content})을 주입합니다.
     * <p>
     * 이 메서드는 {@code value}의 유효성(null 아님, 비어 있지 않음)을 트리거로 사용합니다.
     * 유효할 경우, {@code prefix}와 {@code content}를 결합하여 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 {@code AND}, {@code ORDER BY} 절과 같은 구문 자체를 조건부로 삽입할 때 유용합니다.
     * </p>
     *
     * @param key     Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param value   Reference value to check validity (not null/blank) | 유효성을 검사할 기준 값
     * @param prefix  Prefix to prepend when content is injected (e.g., "ORDER BY ", "AND ") | 내용 주입 시 앞에 붙일 접두사 (예: "ORDER BY ", "AND ")
     * @param content Actual JPQL content to inject when value is valid | 값이 유효할 때 주입할 실제 내용
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          {@code value} acts only as a trigger for existence judgment, actual replacement is done with {@code content}.
     *
     *          <pre>{@code
     * .bindWhen("stage", stage, "s.stage = :stage", "AND ")
     * // Result: "AND s.stage = :stage" if stage exists
     * }</pre>
     */
    Executor<T> bindWhen(String key, Object value, String content, String prefix);

    /**
     * [Value-based binding] Injects specified content only when the provided value is valid.
     * <p>
     * Uses the validity of the value (not null, not empty) as a trigger.
     * When valid, replaces the template key with the specified content.
     * Useful for conditionally inserting JPQL clauses like 'AND' or 'ORDER BY'
     * based on the existence of a specific data value.
     * </p>
     *
     * <p>
     * [한국어 설명]
     * </p>
     * [Value 기반 바인딩] 값({@code value})이 유효할 때 지정된 내용({@code content})을 주입합니다.
     * <p>
     * 이 메서드는 {@code value}의 유효성(null 아님, 비어 있지 않음)을 트리거로 사용합니다.
     * 유효할 경우, {@code content}로 템플릿의 키를 치환합니다.
     * 주로 동적 쿼리에서 파라미터 값의 존재 여부에 따라 구문 자체를 삽입할 때 유용합니다.
     * </p>
     *
     * @param key     Template key to be replaced (e.g., "where_clause") | 템플릿 내의 치환 대상 키 (예: "where_clause")
     * @param value   Reference object to check validity (ignored if null/blank) | 유효성을 검사할 기준 객체 (null/blank 시 무시)
     * @param content Actual content to inject when value is valid | 값이 유효할 때 주입할 실제 내용
     * @return Current instance for method chaining | 메서드 체이닝을 위한 현재 인스턴스
     * @apiNote
     *          {@code value} acts only as a trigger for existence judgment, actual replacement is done with {@code content}.
     *
     *          <pre>{@code
     * .bindWhen("stage", stage, "s.stage = :stage")
     * // Result: "s.stage = :stage" if stage exists
     * }</pre>
     */
    Executor<T> bindWhen(String key, Object value, String content);

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
    Executor<T> bindIn(String key, Collection<?> values, String prefix, String suffix);

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
    Executor<T> bindIn(String key, Collection<?> values, String prefix);

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
    Executor<T> bindIn(String key, Collection<?> values);

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
    Executor<T> bindIn(String key, boolean condition, Collection<?> values, String prefix, String suffix);

    TypedQuery<T> build();

}
