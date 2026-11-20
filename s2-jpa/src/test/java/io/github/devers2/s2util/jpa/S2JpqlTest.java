package io.github.devers2.s2util.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

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

        void recordFailure(String testName, Throwable e) {
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

    @SuppressWarnings("unchecked")
    private static Parameter<Object> namedParam(String name) {
        Parameter<Object> p = mock(Parameter.class);
        when(p.getName()).thenReturn(name);
        return p;
    }

    @Test
    @DisplayName("Supplier 파라미터: 직전 절이 아니라 렌더링된 JPQL 이 그 파라미터를 쓰는지로 실행을 정한다")
    @SuppressWarnings("null")
    void supplierParameterFollowsRenderedJpqlNotPrecedingClause() {
        java.util.Set<Parameter<?>> params = java.util.Set.of(namedParam("pa"), namedParam("pb"));
        when(typedQuery.getParameters()).thenReturn(params);

        S2Jpql.from(entityManager)
                .type(Member.class)
                .query("SELECT m FROM Member m WHERE 1=1 {{=a}} {{=b}}")
                .bindClause("a", true, "AND m.name = :pa")
                .bindClause("b", false, "AND m.email = :pb")
                // 예전에는 직전 절(b)이 false 라 pa 가 바인딩되지 않았다
                .bindParameter("pa", (java.util.function.Supplier<Object>) () -> "kim")
                // pb 의 절은 빠졌으므로 실행되면 안 된다
                .bindParameter("pb", (java.util.function.Supplier<Object>) () -> {
                    throw new IllegalStateException("must not run");
                })
                .build();

        verify(typedQuery).setParameter("pa", "kim");
        verify(typedQuery, org.mockito.Mockito.never()).setParameter(eq("pb"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Supplier 파라미터: 절보다 먼저 호출해도 되고, 이름 일부만 같은 파라미터(:pax)는 쓰는 것으로 보지 않는다")
    @SuppressWarnings("null")
    void supplierParameterOrderIndependentAndExactName() {
        java.util.Set<Parameter<?>> params = java.util.Set.of(namedParam("pa"), namedParam("pax"));
        when(typedQuery.getParameters()).thenReturn(params);

        S2Jpql.from(entityManager)
                .type(Member.class)
                .query("SELECT m FROM Member m WHERE m.email = :pax {{=a}}")
                .bindParameter("pa", (java.util.function.Supplier<Object>) () -> {
                    throw new IllegalStateException("must not run");
                }, LikeMode.START)
                .bindClause("a", false, "AND m.name LIKE :pa")
                .bindParameter("pax", "e@x")
                .build();

        verify(typedQuery).setParameter("pax", "e@x");
        verify(typedQuery, org.mockito.Mockito.never()).setParameter(eq("pa"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("limit(Supplier): null 을 돌려주면 NPE 없이 그 값을 설정하지 않고, LikeMode 는 CharSequence 에도 적용한다")
    @SuppressWarnings("null")
    void limitSupplierNullAndLikeModeCharSequence() {
        java.util.Set<Parameter<?>> params = java.util.Set.of(namedParam("n"));
        when(typedQuery.getParameters()).thenReturn(params);

        S2Jpql.from(entityManager)
                .type(Member.class)
                .query("SELECT m FROM Member m WHERE m.name LIKE :n")
                .bindParameter("n", new StringBuilder("kim"), LikeMode.ANYWHERE)
                .limit(true, () -> null, () -> 10)
                .build();

        verify(typedQuery).setParameter("n", "%kim%");
        verify(typedQuery).setMaxResults(10);
        verify(typedQuery, org.mockito.Mockito.never()).setFirstResult(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("escapeLike: %, _, \\ 를 역슬래시로 이스케이프하고 null 은 그대로 둔다")
    void escapeLikeEscapesWildcards() {
        org.junit.jupiter.api.Assertions.assertEquals("100\\%\\_a\\\\b", S2Jpql.escapeLike("100%_a\\b"));
        org.junit.jupiter.api.Assertions.assertEquals("plain", S2Jpql.escapeLike("plain"));
        org.junit.jupiter.api.Assertions.assertNull(S2Jpql.escapeLike(null));
    }

    @Test
    @DisplayName("bindOrderBy(허용 목록): 정렬 키를 JPQL 경로로 바꾸고, 목록에 없는 키·잘못된 방향은 버린다")
    @SuppressWarnings("null")
    void bindOrderByWithAllowedFieldsTranslatesAndDropsUnknownKeys() {
        when(typedQuery.getParameters()).thenReturn(java.util.Set.of());
        org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        S2Jpql.from(entityManager)
                .type(Member.class)
                .query("SELECT m FROM Member m {{=cond_order}}")
                .bindOrderBy("cond_order", "name desc, password, age, age drop", java.util.Map.of("name", "m.name", "age", "m.age"))
                .build();

        verify(entityManager).createQuery(sqlCaptor.capture(), eq(Member.class));
        org.junit.jupiter.api.Assertions.assertEquals("SELECT m FROM Member m ORDER BY m.name DESC, m.age", sqlCaptor.getValue());
    }

    @Test
    @DisplayName("bindOrderBy(허용 목록): 허용된 키가 하나도 없으면 ORDER BY 를 붙이지 않는다")
    @SuppressWarnings("null")
    void bindOrderByWithAllowedFieldsSkipsWhenNothingAllowed() {
        when(typedQuery.getParameters()).thenReturn(java.util.Set.of());
        org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

        S2Jpql.from(entityManager)
                .type(Member.class)
                .query("SELECT m FROM Member m {{=cond_order}}")
                .bindOrderBy("cond_order", "password desc", java.util.Map.of("name", "m.name"))
                .build();

        verify(entityManager).createQuery(sqlCaptor.capture(), eq(Member.class));
        org.junit.jupiter.api.Assertions.assertEquals("SELECT m FROM Member m", sqlCaptor.getValue());
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
                    java.util.Set.of(nameParam, ageParam));

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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
                            "AND (m.firstName = :name OR m.lastName = :name2)")
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
        }
    }

    @DisplayName("S2Jpql - bindOrderBy 화이트리스트가 인젝션 페이로드를 차단함")
    @SuppressWarnings({ "null" })
    @Test
    void testBindOrderByRejectsInjectionPayload() {
        String testName = "S2Jpql - bindOrderBy rejects injection payload";
        try {
            // Given: bindOrderBy는 클래스 Javadoc상 "화이트리스트 기반"으로 정렬 구문 인젝션을 막는다고
            // 명시되어 있으나, 과거에는 ASC/DESC 방향만 검증하고 필드/표현식 부분은 그대로 통과시켰다.
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_order}}";
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of());

            org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

            // When: 공백 없는 악성 페이로드(서브쿼리/함수 호출)를 정렬 문자열로 주입 시도
            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindOrderBy("cond_order", "(select1from User)")
                    .build();

            // Then: 화이트리스트에 걸러져 렌더링된 JPQL에 ORDER BY 절 자체가 포함되면 안됨
            verify(entityManager).createQuery(sqlCaptor.capture(), eq(Member.class));
            String renderedSql = sqlCaptor.getValue();

            boolean rejected = !renderedSql.contains("ORDER BY") && !renderedSql.contains("select1from");
            if (!rejected) {
                throw new AssertionError("악성 정렬 페이로드가 화이트리스트를 통과해 렌더링된 JPQL에 포함됨: " + renderedSql);
            }

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
        }
    }

    @DisplayName("S2Jpql - bindOrderBy 화이트리스트가 정상적인 다중 컬럼 정렬은 통과시킴")
    @SuppressWarnings({ "null" })
    @Test
    void testBindOrderByAcceptsLegitimateMultiColumnSort() {
        String testName = "S2Jpql - bindOrderBy accepts legitimate multi-column sort";
        try {
            String jpql = "SELECT m FROM Member m WHERE 1=1 {{=cond_order}}";
            when(typedQuery.getParameters()).thenReturn(java.util.Set.of());

            org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

            S2Jpql.from(entityManager)
                    .type(Member.class)
                    .query(jpql)
                    .bindOrderBy("cond_order", "m.name DESC, m.age")
                    .build();

            verify(entityManager).createQuery(sqlCaptor.capture(), eq(Member.class));
            String renderedSql = sqlCaptor.getValue();

            boolean accepted = renderedSql.contains("ORDER BY m.name DESC, m.age");
            if (!accepted) {
                throw new AssertionError("정상적인 정렬 문자열이 렌더링된 JPQL에 정확히 반영되지 않음: " + renderedSql);
            }

            stats.recordSuccess();
            logger.info("✓ " + testName + " PASSED");
        } catch (Throwable e) {
            stats.recordFailure(testName, e);
            logger.error("✗ " + testName + " FAILED: " + e.getMessage());
            throw new AssertionError(testName + " FAILED", e); // Record for the report, then fail the test. | 보고서용으로 기록한 뒤 시험을 실패시킴
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
