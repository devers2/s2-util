package io.github.devers2.s2util.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;

class S2JpqlTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Member> typedQuery;

    @Mock
    private Parameter<String> parameter;

    @SuppressWarnings("null")
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default behavior for any createQuery call
        when(entityManager.createQuery(anyString(), eq(Member.class))).thenReturn(typedQuery);
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testFluentApiChain() {
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
                .applyClause("cond_name", "name", "John", "AND m.name = :name")
                .applyClause("cond_age", "age", 30, "AND m.age = :age")
                .applyOrderBy("cond_order", "m.name ASC")
                .build();

        // Then
        assertNotNull(result);
        System.out.println("Test completed successfully");
        verify(entityManager).createQuery(anyString(), eq(Member.class));
        verify(typedQuery).setParameter("name", "John");
        verify(typedQuery).setParameter("age", 30);
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testLikeModeAnywhere() {
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
                .applyClause("dummy", "name", "John", "dummy", LikeMode.ANYWHERE)
                .build();

        // Then
        verify(typedQuery).setParameter("name", "%John%");
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testLikeModeStart() {
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
                .applyClause("dummy", "name", "John", "dummy", LikeMode.START)
                .build();

        // Then
        verify(typedQuery).setParameter("name", "John%");
    }

    @SuppressWarnings("null")
    @Test
    void testLikeModeEnd() {
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
                .applyClause("dummy", "name", "John", "dummy", LikeMode.END)
                .build();

        // Then
        verify(typedQuery).setParameter("name", "%John");
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void testS2TemplateMethods() {
        // Given
        String jpql = "SELECT m FROM Member m {{=where}} {{=order}}";

        // Mock parameters
        Parameter<Object> nameParam = mock(Parameter.class);
        when(nameParam.getName()).thenReturn("name");
        when(typedQuery.getParameters()).thenReturn(java.util.Set.of(nameParam));

        // When
        S2Jpql.from(entityManager)
                .type(Member.class)
                .query(jpql)
                .bind("where", "WHERE m.active = 1")
                .bindWhen("order", true, "ORDER BY m.name", "")
                .applyClause("dummy", "name", "John", "dummy", LikeMode.ANYWHERE)
                .build();

        // Then
        verify(typedQuery).setParameter("name", "%John%");
    }

    // Dummy entity class for testing
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
