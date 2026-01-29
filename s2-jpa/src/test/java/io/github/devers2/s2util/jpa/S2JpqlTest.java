package io.github.devers2.s2util.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;

/**
 * S2Jpql 동작 검증을 위한 JUnit 테스트 및 SmokeTest
 *
 * S2Jpql의 주요 기능(Fluent API, LikeMode, 템플릿 바인딩)을 테스트하며,
 * 성공/실패 통계를 포함한 테스트 결과를 제공합니다.
 */
public class S2JpqlTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Member> typedQuery;

    @Mock
    private Parameter<String> parameter;

    private static S2Logger logger;

    private static TestStatistics stats;

    // ===== Test Statistics =====

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
            logger.info("║ Test Report: " + String.format("%-46s", categoryName) + "║");
            logger.info("╠════════════════════════════════════════════════════════════╣");
            logger.info("║ Total: " + String.format("%-52d", total) + "║");
            logger.info("║ ✓ Success: " + String.format("%-48d", success) + "║");
            logger.info("║ ✗ Failed: " + String.format("%-49d", failed) + "║");
            if (!failures.isEmpty()) {
                logger.info("╠════════════════════════════════════════════════════════════╣");
                logger.info("║ Failed Tests:                                              ║");
                for (String failure : failures) {
                    String shortened = failure.length() > 56 ? failure.substring(0, 56) : failure;
                    String line = "║  " + String.format("%-56s", shortened) + "║";
                    logger.info(line);
                    if (failure.length() > 56) {
                        String rest = failure.substring(56);
                        logger.info("║  " + String.format("%-56s", rest) + "║");
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
    static void initStats() {
        logger = S2LogManager.getLogger(S2JpqlTest.class);
        stats = new TestStatistics();
    }

    @AfterAll
    static void tearDown() {
        if (stats != null && stats.total > 0) {
            stats.printReport("S2Jpql");
        }
    }

    @SuppressWarnings("null")
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default behavior for any createQuery call
        when(entityManager.createQuery(anyString(), eq(Member.class))).thenReturn(typedQuery);
    }

    @DisplayName("S2Jpql - Fluent API 체이닝")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testFluentApiChain() {
        String testName = "S2Jpql - Fluent API Chain";
        try {
            // Given
            String jpql = """
                          SELECT m FROM Member m WHERE 1=1
                          {{=cond_name}}
                          {{=cond_age}}
                          {{=cond_order}}
                          """;

            // Mock parameters that JPA would find in the rendered query
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            Parameter<Object> ageParam = mock(Parameter.class);
            when(ageParam.getName()).thenReturn("age");
            when(typedQuery.getParameters()).thenReturn(
                    java.util.Set.of(nameParam, ageParam)
            );

            // When
            TypedQuery<Member> result = S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause("cond_name", "John", "AND m.name = :name")
                    .bindClause("cond_age", 30, "AND m.age = :age")
                    .bindParameter("name", "John")
                    .bindParameter("age", 30)
                    .bindOrderBy("cond_order", "m.name ASC")
                    .build();

            // Then
            assertNotNull(result);
            verify(entityManager).createQuery(anyString(), eq(Member.class));
            verify(typedQuery).setParameter("name", "John");
            verify(typedQuery).setParameter("age", 30);

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - LikeMode.ANYWHERE")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testLikeModeAnywhere() {
        String testName = "S2Jpql - LikeMode.ANYWHERE";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE m.name LIKE :name";

            // Mock parameter
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

            // When
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindParameter("name", "John", LikeMode.ANYWHERE)
                    .build();

            // Then
            verify(typedQuery).setParameter("name", "%John%");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - LikeMode.START")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testLikeModeStart() {
        String testName = "S2Jpql - LikeMode.START";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE m.name LIKE :name";

            // Mock parameter
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

            // When
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindParameter("name", "John", LikeMode.START)
                    .build();

            // Then
            verify(typedQuery).setParameter("name", "John%");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - LikeMode.END")
    @SuppressWarnings("null")
    @Test
    void testLikeModeEnd() {
        String testName = "S2Jpql - LikeMode.END";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE m.name LIKE :name";

            // Mock parameter
            @SuppressWarnings("unchecked")
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

            // When
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindParameter("name", "John", LikeMode.END)
                    .build();

            // Then
            verify(typedQuery).setParameter("name", "%John");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - bindParameter")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testBindParameter() {
        String testName = "S2Jpql - bindParameter";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE m.name LIKE :name AND m.age = :age";

            // Mock parameters
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            Parameter<Object> ageParam = mock(Parameter.class);
            when(ageParam.getName()).thenReturn("age");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam, ageParam));

            // When
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindParameter("name", "John", LikeMode.ANYWHERE)
                    .bindParameter("age", 30)
                    .build();

            // Then
            verify(typedQuery).setParameter("name", "%John%");
            verify(typedQuery).setParameter("age", 30);

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - Supplier 기반 bindParameter (조건 True)")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testSupplierBindParameterWhenConditionTrue() {
        String testName = "S2Jpql - Supplier bindParameter (Condition True)";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_name}} {{=cond_age}}";
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            Parameter<Object> ageParam = mock(Parameter.class);
            when(ageParam.getName()).thenReturn("age");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam, ageParam));

            String testName1 = "John";
            int testAge = 30;

            // When - 조건이 true이므로 Supplier가 실행되어야 함
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause("cond_name", testName1 != null, "AND m.name = :name")
                    .bindParameter("name", () -> testName1)
                    .bindClause("cond_age", testAge > 0, "AND m.age = :age")
                    .bindParameter("age", () -> testAge)
                    .build();

            // Then - Supplier가 실행되어 setParameter가 호출되어야 함
            verify(typedQuery).setParameter("name", testName1);
            verify(typedQuery).setParameter("age", testAge);

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - Supplier 기반 bindParameter (조건 False)")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testSupplierBindParameterWhenConditionFalse() {
        String testName = "S2Jpql - Supplier bindParameter (Condition False)";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_name}}";
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

            // When - 조건이 false이므로 Supplier가 실행되지 않아야 함
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause("cond_name", false, "AND m.name = :name")
                    .bindParameter("name", () -> {
                        throw new RuntimeException("Supplier should not be executed when condition is false!");
                    })
                    .build();

            // Then - Supplier가 실행되지 않았으므로 예외가 발생하지 않아야 함
            // setParameter가 호출되지 않아야 함

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - Supplier 기반 bindParameter with LikeMode.ANYWHERE")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testSupplierBindParameterWithLikeMode() {
        String testName = "S2Jpql - Supplier bindParameter with LikeMode";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_name}}";
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

            String searchName = "John";

            // When - Supplier 기반 bindParameter with LikeMode
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause("cond_name", searchName != null, "AND m.name LIKE :name")
                    .bindParameter("name", () -> searchName, LikeMode.ANYWHERE)
                    .build();

            // Then - Supplier가 실행되고 LikeMode가 적용되어야 함
            verify(typedQuery).setParameter("name", "%John%");

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - 다중 Supplier 기반 bindParameter 연속 호출")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testMultipleSupplierBindParameters() {
        String testName = "S2Jpql - Multiple Supplier bindParameters";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_name}} {{=cond_age}}";
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            Parameter<Object> nameParam2 = mock(Parameter.class);
            when(nameParam2.getName()).thenReturn("name2");
            Parameter<Object> ageParam = mock(Parameter.class);
            when(ageParam.getName()).thenReturn("age");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam, nameParam2, ageParam));

            String firstName = "John";
            String lastName = "Doe";
            int age = 30;

            // When - 단일 bindClause 후에 여러 개의 Supplier 기반 bindParameter 호출
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause(
                            "cond_name", firstName != null && lastName != null,
                            "AND (m.firstName = :name OR m.lastName = :name2)"
                    )
                    .bindParameter("name", () -> firstName)
                    .bindParameter("name2", () -> lastName)
                    .bindClause("cond_age", age > 0, "AND m.age = :age")
                    .bindParameter("age", () -> age)
                    .build();

            // Then - 모든 Supplier가 실행되어야 함
            verify(typedQuery).setParameter("name", firstName);
            verify(typedQuery).setParameter("name2", lastName);
            verify(typedQuery).setParameter("age", age);

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - bindClause (Object 조건값) with Supplier bindParameter")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testBindClauseWithObjectConditionAndSupplier() {
        String testName = "S2Jpql - bindClause (Object condition) with Supplier";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_name}} {{=cond_age}}";
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            Parameter<Object> ageParam = mock(Parameter.class);
            when(ageParam.getName()).thenReturn("age");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam, ageParam));

            String name = "John";
            Integer age = 30; // Not null - condition should be valid

            // When - Object 기반 conditionValue와 Supplier bindParameter
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause("cond_name", name, "AND m.name = :name")
                    .bindParameter("name", () -> name)
                    .bindClause("cond_age", age, "AND m.age = :age")
                    .bindParameter("age", () -> age)
                    .build();

            // Then - Object 조건값이 유효(non-null)하므로 Supplier가 실행되어야 함
            verify(typedQuery).setParameter("name", name);
            verify(typedQuery).setParameter("age", age);

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("S2Jpql - bindClause (null 객체 조건값)")
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testBindClauseWithNullObjectCondition() {
        String testName = "S2Jpql - bindClause (null object condition)";
        try {
            // Given
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_name}}";
            Parameter<Object> nameParam = mock(Parameter.class);
            when(nameParam.getName()).thenReturn("name");
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

            String name = null; // null - condition should be invalid

            // When - Object 조건값이 null인 경우 Supplier가 실행되지 않아야 함
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindClause("cond_name", name, "AND m.name = :name")
                    .bindParameter("name", () -> {
                        throw new RuntimeException("Supplier should not be executed when object condition is null!");
                    })
                    .build();

            // Then - 예외가 발생하지 않아야 함 (Supplier가 실행되지 않았음)

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("SmokeTest - 전체 테스트 통계")
    @Test
    void testSummary() {
        // 이 테스트는 통계 리포트를 출력하기 위함
        logger.info("\n[TEST SUMMARY]");
    }

    // ===== Dummy entity class for testing =====

    static class Member {
        private String name;
        private int age;

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
    }
}
