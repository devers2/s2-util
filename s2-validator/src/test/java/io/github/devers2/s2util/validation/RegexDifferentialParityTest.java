package io.github.devers2.s2util.validation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;


/**
 * 같은 정규식·입력을 Java {@code matcher.matches()} 와 s2.validator.js 의 {@code fullMatchRegExp} 로 평가해,
 * {@link S2RegexCompatibility} 가 허용한 패턴은 판정이 같은지 확인한다(허용하지 않은 패턴은 규칙 JSON 으로 내보내지 않음).
 * JDK 17 의 {@code \b} 는 비ASCII 문자를 단어 문자로 보는 JDK 버그(JDK-8264160, 19 에서 수정)가 있어 그런 입력은 넣지 않는다.
 */
class RegexDifferentialParityTest {
    @Test
    void allowedPatternsJudgeTheSameInJavaAndJs() throws Exception {
        Context cx = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).option("engine.WarnInterpreterOnly", "false").build();
        String src;
        try (InputStream is = getClass().getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            src = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        src = src.replaceAll("export\\s+const\\s+", "const ").replace("initS2Validator();", "");
        cx.eval("js", "var console={log(){},warn(){},error(){}};");
        cx.eval("js", src + "\nglobalThis.__m=(p,s)=>{try{return String(fullMatchRegExp(p).test(s));}catch(e){return 'ERR:'+e.name;}};");
        Value m = cx.eval("js", "globalThis.__m");
        String[][] cases = {
            {"\\x{41}", "A"}, {"\\x{41}", "x{41}"},
            {"\\v", "\n"}, {"\\v", "\u000B"},
            {"\\N{LATIN SMALL LETTER A}", "a"},
            {"[]a]+", "]a"}, {"[]a]+", "a]"}, {"[^]a]", "b"}, {"[^]]", "x"},
            {"\\bcafe\\b", "cafe"},
            {".", "\u0085"}, {"[.]", "."}, {".{2}", "\uD83D\uDE00"}, {".", "\uD83D\uDE00"}, {"[^a]", "\uD83D\uDE00"},
            {"\\-?\\d+", "-1"}, {"a\\.b", "a.b"}, {"a.b", "a\nb"},
            {"[\\uD83D\\uDE00]", "\uD83D\uDE00"},
            {"a{,3}", "aa"}, {"a{,3}", "a{,3}"},
            {"\\cJ", "\n"}, {"\\0101", "A"}, {"\\101", "A"}, {"\\12", "\n"},
            {"(a)\\2", "a"}, {"\\k<x>", "k<x>"}, {"(?<x>a)\\k<x>", "aa"},
            {"\\u{41}", "A"}, {"\\_", "_"}, {"\\y", "y"}, {"\\-", "-"},
            {"[a-\\d]", "-"}, {"[\\w-z]", "-"}, {"\\b{g}", ""}, {"a$", "a"}, {"^a", "a"},
            {"(?=a)a", "a"}, {"(?<=a)b", "ab"}, {"(?:a|)", ""}, {"[\\[]", "["}, {"[a[]", "["},
            {"\\p{L}", "a"}, {"\\w", "\u00e9"}, {"\\d", "\u0661"}, {"[[:alpha:]]", "a"}, {"\\i", "i"},
            {"a++", "aa"}, {"a{2}+", "aaaa"}, {"(?i)a", "A"}, {"\\Qa.b\\E", "a.b"}, {"#", "#"}, {"[a-z&&[^b]]", "c"},
        };
        java.util.List<String> undetected = new java.util.ArrayList<>();
        for (String[] c : cases) {
            String java;
            try { java = String.valueOf(Pattern.compile(c[0]).matcher(c[1]).matches()); } catch (Exception e) { java = "ERR:" + e.getClass().getSimpleName(); }
            String js = m.execute(c[0], c[1]).asString();
            String inc = S2RegexCompatibility.findIncompatibility(c[0]);
            if (inc == null && !java.equals(js)) {
                undetected.add(c[0] + " | " + c[1].codePoints().mapToObj(Integer::toHexString).toList() + " java=" + java + " js=" + js);
            }
        }
        cx.close();
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(), undetected);
    }
}
