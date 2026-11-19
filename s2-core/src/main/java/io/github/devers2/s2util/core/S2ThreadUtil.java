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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
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
     * System-wide common executor (lazily initialized on first access).
     * <p>
     * 자원 낭비를 방지하기 위해 최초 접근 시점에 초기화되며,
     * 캐시 유지관리 및 비동기 작업에서 공유하여 사용합니다.
     */
    private static volatile ExecutorService commonExecutor;
    private static volatile boolean shutdownHookRegistered = false;

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
    private static final int CORE_POOL_SIZE = Integer.getInteger("s2.thread.core_pool_size",
            Runtime.getRuntime().availableProcessors());

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

    /**
     * Whether virtual threads are used when the JVM supports them. Set system property
     * {@code s2.thread.virtual.enabled=false} to force platform threads (e.g. to avoid virtual thread pinning
     * on Java 21-23) without code changes.
     * <p>
     * JVM 이 지원할 때 가상 스레드를 쓸지 여부입니다. 시스템 속성 {@code s2.thread.virtual.enabled=false}로 코드 수정 없이
     * 플랫폼 스레드를 강제할 수 있습니다(예: Java 21~23 의 가상 스레드 pinning 회피).
     */
    private static final boolean VIRTUAL_ENABLED = Boolean
            .parseBoolean(System.getProperty("s2.thread.virtual.enabled", "true"));

    /**
     * Maximum number of tasks the virtual-thread common executor runs at once ({@code &le; 0}: unbounded, the
     * default). Configurable via system property {@code s2.thread.virtual.max_concurrency}.
     * <p>
     * 가상 스레드 공용 실행기가 동시에 실행하는 최대 작업 수입니다(0 이하: 제한 없음, 기본값).
     * 시스템 속성 {@code s2.thread.virtual.max_concurrency}로 설정합니다.
     */
    private static final int VIRTUAL_MAX_CONCURRENCY = Integer.getInteger("s2.thread.virtual.max_concurrency", 0);

    /** 제출 대기 중 실행기 종료 여부를 확인하는 간격(ms). */
    private static final long ADMISSION_POLL_MILLIS = 100L;

    // 로그 가독성을 위한 컬러 코드
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";

    static {
        /**
         * 초기화 시점에 환경을 분석하고 팩토리와 가상 스레드 지원 여부만 확인한다.
         * 공용 실행기(COMMON_EXECUTOR)는 최초 접근 시 지연 생성(Lazy Initialization)된다.
         */
        ThreadFactory defaultFactory;
        ThreadFactory platformFactory;
        MethodHandle virtualExecutorMh = null;

        // 기본 플랫폼 데몬 팩토리 설정
        platformFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        };

        if (!VIRTUAL_ENABLED) {
            // s2.thread.virtual.enabled=false: 가상 스레드를 쓰지 않고 플랫폼 스레드로 동작
            defaultFactory = platformFactory;
        } else {
            try {
                // 가상 스레드 지원 여부 확인한다.
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> builderClass = Class.forName("java.lang.Thread$Builder$OfVirtual");
                MethodHandle ofVirtual = lookup.findStatic(Thread.class, "ofVirtual", MethodType.methodType(builderClass));
                MethodHandle factoryMethod = lookup.findVirtual(builderClass, "factory",
                        MethodType.methodType(ThreadFactory.class));

                // 가상 스레드 팩토리 생성
                defaultFactory = (ThreadFactory) factoryMethod.invoke(ofVirtual.invoke());

                // 가상 스레드 실행기 생성용 핸들 확보함
                virtualExecutorMh = lookup.findStatic(Executors.class, "newVirtualThreadPerTaskExecutor",
                        MethodType.methodType(ExecutorService.class));

            } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
                // Java 21 미만 환경 처리
                defaultFactory = platformFactory;
            } catch (Throwable t) {
                defaultFactory = platformFactory;
                String javaVersion = System.getProperty("java.version");
                if (S2Util.isKorean()) {
                    logger.error(
                            "[INIT_ERROR] 예상치 못한 초기화 오류가 발생하였습니다. (Java Version: {}) 플랫폼 팩토리로 대체합니다.",
                            javaVersion, t);
                } else {
                    logger.error(
                            "[INIT_ERROR] An unexpected initialization error occurred. (Java Version: {}) Falling back to platform factory.",
                            javaVersion, t);
                }
            }
        }

        DEFAULT_FACTORY = defaultFactory;
        PLATFORM_FACTORY = platformFactory;
        VIRTUAL_EXECUTOR_MH = virtualExecutorMh;
    }

    private S2ThreadUtil() {
        // Prevent instantiation
    }

    /**
     * Creates the common executor based on the detected environment.
     * Called once during lazy initialization of {@link #commonExecutor}.
     */
    private static ExecutorService createCommonExecutor() {
        if (VIRTUAL_EXECUTOR_MH != null) {
            try {
                ExecutorService executor = (ExecutorService) VIRTUAL_EXECUTOR_MH.invokeExact();
                if (S2Util.isKorean()) {
                    logger.info("[VIRTUAL_THREAD] 가상 스레드 환경을 감지하였습니다. 공용 실행기를 가상 스레드로 설정합니다. (동시 실행 상한: {})",
                            VIRTUAL_MAX_CONCURRENCY > 0 ? VIRTUAL_MAX_CONCURRENCY : "없음");
                } else {
                    logger.info(
                            "[VIRTUAL_THREAD] Virtual thread environment detected. Setting common executor to virtual threads. (Max concurrency: {})",
                            VIRTUAL_MAX_CONCURRENCY > 0 ? VIRTUAL_MAX_CONCURRENCY : "unbounded");
                }
                // Java 24(JEP 491)부터는 synchronized 블로킹이 실행 스레드를 묶지 않으므로 안내는 21~23 에서만
                if (Runtime.version().feature() < 24) {
                    if (S2Util.isKorean()) {
                        logger.info(
                                "{}[CAUTION_SYNCHRONIZED]{} 가이드: Java 21~23 에서는 {}synchronized{} 안에서 블로킹(I/O 등)하면 실행 스레드가 묶여(pinning) 전체가 멈출 수 있습니다. {}ReentrantLock{} 사용 또는 Java 24+ 를 권장하며, 문제가 있으면 -Ds2.thread.virtual.enabled=false 로 플랫폼 스레드를 쓸 수 있습니다.",
                                ANSI_YELLOW, ANSI_RESET, ANSI_GREEN, ANSI_RESET, ANSI_GREEN, ANSI_RESET);
                    } else {
                        logger.info(
                                "{}[CAUTION_SYNCHRONIZED]{} Guide: On Java 21-23, blocking (I/O etc.) inside {}synchronized{} pins carrier threads and can stall all virtual threads. Prefer {}ReentrantLock{} or Java 24+; use -Ds2.thread.virtual.enabled=false to fall back to platform threads.",
                                ANSI_YELLOW, ANSI_RESET, ANSI_GREEN, ANSI_RESET, ANSI_GREEN, ANSI_RESET);
                    }
                }
                return VIRTUAL_MAX_CONCURRENCY > 0 ? new LimitedExecutor(executor, VIRTUAL_MAX_CONCURRENCY, 0) : executor;
            } catch (Throwable t) {
                if (S2Util.isKorean()) {
                    logger.error("[VIRTUAL_ERROR] 가상 스레드 실행기 생성에 실패하였습니다. 플랫폼 풀로 대체합니다.", t);
                } else {
                    logger.error("[VIRTUAL_ERROR] Failed to create virtual thread executor. Falling back to platform pool.", t);
                }
            }
        }

        // 플랫폼 스레드 풀 생성
        String javaVersion = System.getProperty("java.version");
        if (!VIRTUAL_ENABLED) {
            if (S2Util.isKorean()) {
                logger.info("[PLATFORM_THREAD] s2.thread.virtual.enabled=false 설정으로 가상 스레드를 사용하지 않습니다. (Java Version: {})",
                        javaVersion);
                logger.info("[PLATFORM_THREAD] 공용 실행기를 제한적인 플랫폼 스레드로 설정합니다. (Core: {}, Max: {}, Queue: {})",
                        CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
            } else {
                logger.info("[PLATFORM_THREAD] Virtual threads disabled by s2.thread.virtual.enabled=false. (Java Version: {})",
                        javaVersion);
                logger.info("[PLATFORM_THREAD] Setting common executor to limited platform threads. (Core: {}, Max: {}, Queue: {})",
                        CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
            }
        } else if (S2Util.isKorean()) {
            logger.info(
                    "[PLATFORM_THREAD] 가상 스레드를 지원하지 않는 환경입니다. (Java Version: {})",
                    javaVersion);
            logger.info(
                    "[PLATFORM_THREAD] 공용 실행기를 제한적인 플랫폼 스레드로 설정합니다. (Core: {}, Max: {}, Queue: {})",
                    CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        } else {
            logger.info(
                    "[PLATFORM_THREAD] Virtual threads are not supported in this environment. (Java Version: {})",
                    javaVersion);
            logger.info(
                    "[PLATFORM_THREAD] Setting common executor to limited platform threads. (Core: {}, Max: {}, Queue: {})",
                    CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        }

        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                PLATFORM_FACTORY,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Checks if virtual threads are used: the JVM supports them and they are not disabled by
     * {@code s2.thread.virtual.enabled=false}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 가상 스레드를 사용하는지 확인합니다. JVM 이 지원하고 {@code s2.thread.virtual.enabled=false}로 끄지 않았으면 true 입니다.
     *
     * @return {@code true} if virtual threads are used | 가상 스레드를 사용하면 true
     */
    public static boolean isVirtualSupported() {
        return VIRTUAL_EXECUTOR_MH != null;
    }

    /**
     * Returns the system-wide common executor (lazily initialized on first access).
     * <p>
     * The executor is created only when this method is first called, avoiding unnecessary
     * thread pool creation during class loading. This is beneficial for short-lived applications,
     * CLI tools, and test environments where the executor may never be needed.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 시스템 전반에서 공유하여 사용하는 공용 실행기를 반환합니다.
     * <p>
     * 최초 호출 시점에 지연 생성(Lazy Initialization)되므로, 클래스 로딩 시
     * 불필요한 스레드 풀이 미리 생성되는 것을 방지합니다.
     * </p>
     *
     * @return Virtual or sized Platform Executor service | 가상 혹은 제한된 크기의 플랫폼 실행기
     */
    public static ExecutorService getCommonExecutor() {
        ExecutorService executor = commonExecutor;
        if (executor == null) {
            synchronized (S2ThreadUtil.class) {
                executor = commonExecutor;
                if (executor == null) {
                    executor = createCommonExecutor();
                    commonExecutor = executor;
                    if (!shutdownHookRegistered) {
                        Runtime.getRuntime().addShutdownHook(new Thread(S2ThreadUtil::shutdownCommonExecutor));
                        shutdownHookRegistered = true;
                    }
                }
            }
        }
        return executor;
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
     * <p>
     * A positive {@code maxThreads} limits concurrency in both cases: with virtual threads, each task still
     * gets its own virtual thread, but at most {@code maxThreads} tasks run at once (the rest wait on a
     * semaphore, as recommended by JEP 444), so code that limits concurrency for a database pool or an
     * external API behaves the same on Java 17 and Java 21+. With {@code maxThreads} &le; 0, virtual threads
     * are unbounded and the platform pool uses the default maximum size.
     * </p>
     * <p>
     * Waiting tasks are not bounded: if tasks are submitted faster than they finish for a long time, waiting
     * tasks accumulate until memory runs out. Use {@link #newExecutor(int, int)} to bound them.
     * </p>
     * <p>
     * On a bounded virtual-thread executor, {@code shutdownNow()} interrupts tasks that are still waiting for
     * a permit; they end without running and are not included in the returned list.
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
     * <p>
     * {@code maxThreads}가 양수이면 두 경우 모두 동시 실행 수를 제한합니다. 가상 스레드에서도 작업마다 가상 스레드를
     * 만들되 동시에 최대 {@code maxThreads}개만 실행하고 나머지는 세마포어에서 기다리므로(JEP 444 권장 방식),
     * DB 연결 풀이나 외부 API 한도에 맞춰 동시 실행 수를 제한한 코드가 Java 17과 Java 21+에서 같게 동작합니다.
     * {@code maxThreads}가 0 이하이면 가상 스레드는 제한이 없고 플랫폼 풀은 기본 최대 크기를 씁니다.
     * </p>
     * <p>
     * 대기 작업 수에는 제한이 없습니다. 처리보다 빠른 제출이 오래 이어지면 대기 작업이 메모리가 바닥날 때까지 쌓이므로,
     * 제한이 필요하면 {@link #newExecutor(int, int)}를 쓰십시오.
     * </p>
     * <p>
     * 제한된 가상 스레드 실행기에서 {@code shutdownNow()}는 허가를 기다리던 작업을 인터럽트하며, 그 작업은 실행되지 않고
     * 끝나고 반환 목록에도 포함되지 않습니다.
     * </p>
     *
     * @param maxThreads Maximum number of concurrently running tasks (&le; 0: unbounded virtual threads, or the default
     *                   platform pool size) | 동시에 실행할 최대 작업 수 (0 이하: 가상 스레드는 제한 없음, 플랫폼 풀은 기본 크기)
     * @return A new {@link ExecutorService} instance | 새로운 ExecutorService 인스턴스
     */
    public static ExecutorService newExecutor(int maxThreads) {
        ExecutorService virtualExecutor = newVirtualPerTaskExecutor();
        if (virtualExecutor != null) {
            return maxThreads > 0 ? new LimitedExecutor(virtualExecutor, maxThreads, 0) : virtualExecutor;
        }
        int effectiveMax = maxThreads > 0 ? maxThreads : MAX_POOL_SIZE;
        return Executors.newFixedThreadPool(effectiveMax, PLATFORM_FACTORY);
    }

    /**
     * Creates a new {@link ExecutorService} that limits both running and waiting tasks, so overload cannot
     * exhaust memory.
     * <p>
     * At most {@code maxThreads} tasks run at once (virtual threads: one virtual thread per task, limited by a
     * semaphore; platform: a fixed pool of {@code maxThreads}). At most {@code maxPending} more tasks may wait.
     * When {@code maxThreads + maxPending} tasks are in flight, {@code execute}/{@code submit} <b>blocks the
     * submitting thread</b> until a task finishes (backpressure). A submitter blocked when the executor is shut
     * down, or interrupted while waiting, gets a {@link RejectedExecutionException}.
     * </p>
     * <p>
     * <b>Caution:</b> a task that submits subtasks to the same executor and waits for their results can
     * deadlock when the executor is full. Submit such subtasks to a different executor.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 실행 중인 작업과 대기 작업 수를 모두 제한하여 과부하로 메모리가 바닥나지 않는 실행기를 생성합니다.
     * <p>
     * 동시에 최대 {@code maxThreads}개를 실행하고(가상 스레드: 작업마다 가상 스레드 + 세마포어 제한, 플랫폼:
     * {@code maxThreads}개 고정 풀), 최대 {@code maxPending}개가 더 대기할 수 있습니다. 합계
     * {@code maxThreads + maxPending}개가 처리 중이면 {@code execute}/{@code submit}은 작업이 끝날 때까지
     * <b>제출하는 스레드를 기다리게 합니다</b>(역압). 기다리는 중 실행기가 종료되거나 인터럽트되면
     * {@link RejectedExecutionException}이 발생합니다.
     * </p>
     * <p>
     * <b>주의:</b> 작업 안에서 같은 실행기에 하위 작업을 제출하고 그 결과를 기다리면, 실행기가 가득 찼을 때 교착 상태가
     * 될 수 있습니다. 그런 하위 작업은 다른 실행기에 제출하십시오.
     * </p>
     *
     * @param maxThreads Maximum number of concurrently running tasks (must be positive) | 동시에 실행할 최대 작업 수 (양수)
     * @param maxPending Maximum number of tasks waiting to start (0 or more) | 시작을 기다릴 수 있는 최대 작업 수 (0 이상)
     * @return A new {@link ExecutorService} instance | 새로운 ExecutorService 인스턴스
     * @throws IllegalArgumentException if {@code maxThreads} &le; 0 or {@code maxPending} &lt; 0 | 인자가 범위를 벗어난 경우
     */
    public static ExecutorService newExecutor(int maxThreads, int maxPending) {
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("maxThreads must be positive: " + maxThreads);
        }
        if (maxPending < 0) {
            throw new IllegalArgumentException("maxPending must not be negative: " + maxPending);
        }
        int inFlightLimit = (int) Math.min(Integer.MAX_VALUE, (long) maxThreads + maxPending);
        ExecutorService virtualExecutor = newVirtualPerTaskExecutor();
        if (virtualExecutor != null) {
            return new LimitedExecutor(virtualExecutor, maxThreads, inFlightLimit);
        }
        return new LimitedExecutor(Executors.newFixedThreadPool(maxThreads, PLATFORM_FACTORY), 0, inFlightLimit);
    }

    /**
     * Creates a virtual-thread-per-task executor, or returns {@code null} when virtual threads are not used.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 태스크당 가상 스레드 실행기를 만들고, 가상 스레드를 쓰지 않는 환경이면 {@code null}을 반환합니다.
     */
    private static ExecutorService newVirtualPerTaskExecutor() {
        if (!isVirtualSupported()) {
            return null;
        }
        try {
            return (ExecutorService) VIRTUAL_EXECUTOR_MH.invokeExact();
        } catch (Throwable t) {
            if (S2Util.isKorean()) {
                logger.warn("[VIRTUAL_ERROR] 가상 스레드 실행기 생성에 실패하였습니다. 플랫폼 풀로 대체합니다.");
            } else {
                logger.warn(
                        "[VIRTUAL_ERROR] Failed to create virtual thread executor. Falling back to platform pool.");
            }
            return null;
        }
    }

    /**
     * Executor wrapper that optionally limits running tasks (a semaphore inside each task, for virtual threads)
     * and in-flight tasks (a semaphore the submitter must acquire, for backpressure).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 실행 중인 작업 수(가상 스레드용: 각 작업 안의 세마포어)와 처리 중인 작업 수(역압용: 제출자가 얻어야 하는 세마포어)를
     * 선택적으로 제한하는 실행기 래퍼입니다. 가상 스레드를 풀로 재사용하는 대신 JEP 444가 권장하는 세마포어 방식을 씁니다.
     */
    private static final class LimitedExecutor extends AbstractExecutorService {
        private final ExecutorService delegate;
        private final Semaphore running;
        private final Semaphore inFlight;
        // shutdownNow 가 인터럽트를 보내기 전에 세운다. 먼저 인터럽트된 작업이 반납한 허가를 아직 인터럽트되지 않은
        // 대기 작업이 가져가더라도 이 표시를 보고 실행하지 않는다(인터럽트 순서와 무관하게 대기 작업이 실행되지 않음)
        private volatile boolean stopped;

        LimitedExecutor(ExecutorService delegate, int maxRunning, int maxInFlight) {
            this.delegate = delegate;
            this.running = maxRunning > 0 ? new Semaphore(maxRunning) : null;
            this.inFlight = maxInFlight > 0 ? new Semaphore(maxInFlight) : null;
        }

        @Override
        public void execute(Runnable command) {
            Objects.requireNonNull(command, "command");
            if (inFlight != null) {
                acquireInFlight();
            }
            try {
                delegate.execute(() -> {
                    try {
                        if (running != null) {
                            try {
                                running.acquire();
                            } catch (InterruptedException e) {
                                // 허가를 기다리다 shutdownNow 등으로 인터럽트되면 실행하지 않고 끝낸다
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        try {
                            if (!stopped) {
                                command.run();
                            }
                        } finally {
                            if (running != null) {
                                running.release();
                            }
                        }
                    } finally {
                        if (inFlight != null) {
                            inFlight.release();
                        }
                    }
                });
            } catch (RuntimeException e) {
                if (inFlight != null) {
                    inFlight.release();
                }
                throw e;
            }
        }

        /** 처리 중인 작업이 한도에 이르면 제출 스레드를 기다리게 한다(종료·인터럽트 시 거부). */
        private void acquireInFlight() {
            try {
                while (!inFlight.tryAcquire(ADMISSION_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                    if (delegate.isShutdown()) {
                        throw new RejectedExecutionException("Executor has been shut down");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Interrupted while waiting to submit a task", e);
            }
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            stopped = true;
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
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
        ExecutorService executor = commonExecutor;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                synchronized (S2ThreadUtil.class) {
                    commonExecutor = null;
                }
            }
            if (S2Util.isKorean()) {
                logger.info("[COMMON EXECUTOR SHUTDOWN] 공용 실행기를 종료하였습니다.");
            } else {
                logger.info("[COMMON EXECUTOR SHUTDOWN] Common executor has been shut down.");
            }
        }
    }

}
