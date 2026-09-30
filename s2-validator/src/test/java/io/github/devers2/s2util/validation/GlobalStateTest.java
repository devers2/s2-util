package io.github.devers2.s2util.validation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import io.github.devers2.s2util.core.S2ThreadUtil;
import io.github.devers2.s2util.validation.spring.S2BindValidator;

public class GlobalStateTest {

    @BeforeEach
    @AfterEach
    void tearDown() {
        S2Validator.resetAll();
    }

    @Test
    @DisplayName("S2BindValidator.bind()는 등록부를 거치지 않고 검증 및 JSON 생성을 수행한다")
    void testBindValidatorOf() {
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("username", "사용자명").rule(S2RuleType.REQUIRED)
                .build();

        S2BindValidator.BoundContext<Map<String, Object>> context = S2BindValidator.bind(validator);

        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("username", "");

        BindingResult bindingResult = new MapBindingResult(invalidData, "user");
        context.validate(invalidData, bindingResult);

        Assertions.assertTrue(bindingResult.hasErrors());
        Assertions.assertEquals(1, bindingResult.getErrorCount());
        Assertions.assertEquals("username", bindingResult.getFieldError().getField());

        String json = context.getRulesJson();
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("username"));

    }

    @Test
    @DisplayName("S2Validator.getFields()는 불변 뷰를 반환하여 외부 변조를 방지한다")
    void testGetFieldsUnmodifiable() {
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("email", "이메일").rule(S2RuleType.EMAIL)
                .build();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            validator.getFields().clear();
        });
    }

    @Test
    @DisplayName("getRulesJson()·getRulesJson(null)은 기본 로케일로, getRulesJson(locale)은 지정 로케일로 메시지를 만든다")
    void testGetRulesJsonLocale() {
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("userId", "아이디").rule(S2RuleType.REQUIRED)
                .build();

        S2Validator.setDefaultLocale(Locale.ENGLISH);
        Assertions.assertTrue(validator.getRulesJson().contains("is required"), validator.getRulesJson());
        Assertions.assertEquals(validator.getRulesJson(), validator.getRulesJson(null));
        Assertions.assertTrue(validator.getRulesJson(Locale.KOREAN).contains("필수 입력"), validator.getRulesJson(Locale.KOREAN));
    }

    @Test
    @DisplayName("전역 등록부와 S2ValidatorFactory 는 2.0.0 에서 삭제되었다 (S2BindValidator.bind, S2Validator.getRulesJson 만 제공)")
    void testRegistryApiIsRemoved() {
        Assertions.assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.github.devers2.s2util.validation.S2ValidatorFactory"));
        Assertions.assertTrue(java.util.Arrays.stream(S2BindValidator.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("context")));
        Assertions.assertFalse(java.lang.reflect.Modifier.isPublic(S2RulesJsonWriter.class.getModifiers()),
                "JSON writer must stay internal");
    }

    @Test
    @DisplayName("검증기 생성 후 기본 로케일을 변경하면 locale 인자 없는 validate()에 동적으로 반영된다")
    void testDynamicLocaleResolution() {
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("userId", "아이디").rule(S2RuleType.REQUIRED)
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("userId", "");

        // 1. 기본 로케일 한국어 상태 검증
        S2Validator.setDefaultLocale(Locale.KOREAN);
        java.util.List<String> koMessages = new java.util.ArrayList<>();
        validator.validate(data, err -> koMessages.add(err.defaultMessage()));
        Assertions.assertFalse(koMessages.isEmpty());
        Assertions.assertTrue(koMessages.get(0).contains("필수"));

        // 2. 기본 로케일을 영어로 변경 후 동일 인스턴스로 재검증
        S2Validator.setDefaultLocale(Locale.ENGLISH);
        java.util.List<String> enMessages = new java.util.ArrayList<>();
        validator.validate(data, err -> enMessages.add(err.defaultMessage()));
        Assertions.assertFalse(enMessages.isEmpty());
        Assertions.assertTrue(enMessages.get(0).contains("required"));

        // 3. resetDefaultLocale() 후 복원 확인
        S2Validator.resetDefaultLocale();
    }

    @Test
    @DisplayName("S2ThreadUtil.shutdownCommonExecutor() 후 getCommonExecutor() 호출 시 재생성된다")
    void testThreadUtilRecreation() throws ExecutionException, InterruptedException {
        Future<Integer> f1 = S2ThreadUtil.getCommonExecutor().submit(() -> 42);
        Assertions.assertEquals(42, f1.get());

        S2ThreadUtil.shutdownCommonExecutor();

        Future<Integer> f2 = S2ThreadUtil.getCommonExecutor().submit(() -> 99);
        Assertions.assertEquals(99, f2.get());
    }
}
