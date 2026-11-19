package io.github.devers2.s2util.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import io.github.devers2.s2util.validation.spring.S2BindValidator;

/**
 * 전체 코드 리뷰에서 실측한 서버 결함(날짜 비교, 와일드카드 람다 인자, BindingResult 경로)의 회귀 시험.
 */
class ReviewDefectRegressionTest {

    public static class Period {
        private final Object start;
        private final Object end;

        public Period(Object start, Object end) {
            this.start = start;
            this.end = end;
        }

        public Object getStart() {
            return start;
        }

        public Object getEnd() {
            return end;
        }
    }

    public static class Row {
        private final Integer qty;

        public Row(Integer qty) {
            this.qty = qty;
        }

        public Integer getQty() {
            return qty;
        }
    }

    public static class Order {
        private final List<Row> items;
        private final int limit;

        public Order(List<Row> items, int limit) {
            this.items = items;
            this.limit = limit;
        }

        public List<Row> getItems() {
            return items;
        }

        public int getLimit() {
            return limit;
        }
    }

    public static class Member {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static List<String> invalidFields(S2Validator<Period> validator, Period period) {
        List<String> fields = new ArrayList<>();
        validator.validate(period, e -> fields.add(e.fieldName()), Locale.KOREAN);
        return fields;
    }

    private static S2Validator<Period> endAfterStart() {
        return S2Validator.<Period>builder().field("end", "종료일").rule(S2RuleType.DATE_AFTER, "start").build();
    }

    @Test
    @DisplayName("DATE_AFTER: java.util.Date 필드도 비교한다 (이전에는 항상 통과)")
    void dateAfterComparesUtilDate() {
        Date start = Date.from(LocalDate.of(2024, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date earlier = Date.from(LocalDate.of(2024, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date later = Date.from(LocalDate.of(2024, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Assertions.assertEquals(List.of("end"), invalidFields(endAfterStart(), new Period(start, earlier)));
        Assertions.assertEquals(List.of(), invalidFields(endAfterStart(), new Period(start, later)));
    }

    @Test
    @DisplayName("DATE_AFTER: java.sql.Date·Timestamp 도 비교한다")
    void dateAfterComparesSqlTypes() {
        java.sql.Date start = java.sql.Date.valueOf(LocalDate.of(2024, 5, 1));
        java.sql.Timestamp earlier = java.sql.Timestamp.valueOf(LocalDateTime.of(2024, 4, 30, 23, 59));

        Assertions.assertEquals(List.of("end"), invalidFields(endAfterStart(), new Period(start, earlier)));
    }

    @Test
    @DisplayName("DATE_AFTER: 타입이 다른 값(LocalDate·문자열·LocalDateTime)을 예외 없이 비교한다")
    void dateAfterComparesMixedTypes() {
        Assertions.assertEquals(List.of("end"),
                invalidFields(endAfterStart(), new Period(LocalDate.of(2024, 5, 1), "2024-04-01")));
        Assertions.assertEquals(List.of(),
                invalidFields(endAfterStart(), new Period("2024-05-01", LocalDate.of(2024, 5, 1))));
        Assertions.assertEquals(List.of("end"),
                invalidFields(endAfterStart(), new Period(LocalDateTime.of(2024, 5, 1, 9, 0), LocalDate.of(2024, 5, 1))));
    }

    @Test
    @DisplayName("와일드카드 행의 BiPredicate 는 두 번째 인자로 루트 객체를 받는다")
    void wildcardBiPredicateReceivesRoot() {
        S2Validator<Order> validator = S2Validator.<Order>builder()
                .field("items[].qty", "수량").rule((Integer qty, Order order) -> qty <= order.getLimit())
                .build();
        List<String> invalid = new ArrayList<>();

        validator.validate(new Order(List.of(new Row(1), new Row(9)), 5), e -> invalid.add(e.fieldName()));

        Assertions.assertEquals(List.of("items[1].qty"), invalid);
    }

    @Test
    @DisplayName("여러 값(String[]·List)은 toString 이 아니라 요소별로 판정하고, REQUIRED 는 빈 요소만 있으면 실패한다")
    void multiValuesAreJudgedPerElement() {
        S2Validator<java.util.Map<String, Object>> validator = S2Validator.<java.util.Map<String, Object>>builder()
                .field("name", "이름").rule(S2RuleType.MAX_LENGTH, 5)
                .field("tags", "태그").rule(S2RuleType.MAX_LENGTH, 2)
                .field("codes", "코드").rule(S2RuleType.REQUIRED)
                .build();
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("name", new String[] { "abc" });
        params.put("tags", List.of("ab", "abc"));
        params.put("codes", new String[] { "", " " });
        List<String> invalid = new ArrayList<>();

        validator.validate(params, e -> invalid.add(e.fieldName()));

        Assertions.assertEquals(List.of("tags", "codes"), invalid);
    }

    @Test
    @DisplayName("S2BindValidator: 대상에 없는 경로의 오류도 예외 없이 BindingResult 에 기록한다")
    void bindValidatorRecordsUnreadablePath() {
        S2Validator<Member> validator = S2Validator.<Member>builder().field("nickname", "별명").build();
        Member member = new Member();
        BindingResult bindingResult = new BeanPropertyBindingResult(member, "member");

        S2BindValidator.bind(validator).validate(member, bindingResult);

        Assertions.assertEquals(1, bindingResult.getFieldErrorCount());
        Assertions.assertEquals("nickname", bindingResult.getFieldError().getField());
        Assertions.assertNotNull(bindingResult.getFieldError().getDefaultMessage());
    }
}
