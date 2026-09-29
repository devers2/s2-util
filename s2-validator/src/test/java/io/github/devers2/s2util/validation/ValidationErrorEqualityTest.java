package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

/**
 * {@link S2ValidationError} compares its message arguments by content.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2ValidationError}가 메시지 인자를 내용으로 비교하는지 확인합니다.
 */
public class ValidationErrorEqualityTest {

    @Test
    void equalContentIsEqual() {
        var a = new S2ValidationError("name", "valid.err.required", new Object[] { "이름" }, "이름은 필수 입력 항목입니다.");
        var b = new S2ValidationError("name", "valid.err.required", new Object[] { "이름" }, "이름은 필수 입력 항목입니다.");
        Assertions.assertEquals(a, b);
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertTrue(Set.of(a).contains(b));
        Assertions.assertTrue(a.toString().contains("errorArgs=[이름]"), a.toString());
    }

    @Test
    void differentArgsOrNullArgsAreHandled() {
        var a = new S2ValidationError("qty", "valid.err.min", new Object[] { "수량", 1 }, "m");
        Assertions.assertNotEquals(a, new S2ValidationError("qty", "valid.err.min", new Object[] { "수량", 2 }, "m"));
        var n1 = new S2ValidationError("x", "c", null, "m");
        var n2 = new S2ValidationError("x", "c", null, "m");
        Assertions.assertEquals(n1, n2);
        Assertions.assertNotEquals(n1, a);
    }

    @Test
    void errorsFromTwoValidationRunsAreEqual() {
        Map<String, Object> input = new HashMap<>();
        input.put("qty", 0);
        List<S2ValidationError> first = new ArrayList<>();
        List<S2ValidationError> second = new ArrayList<>();
        S2Validator.of(input, false).field("qty", "수량").rule(S2RuleType.MIN_VALUE, 1).validate(first::add);
        S2Validator.of(input, false).field("qty", "수량").rule(S2RuleType.MIN_VALUE, 1).validate(second::add);
        Assertions.assertEquals(1, first.size());
        Assertions.assertEquals(first, second);
    }
}
