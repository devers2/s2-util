package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Browser error display for hidden / non-rendered fields (s2.validator.js {@code applyFieldError}) on a fake DOM
 * that models the parent chain, rendering ({@code getClientRects}) and anchor insertion.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 히든·비표시 필드의 브라우저 오류 표시(s2.validator.js {@code applyFieldError})를 부모 체인, 렌더링 여부({@code getClientRects}),
 * 앵커 삽입을 흉내 낸 가짜 DOM 에서 확인합니다.
 */
public class HiddenFieldAnchorTest {

    private static Context js;

    @BeforeAll
    static void setup() throws IOException {
        js = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).option("engine.WarnInterpreterOnly", "false").build();
        String source;
        try (InputStream is = HiddenFieldAnchorTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        source = source.replaceAll("export\\s+const\\s+", "const ").replaceAll("export\\s+function\\s+", "function ")
                .replaceAll("export\\s+default\\s+", "").replace("initS2Validator();", "// initS2Validator();");

        js.eval("js", """
                var console = { log() {}, warn() {}, error() {} };
                var window = {};
                var MutationObserver = function() { this.observe = function() {}; };
                globalThis.__inserted = [];
                function makeEl(props) {
                    return Object.assign({
                        type: 'text', value: '', checked: false, rendered: true, parentElement: null, offsetParent: {},
                        attrs: {}, style: {}, validity: '',
                        setCustomValidity(m) { this.validity = m; },
                        addEventListener() {},
                        setAttribute(k, v) { this.attrs[k] = v; },
                        getClientRects() {
                            for (let e = this; e; e = e.parentElement) { if (e.rendered === false) return []; }
                            return [{}];
                        },
                        insertAdjacentElement(pos, node) {
                            node.parentElement = this.parentElement;
                            node.insertedAfter = this;
                            globalThis.__inserted.push(node);
                            return node;
                        },
                        remove() { globalThis.__inserted = globalThis.__inserted.filter((n) => n !== this); }
                    }, props);
                }
                var document = {
                    querySelector() { return null; }, querySelectorAll() { return []; }, documentElement: {},
                    readyState: 'complete', addEventListener() {}, createElement() { return makeEl({}); }
                };
                class HTMLFormElement {
                    constructor() { this.elements = []; this.dataset = {}; this.rendered = true; this.parentElement = null; }
                    querySelectorAll(sel) {
                        if (sel === '.__s2_dummy_anchor__') return globalThis.__inserted.slice();
                        const m = sel.match(/^\\[name="(.+)"\\]$/);
                        return m ? this.elements.filter((e) => e.name === m[1]) : [];
                    }
                    querySelector(sel) { return this.querySelectorAll(sel)[0] ?? null; }
                    reportValidity() { return true; }
                }
                """);
        js.eval("js", source + "\nglobalThis.__S2Validator = S2Validator;\n");
    }

    @AfterAll
    static void tearDown() {
        js.close();
    }

    private static Value run(String script) {
        js.eval("js", "globalThis.__inserted = [];");
        return js.eval("js", script);
    }

    private static final String REQUIRED_RULES = "[{\"name\":\"%s\",\"label\":\"x\",\"rules\":[{\"type\":\"REQUIRED\",\"regex\":null,\"message\":\"MSG\",\"value\":null}]}]";

    @Test
    void visibleFieldGetsTheMessageWithoutAnchor() {
        Value r = run("""
                (() => {
                  const form = new HTMLFormElement();
                  const el = makeEl({ name: 'a', parentElement: form });
                  form.elements.push(el);
                  __S2Validator.validate(form, '%s');
                  return { validity: el.validity, anchors: __inserted.length };
                })()
                """.formatted(REQUIRED_RULES.formatted("a")));
        Assertions.assertEquals("MSG", r.getMember("validity").asString());
        Assertions.assertEquals(0, r.getMember("anchors").asInt());
    }

    @Test
    void positionFixedFieldIsTreatedAsRendered() {
        // offsetParent is null for rendered position:fixed elements | 렌더링된 position:fixed 요소도 offsetParent 는 null
        Value r = run("""
                (() => {
                  const form = new HTMLFormElement();
                  const el = makeEl({ name: 'a', parentElement: form, offsetParent: null });
                  form.elements.push(el);
                  __S2Validator.validate(form, '%s');
                  return { validity: el.validity, anchors: __inserted.length };
                })()
                """.formatted(REQUIRED_RULES.formatted("a")));
        Assertions.assertEquals("MSG", r.getMember("validity").asString());
        Assertions.assertEquals(0, r.getMember("anchors").asInt());
    }

    @Test
    void hiddenInputGetsOneAccessibleAnchorRightAfterIt() {
        Value r = run("""
                (() => {
                  const form = new HTMLFormElement();
                  const el = makeEl({ name: 'a', type: 'hidden', parentElement: form });
                  form.elements.push(el);
                  __S2Validator.validate(form, '%s');
                  const a = __inserted[0];
                  return { anchors: __inserted.length, afterField: a.insertedAfter === el, validity: a.validity,
                           label: a.attrs['aria-label'] ?? null, ariaHidden: a.attrs['aria-hidden'] ?? null, name: a.name ?? null };
                })()
                """.formatted(REQUIRED_RULES.formatted("a")));
        Assertions.assertEquals(1, r.getMember("anchors").asInt());
        Assertions.assertTrue(r.getMember("afterField").asBoolean());
        Assertions.assertEquals("MSG", r.getMember("validity").asString());
        Assertions.assertEquals("MSG", r.getMember("label").asString());
        Assertions.assertTrue(r.getMember("ariaHidden").isNull(), "focusable anchor must not be aria-hidden");
        Assertions.assertTrue(r.getMember("name").isNull(), "anchor must not be submitted");
    }

    @Test
    void radioGroupInClosedContainerGetsASingleAnchorOutsideTheContainer() {
        Value r = run("""
                (() => {
                  const form = new HTMLFormElement();
                  const tab = makeEl({ rendered: false, parentElement: form });
                  const inner = makeEl({ parentElement: tab });
                  ['1', '2', '3'].forEach((v) => form.elements.push(makeEl({ name: 'r', type: 'radio', value: v, parentElement: inner })));
                  __S2Validator.validate(form, '%s');
                  return { anchors: __inserted.length, afterTab: __inserted[0].insertedAfter === tab };
                })()
                """.formatted(REQUIRED_RULES.formatted("r")));
        Assertions.assertEquals(1, r.getMember("anchors").asInt(), "one anchor per field, not per radio");
        Assertions.assertTrue(r.getMember("afterTab").asBoolean(), "anchor must be placed after the outermost hidden container");
    }

    @Test
    void wildcardRowHiddenFieldAlsoGetsAnAnchor() {
        Value r = run("""
                (() => {
                  const form = new HTMLFormElement();
                  form.elements.push(makeEl({ name: 'items[0].id', type: 'hidden', parentElement: form }));
                  __S2Validator.validate(form, '%s');
                  return { anchors: __inserted.length };
                })()
                """.formatted(REQUIRED_RULES.formatted("items[].id")));
        Assertions.assertEquals(1, r.getMember("anchors").asInt());
    }
}
