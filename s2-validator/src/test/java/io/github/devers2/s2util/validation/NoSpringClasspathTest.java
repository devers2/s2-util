package io.github.devers2.s2util.validation;

import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.core.S2Util;

/**
 * The validator works on a classpath without Spring: only s2-core and s2-validator are visible to an isolated class
 * loader. The Spring integration classes are the only ones that need Spring.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * Spring 이 없는 클래스패스에서도 검증기가 동작하는지 확인합니다. 격리된 클래스 로더에는 s2-core 와 s2-validator 만 보입니다. Spring 이
 * 필요한 것은 Spring 연동 클래스뿐입니다.
 */
public class NoSpringClasspathTest {

    private static URL location(Class<?> type) throws Exception {
        return type.getProtectionDomain().getCodeSource().getLocation();
    }

    private static URLClassLoader isolatedLoader() throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(location(S2Validator.class));
        urls.add(location(S2Util.class));
        // Resources of s2-validator (JS and messages) live next to the classes directory | s2-validator 리소스는 클래스 디렉터리 옆에 있음
        Path classes = Path.of(location(S2Validator.class).toURI());
        Path resources = classes.getParent().getParent().getParent().resolve("resources").resolve("main");
        if (Files.isDirectory(resources)) {
            urls.add(resources.toUri().toURL());
        }
        return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    @Test
    void validatorRunsWithoutSpringOnTheClasspath() throws Exception {
        try (URLClassLoader loader = isolatedLoader()) {
            Assertions.assertThrows(ClassNotFoundException.class,
                    () -> loader.loadClass("org.springframework.context.MessageSource"), "Spring must not be visible");

            Class<?> validatorType = loader.loadClass(S2Validator.class.getName());
            Class<?> featureType = loader.loadClass("io.github.devers2.s2util.json.S2JsonUtil$Feature");
            Object validator = validatorType.getMethod("fromJson", String.class, featureType.arrayType())
                    .invoke(null, "{\"schemaVersion\":1,\"fields\":[{\"name\":\"name\",\"label\":\"이름\"}]}",
                            Array.newInstance(featureType, 0));

            List<Object> errors = new ArrayList<>();
            Object valid = validatorType.getMethod("validate", Object.class, Consumer.class)
                    .invoke(validator, Map.of(), (Consumer<Object>) errors::add);
            Assertions.assertEquals(false, valid);
            Assertions.assertEquals(1, errors.size());

            String rules = (String) validatorType.getMethod("getRulesJson").invoke(validator);
            Assertions.assertTrue(rules.startsWith("{\"schemaVersion\":1"), rules);
        }
    }

    @Test
    void onlyTheSpringIntegrationNeedsSpring() throws Exception {
        try (URLClassLoader loader = isolatedLoader()) {
            Class<?> autoConfiguration = loader.loadClass(
                    "io.github.devers2.s2util.validation.spring.S2ValidatorAutoConfiguration");
            // Loading is lazy; using the class needs Spring, which is absent | 로드는 지연되며, 사용하려면 없는 Spring 이 필요함
            Assertions.assertThrows(Throwable.class, autoConfiguration::getDeclaredMethods);
        }
    }
}
