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
package io.github.devers2.s2util.log;

/**
 * Core logging interface for the S2Util library.
 * <p>
 * This interface defines a standard way to record messages across different
 * levels (DEBUG, INFO, WARN, ERROR). It supports SLF4J-style message formatting
 * using the {@code "{}"} placeholder and handles variable arguments efficiently.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 핵심 로깅 인터페이스입니다.
 * <p>
 * 로그 레벨(DEBUG, INFO, WARN, ERROR)별 기록 방식을 정의하며, SLF4J 스타일의 플레이스홀더({@code "{}"})를
 * 통한 메시지 치환을 지원합니다. 가변 인자 처리를 최적화하여 성능 저하 없이 직관적인 로깅 API를 제공합니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see S2LogManager
 */
public interface S2Logger {

    // ========================================================
    // 핵심 구현 메서드 (구현체에서 반드시 구현해야 함)
    // ========================================================

    /**
     * Primary entry point for recording a log entry.
     * <p>
     * All level-specific default methods (e.g., {@link #info(String)}) eventually
     * route their requests through this method. Implementations should handle
     * placeholder substitution and actual output delivery.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 로그 기록을 위한 통합 진입 메서드입니다.
     * <p>
     * {@link #info(String)} 등 모든 레벨별 단축 메서드들은 최종적으로 이 메서드를 호출합니다.
     * 실제 구현체는 이 메서드에서 플레이스홀더 치환과 실제 출력을 담당해야 합니다.
     * </p>
     *
     * @param level   Log level (DEBUG, INFO, WARN, ERROR) | 로그 레벨 (DEBUG, INFO, WARN, ERROR)
     * @param message Message template with optional "{}" placeholders | "{}" 플레이스홀더를 포함할 수 있는 메시지 템플릿
     * @param args    Arguments to substitute into the placeholders | 플레이스홀더에 치환될 인자 목록
     */
    void log(String level, String message, Object[] args);

    // ========================================================
    // Level-specific Default Methods (MethodKey 패턴 적용)
    // 인자 개수별로 명시적으로 오버로딩하여 타입 추론의 모호함을 제거한다.
    // ========================================================

    // --- DEBUG ---

    default void debug(String message) {
        if (isDebugEnabled())
            log("DEBUG", message, combine());
    }

    default void debug(String message, Object arg) {
        if (isDebugEnabled())
            log("DEBUG", message, combine(arg));
    }

    default void debug(String message, Object arg1, Object arg2) {
        if (isDebugEnabled())
            log("DEBUG", message, combine(arg1, arg2));
    }

    default void debug(String message, Object arg1, Object arg2, Object... rest) {
        if (isDebugEnabled())
            log("DEBUG", message, combine(arg1, arg2, rest));
    }

    // --- INFO ---

    default void info(String message) {
        if (isInfoEnabled())
            log("INFO", message, combine());
    }

    default void info(String message, Object arg) {
        if (isInfoEnabled())
            log("INFO", message, combine(arg));
    }

    default void info(String message, Object arg1, Object arg2) {
        if (isInfoEnabled())
            log("INFO", message, combine(arg1, arg2));
    }

    default void info(String message, Object arg1, Object arg2, Object... rest) {
        if (isInfoEnabled())
            log("INFO", message, combine(arg1, arg2, rest));
    }

    // --- WARN ---

    default void warn(String message) {
        if (isWarnEnabled())
            log("WARN", message, combine());
    }

    default void warn(String message, Object arg) {
        if (isWarnEnabled())
            log("WARN", message, combine(arg));
    }

    default void warn(String message, Object arg1, Object arg2) {
        if (isWarnEnabled())
            log("WARN", message, combine(arg1, arg2));
    }

    default void warn(String message, Object arg1, Object arg2, Object... rest) {
        if (isWarnEnabled())
            log("WARN", message, combine(arg1, arg2, rest));
    }

    // --- ERROR ---

    default void error(String message) {
        if (isErrorEnabled())
            log("ERROR", message, combine());
    }

    default void error(String message, Object arg) {
        if (isErrorEnabled())
            log("ERROR", message, combine(arg));
    }

    default void error(String message, Object arg1, Object arg2) {
        if (isErrorEnabled())
            log("ERROR", message, combine(arg1, arg2));
    }

    default void error(String message, Object arg1, Object arg2, Object... rest) {
        if (isErrorEnabled())
            log("ERROR", message, combine(arg1, arg2, rest));
    }

    // ========================================================
    // 유틸리티 메서드
    // ========================================================

    /**
     * Consolidates arguments into a single object array.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 전달된 인자들을 하나의 객체 배열로 병합합니다.
     */
    static Object[] combine() {
        return new Object[0];
    }

    static Object[] combine(Object arg) {
        return new Object[] { arg };
    }

    static Object[] combine(Object arg1, Object arg2) {
        return new Object[] { arg1, arg2 };
    }

    static Object[] combine(Object arg1, Object arg2, Object... rest) {
        if (rest == null || rest.length == 0) {
            return new Object[] { arg1, arg2 };
        }
        Object[] args = new Object[2 + rest.length];
        args[0] = arg1;
        args[1] = arg2;
        System.arraycopy(rest, 0, args, 2, rest.length);
        return args;
    }

    // ========================================================
    // 레벨 체크용 기본 구현 (하위 호환성 유지)
    // ========================================================

    /**
     * Checks if the DEBUG level is enabled.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * DEBUG 레벨이 활성화되어 있는지 확인합니다.
     *
     * @return {@code true} if enabled | 활성화된 경우 true
     */
    default boolean isDebugEnabled() {
        return true;
    }

    /**
     * Checks if the INFO level is enabled.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * INFO 레벨이 활성화되어 있는지 확인합니다.
     *
     * @return {@code true} if enabled | 활성화된 경우 true
     */
    default boolean isInfoEnabled() {
        return true;
    }

    /**
     * Checks if the WARN level is enabled.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * WARN 레벨이 활성화되어 있는지 확인합니다.
     *
     * @return {@code true} if enabled | 활성화된 경우 true
     */
    default boolean isWarnEnabled() {
        return true;
    }

    /**
     * Checks if the ERROR level is enabled.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * ERROR 레벨이 활성화되어 있는지 확인합니다.
     *
     * @return {@code true} if enabled | 활성화된 경우 true
     */
    default boolean isErrorEnabled() {
        return true;
    }

}
