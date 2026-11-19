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
}
