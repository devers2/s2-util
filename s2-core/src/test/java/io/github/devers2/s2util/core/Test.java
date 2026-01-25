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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * S2Util 동작 검증을 위한 JUnit 테스트 및 SmokeTest
 *
 * getValue, setValue, S2Copier의 주요 기능을 테스트하며,
 * 성공/실패 통계를 포함한 테스트 결과를 제공합니다.
 */
public class Test {

    private static S2Logger logger;
    private static TestStatistics stats;

    // ===== Test Fixtures =====

    /** 간단한 DTO 클래스 */
    public static class UserDto {
        private String id;
        private String name;
        private int age;
        private String email;

        public UserDto() {
        }

        public UserDto(String id, String name, int age, String email) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "UserDto{" + "id='" + id + "', name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }

    /** Java Record */
    public record UserRecord(String id, String name, int age, String email) {}

    /** 중첩 객체를 포함한 DTO */
    public static class AddressDto {
        private String street;
        private String city;
        private String zipCode;

        public AddressDto() {
        }

        public AddressDto(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        @Override
        public String toString() {
            return "AddressDto{" + "street='" + street + "', city='" + city + "', zipCode='" + zipCode + "'}";
        }
    }

    /** 주소를 포함한 복합 DTO */
    public static class UserWithAddressDto {
        private String id;
        private String name;
        private AddressDto address;

        public UserWithAddressDto() {
        }

        public UserWithAddressDto(String id, String name, AddressDto address) {
            this.id = id;
            this.name = name;
            this.address = address;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AddressDto getAddress() {
            return address;
        }

        public void setAddress(AddressDto address) {
            this.address = address;
        }

        @Override
        public String toString() {
            return "UserWithAddressDto{" + "id='" + id + "', name='" + name + "', address=" + address + "}";
        }
    }

    /** 테스트 통계 클래스 */
    static class TestStatistics {
        private int total = 0;
        private int success = 0;
        private int failed = 0;
        private List<String> failures = new ArrayList<>();

        void recordSuccess() {
            this.total++;
            this.success++;
        }

        void recordFailure(String testName, Exception e) {
            this.total++;
            this.failed++;
            String msg = testName + " [FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "]";
            this.failures.add(msg);
        }

        void printReport(String categoryName) {
            logger.info("");
            logger.info("╔════════════════════════════════════════════════════════════╗");
            logger.info("║ Test Report: " + String.format("%-48s", categoryName) + "║");
            logger.info("╠════════════════════════════════════════════════════════════╣");
            logger.info("║ Total: " + String.format("%-52d", total) + "║");
            logger.info("║ ✓ Success: " + String.format("%-48d", success) + "║");
            logger.info("║ ✗ Failed: " + String.format("%-49d", failed) + "║");
            if (!failures.isEmpty()) {
                logger.info("╠════════════════════════════════════════════════════════════╣");
                logger.info("║ Failed Tests:                                              ║");
                for (String failure : failures) {
                    String line = "║  " + String.format("%-56s", failure.substring(0, Math.min(56, failure.length()))) + "║";
                    logger.info(line);
                    if (failure.length() > 56) {
                        logger.info("║  " + String.format("%-56s", failure.substring(56)) + "║");
                    }
                }
            }
            logger.info("╚════════════════════════════════════════════════════════════╝");
        }

        void reset() {
            this.total = 0;
            this.success = 0;
            this.failed = 0;
            this.failures.clear();
        }
    }

    @BeforeAll
    static void setup() {
        logger = S2LogManager.getLogger(Test.class);
        stats = new TestStatistics();

        // 현재 사용 중인 캐시 구현체 확인
        S2Cache.isCaffeineEnabled();
    }

    @AfterAll
    static void tearDown() {
        if (stats != null && stats.total > 0) {
            stats.printReport("Final Summary");
        }
    }

    // ===== getValue Tests =====

    @DisplayName("getValue - 단순 DTO 필드 조회")
    @org.junit.jupiter.api.Test
    void testGetValue_SimpleDTO() {
        String testName = "getValue - Simple DTO";
        try {
            UserDto user = new UserDto("user123", "John Doe", 30, "john@example.com");

            assert "user123".equals(S2Util.getValue(user, "id"));
            assert "John Doe".equals(S2Util.getValue(user, "name"));
            assert 30 == (Integer) S2Util.getValue(user, "age");
            assert "john@example.com".equals(S2Util.getValue(user, "email"));

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("getValue - Record 객체 조회")
    @org.junit.jupiter.api.Test
    void testGetValue_Record() {
        String testName = "getValue - Record";
        try {
            UserRecord user = new UserRecord("rec001", "Jane Smith", 28, "jane@example.com");

            assert "rec001".equals(S2Util.getValue(user, "id"));
            assert "Jane Smith".equals(S2Util.getValue(user, "name"));
            assert 28 == (Integer) S2Util.getValue(user, "age");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("getValue - Map 키 조회")
    @org.junit.jupiter.api.Test
    void testGetValue_Map() {
        String testName = "getValue - Map";
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "map001");
            map.put("name", "Map User");
            map.put("age", 35);

            assert "map001".equals(S2Util.getValue(map, "id"));
            assert "Map User".equals(S2Util.getValue(map, "name"));
            assert 35 == (Integer) S2Util.getValue(map, "age");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("getValue - List 인덱스 조회")
    @org.junit.jupiter.api.Test
    void testGetValue_List() {
        String testName = "getValue - List";
        try {
            List<String> list = List.of("first", "second", "third");

            assert "first".equals(S2Util.getValue(list, 0));
            assert "second".equals(S2Util.getValue(list, 1));
            assert "third".equals(S2Util.getValue(list, "2"));

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("getValue - 중첩 경로 조회 (Dot Notation)")
    @org.junit.jupiter.api.Test
    void testGetValue_NestedPath() {
        String testName = "getValue - Nested Path";
        try {
            AddressDto address = new AddressDto("123 Main St", "Seoul", "12345");
            UserWithAddressDto user = new UserWithAddressDto("user001", "John", address);

            assert "Seoul".equals(S2Util.getValue(user, "address.city"));
            assert "123 Main St".equals(S2Util.getValue(user, "address.street"));
            assert "12345".equals(S2Util.getValue(user, "address.zipCode"));

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("getValue - Array 인덱스 조회")
    @org.junit.jupiter.api.Test
    void testGetValue_Array() {
        String testName = "getValue - Array";
        try {
            String[] arr = { "alpha", "beta", "gamma" };

            assert "alpha".equals(S2Util.getValue(arr, 0));
            assert "beta".equals(S2Util.getValue(arr, 1));
            assert "gamma".equals(S2Util.getValue(arr, "2"));

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    // ===== setValue Tests =====

    @DisplayName("setValue - 단순 DTO 필드 설정")
    @org.junit.jupiter.api.Test
    void testSetValue_SimpleDTO() {
        String testName = "setValue - Simple DTO";
        try {
            UserDto user = new UserDto();

            S2Util.setValue(user, "id", "newId123");
            S2Util.setValue(user, "name", "Alice");
            S2Util.setValue(user, "age", 25);
            S2Util.setValue(user, "email", "alice@example.com");

            assert "newId123".equals(user.getId());
            assert "Alice".equals(user.getName());
            assert 25 == user.getAge();
            assert "alice@example.com".equals(user.getEmail());

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("setValue - Map 키 설정")
    @org.junit.jupiter.api.Test
    void testSetValue_Map() {
        String testName = "setValue - Map";
        try {
            Map<String, Object> map = new HashMap<>();

            S2Util.setValue(map, "id", "mapId001");
            S2Util.setValue(map, "name", "Test User");
            S2Util.setValue(map, "age", 40);

            assert "mapId001".equals(map.get("id"));
            assert "Test User".equals(map.get("name"));
            assert 40 == (Integer) map.get("age");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("setValue - List 인덱스 설정")
    @org.junit.jupiter.api.Test
    void testSetValue_List() {
        String testName = "setValue - List";
        try {
            List<String> list = new ArrayList<>(List.of("initial1", "initial2", "initial3"));

            S2Util.setValue(list, 0, "modified1");
            S2Util.setValue(list, "1", "modified2");
            S2Util.setValue(list, 2, "modified3");

            assert "modified1".equals(list.get(0));
            assert "modified2".equals(list.get(1));
            assert "modified3".equals(list.get(2));

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("setValue - 중첩 경로 설정 (Dot Notation)")
    @org.junit.jupiter.api.Test
    void testSetValue_NestedPath() {
        String testName = "setValue - Nested Path";
        try {
            AddressDto address = new AddressDto("Old St", "Old City", "00000");
            UserWithAddressDto user = new UserWithAddressDto("user001", "Original", address);

            S2Util.setValue(user, "address.street", "New Street 456");
            S2Util.setValue(user, "address.city", "Busan");
            S2Util.setValue(user, "address.zipCode", "54321");

            assert "New Street 456".equals(user.getAddress().getStreet());
            assert "Busan".equals(user.getAddress().getCity());
            assert "54321".equals(user.getAddress().getZipCode());

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("setValue - Array 인덱스 설정")
    @org.junit.jupiter.api.Test
    void testSetValue_Array() {
        String testName = "setValue - Array";
        try {
            String[] arr = new String[3];

            S2Util.setValue(arr, 0, "element0");
            S2Util.setValue(arr, "1", "element1");
            S2Util.setValue(arr, 2, "element2");

            assert "element0".equals(arr[0]);
            assert "element1".equals(arr[1]);
            assert "element2".equals(arr[2]);

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    // ===== S2Copier Tests =====

    @DisplayName("S2Copier - 단순 DTO 복사")
    @org.junit.jupiter.api.Test
    void testCopier_SimpleDTO() {
        String testName = "S2Copier - Simple DTO";
        try {
            UserDto source = new UserDto("src123", "Source User", 30, "source@example.com");
            UserDto target = S2Copier.from(source).to(UserDto.class);

            assert "src123".equals(target.getId());
            assert "Source User".equals(target.getName());
            assert 30 == target.getAge();
            assert "source@example.com".equals(target.getEmail());

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("S2Copier - 필드 제외")
    @org.junit.jupiter.api.Test
    void testCopier_Exclude() {
        String testName = "S2Copier - Exclude";
        try {
            UserDto source = new UserDto("src123", "Source User", 30, "source@example.com");
            UserDto target = S2Copier.from(source)
                    .exclude("email", "age")
                    .to(UserDto.class);

            assert "src123".equals(target.getId());
            assert "Source User".equals(target.getName());
            assert 0 == target.getAge(); // 제외되었으므로 초기값
            assert target.getEmail() == null; // 제외되었으므로 null

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("S2Copier - 필드 매핑")
    @org.junit.jupiter.api.Test
    void testCopier_Mapping() {
        String testName = "S2Copier - Mapping";
        try {
            // 다른 필드명의 소스
            class OtherDto {
                public String userId;
                public String userName;

                public OtherDto(String userId, String userName) {
                    this.userId = userId;
                    this.userName = userName;
                }
            }

            OtherDto source = new OtherDto("mapped123", "Mapped User");
            UserDto target = S2Copier.from(source)
                    .map("userId", "id")
                    .map("userName", "name")
                    .to(UserDto.class);

            assert "mapped123".equals(target.getId());
            assert "Mapped User".equals(target.getName());

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("S2Copier - ignoreNulls 설정")
    @org.junit.jupiter.api.Test
    void testCopier_IgnoreNulls() {
        String testName = "S2Copier - ignoreNulls";
        try {
            UserDto source = new UserDto("src456", "User", 0, null); // email이 null
            UserDto target = new UserDto("original", "Original", 25, "original@example.com");

            S2Copier.from(source)
                    .ignoreNulls()
                    .to(target);

            assert "src456".equals(target.getId());
            assert "User".equals(target.getName());
            assert 0 == target.getAge();
            assert "original@example.com".equals(target.getEmail()); // null이므로 무시됨

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("S2Copier - 중첩 객체 복사")
    @org.junit.jupiter.api.Test
    void testCopier_NestedObject() {
        String testName = "S2Copier - Nested Object";
        try {
            AddressDto srcAddr = new AddressDto("100 Oak Ave", "Daejeon", "34567");
            UserWithAddressDto source = new UserWithAddressDto("nested001", "Nested User", srcAddr);

            UserWithAddressDto target = S2Copier.from(source).to(UserWithAddressDto.class);

            assert "nested001".equals(target.getId());
            assert "Nested User".equals(target.getName());
            assert target.getAddress() != null;
            assert "100 Oak Ave".equals(target.getAddress().getStreet());
            assert "Daejeon".equals(target.getAddress().getCity());
            assert "34567".equals(target.getAddress().getZipCode());

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    @DisplayName("S2Copier - 체이닝 조합")
    @org.junit.jupiter.api.Test
    void testCopier_ChainingCombination() {
        String testName = "S2Copier - Chaining Combination";
        try {
            class FlexibleDto {
                public String userId;
                public String userName;
                public int userAge;
                public String userEmail;

                public FlexibleDto(String userId, String userName, int userAge, String userEmail) {
                    this.userId = userId;
                    this.userName = userName;
                    this.userAge = userAge;
                    this.userEmail = userEmail;
                }
            }

            FlexibleDto source = new FlexibleDto("flex001", "Flexible", 28, null);
            UserDto target = S2Copier.from(source)
                    .map("userId", "id")
                    .map("userName", "name")
                    .map("userAge", "age")
                    .map("userEmail", "email")
                    .exclude("email")
                    .ignoreNulls()
                    .to(UserDto.class);

            assert "flex001".equals(target.getId());
            assert "Flexible".equals(target.getName());
            assert 28 == target.getAge();
            assert target.getEmail() == null; // 제외됨

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED", e);
        }
    }

    // ===== Test Summary =====

    @DisplayName("SmokeTest - 전체 테스트 통계")
    @org.junit.jupiter.api.Test
    void testSummary() {
        stats.printReport("getValue, setValue, S2Copier");
    }
}
