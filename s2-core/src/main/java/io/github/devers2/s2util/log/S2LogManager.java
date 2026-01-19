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
 * <li><b>Self-Diagnostic:</b> Includes an auto-warning system that alerts developers
 * if no external logging adapter is configured within the first 5 seconds.</li>
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

    static {
        /**
         * [🚀 실시간 경고 시스템]
         * 사용자가 로거를 명시적으로 호출하지 않더라도, 이 라이브러리가 로드된 시점으로부터
         * 약 5초 뒤에 어댑터 설정 여부를 자동으로 체크하여 경고 배너를 출력한다.
         *
         * 5초의 지연은 Spring Boot 등 프레임워크가 가동되면서 사용자가 setLoggerFactory()를
         * 호출할 시간을 충분히 확보하기 위함이다.
         *
         * 1회성 데몬 스레드를 사용하므로 임무 완료 후 즉시 소멸하며 시스템 성능에 영향을 주지 않는다.
         *
         * ⚠️ 단, SLF4J가 감지된 경우에는 경고를 표시하지 않습니다.
         */
        var autoWarningTrigger = new Thread(() -> {
            try {
                // 애플리케이션 초기 부트스트랩 시간을 고려하여 대기 (5초)
                Thread.sleep(5000);

                // 5초 후에도 여전히 기본 팩토리이고, SLF4J도 아니라면 설정 누락으로 간주하고 배너 출력
                if (factory instanceof DefaultS2LoggerFactory) {
                    DefaultS2Logger.printWarningBannerOnce();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "S2-Logger-AutoWarning-Trigger");

        autoWarningTrigger.setDaemon(true);
        autoWarningTrigger.start();
    }

    /**
     * Forces class loading of the manager to trigger the static initialization block.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 클래스 로딩을 강제하여 정적 초기화 블록(static block)의 실행을 유도합니다.
     */
    public static void touch() {
        // 아무것도 하지 않으나 클래스 로드 및 static block 트리거 유도
    }

    /**
     * Registers a custom log factory.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 사용자 정의 로깅 팩토리를 등록합니다.
     * <p>
     * 등록 즉시 기존 로거 캐시가 초기화되며, 이후 모든 로깅 요청은 새로운 팩토리로 위임됩니다.
     * </p>
     *
     * @param customFactory The custom {@link S2LoggerFactory} to use
     */
    public static void setLoggerFactory(S2LoggerFactory customFactory) {
        if (customFactory != null) {
            // 커스텀 어댑터가 설정되었으므로 경고 배너 출력을 사전에 차단
            DefaultS2Logger.markAdapterConfigured();

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
