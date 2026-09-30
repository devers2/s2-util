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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.github.devers2.s2util.json.S2JsonUtil;

/**
 * Builds an {@link S2Validator} from rules JSON (the inverse of {@link S2RulesJsonWriter}); the entry point is
 * {@link S2Validator#fromJson(String)}.
 * <p>
 * Strict on purpose: a rules definition usually comes from a database or configuration, and a typo that is silently
 * ignored would silently drop validation. Unknown keys, unknown rule types or operators, and criteria that do not fit a
 * rule are rejected with the JSON path of the problem.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 규칙 JSON 으로 {@link S2Validator}를 만듭니다({@link S2RulesJsonWriter}의 역방향). 진입점은 {@link S2Validator#fromJson(String)}입니다.
 * <p>
 * 의도적으로 엄격합니다. 규칙 정의는 보통 DB 나 설정에서 오므로, 오타를 조용히 무시하면 검증이 조용히 빠집니다. 모르는 키, 모르는 규칙
 * 타입·연산자, 규칙에 맞지 않는 기준값은 문제 위치의 JSON 경로와 함께 거부합니다.
 * </p>
 */
final class S2RulesJsonReader {

    private static final Set<String> ROOT_KEYS = Set.of("schemaVersion", "fields");
    private static final Set<String> FIELD_KEYS = Set.of("name", "label", "rules", "conditions");
    private static final Set<String> RULE_KEYS = Set.of("type", "value", "regex", "message", "messages", "key", "nestedRules");
    private static final Set<String> CONDITION_KEYS = Set.of("field", "op", "value");

    private S2RulesJsonReader() {
    }

    /**
     * Reads rules JSON into a validator.
     *
     * @param <T>  Target type | 대상 타입
     * @param json Rules JSON | 규칙 JSON
     * @return The validator | 검증기
     */
    static <T> S2Validator<T> read(String json) {
        Map<String, Object> root = S2JsonUtil.parseObject(json);
        checkKeys(root, ROOT_KEYS, "$");
        Object version = root.get("schemaVersion");
        if (!(version instanceof Long v)) {
            throw error("$.schemaVersion", "required integer, but was " + version);
        }
        if (v < 1 || v > S2RulesJsonWriter.SCHEMA_VERSION) {
            throw error("$.schemaVersion", "unsupported version " + v + " (this library reads 1.."
                    + S2RulesJsonWriter.SCHEMA_VERSION + ")");
        }
        return readFields(root.get("fields"), "$.fields");
    }

    private static <T> S2Validator<T> readFields(Object fieldsNode, String path) {
        List<?> fields = asList(fieldsNode, path);
        if (fields.isEmpty()) {
            throw error(path, "must contain at least one field");
        }
        S2FieldStep.BuilderStartStep<T> start = S2Validator.builder();
        S2FieldStep.BuilderFieldStep<T> step = null;
        for (int i = 0; i < fields.size(); i++) {
            String fieldPath = path + "[" + i + "]";
            Map<String, Object> field = asMap(fields.get(i), fieldPath);
            checkKeys(field, FIELD_KEYS, fieldPath);
            String name = asString(field.get("name"), fieldPath + ".name", true);
            String label = asString(field.get("label"), fieldPath + ".label", false);
            step = step == null ? start.field(name, label) : step.field(name, label);
            step = readRules(step, field.get("rules"), fieldPath + ".rules");
            step = readConditions(step, field.get("conditions"), fieldPath + ".conditions");
        }
        return step.build();
    }

    private static <T> S2FieldStep.BuilderFieldStep<T> readRules(S2FieldStep.BuilderFieldStep<T> step, Object rulesNode,
            String path) {
        if (rulesNode == null) {
            return step;
        }
        List<?> rules = asList(rulesNode, path);
        for (int i = 0; i < rules.size(); i++) {
            String rulePath = path + "[" + i + "]";
            Map<String, Object> rule = asMap(rules.get(i), rulePath);
            checkKeys(rule, RULE_KEYS, rulePath);
            S2RuleType type = ruleType(asString(rule.get("type"), rulePath + ".type", true), rulePath + ".type");
            Object value = rule.get("value");
            if (type == S2RuleType.NESTED || type == S2RuleType.EACH) {
                if (!rule.containsKey("nestedRules")) {
                    throw error(rulePath + ".nestedRules", "required for " + type);
                }
                value = readFields(rule.get("nestedRules"), rulePath + ".nestedRules");
            } else if (rule.containsKey("nestedRules")) {
                throw error(rulePath + ".nestedRules", "only allowed for NESTED/EACH");
            }
            String key = asString(rule.get("key"), rulePath + ".key", false);
            S2RuleStep.BuilderRuleStep<T> ruleStep;
            try {
                ruleStep = step.rule(type, value, key);
            } catch (RuntimeException e) {
                throw error(rulePath + ".value", e.getMessage(), e);
            }
            String message = asString(rule.get("message"), rulePath + ".message", false);
            if (message != null) {
                ruleStep = ruleStep.message(message);
            }
            if (rule.get("messages") != null) {
                Map<String, Object> messages = asMap(rule.get("messages"), rulePath + ".messages");
                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    String template = asString(entry.getValue(), rulePath + ".messages." + entry.getKey(), true);
                    ruleStep = ruleStep.message(template, Locale.forLanguageTag(entry.getKey()));
                }
            }
            step = ruleStep;
        }
        return step;
    }

    private static <T> S2FieldStep.BuilderFieldStep<T> readConditions(S2FieldStep.BuilderFieldStep<T> step,
            Object conditionsNode, String path) {
        if (conditionsNode == null) {
            return step;
        }
        List<?> groups = asList(conditionsNode, path);
        for (int g = 0; g < groups.size(); g++) {
            String groupPath = path + "[" + g + "]";
            List<?> group = asList(groups.get(g), groupPath);
            if (group.isEmpty()) {
                throw error(groupPath, "a condition group must not be empty");
            }
            S2ConditionStep.BuilderConditionStep<T> conditionStep = null;
            for (int c = 0; c < group.size(); c++) {
                String condPath = groupPath + "[" + c + "]";
                Map<String, Object> cond = asMap(group.get(c), condPath);
                checkKeys(cond, CONDITION_KEYS, condPath);
                String field = asString(cond.get("field"), condPath + ".field", true);
                String op = asString(cond.get("op"), condPath + ".op", false);
                S2Operator operator = op == null ? S2Operator.EQ : operator(op, condPath + ".op");
                try {
                    conditionStep = conditionStep == null ? step.when(field, operator, cond.get("value"))
                            : conditionStep.and(field, operator, cond.get("value"));
                } catch (RuntimeException e) {
                    throw error(condPath + ".value", e.getMessage(), e);
                }
            }
            step = conditionStep;
        }
        return step;
    }

    private static S2RuleType ruleType(String name, String path) {
        try {
            return S2RuleType.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw error(path, "unknown rule type '" + name + "'");
        }
    }

    private static S2Operator operator(String name, String path) {
        try {
            return S2Operator.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw error(path, "unknown operator '" + name + "'");
        }
    }

    private static void checkKeys(Map<String, Object> node, Set<String> allowed, String path) {
        for (String key : node.keySet()) {
            if (!allowed.contains(key)) {
                throw error(path + "." + key, "unknown key (allowed: " + String.join(", ", new java.util.TreeSet<>(allowed)) + ")");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object node, String path) {
        if (!(node instanceof Map)) {
            throw error(path, "expected an object");
        }
        return (Map<String, Object>) node;
    }

    private static List<?> asList(Object node, String path) {
        if (!(node instanceof List<?> list)) {
            throw error(path, "expected an array");
        }
        return list;
    }

    private static String asString(Object node, String path, boolean required) {
        if (node == null) {
            if (required) {
                throw error(path, "required");
            }
            return null;
        }
        if (!(node instanceof String s)) {
            throw error(path, "expected a string");
        }
        if (required && s.isBlank()) {
            throw error(path, "must not be blank");
        }
        return s;
    }

    private static IllegalArgumentException error(String path, String message) {
        return error(path, message, null);
    }

    private static IllegalArgumentException error(String path, String message, Throwable cause) {
        return new IllegalArgumentException("[S2Validator.fromJson] " + path + ": " + message, cause);
    }
}
