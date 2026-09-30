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
package io.github.devers2.s2util.json;

import io.github.devers2.s2util.exception.S2RuntimeException;

/**
 * Thrown by {@link S2JsonUtil} when JSON cannot be parsed, written or mapped. Parse errors carry the character position
 * ({@link #getPosition()}); other errors carry the property path in the message (for example {@code items[2].price}).
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2JsonUtil}이 JSON 을 파싱·생성·매핑할 수 없을 때 던집니다. 파싱 오류는 문자 위치({@link #getPosition()})를, 그 밖의 오류는
 * 메시지에 속성 경로(예: {@code items[2].price})를 담습니다.
 *
 * @author devers2
 * @since 2.0.0
 */
public class S2JsonException extends S2RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Character position of a parse error, or -1 | 파싱 오류의 문자 위치, 없으면 -1 */
    private final int position;

    /**
     * Creates an exception without a position.
     *
     * @param message The message | 메시지
     */
    public S2JsonException(String message) {
        this(message, -1, null);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param message The message | 메시지
     * @param cause   The cause | 원인
     */
    public S2JsonException(String message, Throwable cause) {
        this(message, -1, cause);
    }

    /**
     * Creates a parse exception at a character position.
     *
     * @param message  The message | 메시지
     * @param position The character position (0-based), or -1 | 문자 위치 (0부터), 없으면 -1
     * @param cause    The cause, or null | 원인 (없으면 null)
     */
    public S2JsonException(String message, int position, Throwable cause) {
        super(position >= 0 ? message + " (at position " + position + ")" : message, cause);
        this.position = position;
    }

    /**
     * Returns the 0-based character position of a parse error, or -1 when the error is not about the JSON text.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 파싱 오류의 문자 위치(0부터)를 반환합니다. JSON 문자열에 관한 오류가 아니면 -1 입니다.
     *
     * @return The position, or -1 | 위치, 없으면 -1
     */
    public int getPosition() {
        return position;
    }
}
