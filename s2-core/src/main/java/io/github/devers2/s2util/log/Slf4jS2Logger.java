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
 * SLF4J binding implementation using Reflection.
 * <p>
 * To maintain zero compile-time dependencies on specific logging frameworks,
 * this class dynamically discovers and invokes SLF4J's Logger methods at runtime.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 리플렉션을 기반으로 한 SLF4J 바인딩 구현체입니다.
 * <p>
 * 특정 로깅 프레임워크에 대한 컴파일 타임 의존성을 제거하기 위해, 런타임에 SLF4J API의 존재를 확인하고
 * 리플렉션을 통해 로그 기록 메서드를 동적으로 호출하도록 설계되었습니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
class Slf4jS2Logger implements S2Logger {

    private final Object slf4jLogger; // org.slf4j.Logger 인스턴스
    private final Method debugMethod;
    private final Method infoMethod;
    private final Method warnMethod;
    private final Method errorMethod;
    private final Method isDebugEnabledMethod;
    private final Method isInfoEnabledMethod;
    private final Method isWarnEnabledMethod;
    private final Method isErrorEnabledMethod;

    /**
     * Constructs a Slf4jS2Logger by discovering SLF4J methods via reflection.
     *
     * @param slf4jLogger The actual SLF4J Logger instance | 실제 SLF4J 로거 인스턴스
     * @throws Exception If reflection fails to find necessary methods | 리플렉션으로 메서드 조회 실패 시
     */
    Slf4jS2Logger(Object slf4jLogger) throws Exception {
        this.slf4jLogger = slf4jLogger;

        // 리플렉션을 통해 SLF4J Logger 메서드 획득
        Class<?> loggerClass = slf4jLogger.getClass();
        this.debugMethod = findMethod(loggerClass, "debug", String.class, Object[].class);
        this.infoMethod = findMethod(loggerClass, "info", String.class, Object[].class);
        this.warnMethod = findMethod(loggerClass, "warn", String.class, Object[].class);
        this.errorMethod = findMethod(loggerClass, "error", String.class, Object[].class);

        this.isDebugEnabledMethod = loggerClass.getMethod("isDebugEnabled");
        this.isInfoEnabledMethod = loggerClass.getMethod("isInfoEnabled");
        this.isWarnEnabledMethod = loggerClass.getMethod("isWarnEnabled");
        this.isErrorEnabledMethod = loggerClass.getMethod("isErrorEnabled");
    }

    /**
     * Helper to find a public method in a class.
     *
     * @param clazz      Class to search | 검색 대상 클래스
     * @param methodName Name of the method | 메서드 이름
     * @param paramTypes Parameter types | 파라미터 타입들
     * @return The found Method (or null if not found) | 검색된 Method 인스턴스 (없으면 null)
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            // 메서드가 없으면 null 반환 (안전한 fallback)
            return null;
        }
    }

    @Override
    public void log(String level, String message, Object[] args) {
        if (slf4jLogger == null) {
            return;
        }

        try {
            Method method = switch (level) {
                case "DEBUG" -> debugMethod;
                case "INFO" -> infoMethod;
                case "WARN" -> warnMethod;
                case "ERROR" -> errorMethod;
                default -> infoMethod;
            };

            if (method != null) {
                method.invoke(slf4jLogger, message, args);
            }
        } catch (Exception e) {
            // SLF4J 호출 실패 시 조용히 무시 (fallback 없음)
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return invokeIsEnabled(isDebugEnabledMethod);
    }

    @Override
    public boolean isInfoEnabled() {
        return invokeIsEnabled(isInfoEnabledMethod);
    }

    @Override
    public boolean isWarnEnabled() {
        return invokeIsEnabled(isWarnEnabledMethod);
    }

    @Override
    public boolean isErrorEnabled() {
        return invokeIsEnabled(isErrorEnabledMethod);
    }

    /**
     * Safely invokes the SLF4J {@code is*Enabled} method.
     *
     * @param method The method to invoke | 실행할 메서드 인스턴스
     * @return The result of the invocation | 실행 결과
     */
    private boolean invokeIsEnabled(Method method) {
        if (slf4jLogger == null || method == null) {
            return true;
        }
        try {
            return (Boolean) method.invoke(slf4jLogger);
        } catch (Exception e) {
            return true; // 에러 시 기본값 true 반환
        }
    }
}
