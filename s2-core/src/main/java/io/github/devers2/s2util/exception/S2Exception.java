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
package io.github.devers2.s2util.exception;

import io.github.devers2.s2util.core.S2StringUtil;

/**
 * Base checked exception for the S2Util library.
 * <p>
 * This class serves as the root of the checked exception hierarchy in S2Util.
 * It provides utility methods for extracting and formatting stack traces,
 * including byte-aware truncation for storage constraints.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 기본 체크 예외(Checked Exception) 클래스입니다.
 * <p>
 * 라이브러리 내부에서 발생하는 주요 예외 상황의 최상위 클래스 역할을 수행하며,
 * 디버깅 및 로깅을 위해 스택 트레이스를 문자열로 추출하거나 저장 공간 제약에 맞춰
 * 바이트 단위로 절삭하는 유틸리티 기능을 포함합니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2Exception extends Exception {

    private static final long serialVersionUID = 3451115851098577666L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 상세 오류 메시지를 사용하여 새로운 예외를 생성합니다.
     *
     * @param defaultMessage The detail message | 상세 오류 메시지
     */
    public S2Exception(String defaultMessage) {
        super(defaultMessage);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 상세 오류 메시지와 원인이 되는 예외를 사용하여 새로운 예외를 생성합니다.
     *
     * @param defaultMessage The detail message (format string supported) | 상세 오류 메시지 (포맷 문자열 지원)
     * @param cause          The cause of the exception | 원인 예외
     */
    public S2Exception(String defaultMessage, Throwable cause) {
        super(defaultMessage, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원인이 되는 예외를 사용하여 새로운 예외를 생성합니다.
     *
     * @param cause The cause of the exception | 원인 예외
     */
    public S2Exception(Throwable cause) {
        super(cause);
    }

    /**
     * Converts a {@link Throwable}'s stack trace into a string.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 예외(Throwable)의 전체 스택 트레이스(Stack Trace)를 문자열로 변환합니다.
     *
     * @param e The exception to process | 처리할 예외 객체
     * @return The full stack trace string, or an empty string if {@code e} is null | 전체 스택 트레이스 문자열 (null인 경우 빈 문자열)
     */
    public static String getStackTrace(Throwable e) {
        return getStackTrace(e, null);
    }

    /**
     * Converts a {@link Throwable}'s stack trace into a string, truncated by byte length.
     * <p>
     * This is particularly useful for logging stack traces into database columns
     * with fixed byte-size limits. It uses {@link java.io.StringWriter} for
     * memory-efficient string generation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 예외(Throwable)의 스택 트레이스를 지정된 바이트 길이만큼 잘라서 문자열로 반환합니다.
     * <p>
     * DB 컬럼 크기 제한이 있는 환경에서 로그를 저장할 때 유용하며, {@link java.io.StringWriter}를
     * 사용하여 불필요한 IO 부하 없이 효율적으로 텍스트를 생성합니다.
     * </p>
     *
     * @param e       The exception to process | 처리할 예외 객체
     * @param maxByte The maximum byte length for the result. If null, the full trace is returned. | 결과의 최대 바이트 길이 (null인 경우 전체 반환)
     * @return The truncated stack trace string | 절삭된 스택 트레이스 문자열
     */
    public static String getStackTrace(Throwable e, Integer maxByte) {
        if (e == null)
            return "";

        var sw = new java.io.StringWriter();
        var pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw); // 원인(Cause)과 억제된 예외까지 모두 출력해줌

        var stackTrace = sw.toString();
        return maxByte != null ? S2StringUtil.substringByBytes(stackTrace, 0, maxByte) : stackTrace;
    }

}
