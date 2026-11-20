package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.core.S2Util;

/**
 * 공백 정의가 세 곳에서 같은지 BMP 전체 문자로 확인한다: s2-core {@code S2Util.isEmpty}(빈 값 판단),
 * s2-validator {@link S2Whitespace}(판정 전 trim), 브라우저 JS {@code String.prototype.trim()}.
 * 하나라도 다르면 공백만 있는 값의 REQUIRED·형식 규칙이 서버와 브라우저에서 갈린다(예: U+001F).
 */
class WhitespaceDefinitionParityTest {

    @Test
    void coreValidatorAndJsAgreeOnWhitespace() {
        try (Context js = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
            Value jsBlank = js.eval("js", "(s) => s.trim() === ''");
            List<String> diff = new ArrayList<>();
            for (int c = 0; c <= 0xFFFF; c++) {
                if (c >= 0xD800 && c <= 0xDFFF) {
                    continue; // 홀로 있는 서로게이트는 문자로 넘길 수 없음
                }
                String s = String.valueOf((char) c);
                boolean core = S2Util.isEmpty(s);
                boolean validator = S2Whitespace.isBlank(s);
                boolean browser = jsBlank.execute(s).asBoolean();
                if (core != validator || validator != browser) {
                    diff.add(String.format("U+%04X core=%s S2Whitespace=%s js=%s", c, core, validator, browser));
                }
            }
            Assertions.assertEquals(List.of(), diff);
        }
    }
}
