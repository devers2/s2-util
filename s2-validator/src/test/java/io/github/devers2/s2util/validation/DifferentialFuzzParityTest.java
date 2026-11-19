package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.json.S2JsonUtil;

/**
 * 서버(Java)와 클라이언트(s2.validator.js)에 같은 무작위 입력을 넣어 판정을 비교하는 차분 퍼즈 시험.
 * <p>
 * {@link ServerClientParityTest}는 사람이 고른 입력만 보므로 공백·유니코드·날짜 변형 같은 경계 입력의 불일치를 놓친다. 이 시험은 고정
 * 시드의 무작위 입력으로 같은 규칙의 판정이 양쪽에서 같은지 확인한다(실패하면 재현 가능한 입력을 보여 줌).
 * </p>
 */
class DifferentialFuzzParityTest {

    /** 고정 시드 — 실패를 그대로 재현하기 위함 */
    private static final long SEED = 20261119L;

    /** 규칙·조건마다 만들 무작위 입력 수 */
    private static final int SAMPLES = 400;

    /** 무작위 문자열 재료: 숫자·영문·한글·공백류(NBSP·전각·탭)·구분자·부호·서로게이트 쌍 제외 */
    private static final String[] PIECES = {
            "0", "1", "2", "5", "9", "12", "2024", "01", "13", "29", "31", "a", "Z", "x", "가", "힣", "ㄱ",
            " ", "  ", "\t", " ", "　", "-", ".", ",", "+", "e", "E", "@", "_", "(", ")", "T", ":", "/",
            "!", "#", "%", "'", "\"", "\\", "abc", "test", "com", "kr" };

    /** 그럴듯한 값 틀 — 일부를 무작위로 바꿔 경계 근처를 노린다 */
    private static final String[] TEMPLATES = {
            "2024-01-31", "20240229", "2023.02.29", "2024-01-01T09:30", "2024-01-01 23:59:59", "2024-01-01T24:00",
            "010-1234-5678", "02-123-4567", "0212345678", "+82 10 1234 5678", "user@example.com", "a.b@c.co.kr",
            "12345", "123-45-67890", "1234567890", "123-45-67890-1", "900101-1234567", "0001014234567",
            "Passw0rd!", "abcdefgh1!", "홍길동 Hong-2 (A),B.", "  25  ", "-3.5", "1e3", "1,000", ".5", "5.", "NaN",
            "true", "on", "false", "off", "", "   " };

    private static Context jsContext;
    private static Value validateCheck;
    private static Value conditionSatisfied;

    @BeforeAll
    static void setupJsEngine() throws IOException {
        jsContext = Context.newBuilder("js").allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false").build();
        String source;
        try (InputStream is = DifferentialFuzzParityTest.class
                .getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            Assertions.assertNotNull(is, "s2.validator.js not found on classpath");
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        // ESM export 제거, DOM 초기화 생략
        source = source.replaceAll("export\\s+const\\s+", "const ").replace("initS2Validator();", "");
        jsContext.eval("js", """
                var console = { log() {}, warn() {}, error() {} };
                function Blob(parts) {
                  this.size = new TextEncoder().encode(parts.map(String).join('')).length;
                }
                """);
        if (jsContext.eval("js", "typeof TextEncoder").asString().equals("undefined")) {
            // GraalJS 에 TextEncoder 가 없으면 UTF-8 바이트 수를 직접 계산
            jsContext.eval("js", """
                    function Blob(parts) {
                      var s = parts.map(String).join(''), n = 0;
                      for (var i = 0; i < s.length; i++) {
                        var c = s.charCodeAt(i);
                        if (c < 0x80) n += 1; else if (c < 0x800) n += 2;
                        else if (c >= 0xD800 && c <= 0xDBFF && i + 1 < s.length) { n += 4; i++; } else n += 3;
                      }
                      this.size = n;
                    }
                    """);
        }
        jsContext.eval("js", source + """

                globalThis.__check = (valueJson, ruleJson, dataJson) =>
                  validateCheck(JSON.parse(valueJson), JSON.parse(ruleJson), JSON.parse(dataJson), '', 'f');
                globalThis.__cond = (ruleJson, dataJson) => isConditionSatisfied(JSON.parse(ruleJson), JSON.parse(dataJson));
                """);
        validateCheck = jsContext.eval("js", "globalThis.__check");
        conditionSatisfied = jsContext.eval("js", "globalThis.__cond");
    }

    @AfterAll
    static void tearDown() {
        if (jsContext != null) {
            jsContext.close();
        }
    }

    /** 무작위 문자열: 틀을 바꾸거나 조각을 이어 붙임 */
    private static String randomValue(Random random) {
        if (random.nextInt(4) == 0) {
            return null;
        }
        if (random.nextBoolean()) {
            StringBuilder sb = new StringBuilder(TEMPLATES[random.nextInt(TEMPLATES.length)]);
            int edits = random.nextInt(3);
            for (int i = 0; i < edits && sb.length() > 0; i++) {
                int at = random.nextInt(sb.length());
                switch (random.nextInt(3)) {
                    case 0 -> sb.deleteCharAt(at);
                    case 1 -> sb.insert(at, PIECES[random.nextInt(PIECES.length)]);
                    default -> sb.replace(at, at + 1, PIECES[random.nextInt(PIECES.length)]);
                }
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder();
        int parts = random.nextInt(8);
        for (int i = 0; i < parts; i++) {
            sb.append(PIECES[random.nextInt(PIECES.length)]);
        }
        return sb.toString();
    }

    /** 무작위 여러 값: 0~3개 */
    private static List<String> randomValues(Random random) {
        List<String> values = new ArrayList<>();
        int size = random.nextInt(4);
        for (int i = 0; i < size; i++) {
            values.add(randomValue(random));
        }
        return values;
    }

    /** 비교할 규칙과 기준값 (교차 필드 규칙의 기준값은 대상 필드명 "other") */
    private static Map<String, Object[]> ruleConfigs() {
        Map<String, Object[]> configs = new LinkedHashMap<>();
        for (S2RuleType type : S2RuleType.values()) {
            Object criterion = switch (type) {
                case LENGTH, MAX_LENGTH, MAX_BYTE -> 5;
                case MIN_SIZE, MAX_SIZE -> 2;
                case MIN_LENGTH, MIN_BYTE -> 3;
                case MIN_VALUE -> 10;
                case MAX_VALUE -> "100";
                case REGEX -> "[a-z]+\\d*";
                case JUMIN, BIZRNO -> true;
                case EQUALS_FIELD, DATE_AFTER, DATE_BEFORE -> "other";
                default -> null;
            };
            if (type != S2RuleType.NESTED && type != S2RuleType.EACH) {
                configs.put(type.name(), new Object[] { type, criterion });
            }
        }
        // 검증번호 검사를 끈 주민·사업자 번호도 비교
        configs.put("JUMIN(false)", new Object[] { S2RuleType.JUMIN, null });
        configs.put("BIZRNO(false)", new Object[] { S2RuleType.BIZRNO, null });
        return configs;
    }

    @Test
    @DisplayName("규칙마다 무작위 입력의 서버·클라이언트 판정이 같다")
    void rulesJudgeRandomInputsTheSame() {
        Random random = new Random(SEED);
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, Object[]> config : ruleConfigs().entrySet()) {
            S2RuleType type = (S2RuleType) config.getValue()[0];
            Object criterion = config.getValue()[1];
            S2Rule rule = new S2Rule(type, criterion);
            Map<String, Object> jsRule = new LinkedHashMap<>();
            jsRule.put("type", type.name());
            jsRule.put("value", criterion);
            jsRule.put("regex", type.getRegex());
            String ruleJson = S2JsonUtil.toJson(jsRule);

            for (int i = 0; i < SAMPLES; i++) {
                // 4건 중 1건은 여러 값(다중 선택·String[] 파라미터 같은 배열)
                Object value = random.nextInt(4) == 0 ? randomValues(random) : randomValue(random);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("f", value);
                data.put("other", randomValue(random));
                boolean server = rule.isValid(value, data);
                boolean client = validateCheck.execute(S2JsonUtil.toJson(value), ruleJson, S2JsonUtil.toJson(data))
                        .asBoolean();
                if (server != client) {
                    mismatches.add(config.getKey() + " value=" + S2JsonUtil.toJson(value) + " other="
                            + S2JsonUtil.toJson(data.get("other")) + " server=" + server + " client=" + client);
                }
            }
        }
        Assertions.assertTrue(mismatches.isEmpty(),
                mismatches.size() + " mismatches (first 20):\n" + String.join("\n", mismatches.subList(0, Math.min(20, mismatches.size()))));
    }

    @Test
    @DisplayName("조건 연산자마다 무작위 입력의 서버·클라이언트 판정이 같다")
    void conditionsJudgeRandomInputsTheSame() {
        Random random = new Random(SEED + 1);
        List<String> mismatches = new ArrayList<>();
        for (S2Operator op : S2Operator.values()) {
            for (int i = 0; i < SAMPLES; i++) {
                Object criterion;
                if (op.isUnary()) {
                    criterion = null;
                } else if (op.isList()) {
                    criterion = List.of(String.valueOf(randomValue(random)), String.valueOf(randomValue(random)));
                } else if (op.isNumeric()) {
                    criterion = random.nextInt(200) - 50;
                } else {
                    criterion = randomValue(random);
                }
                String value = randomValue(random);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("f", value);

                boolean server = new S2Condition("f", op, criterion).isSatisfied(data);
                Map<String, Object> cond = new LinkedHashMap<>();
                cond.put("field", "f");
                cond.put("op", op.name());
                cond.put("value", criterion);
                String ruleJson = S2JsonUtil.toJson(Map.of("conditions", List.of(List.of(cond))));
                boolean client = conditionSatisfied.execute(ruleJson, S2JsonUtil.toJson(data)).asBoolean();
                if (server != client) {
                    mismatches.add(op + " value=" + S2JsonUtil.toJson(value) + " criterion=" + S2JsonUtil.toJson(criterion)
                            + " server=" + server + " client=" + client);
                }
            }
        }
        Assertions.assertTrue(mismatches.isEmpty(),
                mismatches.size() + " mismatches (first 20):\n" + String.join("\n", mismatches.subList(0, Math.min(20, mismatches.size()))));
    }
}
