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
package io.github.devers2.s2util.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Advanced concurrency utility providing seamless support for Java 21+ Virtual Threads.
 * <p>
 * This utility manages system-wide {@link ExecutorService} and {@link ThreadFactory} instances.
 * It follows a "Virtual-First" strategy: if the JVM supports Virtual Threads (Java 21 or higher),
 * it utilizes them for maximum scalability; otherwise, it falls back to a highly optimized
 * and tunable Platform Thread pool.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * Java 21+ 가상 스레드(Virtual Thread)를 지원하는 고성능 병렬 처리 유틸리티 클래스입니다.
 * <p>
 * 시스템 전반에서 사용하는 스레드 실행기(Executor)와 팩토리(ThreadFactory)를 관리합니다.
 * "가상 스레드 우선" 전략을 채택하여, 실행 환경이 가상 스레드를 지원하면 이를 적극 활용하고,
 * 지원하지 않는 환경(Java 17 등)에서는 튜닝된 플랫폼 스레드 풀을 통해 안정적인 동작을 보장합니다.
 * </p>
 *
 * <h3>Key Design Principles (설계 원칙)</h3>
 * <ul>
 * <li><b>Polyfill via MethodHandles:</b> Uses {@link MethodHandle} to access Java 21 features,
 * allowing the library to remain compatible with older Java versions at compile-time.</li>
 * <li><b>Shared Common Executor:</b> Provides a singleton {@link #getCommonExecutor()} to reuse
 * resources across different components, preventing thread explosion.</li>
 * <li><b>Graceful Lifecycle Management:</b> Automatically registers a shutdown hook to ensure
 * proper termination of pooled resources when the JVM exits.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2ThreadUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2ThreadUtil.class);

    /**
     * Default thread factory.
     * <p>
     * Java 21+ 환경에서는 가상 스레드 팩토리를, 미만 환경에서는 플랫폼 데몬 스레드 팩토리를 보관합니다.
     */
    private static final ThreadFactory DEFAULT_FACTORY;

    /**
     * System-wide common executor.
     * <p>
     * 자원 낭비를 방지하기 위해 캐시 유지관리 및 비동기 작업에서 공유하여 사용합니다.
     */
    private static final ExecutorService COMMON_EXECUTOR;

    /**
     * Explicit platform daemon thread factory.
     * <p>
     * 명시적인 플랫폼 데몬 스레드 팩토리입니다.
     */
    private static final ThreadFactory PLATFORM_FACTORY;

    /**
     * Method handle for creating virtual thread executors.
     * <p>
     * 가상 스레드 실행기 생성을 위한 메서드 핸들입니다.
     */
    private static final MethodHandle VIRTUAL_EXECUTOR_MH;

    /**
     * The core number of threads to keep in the pool (Platform Thread environment only).
     * Configurable via system property {@code s2.thread.core_pool_size}.
     */
    private static final int CORE_POOL_SIZE = Integer.getInteger("s2.thread.core_pool_size", Runtime.getRuntime().availableProcessors());

    /**
     * The maximum number of threads allowed in the pool (Platform Thread environment only).
     * Configurable via system property {@code s2.thread.max_pool_size}.
     */
    private static final int MAX_POOL_SIZE = Integer.getInteger("s2.thread.max_pool_size", CORE_POOL_SIZE * 2);

    /**
     * The capacity of the task queue (Platform Thread environment only).
     * Configurable via system property {@code s2.thread.queue_capacity}.
     */
    private static final int QUEUE_CAPACITY = Integer.getInteger("s2.thread.queue_capacity", 1000);

    // 로그 가독성을 위한 컬러 코드
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";

    static {
        /**
         * 초기화 시점에 환경을 분석하고 기본 자원을 할당한다.
         */
        ThreadFactory defaultFactory = null;
        ThreadFactory platformFactory = null;
        ExecutorService commonExecutor = null;
        MethodHandle virtualExecutorMh = null;

        // 기본 플랫폼 데몬 팩토리 설정
        platformFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        };

        try {
            // 가상 스레드 지원 여부 확인한다.
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> builderClass = Class.forName("java.lang.Thread$Builder$OfVirtual");
            MethodHandle ofVirtual = lookup.findStatic(Thread.class, "ofVirtual", MethodType.methodType(builderClass));
            MethodHandle factoryMethod = lookup.findVirtual(builderClass, "factory", MethodType.methodType(ThreadFactory.class));

            // 가상 스레드 팩토리 생성
            defaultFactory = (ThreadFactory) factoryMethod.invoke(ofVirtual.invoke());

            // 가상 스레드 실행기 생성용 핸들 확보함
            virtualExecutorMh = lookup.findStatic(Executors.class, "newVirtualThreadPerTaskExecutor", MethodType.methodType(ExecutorService.class));

            // 가상 스레드 환경은 무제한 생성 실행기를 공용으로 사용함
            commonExecutor = (ExecutorService) virtualExecutorMh.invokeExact();

            // 가상 스레드 감지 및 주의사항을 안내함
            if (S2Util.isKorean(Locale.getDefault())) {
                logger.info("[VIRTUAL_THREAD] 가상 스레드 환경을 감지하였습니다. 공용 실행기를 가상 스레드로 설정합니다.");
                logger.info(
                        "{}[CAUTION_SYNCHRONIZED]{} 가이드: 성능 저하 방지를 위해 {}synchronized{} 대신 {}java.util.concurrent.locks.ReentrantLock{} 사용을 권장합니다.",
                        ANSI_YELLOW, ANSI_RESET, ANSI_GREEN, ANSI_RESET, ANSI_GREEN, ANSI_RESET
                );
            } else {
                logger.info("[VIRTUAL_THREAD] Virtual thread environment detected. Setting common executor to virtual threads.");
                logger.info(
                        "{}[CAUTION_SYNCHRONIZED]{} Guide: To prevent performance degradation, consider using {}java.util.concurrent.locks.ReentrantLock{} instead of {}synchronized{}.",
                        ANSI_YELLOW, ANSI_RESET, ANSI_GREEN, ANSI_RESET, ANSI_GREEN, ANSI_RESET
                );
            }

        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            // Java 21 미만 환경 처리
            defaultFactory = platformFactory;
            commonExecutor = new ThreadPoolExecutor(
                    CORE_POOL_SIZE,
                    MAX_POOL_SIZE,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                    platformFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            String javaVersion = System.getProperty("java.version");
            if (S2Util.isKorean(Locale.getDefault())) {
                logger.info(
                        "[PLATFORM_THREAD] 가상 스레드를 지원하지 않는 환경입니다. (Java Version: {})",
                        javaVersion
                );
                logger.info(
                        "[PLATFORM_THREAD] 공용 실행기를 제한적인 플랫폼 스레드로 설정합니다. (Core: {}, Max: {}, Queue: {})",
                        CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY
                );
            } else {
                logger.info(
                        "[PLATFORM_THREAD] Virtual threads are not supported in this environment. (Java Version: {})",
                        javaVersion
                );
                logger.info(
                        "[PLATFORM_THREAD] Setting common executor to limited platform threads. (Core: {}, Max: {}, Queue: {})",
                        CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY
                );
            }
        } catch (Throwable t) {
            defaultFactory = platformFactory;
            commonExecutor = Executors.newCachedThreadPool(platformFactory);
            String javaVersion = System.getProperty("java.version");
            if (S2Util.isKorean(Locale.getDefault())) {
                logger.error(
                        "[INIT_ERROR] 예상치 못한 초기화 오류가 발생하였습니다. (Java Version: {}) 기본 CachedThreadPool로 대체합니다.",
                        javaVersion, t
                );
            } else {
                logger.error(
                        "[INIT_ERROR] An unexpected initialization error occurred. (Java Version: {}) Falling back to basic CachedThreadPool.",
                        javaVersion, t
                );
            }
        }

        DEFAULT_FACTORY = defaultFactory;
        PLATFORM_FACTORY = platformFactory;
        COMMON_EXECUTOR = commonExecutor;
        VIRTUAL_EXECUTOR_MH = virtualExecutorMh;

        /*
         * JVM 종료 지연 또는 방해, 리소스 누출, 비동기 작업 미완료 등의 문제를 방지하기 위해 시스템 종료 시 자동으로 호출되도록 등록한다.
         */
        Runtime.getRuntime().addShutdownHook(new Thread(S2ThreadUtil::shutdownCommonExecutor));
    }

    private S2ThreadUtil() {
        // Prevent instantiation
    }

    /**
     * Checks if the current JVM supports Virtual Threads.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 실행 환경(JVM)이 가상 스레드를 지원하는지 여부를 확인합니다.
     *
     * @return {@code true} if virtual threads are supported | 가상 스레드 지원 시 true
     */
    public static boolean isVirtualSupported() {
        return VIRTUAL_EXECUTOR_MH != null;
    }

    /**
     * Returns the system-wide common executor.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 시스템 전반에서 공유하여 사용하는 공용 실행기를 반환합니다.
     *
     * @return Virtual or sized Platform Executor service | 가상 혹은 제한된 크기의 플랫폼 실행기
     */
    public static ExecutorService getCommonExecutor() {
        return COMMON_EXECUTOR;
    }

    /**
     * Returns the default thread factory.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 라이브러리 기본 스레드 팩토리를 반환합니다.
     *
     * @return Virtual or Platform Daemon ThreadFactory | 가상 스레드 또는 플랫폼 데몬 스레드 팩토리
     */
    public static ThreadFactory getFactory() {
        return DEFAULT_FACTORY;
    }

    /**
     * Returns a thread factory that explicitly creates Platform Threads.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 플랫폼 전용 스레드 팩토리를 반환합니다.
     *
     * @return Platform Daemon ThreadFactory | 플랫폼 데몬 스레드 팩토리
     */
    public static ThreadFactory getPlatformFactory() {
        return PLATFORM_FACTORY;
    }

    /**
     * Creates and returns a new {@link ExecutorService} optimized for the current environment.
     * <p>
     * Returns a Virtual Thread Per-Task Executor if supported. Otherwise, returns a Fixed Platform
     * Thread Pool with the specified size.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 실행 환경에 최적화된 새로운 실행기(Executor)를 생성하여 반환합니다.
     * <p>
     * 가상 스레드 지원 시 '태스크당 가상 스레드 실행기'를 반환하며, 미지원 시 지정된 크기의
     * 고정 플랫폼 스레드 풀을 생성합니다.
     * </p>
     *
     * @param maxThreads Maximum threads if platform pool is used (uses default if &le; 0) | 플랫폼 풀 사용 시 최대 스레드 수 (0 이하 시 기본값 사용)
     * @return A new {@link ExecutorService} instance | 새로운 ExecutorService 인스턴스
     */
    public static ExecutorService newExecutor(int maxThreads) {
        if (isVirtualSupported()) {
            try {
                return (ExecutorService) VIRTUAL_EXECUTOR_MH.invokeExact();
            } catch (Throwable t) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.warn("[VIRTUAL_ERROR] 가상 스레드 실행기 생성에 실패하였습니다. 플랫폼 풀로 대체합니다.");
                } else {
                    logger.warn("[VIRTUAL_ERROR] Failed to create virtual thread executor. Falling back to platform pool.");
                }
            }
        }
        int effectiveMax = maxThreads > 0 ? maxThreads : MAX_POOL_SIZE;
        return Executors.newFixedThreadPool(effectiveMax, PLATFORM_FACTORY);
    }

    /**
     * Safely shuts down pooled resources during application termination.
     * <p>
     * <b>Note:</b> This only shuts down the {@code COMMON_EXECUTOR}. The factories
     * do not require manual shutdown as they are stateless and produced threads
     * are marked as daemons, which terminate automatically with the JVM.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 애플리케이션 종료 시 풀을 안전하게 종료합니다. (공용 실행기 전용)
     */
    public static void shutdownCommonExecutor() {
        if (COMMON_EXECUTOR != null && !COMMON_EXECUTOR.isShutdown()) {
            COMMON_EXECUTOR.shutdown();
            try {
                if (!COMMON_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                    COMMON_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                COMMON_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
            if (S2Util.isKorean(Locale.getDefault())) {
                logger.info("[COMMON EXECUTOR SHUTDOWN] 공용 실행기를 종료하였습니다.");
            } else {
                logger.info("[COMMON EXECUTOR SHUTDOWN] Common executor has been shut down.");
            }
        }
    }

}
