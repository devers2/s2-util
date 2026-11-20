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

/**
 * Thrown when reading a property fails because its getter itself threw an exception (for example a lazy-loading
 * failure or a {@code NullPointerException} inside a computed getter). A property that does not exist or whose value
 * is {@code null} is not an error; only failures of the getter are reported, so that they are not mistaken for an
 * empty value.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 속성을 읽을 때 getter 자체가 예외를 던져 실패하면 발생합니다(예: 지연 로딩 실패, 계산 getter 안의
 * {@code NullPointerException}). 속성이 없거나 값이 {@code null} 인 것은 오류가 아니며, getter 의 실패만 알려
 * 빈 값으로 오인되지 않게 합니다.
 *
 * @author devers2
 * @since 2.2.0
 */
public class S2PropertyAccessException extends S2RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String propertyPath;
    private final Class<?> targetClass;

    /**
     * Creates the exception for the property whose getter failed.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * getter 가 실패한 속성에 대한 예외를 생성합니다.
     *
     * @param propertyPath Property name or path that was read (e.g. "address.zip") | 읽으려던 속성 이름 또는 경로 (예: "address.zip")
     * @param targetClass  Class of the object the path was read from | 경로를 읽은 객체의 클래스
     * @param cause        Exception thrown by the getter | getter 가 던진 예외
     */
    public S2PropertyAccessException(String propertyPath, Class<?> targetClass, Throwable cause) {
        super("Failed to read property '" + propertyPath + "' of "
                + (targetClass != null ? targetClass.getName() : "null") + ": " + cause, cause);
        this.propertyPath = propertyPath;
        this.targetClass = targetClass;
    }

    /**
     * Returns the property name or path that was read.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 읽으려던 속성 이름 또는 경로를 반환합니다.
     *
     * @return Property name or path | 속성 이름 또는 경로
     */
    public String getPropertyPath() {
        return propertyPath;
    }

    /**
     * Returns the class of the object the property was read from.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 속성을 읽은 객체의 클래스를 반환합니다.
     *
     * @return Target class | 대상 클래스
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }
}
