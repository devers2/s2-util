package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

/**
 * Circular reference and nesting depth guards for NESTED/EACH validation.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * NESTED/EACH 검증의 순환 참조·중첩 깊이 보호를 확인합니다.
 */
public class NestedGuardTest {

    @Test
    void circularReferenceMessageIsLocalizedAndUsesBundleKey() {
        Map<String, Object> self = new HashMap<>();
        self.put("name", "x");
        self.put("child", self);
        S2Validator<Object> nested = S2Validator.builder().field("name", "이름").build();
        S2Validator<Object> validator = S2Validator.builder().field("child", "자식").rule(S2RuleType.NESTED, nested).build();

        List<S2ValidationError> en = new ArrayList<>();
        validator.validate(self, en::add, Locale.ENGLISH);
        Assertions.assertEquals(1, en.size());
        Assertions.assertEquals("valid.err.circular", en.get(0).errorCode());
        Assertions.assertEquals("Circular reference detected at 자식.", en.get(0).defaultMessage());

        List<S2ValidationError> ko = new ArrayList<>();
        validator.validate(self, ko::add, Locale.KOREAN);
        Assertions.assertEquals("자식에서 순환 참조가 감지되었습니다.", ko.get(0).defaultMessage());
    }

    @Test
    void nestingDeeperThanTheLimitIsReportedInsteadOfOverflowingTheStack() {
        int levels = S2Validator.Runner.MAX_NESTED_DEPTH + 5;

        S2Validator<Object> validator = S2Validator.builder().field("name", "이름").build();
        for (int i = 0; i < levels; i++) {
            validator = S2Validator.builder().field("child", "자식").rule(S2RuleType.NESTED, validator).build();
        }
        Map<String, Object> leaf = new HashMap<>();
        leaf.put("name", "x");
        Map<String, Object> root = leaf;
        for (int i = 0; i < levels; i++) {
            Map<String, Object> parent = new HashMap<>();
            parent.put("child", root);
            root = parent;
        }

        List<S2ValidationError> errors = new ArrayList<>();
        boolean valid = validator.validate(root, errors::add, Locale.ENGLISH);

        Assertions.assertFalse(valid);
        Assertions.assertEquals(1, errors.size(), errors.toString());
        Assertions.assertEquals("valid.err.maxdepth", errors.get(0).errorCode());
        Assertions.assertTrue(errors.get(0).defaultMessage().contains(String.valueOf(S2Validator.Runner.MAX_NESTED_DEPTH)),
                errors.get(0).defaultMessage());
    }

    @Test
    void nestingWithinTheLimitIsValidated() {
        S2Validator<Object> validator = S2Validator.builder().field("name", "이름").build();
        for (int i = 0; i < 10; i++) {
            validator = S2Validator.builder().field("child", "자식").rule(S2RuleType.NESTED, validator).build();
        }
        Map<String, Object> root = new HashMap<>();
        root.put("name", "");
        for (int i = 0; i < 10; i++) {
            Map<String, Object> parent = new HashMap<>();
            parent.put("child", root);
            root = parent;
        }

        List<S2ValidationError> errors = new ArrayList<>();
        Assertions.assertFalse(validator.validate(root, errors::add, Locale.ENGLISH));
        Assertions.assertEquals(1, errors.size());
        Assertions.assertEquals(S2RuleType.REQUIRED.getErrorMessageKey(), errors.get(0).errorCode());
        Assertions.assertTrue(errors.get(0).fieldName().endsWith("child.name"), errors.get(0).fieldName());
    }
}
