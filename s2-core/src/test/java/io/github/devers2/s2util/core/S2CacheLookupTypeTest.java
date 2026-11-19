package io.github.devers2.s2util.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.LookupType;
import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.MethodKey;

/**
 * MethodHandle 캐시가 조회 종류(LookupType)별로 결과를 따로 갖는지 확인한다.
 * 같은 MethodKey 를 먼저 조회한 종류의 결과가 다른 종류의 조회에 쓰이면 안 된다.
 */
class S2CacheLookupTypeTest {

    static class MethodFirst {
        @SuppressWarnings("unused")
        private String secret() {
            return "s";
        }
    }

    static class BothFirst {
        @SuppressWarnings("unused")
        private String secret() {
            return "s";
        }
    }

    @Test
    void methodLookupDoesNotHidePrivateMemberFromBothLookup() {
        assertFalse(S2Cache.getMethodHandle(new MethodKey(MethodFirst.class, "secret"), LookupType.METHOD).isPresent());
        assertTrue(S2Cache.getMethodHandle(new MethodKey(MethodFirst.class, "secret"), LookupType.BOTH).isPresent());
    }

    @Test
    void bothLookupDoesNotLeakPrivateMethodToPublicOnlyLookup() {
        assertTrue(S2Cache.getMethodHandle(new MethodKey(BothFirst.class, "secret"), LookupType.BOTH).isPresent());
        assertFalse(S2Cache.getMethodHandle(new MethodKey(BothFirst.class, "secret"), LookupType.METHOD).isPresent());
    }

    @Test
    void parentLoadedClassesUseSharedCacheCorrectly() {
        // JDK 클래스는 클래스별 캐시 대신 공용 캐시를 쓴다 — 결과는 같아야 한다(JDK 모듈의 private 필드는 원래 접근 불가라 시험하지 않음)
        assertTrue(S2Cache.getMethodHandle(new MethodKey(java.time.LocalDate.class, "getYear"), LookupType.METHOD).isPresent());
        assertTrue(S2Cache.getMethodHandle(new MethodKey(java.time.LocalDate.class, "getYear"), LookupType.METHOD).isPresent());
        assertFalse(S2Cache.getMethodHandle(new MethodKey(java.time.LocalDate.class, "year"), LookupType.METHOD).isPresent());
    }

    static class ManyLookups {
        public String getName() {
            return "n";
        }
    }

    @Test
    void resultsStayCorrectBeyondPerClassLimit() {
        // 클래스당 캐시 상한(기본 512)을 넘는 이름을 조회해도 결과는 맞아야 한다(넘는 것은 매번 조회)
        for (int i = 0; i < 1000; i++) {
            assertFalse(S2Cache.getMethodHandle(new MethodKey(ManyLookups.class, "missing" + i), LookupType.METHOD).isPresent());
        }
        assertTrue(S2Cache.getMethodHandle(new MethodKey(ManyLookups.class, "getName"), LookupType.METHOD).isPresent());
    }
}
