package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Keeps the browser test rules ({@code browser-test/demo/rules.json}) in sync with the server validators.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 브라우저 시험 규칙({@code browser-test/demo/rules.json})이 서버 검증기와 같게 유지되는지 확인합니다.
 */
public class BrowserDemoRulesTest {

    @Test
    void committedBrowserDemoRulesAreUpToDate() throws IOException {
        Assertions.assertTrue(Files.exists(BrowserDemoRules.OUTPUT), "missing " + BrowserDemoRules.OUTPUT.toAbsolutePath()
                + " — run ./gradlew :s2-validator:writeBrowserDemoRules");
        String committed = Files.readString(BrowserDemoRules.OUTPUT, StandardCharsets.UTF_8);
        Assertions.assertEquals(BrowserDemoRules.render(), committed,
                "browser-test/demo/rules.json is out of date — run ./gradlew :s2-validator:writeBrowserDemoRules");
    }
}
