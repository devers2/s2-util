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
 * Default implementation of {@link S2LoggerFactory} that creates {@link DefaultS2Logger} instances.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link DefaultS2Logger} 인스턴스를 생성하는 기본 로거 팩토리 구현체입니다.
 */
public class DefaultS2LoggerFactory implements S2LoggerFactory {
    /**
     * Retrieves an S2Logger for the specified class.
     *
     * @param <T>   The type of the class | 클래스 타입
     * @param clazz The class to get the logger for | 로거를 획득할 클래스
     * @return A new DefaultS2Logger instance | 새로운 DefaultS2Logger 인스턴스
     */
    @Override
    public <T> S2Logger getLogger(Class<T> clazz) {
        return new DefaultS2Logger(clazz.getName());
    }

    /**
     * Retrieves an S2Logger for the specified name.
     *
     * @param name The name of the logger | 로거 이름
     * @return A new DefaultS2Logger instance | 새로운 DefaultS2Logger 인스턴스
     */
    @Override
    public S2Logger getLogger(String name) {
        return new DefaultS2Logger(name);
    }
}
