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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Central management hub for the S2Util logging system.
 * <p>
 * This manager provides a lightweight abstraction over various logging frameworks.
 * It features a sophisticated <b>Automatic Discovery</b> mechanism that detects
 * the presence of SLF4J at runtime and binds to it automatically. If SLF4J is
 * missing, it falls back to a built-in Console logger.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 로깅 시스템의 중앙 관리 허브입니다.
 * <p>
 * 다양한 로깅 프레임워크에 대한 경량 추상화 레이어를 제공합니다. 런타임에 클래스패스를 분석하여
 * SLF4J 존재 여부를 자동으로 감지(Automatic Discovery)하고 연결하는 기능을 갖추고 있습니다.
 * SLF4J를 찾을 수 없는 경우 내장된 콘솔 로거로 자동 전환(Fallback)됩니다.
 * </p>
 *
 * <h3>Design Features (설계 특징)</h3>
 * <ul>
 * <li><b>Adapter Pattern:</b> Decouples the library from specific logging implementations.</li>
 * <li><b>Hot Reloading:</b> Supports changing the logging engine at runtime via
 * {@link #setLoggerFactory(S2LoggerFactory)} without losing existing logger references.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2LogManager {

    private static volatile S2LoggerFactory factory = createDefaultFactory();

    // name → DelegatingS2Logger 캐시
    private static final ConcurrentMap<String, S2Logger> loggers = new ConcurrentHashMap<>();

    /**
     * Attempts to find the best logging factory based on the runtime classpath.
     * <p>
     * <b>Priority:</b> SLF4J Binding -> Internal Default (Console)
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 런타임 클래스패스 상태를 기반으로 가장 적합한 로깅 팩토리를 탐색하여 생성합니다.
     * <p>
     * <b>우선순위:</b> SLF4J 바인딩 -> 내장 기본 로거 (콘솔)
     * </p>
     *
     * @return The detected {@link S2LoggerFactory}
     */
    private static S2LoggerFactory createDefaultFactory() {
        // SLF4J 감지 시도
        try {
            Class.forName("org.slf4j.Logger");
            Class.forName("org.slf4j.LoggerFactory");
            // SLF4J가 클래스패스에 존재하면 SLF4J 팩토리 사용
            return new Slf4jS2LoggerFactory();
        } catch (ClassNotFoundException e) {
            // SLF4J가 없으면 기본 팩토리 사용
            return new DefaultS2LoggerFactory();
        } catch (Exception e) {
            // SLF4J 팩토리 생성 실패 시 기본 팩토리로 fallback
            return new DefaultS2LoggerFactory();
        }
    }


    /**
     * Registers a custom log factory.
     * <p>
     * Immediately replaces the current logger factory and clears the logger cache,
     * delegating all subsequent logging operations to the new factory.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 사용자 정의 로깅 팩토리를 등록합니다.
     * <p>
     * 등록 즉시 기존 로거 캐시가 초기화되며, 이후 모든 로깅 요청은 새로운 팩토리로 위임됩니다.
     * </p>
     *
     * <h3>Usage Examples (사용 예시)</h3>
     *
     * <h4>1. Spring Boot Configuration Example</h4>
     * <pre>{@code
     * import org.springframework.context.annotation.Configuration;
     * import jakarta.annotation.PostConstruct; // or javax.annotation.PostConstruct
     * import io.github.devers2.s2util.log.S2LogManager;
     * import io.github.devers2.s2util.log.S2Logger;
     * import io.github.devers2.s2util.log.S2LoggerFactory;
     *
     * @Configuration
     * public class S2LogConfig {
     *
     *     @PostConstruct
     *     public void init() {
     *         S2LogManager.setLoggerFactory(new S2LoggerFactory() {
     *             @Override
     *             public S2Logger getLogger(String name) {
     *                 final org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(name);
     *                 return new S2Logger() {
     *                     @Override
     *                     public void log(String level, String message, Object[] args) {
     *                         if ("DEBUG".equals(level)) slf4jLogger.debug(message, args);
     *                         else if ("INFO".equals(level)) slf4jLogger.info(message, args);
     *                         else if ("WARN".equals(level)) slf4jLogger.warn(message, args);
     *                         else if ("ERROR".equals(level)) slf4jLogger.error(message, args);
     *                     }
     *
     *                     @Override public boolean isDebugEnabled() { return slf4jLogger.isDebugEnabled(); }
     *                     @Override public boolean isInfoEnabled()  { return slf4jLogger.isInfoEnabled(); }
     *                     @Override public boolean isWarnEnabled()  { return slf4jLogger.isWarnEnabled(); }
     *                     @Override public boolean isErrorEnabled() { return slf4jLogger.isErrorEnabled(); }
     *                 };
     *             }
     *         });
     *     }
     * }
     * }</pre>
     *
     * <h4>2. Pure Java Custom Logger Example</h4>
     * <pre>{@code
     * S2LogManager.setLoggerFactory(name -> new S2Logger() {
     *     @Override
     *     public void log(String level, String message, Object[] args) {
     *         System.out.printf("[%s] [%s] %s%n", level, name, message);
     *     }
     * });
     * }</pre>
     *
     * @param customFactory The custom {@link S2LoggerFactory} to use | 등록할 사용자 정의 로거 팩토리
     */
    public static void setLoggerFactory(S2LoggerFactory customFactory) {
        if (customFactory != null) {
            // 먼저 교체
            factory = customFactory;
            // 기존 캐시 비우기
            loggers.clear();
        }
    }

    public static <T> S2Logger getLogger(Class<T> clazz) {
        var name = clazz == null ? "unknown" : clazz.getName();
        return getLogger(name);
    }

    /**
     * Retrieves a logger for the specified name.
     * <p>
     * This method returns a thread-safe delegating proxy that can adapt to
     * logging engine changes at runtime.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 이름에 해당하는 로거 인스턴스를 반환합니다.
     * <p>
     * 반환된 로거는 내부적으로 위임(Delegating) 프록시 구조를 취하고 있어, 실행 중에 로깅 엔진이
     * 변경되더라도(setLoggerFactory 호출 등) 객체 교체 없이 실시간으로 새 엔진에 적응합니다.
     * </p>
     *
     * @param name The name of the logger (usually a fully qualified class name)
     * @return The {@link S2Logger} instance
     */
    public static S2Logger getLogger(String name) {
        var loggerName = name == null ? "unknown" : name;
        return loggers.computeIfAbsent(loggerName, DelegatingS2Logger::new);
    }

    /**
     * Internal proxy class that delegates all logging calls to the actual resolved logger.
     * <p>
     * This allows the logging engine to be swapped at runtime without invalidating
     * existing logger references held by the application.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 실제 로거에게 모든 기능을 위임하는 내부 프록시 클래스입니다.
     * <p>
     * 실행 중에 로깅 엔진이 변경되더라도 기존에 앱에서 보유한 로거 객체 인덱스를 유지할 수 있게 해줍니다.
     * </p>
     */
    private static class DelegatingS2Logger implements S2Logger {
        private final String name;
        private volatile S2Logger delegate;
        private volatile S2LoggerFactory lastFactory;

        DelegatingS2Logger(String name) {
            this.name = name;
        }

        private S2Logger resolve() {
            var current = factory;
            var d = delegate;
            if (d == null || lastFactory != current) {
                synchronized (this) {
                    if (delegate == null || lastFactory != current) {
                        lastFactory = current;
                        delegate = (current != null) ? current.getLogger(name) : new DefaultS2Logger(name);
                    }
                    d = delegate;
                }
            }
            return d;
        }

        // --- 로그 메서드 위임 ---

        @Override
        public void log(String level, String message, Object[] args) {
            resolve().log(level, message, args);
        }

        // --- 기존 단일 메시지 메서드들은 S2Logger 인터페이스의 default 메서드 구현을 사용함 ---

        // --- 레벨 체크 위임 (S2Logger 인터페이스에 default 메서드 추가되어 있어도 위임하면 실제 상태 반영) ---

        @Override
        public boolean isDebugEnabled() {
            return resolve().isDebugEnabled();
        }

        @Override
        public boolean isInfoEnabled() {
            return resolve().isInfoEnabled();
        }

        @Override
        public boolean isWarnEnabled() {
            return resolve().isWarnEnabled();
        }

        @Override
        public boolean isErrorEnabled() {
            return resolve().isErrorEnabled();
        }
    }
}
