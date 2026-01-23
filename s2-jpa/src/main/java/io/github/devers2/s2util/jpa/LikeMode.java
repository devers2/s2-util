package io.github.devers2.s2util.jpa;

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
