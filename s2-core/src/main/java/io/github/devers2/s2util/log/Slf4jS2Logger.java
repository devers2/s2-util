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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * SLF4J binding implementation without a compile-time dependency on SLF4J.
 * <p>
 * Methods are resolved once on the public {@code org.slf4j.Logger} interface (found in the logger's own type
 * hierarchy, so the same class loader is used) and bound to the logger instance as {@link MethodHandle}s; each log
 * call is a direct {@code invokeExact} without per-call reflection checks or argument array boxing. An object that is
 * not an {@code org.slf4j.Logger} is rejected at construction, so the factory falls back to {@link DefaultS2Logger}.
 * This class is used only when SLF4J is on the class path; otherwise {@link S2LogManager} uses {@link DefaultS2Logger}.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * SLF4J 에 컴파일 타임 의존성 없이 동작하는 SLF4J 바인딩 구현체입니다.
 * <p>
 * 공개 인터페이스 {@code org.slf4j.Logger}(로거 자신의 타입 계층에서 찾아 같은 클래스로더를 사용)에서 메서드를 한 번 찾아 로거 인스턴스에 묶은
 * {@link MethodHandle}로 보관하고, 로그 호출마다 호출별 리플렉션 검사나 인자 배열 포장 없이 {@code invokeExact}로 직접 호출합니다.
 * {@code org.slf4j.Logger}가 아닌 객체는 생성 시점에 거부하므로 팩토리가 {@link DefaultS2Logger}로 대체합니다. SLF4J 가 클래스패스에 있을 때만
 * 쓰이며, 없으면 {@link S2LogManager}가 {@link DefaultS2Logger}를 사용합니다.
 * </p>
 *
 * @author devers2
 * @version 1.6
 * @since 1.0
 */
class Slf4jS2Logger implements S2Logger {

    private static final String SLF4J_LOGGER = "org.slf4j.Logger";
    private static final MethodType LOG_TYPE = MethodType.methodType(void.class, String.class, Object[].class);
    private static final MethodType IS_ENABLED_TYPE = MethodType.methodType(boolean.class);

    /** Handles bound to the logger, typed {@code (String, Object[])void} | 로거에 묶인 핸들, 타입 {@code (String, Object[])void} */
    private final MethodHandle debugHandle;
    private final MethodHandle infoHandle;
    private final MethodHandle warnHandle;
    private final MethodHandle errorHandle;

    /** Handles bound to the logger, typed {@code ()boolean} | 로거에 묶인 핸들, 타입 {@code ()boolean} */
    private final MethodHandle isDebugEnabledHandle;
    private final MethodHandle isInfoEnabledHandle;
    private final MethodHandle isWarnEnabledHandle;
    private final MethodHandle isErrorEnabledHandle;

    /**
     * Constructs a Slf4jS2Logger by binding the SLF4J {@code Logger} interface methods to the given instance.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * SLF4J {@code Logger} 인터페이스 메서드를 주어진 인스턴스에 묶어 생성합니다.
     *
     * @param slf4jLogger The actual SLF4J Logger instance | 실제 SLF4J 로거 인스턴스
     * @throws Exception If the object is not an {@code org.slf4j.Logger} or a method cannot be resolved | {@code org.slf4j.Logger}가
     *                   아니거나 메서드를 찾을 수 없는 경우
     */
    Slf4jS2Logger(Object slf4jLogger) throws Exception {
        Class<?> api = findInterface(slf4jLogger.getClass(), SLF4J_LOGGER);
        if (api == null) {
            throw new IllegalArgumentException("Not an " + SLF4J_LOGGER + ": " + slf4jLogger.getClass().getName());
        }
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        this.debugHandle = bind(lookup, api, "debug", LOG_TYPE, slf4jLogger);
        this.infoHandle = bind(lookup, api, "info", LOG_TYPE, slf4jLogger);
        this.warnHandle = bind(lookup, api, "warn", LOG_TYPE, slf4jLogger);
        this.errorHandle = bind(lookup, api, "error", LOG_TYPE, slf4jLogger);
        this.isDebugEnabledHandle = bind(lookup, api, "isDebugEnabled", IS_ENABLED_TYPE, slf4jLogger);
        this.isInfoEnabledHandle = bind(lookup, api, "isInfoEnabled", IS_ENABLED_TYPE, slf4jLogger);
        this.isWarnEnabledHandle = bind(lookup, api, "isWarnEnabled", IS_ENABLED_TYPE, slf4jLogger);
        this.isErrorEnabledHandle = bind(lookup, api, "isErrorEnabled", IS_ENABLED_TYPE, slf4jLogger);
    }

    private static MethodHandle bind(MethodHandles.Lookup lookup, Class<?> api, String name, MethodType type, Object target)
            throws ReflectiveOperationException {
        return lookup.findVirtual(api, name, type).bindTo(target);
    }

    /**
     * Finds an interface by name in the type hierarchy (superclasses and super-interfaces).
     *
     * @param type          The class to search from | 탐색 시작 클래스
     * @param interfaceName The fully qualified interface name | 인터페이스 전체 이름
     * @return The interface, or {@code null} if not implemented | 인터페이스 (구현하지 않으면 null)
     */
    private static Class<?> findInterface(Class<?> type, String interfaceName) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (interfaceName.equals(i.getName())) {
                    return i;
                }
                Class<?> found = findInterface(i, interfaceName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Override
    public void log(String level, String message, Object[] args) {
        MethodHandle handle = switch (level) {
            case "DEBUG" -> debugHandle;
            case "WARN" -> warnHandle;
            case "ERROR" -> errorHandle;
            default -> infoHandle;
        };
        try {
            handle.invokeExact(message, args);
        } catch (Throwable e) {
            // SLF4J 호출 실패 시 조용히 무시 (fallback 없음)
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return invokeIsEnabled(isDebugEnabledHandle);
    }

    @Override
    public boolean isInfoEnabled() {
        return invokeIsEnabled(isInfoEnabledHandle);
    }

    @Override
    public boolean isWarnEnabled() {
        return invokeIsEnabled(isWarnEnabledHandle);
    }

    @Override
    public boolean isErrorEnabled() {
        return invokeIsEnabled(isErrorEnabledHandle);
    }

    /**
     * Safely invokes an SLF4J {@code is*Enabled} handle.
     *
     * @param handle The bound handle | 묶인 핸들
     * @return The result, or {@code true} on failure | 실행 결과 (실패 시 true)
     */
    private static boolean invokeIsEnabled(MethodHandle handle) {
        try {
            return (boolean) handle.invokeExact();
        } catch (Throwable e) {
            return true; // 에러 시 기본값 true 반환
        }
    }
}
