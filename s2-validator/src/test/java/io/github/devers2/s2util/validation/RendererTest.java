package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Error display extension point of s2.validator.js ({@code setRenderer}, {@code classRenderer}) on a fake DOM.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * s2.validator.js 의 오류 표시 확장점({@code setRenderer}, {@code classRenderer})을 가짜 DOM 에서 확인합니다.
 */
public class RendererTest {

    private static Context js;

    @BeforeAll
    static void setup() throws IOException {
        js = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).option("engine.WarnInterpreterOnly", "false").build();
        String source;
        try (InputStream is = RendererTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        source = source.replaceAll("export\\s+const\\s+", "const ").replaceAll("export\\s+function\\s+", "function ")
                .replaceAll("export\\s+default\\s+", "").replace("initS2Validator();", "// initS2Validator();");

        js.eval("js", """
                globalThis.__consoleErrors = [];
                var console = { log() {}, warn() {}, error(...a) { globalThis.__consoleErrors.push(a.join(' ')); } };
                var window = {};
                var MutationObserver = function() { this.observe = function() {}; };
                globalThis.__inserted = [];
                function makeEl(props) {
                    const classes = new Set();
                    return Object.assign({
                        type: 'text', value: '', checked: false, parentElement: null, offsetParent: {},
                        attrs: {}, style: {}, validity: '', textContent: '', focused: false, listeners: {},
                        classList: { add: (c) => classes.add(c), remove: (c) => classes.delete(c), contains: (c) => classes.has(c) },
                        setCustomValidity(m) { this.validity = m; },
                        addEventListener(type, fn) { (this.listeners[type] = this.listeners[type] || []).push(fn); },
                        setAttribute(k, v) { this.attrs[k] = v; },
                        getClientRects() { return this.type === 'hidden' ? [] : [{}]; },
                        insertAdjacentElement(pos, node) { globalThis.__inserted.push(node); return node; },
                        focus() { this.focused = true; },
                        remove() {}
                    }, props);
                }
                var document = {
                    querySelector() { return null; }, querySelectorAll() { return []; }, documentElement: {},
                    readyState: 'complete', addEventListener() {}, createElement() { return makeEl({}); }
                };
                class HTMLFormElement {
                    constructor() { this.elements = []; this.extras = []; this.dataset = {}; this.reported = 0; }
                    all() { return this.elements.concat(this.extras); }
                    querySelectorAll(sel) {
                        if (sel === '.__s2_dummy_anchor__') return globalThis.__inserted.slice();
                        let m = sel.match(/^\\[name="(.+)"\\]$/);
                        if (m) return this.elements.filter((e) => e.name === m[1]);
                        m = sel.match(/^\\[([\\w-]+)="(.+)"\\]$/);
                        if (m) return this.all().filter((e) => e.attrs[m[1]] === m[2]);
                        m = sel.match(/^\\[([\\w-]+)\\]$/);
                        if (m) return this.all().filter((e) => m[1] in e.attrs);
                        m = sel.match(/^\\.([\\w-]+)$/);
                        if (m) return this.all().filter((e) => e.classList.contains(m[1]));
                        return [];
                    }
                    querySelector(sel) { return this.querySelectorAll(sel)[0] ?? null; }
                    reportValidity() { this.reported++; return true; }
                }
                globalThis.__makeForm = function() {
                    const form = new HTMLFormElement();
                    const email = makeEl({ name: 'email', value: '' });
                    const name = makeEl({ name: 'name', value: '홍길동' });
                    const hidden = makeEl({ name: 'id', type: 'hidden', value: '' });
                    form.elements.push(email, name, hidden);
                    form.extras.push(makeEl({ attrs: { 'data-s2-error-for': 'email' } }), makeEl({ attrs: { 'data-s2-error-for': 'id' } }));
                    return { form, email, name, hidden, emailMsg: form.extras[0], idMsg: form.extras[1] };
                };
                globalThis.__rules = JSON.stringify([
                    { name: 'email', label: '이메일', rules: [{ type: 'REQUIRED', regex: null, message: '이메일 필수', value: null }] },
                    { name: 'name', label: '이름', rules: [{ type: 'REQUIRED', regex: null, message: '이름 필수', value: null }] },
                    { name: 'id', label: 'ID', rules: [{ type: 'REQUIRED', regex: null, message: 'ID 필수', value: null }] }
                ]);
                """);
        js.eval("js", source + "\nglobalThis.__S2Validator = S2Validator;\n");
    }

    @AfterEach
    void resetRenderer() {
        js.eval("js", "__S2Validator.setRenderer(null); globalThis.__inserted = []; globalThis.__consoleErrors = [];");
    }

    @AfterAll
    static void tearDown() {
        js.close();
    }

    @Test
    void customRendererReceivesErrorsAndNativeUiIsNotUsed() {
        Value r = js.eval("js", """
                (() => {
                  const calls = [];
                  __S2Validator.setRenderer({
                    clear(form) { calls.push('clear'); },
                    show(form, errors) { calls.push('show:' + Object.keys(errors).sort().join(',')); }
                  });
                  const f = __makeForm();
                  const errors = __S2Validator.validate(f.form, __rules);
                  return { calls: calls.join('|'), errorKeys: Object.keys(errors).sort().join(','),
                           emailValidity: f.email.validity, anchors: __inserted.length, reported: f.form.reported };
                })()
                """);
        Assertions.assertEquals("clear|show:email,id", r.getMember("calls").asString());
        Assertions.assertEquals("email,id", r.getMember("errorKeys").asString());
        Assertions.assertEquals("", r.getMember("emailValidity").asString(), "native bubble must not be set");
        Assertions.assertEquals(0, r.getMember("anchors").asInt(), "hidden-field anchor must not be created");
        Assertions.assertEquals(0, r.getMember("reported").asInt(), "reportValidity must not be called");
    }

    @Test
    void rendererExceptionIsLoggedAndErrorsAreStillReturned() {
        Value r = js.eval("js", """
                (() => {
                  __S2Validator.setRenderer({ show() { throw new Error('boom'); } });
                  const errors = __S2Validator.validate(__makeForm().form, __rules);
                  return { errorCount: Object.keys(errors).length, logged: __consoleErrors.join(' ') };
                })()
                """);
        Assertions.assertEquals(2, r.getMember("errorCount").asInt(), "errors must still block submit");
        Assertions.assertTrue(r.getMember("logged").asString().contains("renderer.show()"), r.getMember("logged").asString());
    }

    @Test
    void settingNullRestoresTheNativeUi() {
        Value r = js.eval("js", """
                (() => {
                  __S2Validator.setRenderer({ show() {} });
                  __S2Validator.setRenderer(null);
                  const f = __makeForm();
                  __S2Validator.validate(f.form, __rules);
                  return { emailValidity: f.email.validity, reported: f.form.reported, anchors: __inserted.length };
                })()
                """);
        Assertions.assertEquals("이메일 필수", r.getMember("emailValidity").asString());
        Assertions.assertEquals(1, r.getMember("reported").asInt());
        Assertions.assertEquals(1, r.getMember("anchors").asInt(), "the hidden 'id' field gets the native anchor again");
    }

    @Test
    void classRendererMarksFieldsWritesMessagesAndFocusesFirstError() {
        Value r = js.eval("js", """
                (() => {
                  __S2Validator.setRenderer(__S2Validator.classRenderer());
                  const f = __makeForm();
                  __S2Validator.validate(f.form, __rules);
                  return { emailInvalid: f.email.classList.contains('is-invalid'), nameInvalid: f.name.classList.contains('is-invalid'),
                           hiddenInvalid: f.hidden.classList.contains('is-invalid'), emailMsg: f.emailMsg.textContent,
                           idMsg: f.idMsg.textContent, emailFocused: f.email.focused };
                })()
                """);
        Assertions.assertTrue(r.getMember("emailInvalid").asBoolean());
        Assertions.assertFalse(r.getMember("nameInvalid").asBoolean());
        Assertions.assertTrue(r.getMember("hiddenInvalid").asBoolean());
        Assertions.assertEquals("이메일 필수", r.getMember("emailMsg").asString());
        Assertions.assertEquals("ID 필수", r.getMember("idMsg").asString(), "hidden field message is shown in its message element");
        Assertions.assertTrue(r.getMember("emailFocused").asBoolean());
    }

    @Test
    void classRendererClearsOnRevalidationAndOnUserInput() {
        Value r = js.eval("js", """
                (() => {
                  __S2Validator.setRenderer(__S2Validator.classRenderer({ invalidClass: 'error' }));
                  const f = __makeForm();
                  __S2Validator.validate(f.form, __rules);
                  const afterFirst = f.email.classList.contains('error');
                  // user edits the email field -> clearField
                  f.email.value = 'a@b.com';
                  f.email.listeners['input'].forEach((fn) => fn());
                  const afterInput = f.email.classList.contains('error') || f.emailMsg.textContent !== '';
                  // re-validate with valid email: clear() removes remaining marks, id still invalid
                  __S2Validator.validate(f.form, __rules);
                  return { afterFirst, afterInput, emailAfter: f.email.classList.contains('error'),
                           idStill: f.hidden.classList.contains('error') };
                })()
                """);
        Assertions.assertTrue(r.getMember("afterFirst").asBoolean());
        Assertions.assertFalse(r.getMember("afterInput").asBoolean(), "clearField must clear the edited field");
        Assertions.assertFalse(r.getMember("emailAfter").asBoolean());
        Assertions.assertTrue(r.getMember("idStill").asBoolean());
    }
}
