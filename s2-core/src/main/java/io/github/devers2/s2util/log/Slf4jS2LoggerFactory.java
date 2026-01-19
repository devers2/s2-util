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

import java.lang.reflect.Method;

/**
 * SLF4J-based logger factory using Reflection.
 * <p>
 * This factory is automatically selected at runtime if the SLF4J API is detected
 * on the classpath.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 리플렉션을 사용하는 SLF4J 기반 로거 팩토리입니다.
 * 런타임에 클래스패스에서 SLF4J API가 감지되면 이 팩토리가 자동으로 사용됩니다.
 */
class Slf4jS2LoggerFactory implements S2LoggerFactory {

    private final Class<?> loggerFactoryClass;
    private final Method getLoggerMethod;

    /**
     * Constructs a Slf4jS2LoggerFactory by loading the SLF4J SPI.
     *
     * @throws Exception If SLF4J classes are not found | SLF4J 클래스를 찾을 수 없는 경우
     */
    Slf4jS2LoggerFactory() throws Exception {
        // SLF4J LoggerFactory 클래스 로드
        this.loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
        this.getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String.class);

        // 성공적으로 초기화된 경우 경고 배너 억제
        DefaultS2Logger.markAdapterConfigured();
    }

    /**
     * Retrieves an S2Logger for the specified class by wrapping an SLF4J logger.
     *
     * @param <T>   The type of the class | 클래스 타입
     * @param clazz The class to get the logger for | 로거를 획득할 클래스
     * @return An S2Logger instance bridging to SLF4J | SLF4J와 연결된 S2Logger 인스턴스
     */
    @Override
    public <T> S2Logger getLogger(Class<T> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Retrieves an S2Logger for the specified name by wrapping an SLF4J logger.
     *
     * @param name The name of the logger | 로거 이름
     * @return An S2Logger instance bridging to SLF4J | SLF4J와 연결된 S2Logger 인스턴스
     */
    @Override
    public S2Logger getLogger(String name) {
        try {
            // org.slf4j.LoggerFactory.getLogger(name) 호출
            Object slf4jLogger = getLoggerMethod.invoke(null, name);
            return new Slf4jS2Logger(slf4jLogger);
        } catch (Exception e) {
            // SLF4J 로거 생성 실패 시 기본 로거로 fallback
            return new DefaultS2Logger(name);
        }
    }
}
