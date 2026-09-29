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
 * {@code S2Validator.reindex(form, collection)} on a fake DOM where {@code name} is an attribute.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@code name}을 속성으로 다루는 가짜 DOM 에서 {@code S2Validator.reindex(form, collection)}을 확인합니다.
 */
public class ReindexTest {

    private static Context js;

    @BeforeAll
    static void setup() throws IOException {
        js = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).option("engine.WarnInterpreterOnly", "false").build();
        String source;
        try (InputStream is = ReindexTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        source = source.replaceAll("export\\s+const\\s+", "const ").replaceAll("export\\s+function\\s+", "function ")
                .replaceAll("export\\s+default\\s+", "").replace("initS2Validator();", "// initS2Validator();");
        js.eval("js", """
                var console = { log() {}, warn() {}, error() {} };
                var window = {};
                var MutationObserver = function() { this.observe = function() {}; };
                function makeEl(attrs, extra) {
                    const el = Object.assign({
                        attrs: Object.assign({}, attrs), type: 'text', value: '', checked: false, offsetParent: {}, validity: '',
                        getAttribute(k) { return k in this.attrs ? this.attrs[k] : null; },
                        setAttribute(k, v) { this.attrs[k] = String(v); },
                        setCustomValidity(m) { this.validity = m; },
                        addEventListener() {}, getClientRects() { return [{}]; },
                        classList: { add() {}, remove() {}, contains() { return false; } }
                    }, extra);
                    Object.defineProperty(el, 'name', { get() { return this.attrs.name; } });
                    return el;
                }
                var document = { querySelector() { return null; }, querySelectorAll() { return []; }, documentElement: {},
                                 readyState: 'complete', addEventListener() {}, createElement() { return makeEl({}); } };
                class HTMLFormElement {
                    constructor(children) { this.children = children; this.dataset = {}; }
                    get elements() { return this.children.filter((e) => e.isControl); }
                    querySelectorAll(sel) {
                        if (sel === '[name], [data-s2-error-for]') return this.children.filter((e) => 'name' in e.attrs || 'data-s2-error-for' in e.attrs);
                        if (sel === '.__s2_dummy_anchor__') return [];
                        const m = sel.match(/^\\[([\\w-]+)="(.+)"\\]$/);
                        return m ? this.children.filter((e) => e.attrs[m[1]] === m[2]) : [];
                    }
                    querySelector(sel) { return this.querySelectorAll(sel)[0] ?? null; }
                    reportValidity() { return true; }
                }
                globalThis.__names = (form) => form.children.map((e) => e.getAttribute('name') ?? ('for:' + e.getAttribute('data-s2-error-for'))).join('|');
                """);
        js.eval("js", source + "\nglobalThis.__S2Validator = S2Validator;\n");
    }

    @AfterAll
    static void tearDown() {
        js.close();
    }

    @Test
    void gapsAreRenumberedInDocumentOrderIncludingSubPathsProxiesAndErrorTargets() {
        Value r = js.eval("js", """
                (() => {
                  const c = (name, extra) => makeEl({ name }, Object.assign({ isControl: true }, extra));
                  const form = new HTMLFormElement([
                    c('title'),
                    c('items[0].name'), c('items[0].addr.zip'),
                    c('items[2].name'), makeEl({ name: 'items[2].name_error' }), makeEl({ 'data-s2-error-for': 'items[2].name' }),
                    c('items[5].name'), c('items[5].addr.zip')
                  ]);
                  const rows = __S2Validator.reindex(form, 'items');
                  return { rows, names: __names(form) };
                })()
                """);
        Assertions.assertEquals(3, r.getMember("rows").asInt());
        Assertions.assertEquals("title|items[0].name|items[0].addr.zip|items[1].name|items[1].name_error|for:items[1].name"
                + "|items[2].name|items[2].addr.zip", r.getMember("names").asString());
    }

    @Test
    void documentOrderDecidesTheNewIndexNotTheOldNumber() {
        Value r = js.eval("js", """
                (() => {
                  const form = new HTMLFormElement([ makeEl({ name: 'items[3].name' }), makeEl({ name: 'items[1].name' }) ]);
                  __S2Validator.reindex(form, 'items');
                  return __names(form);
                })()
                """);
        Assertions.assertEquals("items[0].name|items[1].name", r.asString());
    }

    @Test
    void otherCollectionsAndSimilarPrefixesAreNotTouched() {
        Value r = js.eval("js", """
                (() => {
                  const form = new HTMLFormElement([
                    makeEl({ name: 'items[2].name' }), makeEl({ name: 'itemsExtra[2].name' }), makeEl({ name: 'orders[2].no' }),
                    makeEl({ name: 'my.items[2].name' })
                  ]);
                  __S2Validator.reindex(form, 'items');
                  return __names(form);
                })()
                """);
        Assertions.assertEquals("items[0].name|itemsExtra[2].name|orders[2].no|my.items[2].name", r.asString());
    }

    @Test
    void nestedCollectionIsRenumberedOnlyUnderTheGivenPath() {
        Value r = js.eval("js", """
                (() => {
                  const form = new HTMLFormElement([
                    makeEl({ name: 'items[0].options[3].v' }), makeEl({ name: 'items[0].options[7].v' }), makeEl({ name: 'items[1].options[3].v' })
                  ]);
                  __S2Validator.reindex(form, 'items[0].options');
                  return __names(form);
                })()
                """);
        Assertions.assertEquals("items[0].options[0].v|items[0].options[1].v|items[1].options[3].v", r.asString());
    }

    @Test
    void validationAfterReindexReportsTheNewIndices() {
        Value r = js.eval("js", """
                (() => {
                  const c = (name, value) => makeEl({ name }, { isControl: true, value });
                  const form = new HTMLFormElement([ c('items[0].name', 'A'), c('items[2].name', '') ]);
                  __S2Validator.reindex(form, 'items');
                  const rules = JSON.stringify([{ name: 'items[].name', label: '품목명',
                    rules: [{ type: 'REQUIRED', regex: null, message: '필수', value: null }] }]);
                  return Object.keys(__S2Validator.validate(form, rules)).join(',');
                })()
                """);
        Assertions.assertEquals("items[1].name", r.asString());
    }

    @Test
    void missingFormOrCollectionIsANoOp() {
        Assertions.assertEquals(0, js.eval("js", "__S2Validator.reindex(null, 'items')").asInt());
        Assertions.assertEquals(0, js.eval("js", "__S2Validator.reindex(new HTMLFormElement([makeEl({ name: 'a' })]), 'items')").asInt());
    }
}
