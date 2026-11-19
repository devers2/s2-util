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
package io.github.devers2.s2util.validation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Warns once when a validated field name does not exist on the target class (e.g. a typo), since such a field always
 * reads as {@code null} and its non-required rules never run.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 검증 필드명이 대상 클래스에 없을 때(오타 등) 한 번 경고합니다. 그런 필드는 항상 {@code null}로 읽혀 필수 외 규칙이 실행되지 않기 때문입니다.
 */
final class S2PropertyCheck {

    private static final S2Logger logger = S2LogManager.getLogger(S2PropertyCheck.class);

    /** 클래스별로 이미 확인한 필드명 (ClassValue 라 클래스가 언로드되면 함께 정리됨) */
    private static final ClassValue<Set<String>> CHECKED = new ClassValue<>() {
        @Override
        protected Set<String> computeValue(Class<?> type) {
            return ConcurrentHashMap.newKeySet();
        }
    };

    private S2PropertyCheck() {
    }

    /**
     * Logs a warning (once per class and field) if the first segment of {@code fieldName} is not a readable property
     * of {@code target}. Maps, collections, arrays and {@code Optional} are not checked.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code fieldName}의 첫 마디가 {@code target}의 읽을 수 있는 속성이 아니면 경고를 남깁니다(클래스·필드당 한 번). Map·컬렉션·배열·
     * {@code Optional}은 확인하지 않습니다.
     *
     * @param target    The validated object | 검증 대상
     * @param fieldName The declared field name | 선언한 필드명
     */
    static void warnIfMissing(Object target, String fieldName) {
        if (target == null || target instanceof Map || target instanceof Collection || target instanceof Optional
                || target.getClass().isArray()) {
            return;
        }
        Class<?> type = target.getClass();
        if (!CHECKED.get(type).add(fieldName)) {
            return;
        }
        String property = firstSegment(fieldName);
        if (property.isEmpty() || hasProperty(type, property)) {
            return;
        }
        if (S2Util.isKorean()) {
            logger.warn("검증 필드 '{}'를 {} 에서 찾을 수 없습니다. 항상 빈 값으로 처리되어 필수 외 규칙이 실행되지 않습니다(필드명 오타 확인).",
                    fieldName, type.getName());
        } else {
            logger.warn("Validated field '{}' was not found on {}. It always reads as empty, so its non-required rules never "
                    + "run (check the field name).", fieldName, type.getName());
        }
    }

    private static String firstSegment(String fieldName) {
        int end = fieldName.length();
        int dot = fieldName.indexOf('.');
        int bracket = fieldName.indexOf('[');
        if (dot >= 0) {
            end = dot;
        }
        if (bracket >= 0 && bracket < end) {
            end = bracket;
        }
        return fieldName.substring(0, end);
    }

    static boolean hasProperty(Class<?> type, String property) {
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (component.getName().equals(property)) {
                    return true;
                }
            }
        }
        String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers())
                    && (method.getName().equals("get" + suffix) || method.getName().equals("is" + suffix)
                            || method.getName().equals(property))) {
                return true;
            }
        }
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getName().equals(property)) {
                    return true;
                }
            }
        }
        return false;
    }
}
