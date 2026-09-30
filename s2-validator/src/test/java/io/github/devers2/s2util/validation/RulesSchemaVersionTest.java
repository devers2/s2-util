package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The rules JSON carries {@code schemaVersion}; s2.validator.js reads it, still accepts the pre-2.0.0 bare array, and
 * warns once about a newer version.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 규칙 JSON 이 {@code schemaVersion}을 담고, s2.validator.js 가 이를 읽으며 2.0.0 이전 배열 형식도 받고, 더 높은 버전은 한 번 경고하는지
 * 확인합니다.
 */
public class RulesSchemaVersionTest {

    private static Context js;
    private static String jsSource;

    @BeforeAll
    static void setUp() throws IOException {
        try (InputStream is = RulesSchemaVersionTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            jsSource = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        js = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).option("engine.WarnInterpreterOnly", "false").build();
        js.eval("js", """
                var document = { querySelector: () => null, querySelectorAll: () => [], documentElement: {},
                    readyState: 'complete', addEventListener: () => {}, createElement: () => ({}) };
                var window = {};
                var MutationObserver = function() { this.observe = () => {}; };
                globalThis.__warnings = [];
                var console = { log: () => {}, error: () => {}, warn: (m) => __warnings.push(String(m)) };
                class HTMLFormElement {
                    constructor(elements) { this.elements = elements; this.dataset = {}; }
                    querySelectorAll(sel) {
                        const m = sel.match(/^\\[name="(.+)"\\]$/);
                        return m ? this.elements.filter((e) => e.name === m[1]) : [];
                    }
                    querySelector(sel) { return this.querySelectorAll(sel)[0] ?? null; }
                    reportValidity() { return true; }
                }
                globalThis.__emptyNameForm = () => new HTMLFormElement([{ name: 'name', type: 'text', value: '',
                    offsetParent: {}, setCustomValidity: () => {}, addEventListener: () => {} }]);
                """);
        String module = jsSource.replaceAll("export\\s+const\\s+", "const ").replace("initS2Validator();", "");
        js.eval("js", module + "\nglobalThis.__S2Validator = S2Validator;\n");
    }

    @AfterAll
    static void tearDown() {
        js.close();
    }

    private static String rulesJson() {
        return S2Validator.<Map<String, Object>>builder().field("name", "이름").rule(S2RuleType.REQUIRED).build()
                .getRulesJson(Locale.KOREAN);
    }

    private static Value validate(String rulesJson) {
        return js.eval("js", "(r) => __S2Validator.validate(__emptyNameForm(), r)").execute(rulesJson);
    }

    @Test
    void serverWritesSchemaVersionAndFields() {
        Assertions.assertTrue(rulesJson().startsWith("{\"schemaVersion\":" + S2RulesJsonWriter.SCHEMA_VERSION + ",\"fields\":["),
                rulesJson());
    }

    @Test
    void javaAndJavaScriptVersionsMatch() {
        Matcher m = Pattern.compile("const SCHEMA_VERSION = (\\d+);").matcher(jsSource);
        Assertions.assertTrue(m.find(), "SCHEMA_VERSION not found in s2.validator.js");
        Assertions.assertEquals(S2RulesJsonWriter.SCHEMA_VERSION, Integer.parseInt(m.group(1)));
    }

    @Test
    void currentAndLegacyFormatsValidateTheSame() {
        String current = rulesJson();
        String legacy = current.substring(current.indexOf('['), current.lastIndexOf(']') + 1);
        Assertions.assertTrue(validate(current).hasMember("name"), "current format");
        Assertions.assertTrue(validate(legacy).hasMember("name"), "pre-2.0.0 bare array");
    }

    @Test
    void newerVersionWarnsOnceAndStillValidates() {
        js.eval("js", "__warnings.length = 0");
        String newer = rulesJson().replace("\"schemaVersion\":" + S2RulesJsonWriter.SCHEMA_VERSION,
                "\"schemaVersion\":" + (S2RulesJsonWriter.SCHEMA_VERSION + 90));
        Assertions.assertTrue(validate(newer).hasMember("name"));
        Assertions.assertTrue(validate(newer).hasMember("name"));
        Value warnings = js.eval("js", "__warnings");
        Assertions.assertEquals(1, warnings.getArraySize(), warnings.toString());
        Assertions.assertTrue(warnings.getArrayElement(0).asString().contains("schemaVersion"));
    }
}
