package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 커밋된 공개 데모 규칙(browser-test/site/rules.json)이 서버 검증기와 같은지 확인한다.
 */
class PublicDemoRulesTest {

    @Test
    void committedPublicDemoRulesAreUpToDate() throws IOException {
        Assertions.assertTrue(Files.exists(PublicDemoRules.OUTPUT), "missing " + PublicDemoRules.OUTPUT.toAbsolutePath()
                + " — run ./gradlew :s2-validator:writePublicDemoRules");
        Assertions.assertEquals(PublicDemoRules.render(), Files.readString(PublicDemoRules.OUTPUT, StandardCharsets.UTF_8),
                "browser-test/site/rules.json is out of date — run ./gradlew :s2-validator:writePublicDemoRules");
    }
}
