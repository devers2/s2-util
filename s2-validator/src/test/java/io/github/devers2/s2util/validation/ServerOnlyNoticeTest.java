package io.github.devers2.s2util.validation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import io.github.devers2.s2util.log.S2LoggerFactory;

/**
 * The rules JSON logs once per definition site that custom lambda rules are server-only.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 규칙 JSON 생성 시 커스텀 람다 규칙이 서버 전용이라는 안내를 정의 위치마다 한 번 기록하는지 확인합니다.
 */
public class ServerOnlyNoticeTest {

    private final List<String> infos = new CopyOnWriteArrayList<>();
    private Object originalFactory;

    @BeforeEach
    void captureLogs() throws Exception {
        Field factory = S2LogManager.class.getDeclaredField("factory");
        factory.setAccessible(true);
        originalFactory = factory.get(null);
        S2LogManager.setLoggerFactory(new S2LoggerFactory() {
            @Override
            public <T> S2Logger getLogger(Class<T> clazz) {
                return getLogger(clazz.getName());
            }

            @Override
            public S2Logger getLogger(String name) {
                return new S2Logger() {
                    @Override
                    public void log(String level, String message, Object[] args) {
                        if ("INFO".equals(level) && name.endsWith("S2RulesJsonWriter")) {
                            infos.add(message + " " + java.util.Arrays.toString(args));
                        }
                    }

                    @Override
                    public boolean isDebugEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isInfoEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isWarnEnabled() {
                        return true;
                    }

                    @Override
                    public boolean isErrorEnabled() {
                        return true;
                    }
                };
            }
        });
        S2RulesJsonWriter.resetServerOnlyNotices();
    }

    @AfterEach
    void restoreLogs() {
        S2LogManager.setLoggerFactory((S2LoggerFactory) originalFactory);
        S2RulesJsonWriter.resetServerOnlyNotices();
    }

    private static S2Validator<Map<String, Object>> withLambda() {
        return S2Validator.<Map<String, Object>>builder()
                .field("code", "코드").rule((String s) -> s.startsWith("ADM-"))
                .build();
    }

    @Test
    void loggedOncePerDefinitionSiteEvenWhenValidatorsAreRebuilt() {
        // bind(rules()) rebuilds the validator on every request; the notice must not repeat | bind(rules())는 요청마다 검증기를 다시 만들지만 안내는 반복되면 안 됨
        for (int i = 0; i < 5; i++) {
            withLambda().getRulesJson(Locale.KOREAN);
        }
        Assertions.assertEquals(1, infos.size(), infos.toString());
        Assertions.assertTrue(infos.get(0).contains("code"), infos.get(0));
    }

    @Test
    void differentDefinitionSitesAreEachLogged() {
        withLambda().getRulesJson(Locale.KOREAN);
        S2Validator.<Map<String, Object>>builder()
                .field("code", "코드").rule((String s, Map<String, Object> t) -> !s.isEmpty())
                .build().getRulesJson(Locale.KOREAN);
        Assertions.assertEquals(2, infos.size(), infos.toString());
    }

    @Test
    void notLoggedWithoutCustomRulesOrForServerValidation() {
        S2Validator.<Map<String, Object>>builder().field("name", "이름").rule(S2RuleType.REQUIRED).build()
                .getRulesJson(Locale.KOREAN);
        withLambda().validate(Map.of("code", "USR"), e -> {});
        Assertions.assertEquals(0, infos.size(), infos.toString());
    }

    @Test
    void lambdaFieldIsStillExportedWithoutItsServerOnlyRule() {
        String json = withLambda().getRulesJson(Locale.KOREAN);
        Assertions.assertTrue(json.contains("\"name\":\"code\"") && json.contains("\"rules\":[]"), json);
    }
}
