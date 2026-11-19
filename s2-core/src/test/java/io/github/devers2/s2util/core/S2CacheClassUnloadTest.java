package io.github.devers2.s2util.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.LookupType;
import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.MethodKey;

/**
 * S2Cache 의 클래스별 캐시(메서드 핸들·필드·실제 클래스)가 재배포·재시작된 클래스의 ClassLoader 를
 * 붙잡지 않는지 확인한다(붙잡으면 재배포마다 이전 ClassLoader 전체가 메모리에 남는다).
 */
class S2CacheClassUnloadTest {

    /** 재배포 대상 역할의 클래스 — 별도 ClassLoader 로 다시 정의해 쓴다. */
    public static class ReloadedBean {
        private String name = "n";

        public String getName() {
            return name;
        }
    }

    /** {@link ReloadedBean} 만 자기가 직접 정의하는 ClassLoader(재배포된 앱의 ClassLoader 역할). */
    static final class ReloadingLoader extends ClassLoader {
        ReloadingLoader() {
            super(S2CacheClassUnloadTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(ReloadedBean.class.getName())) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }
                String path = name.replace('.', '/') + ".class";
                try (InputStream in = getParent().getResourceAsStream(path)) {
                    byte[] bytes = in.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
    }

    /** 다른 ClassLoader 의 클래스로 캐시를 채우고, 그 ClassLoader 의 약한 참조만 돌려준다. */
    private static WeakReference<ClassLoader> useCachesWithReloadedClass() throws Exception {
        ReloadingLoader loader = new ReloadingLoader();
        Class<?> type = loader.loadClass(ReloadedBean.class.getName());
        assertTrue(type != ReloadedBean.class);

        Object bean = type.getDeclaredConstructor().newInstance();
        Optional<?> getter = S2Cache.getMethodHandle(new MethodKey(type, "getName"), LookupType.METHOD);
        Optional<?> field = S2Cache.getMethodHandle(new MethodKey(type, "name"), LookupType.BOTH);
        Optional<?> missing = S2Cache.getMethodHandle(new MethodKey(type, "getNothing"), LookupType.METHOD);
        assertTrue(getter.isPresent() && field.isPresent() && missing.isEmpty());
        assertTrue(S2Cache.getFields(type).isPresent());
        assertTrue(S2Cache.getRealClass(bean) == type);
        return new WeakReference<>(loader);
    }

    @Test
    void cachesDoNotRetainReloadedClassLoader() throws Exception {
        WeakReference<ClassLoader> ref = useCachesWithReloadedClass();

        for (int i = 0; i < 50 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(ref.get(), "S2Cache retained the class loader of a reloaded class");
    }
}
