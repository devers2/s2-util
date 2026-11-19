package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 규칙 JSON 이 아닌 스크립트 자체의 시스템 메시지는 페이지 언어(html lang → 브라우저 언어)를 따르고,
 * 언어를 알 수 없으면(브라우저 밖) 기존처럼 한국어를 쓴다.
 */
class SystemMessageLanguageTest {

    private static String systemError(String setup) throws IOException {
        String source;
        try (InputStream is = SystemMessageLanguageTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Context js = Context.newBuilder("js").allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false").build()) {
            js.eval("js", "var console = { log() {}, warn() {}, error() {} };" + setup);
            js.eval("js", source.replaceAll("export\\s+const\\s+", "const ") + "\nglobalThis.__S2Validator = S2Validator;\n");
            return js.eval("js", "__S2Validator.check('not json', {}).__system_error__[0]").asString();
        }
    }

    @Test
    void followsPageLanguage() throws IOException {
        Assertions.assertTrue(systemError("").contains("검증 규칙"), "no language → Korean");
        Assertions.assertTrue(systemError("globalThis.navigator = { language: 'ko-KR' };").contains("검증 규칙"));
        Assertions.assertTrue(systemError("globalThis.navigator = { language: 'en-US' };").contains("validation rules"));
        // html lang 이 브라우저 언어보다 우선
        Assertions.assertTrue(systemError("globalThis.navigator = { language: 'ko-KR' };"
                + "globalThis.document = { documentElement: { lang: 'en' }, readyState: 'complete',"
                + " querySelectorAll() { return []; }, querySelector() { return null; }, addEventListener() {} };"
                + "globalThis.MutationObserver = class { observe() {} disconnect() {} };")
                .contains("validation rules"));
    }
}
