/**
 * S2Util Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */
package io.github.devers2.s2util.message;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.devers2.s2util.core.S2StringUtil;

/**
 * A message key with its arguments, carried by an exception so that the message shown to a user can be rendered in the
 * user's language later ({@link S2Localizable}). The exception's own message stays in English for logs and searches.
 * <p>
 * Lookup order for a locale such as {@code ko_KR}: the application's bundle ({@link S2ResourceBundle#getDefaultBasename()},
 * so an application can override any text), then the key's own bundle; in each, {@code _ko_KR}, {@code _ko}, then the
 * base file. Unlike {@link java.util.ResourceBundle#getBundle(String, Locale)}, it never falls back to the server's
 * default locale, so an English request is answered from the base (English) file even on a Korean server. Files are
 * read as UTF-8 and cached per class loader.
 * </p>
 * <p>
 * Arguments are kept as strings, so the exception holds no reference to large objects and stays serializable. Do not
 * put server paths or other internal details into arguments of user-facing messages.
 * </p>
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 예외에 담아 두는 메시지 키와 인자입니다. 사용자에게 보여 줄 문구를 나중에 사용자의 언어로 만들 수 있게 합니다
 * ({@link S2Localizable}). 예외 자체의 메시지는 로그·검색용으로 영어를 유지합니다.
 * <p>
 * {@code ko_KR} 로케일이면 애플리케이션 번들({@link S2ResourceBundle#getDefaultBasename()}, 앱이 문구를 덮어쓸 수 있음), 그다음 키의
 * 번들 순서로 찾고, 각각 {@code _ko_KR}, {@code _ko}, 기본 파일 순서로 봅니다. {@link java.util.ResourceBundle#getBundle(String, Locale)}
 * 과 달리 서버 기본 로케일로 대체하지 않으므로, 한국어 서버에서도 영어 요청은 기본(영어) 파일로 답합니다. 파일은 UTF-8 로 읽고
 * 클래스 로더별로 캐시합니다.
 * </p>
 * <p>
 * 인자는 문자열로 보관하므로 예외가 큰 객체를 붙잡지 않고 직렬화도 됩니다. 사용자에게 보일 메시지의 인자에 서버 경로 등 내부 정보를
 * 넣지 마십시오.
 * </p>
 */
public final class S2MessageKey implements Serializable {

    private static final long serialVersionUID = 1L;

    /* 클래스 로더 → (파일 경로 → 읽은 내용). 로더가 사라지면(재배포) 함께 비워지도록 약한 키로 둔다 */
    private static final Map<ClassLoader, Map<String, Optional<Properties>>> FILES = Collections
            .synchronizedMap(new WeakHashMap<>());

    private final String basename;
    private final String key;
    private final String[] args;

    private S2MessageKey(String basename, String key, String[] args) {
        this.basename = basename;
        this.key = key;
        this.args = args;
    }

    /**
     * Creates a message key. Arguments are converted to strings ({@code null} becomes an empty string).
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 메시지 키를 만듭니다. 인자는 문자열로 바꿔 보관합니다({@code null} 은 빈 문자열).
     * @param basename Bundle base name, for example {@code io.github.devers2.s2kit.messages} | 번들 기본 이름
     * @param key      Message key | 메시지 키
     * @param args     Values for {@code {0}}, {@code {1}}, ... (Korean josa such as {@code {0|을}} is supported) | 치환할 값
     *                 (조사 표기 {@code {0|을}} 지원)
     * @return Message key | 메시지 키
     * @throws IllegalArgumentException if basename or key is blank | basename 이나 key 가 비었을 때
     */
    public static S2MessageKey of(String basename, String key, Object... args) {
        if (basename == null || basename.isBlank() || key == null || key.isBlank()) {
            throw new IllegalArgumentException("basename and key are required");
        }
        var values = args == null ? new String[0]
                : Arrays.stream(args).map(arg -> arg == null ? "" : String.valueOf(arg)).toArray(String[]::new);
        return new S2MessageKey(basename, key, values);
    }

    /**
     * Bundle base name.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 번들 기본 이름.
     * @return Base name | 기본 이름
     */
    public String getBasename() {
        return basename;
    }

    /**
     * Message key.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 메시지 키.
     * @return Key | 키
     */
    public String getKey() {
        return key;
    }

    /**
     * Arguments as strings.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 문자열로 바꾼 인자.
     * @return Copy of the arguments | 인자 사본
     */
    public List<String> getArgs() {
        return List.of(args);
    }

    /**
     * Renders the message for a locale, or returns empty when no bundle has the key.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 로케일에 맞는 문구를 만듭니다. 키가 있는 번들이 없으면 빈 값을 돌려줍니다.
     * @param locale Locale of the user ({@code null}: the JVM default) | 사용자 로케일 ({@code null}: JVM 기본)
     * @return Rendered message | 만든 문구
     */
    public Optional<String> resolve(Locale locale) {
        var target = locale != null ? locale : Locale.getDefault();
        var appBasename = S2ResourceBundle.getDefaultBasename();
        Optional<String> template = Optional.empty();
        if (appBasename != null && !appBasename.isBlank() && !appBasename.equals(basename)) {
            template = find(appBasename, key, target);
        }
        if (template.isEmpty()) {
            template = find(basename, key, target);
        }
        return template.map(text -> args.length == 0 ? text : S2StringUtil.formatMessage(text, (Object[]) args));
    }

    /* 요청 언어 → 기본 파일 순서로, 컨텍스트 로더(웹앱)와 이 라이브러리의 로더에서 찾는다 */
    private static Optional<String> find(String basename, String key, Locale locale) {
        var path = basename.replace('.', '/');
        var suffixes = new ArrayList<String>();
        if (!locale.getLanguage().isEmpty()) {
            if (!locale.getCountry().isEmpty()) {
                suffixes.add("_" + locale.getLanguage() + "_" + locale.getCountry());
            }
            suffixes.add("_" + locale.getLanguage());
        }
        suffixes.add("");
        var loaders = new LinkedHashSet<ClassLoader>();
        var context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            loaders.add(context);
        }
        var own = S2MessageKey.class.getClassLoader();
        if (own != null) {
            loaders.add(own);
        }
        for (var suffix : suffixes) {
            for (var loader : loaders) {
                var file = load(loader, path + suffix + ".properties");
                if (file.isPresent() && file.get().containsKey(key)) {
                    return Optional.of(file.get().getProperty(key));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Properties> load(ClassLoader loader, String file) {
        var files = FILES.computeIfAbsent(loader, l -> new ConcurrentHashMap<>());
        return files.computeIfAbsent(file, name -> {
            try (var in = loader.getResourceAsStream(name)) {
                if (in == null) {
                    return Optional.empty();
                }
                var properties = new Properties();
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                return Optional.of(properties);
            } catch (IOException | IllegalArgumentException e) {
                return Optional.empty();
            }
        });
    }

    @Override
    public String toString() {
        return basename + ":" + key + Arrays.toString(args);
    }

}
