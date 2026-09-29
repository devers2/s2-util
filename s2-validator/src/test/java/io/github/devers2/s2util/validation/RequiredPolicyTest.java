package io.github.devers2.s2util.validation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Locks the documented required-check policy: a field without rules is required; once any rule is added, REQUIRED
 * must be stated explicitly.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 문서화된 필수 검증 정책을 고정합니다. 규칙 없는 필드는 필수이며, 규칙을 하나라도 추가하면 REQUIRED 를 명시해야 합니다.
 */
public class RequiredPolicyTest {

    private static Map<String, Object> input(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    @Test
    void fieldWithoutRulesIsRequired() {
        Assertions.assertFalse(S2Validator.of(input("name", ""), false).field("name", "이름").validate());
        Assertions.assertFalse(S2Validator.of(new HashMap<String, Object>(), false).field("name", "이름").validate());
        Assertions.assertTrue(S2Validator.of(input("name", "홍길동"), false).field("name", "이름").validate());
    }

    @Test
    void addingAnyRuleDropsTheImplicitRequired() {
        // A format rule skips empty values, so an empty email passes | 형식 규칙은 빈 값을 건너뛰므로 빈 이메일은 통과
        Assertions.assertTrue(S2Validator.of(input("email", ""), false)
                .field("email", "이메일").rule(S2RuleType.EMAIL).validate());
        Assertions.assertFalse(S2Validator.of(input("email", ""), false)
                .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL).validate());
    }

    @Test
    void customRuleOnlyFieldIsNotImplicitlyRequired() {
        Assertions.assertTrue(S2Validator.of(input("code", ""), false)
                .field("code", "코드").rule((String s) -> s.startsWith("ADM-")).validate());
    }

    @Test
    void rulesJsonFollowsTheSamePolicy() {
        String noRules = S2Validator.<Map<String, Object>>builder().field("name", "이름").build()
                .getRulesJson(Locale.KOREAN);
        Assertions.assertTrue(noRules.contains("\"type\":\"REQUIRED\""), noRules);

        String withRule = S2Validator.<Map<String, Object>>builder().field("email", "이메일").rule(S2RuleType.EMAIL)
                .build().getRulesJson(Locale.KOREAN);
        Assertions.assertFalse(withRule.contains("\"type\":\"REQUIRED\""), withRule);
    }
}
