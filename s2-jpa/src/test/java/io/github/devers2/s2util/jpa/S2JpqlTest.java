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
            System.out.println("");
            System.out.println("╔════════════════════════════════════════════════════════════╗");
            System.out.println("║ Test Report: " + String.format("%-46s", categoryName) + "║");
            System.out.println("╠════════════════════════════════════════════════════════════╣");
            System.out.println("║ Total: " + String.format("%-52d", total) + "║");
            System.out.println("║ ✓ Success: " + String.format("%-48d", success) + "║");
            System.out.println("║ ✗ Failed: " + String.format("%-49d", failed) + "║");
            if (!failures.isEmpty()) {
                System.out.println("╠════════════════════════════════════════════════════════════╣");
                System.out.println("║ Failed Tests:                                              ║");
                for (String failure : failures) {
                    String shortened = failure.length() > 56 ? failure.substring(0, 56) : failure;
                    String line = "║  " + String.format("%-56s", shortened) + "║";
                    System.out.println(line);
                    if (failure.length() > 56) {
                        String rest = failure.substring(56);
                        System.out.println("║  " + String.format("%-56s", rest) + "║");
                    }
                }
            }
            System.out.println("╚════════════════════════════════════════════════════════════╝");
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
            System.out.println("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            System.err.println("✗ " + testName + " FAILED: " + e.getMessage());
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
            System.out.println("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            System.err.println("✗ " + testName + " FAILED: " + e.getMessage());
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
            System.out.println("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            System.err.println("✗ " + testName + " FAILED: " + e.getMessage());
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
            System.out.println("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            System.err.println("✗ " + testName + " FAILED: " + e.getMessage());
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
            System.out.println("✓ " + testName + " PASSED");
        } catch (Exception e) {
            stats.recordFailure(testName, e);
            System.err.println("✗ " + testName + " FAILED: " + e.getMessage());
        }
    }

    @DisplayName("SmokeTest - 전체 테스트 통계")
    @Test
    void testSummary() {
        // 이 테스트는 통계 리포트를 출력하기 위함
        System.out.println("\n[TEST SUMMARY]");
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
