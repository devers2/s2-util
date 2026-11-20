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

import io.github.devers2.s2util.message.S2Localizable;
import io.github.devers2.s2util.message.S2MessageKey;
/**
 * Base runtime (unchecked) exception for the S2Util library.
 * <p>
 * This exception is thrown for unrecoverable errors, such as configuration
 * issues, internal illegal states, or critical failures where the caller is
 * not expected to handle the exception explicitly.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 기본 런타임 예외(Unchecked Exception) 클래스입니다.
 * <p>
 * 명백한 프로그래밍 오류, 복구 불가능한 시스템 장애, 또는 호출 측에서 별도의 예외 처리가
 * 필요하지 않은 런타임 상황에서 발생합니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2RuntimeException extends RuntimeException implements S2Localizable {

    private static final long serialVersionUID = 3451115851098577666L;

    /* 사용자에게 보여 줄 문구의 키 (없으면 null). getMessage() 는 영어 그대로 둔다 */
    private final S2MessageKey messageKey;

    /**
     * Constructs a new runtime exception with the specified detail message.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 상세 오류 메시지를 사용하여 새로운 런타임 예외를 생성합니다.
     *
     * @param errorMessage The detail message | 상세 오류 메시지
     */
    public S2RuntimeException(String errorMessage) {
        super(errorMessage);
        this.messageKey = null;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and cause.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 상세 오류 메시지와 원인이 되는 예외를 사용하여 새로운 런타임 예외를 생성합니다.
     *
     * @param errorMessage The detail message | 상세 오류 메시지
     * @param cause        The cause of the exception | 원인 예외
     */
    public S2RuntimeException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.messageKey = null;
    }

    /**
     * Constructs a new runtime exception with the specified cause.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원인이 되는 예외를 사용하여 새로운 런타임 예외를 생성합니다.
     *
     * @param cause The cause of the exception | 원인 예외
     */
    public S2RuntimeException(Throwable cause) {
        super(cause);
        this.messageKey = null;
    }


    /**
     * Constructs an exception with an English message for logs and a message key for the user's language.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 로그용 영어 메시지와 사용자 언어용 메시지 키로 예외를 생성합니다.
     * @param errorMessage English message for logs | 로그용 영어 메시지
     * @param messageKey Key of the message shown to users ({@link S2Localizable#getLocalizedMessage(java.util.Locale)}) |
     *                   사용자에게 보여 줄 문구의 키
     */
    public S2RuntimeException(String errorMessage, S2MessageKey messageKey) {
        super(errorMessage);
        this.messageKey = messageKey;
    }

    /**
     * Constructs an exception with an English message for logs, a cause and a message key for the user's language.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 로그용 영어 메시지, 원인, 사용자 언어용 메시지 키로 예외를 생성합니다.
     * @param errorMessage English message for logs | 로그용 영어 메시지
     * @param cause        The cause of the exception | 원인 예외
     * @param messageKey   Key of the message shown to users | 사용자에게 보여 줄 문구의 키
     */
    public S2RuntimeException(String errorMessage, Throwable cause, S2MessageKey messageKey) {
        super(errorMessage, cause);
        this.messageKey = messageKey;
    }

    /**
     * Key of the message shown to users, or {@code null}.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 사용자에게 보여 줄 문구의 키. 없으면 {@code null}.
     * @return Message key | 메시지 키
     */
    @Override
    public S2MessageKey getMessageKey() {
        return messageKey;
    }

}
