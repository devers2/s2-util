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
 * Factory interface for creating {@link S2Logger} instances.
 * <p>
 * Implementations of this interface serve as bridges to external logging frameworks.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2Logger} 인스턴스를 생성하기 위한 팩토리 인터페이스입니다.
 * <p>
 * 이 인터페이스의 구현체들은 외부 로깅 프레임워크와의 연동을 담당하는 브리지 역할을 수행합니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public interface S2LoggerFactory {
    /**
     * Retrieves an S2Logger for the specified class.
     *
     * @param <T>   The type of the class | 클래스 타입
     * @param clazz The class to get the logger for | 로거를 획득할 클래스
     * @return The S2Logger instance | S2Logger 인스턴스
     */
    public <T> S2Logger getLogger(Class<T> clazz);

    /**
     * Retrieves an S2Logger for the specified name.
     *
     * @param name The name of the logger | 로거 이름
     * @return The S2Logger instance | S2Logger 인스턴스
     */
    S2Logger getLogger(String name);
}
