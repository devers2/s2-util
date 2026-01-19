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
package io.github.devers2.s2util.core;

import org.junit.jupiter.api.BeforeAll;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * S2Util 동작 검증을 위한 JUnit 테스트
 */
public class Test {

    private static S2Logger logger;

    @BeforeAll
    static void setup() {
        logger = S2LogManager.getLogger(Test.class);
    }

    @org.junit.jupiter.api.Test
    void testLogOutputs() {

        logger.debug("test");

    }
}
