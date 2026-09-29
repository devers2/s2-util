package io.github.devers2.s2util.log;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the SLF4J bridge in isolated class loaders, so the rest of the s2-core tests keep running without SLF4J.
 * <p>
 * The build passes slf4j-api as a jar path ({@code s2.test.slf4jApiJar}) instead of adding it to the test class path.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * SLF4J 브리지를 격리된 클래스로더에서 시험하여, 나머지 s2-core 시험은 계속 SLF4J 없이 실행되게 합니다.
 * <p>
 * 빌드는 slf4j-api 를 시험 클래스패스에 넣지 않고 jar 경로({@code s2.test.slf4jApiJar})로 넘깁니다.
 * </p>
 */
public class Slf4jBridgeTest {

    private static URL mainClasses() {
        return S2LogManager.class.getProtectionDomain().getCodeSource().getLocation();
    }

    private static URL slf4jApiJar() throws Exception {
        String path = System.getProperty("s2.test.slf4jApiJar");
        assertNotNull(path, "s2.test.slf4jApiJar must be set by the build");
        return Path.of(path).toUri().toURL();
    }

    private static String selectedFactory(ClassLoader loader) throws Exception {
        Class<?> manager = Class.forName("io.github.devers2.s2util.log.S2LogManager", true, loader);
        Field factory = manager.getDeclaredField("factory");
        factory.setAccessible(true);
        return factory.get(null).getClass().getSimpleName();
    }

    @Test
    void withoutSlf4jTheDefaultLoggerIsUsed() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { mainClasses() }, ClassLoader.getPlatformClassLoader())) {
            assertEquals("DefaultS2LoggerFactory", selectedFactory(loader));
        }
    }

    @Test
    void withSlf4jTheBridgeFactoryIsUsed() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { mainClasses(), slf4jApiJar() }, ClassLoader.getPlatformClassLoader())) {
            assertEquals("Slf4jS2LoggerFactory", selectedFactory(loader));
        }
    }

    @Test
    void bridgeForwardsCallsThroughTheLoggerInterface() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { mainClasses(), slf4jApiJar() }, ClassLoader.getPlatformClassLoader())) {
            Class<?> loggerApi = Class.forName("org.slf4j.Logger", false, loader);
            List<Object[]> calls = new ArrayList<>();
            // A recording fake org.slf4j.Logger (no SLF4J provider needed) | 호출을 기록하는 가짜 org.slf4j.Logger (SLF4J 구현체 불필요)
            Object fakeLogger = Proxy.newProxyInstance(loader, new Class<?>[] { loggerApi }, (proxy, method, args) -> {
                calls.add(new Object[] { method.getName(), args });
                if (method.getName().startsWith("is") && method.getName().endsWith("Enabled")) {
                    return method.getName().equals("isDebugEnabled") ? Boolean.FALSE : Boolean.TRUE;
                }
                if (method.getReturnType() == String.class) {
                    return "fake";
                }
                return null;
            });

            Class<?> bridgeClass = Class.forName("io.github.devers2.s2util.log.Slf4jS2Logger", true, loader);
            Constructor<?> ctor = bridgeClass.getDeclaredConstructor(Object.class);
            ctor.setAccessible(true);
            Object bridge = ctor.newInstance(fakeLogger);

            Method log = bridgeClass.getMethod("log", String.class, String.class, Object[].class);
            log.setAccessible(true); // the class is package-private in another class loader | 다른 클래스로더의 package-private 클래스
            log.invoke(bridge, "WARN", "hello {}", new Object[] { "x" });
            log.invoke(bridge, "ERROR", "boom", new Object[0]);

            assertEquals(2, calls.size());
            assertEquals("warn", calls.get(0)[0]);
            assertArrayEquals(new Object[] { "hello {}", new Object[] { "x" } }, (Object[]) calls.get(0)[1]);
            assertEquals("error", calls.get(1)[0]);

            Method isDebug = bridgeClass.getMethod("isDebugEnabled");
            Method isWarn = bridgeClass.getMethod("isWarnEnabled");
            isDebug.setAccessible(true);
            isWarn.setAccessible(true);
            assertFalse((Boolean) isDebug.invoke(bridge));
            assertTrue((Boolean) isWarn.invoke(bridge));
        }
    }

    @Test
    void nonLoggerObjectIsRejectedSoTheFactoryFallsBack() throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] { mainClasses(), slf4jApiJar() }, ClassLoader.getPlatformClassLoader())) {
            Class<?> bridgeClass = Class.forName("io.github.devers2.s2util.log.Slf4jS2Logger", true, loader);
            Constructor<?> ctor = bridgeClass.getDeclaredConstructor(Object.class);
            ctor.setAccessible(true);
            Throwable thrown = null;
            try {
                ctor.newInstance("not a logger");
            } catch (java.lang.reflect.InvocationTargetException e) {
                thrown = e.getCause();
            }
            assertTrue(thrown instanceof IllegalArgumentException, String.valueOf(thrown));
        }
    }
}
