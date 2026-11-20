package io.github.devers2.s2util.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void resetGlobalState() {
        S2Validator.resetAll();
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
    @DisplayName("메시지 우선순위: 명시 키 번들 > 규칙에 단 메시지 > 내장 키 번들 > 내장 메시지")
    void ruleMessageWinsOverBuiltInKey() {
        S2Validator.setMessageResolver((key, locale) -> switch (key) {
            case "valid.err.required" -> java.util.Optional.of("공통: {0|을} 입력하세요.");
            case "member.name.required" -> java.util.Optional.of("명시 키: {0|을} 입력하세요.");
            default -> java.util.Optional.empty();
        });
        S2Validator<Member> validator = S2Validator.<Member>builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED).ko("규칙 메시지: {0|은} 필수")
                .build();
        S2Validator<Member> builtIn = S2Validator.<Member>builder().field("name", "이름").rule(S2RuleType.REQUIRED).build();
        S2Validator<Member> explicit = S2Validator.<Member>builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED, null, "member.name.required").ko("규칙 메시지")
                .build();

        Assertions.assertEquals("규칙 메시지: 이름은 필수", firstMessage(validator));
        Assertions.assertEquals("공통: 이름을 입력하세요.", firstMessage(builtIn));
        Assertions.assertEquals("명시 키: 이름을 입력하세요.", firstMessage(explicit));
    }

    private static String firstMessage(S2Validator<Member> validator) {
        List<String> messages = new ArrayList<>();
        validator.validate(new Member(), e -> messages.add(e.defaultMessage()), Locale.KOREAN);
        return messages.get(0);
    }

    public record Point(int x, int y) {
    }

    public static class Flags extends Member {
        public boolean isActive() {
            return true;
        }
    }

    @Test
    @DisplayName("필드명 오타 경고: getter·is getter·상속 필드·record 구성요소는 있는 속성으로, 오타는 없는 속성으로 판단한다")
    void propertyCheckFindsTypos() {
        Assertions.assertTrue(S2PropertyCheck.hasProperty(Member.class, "name"));
        Assertions.assertTrue(S2PropertyCheck.hasProperty(Flags.class, "active"));
        Assertions.assertTrue(S2PropertyCheck.hasProperty(Flags.class, "name"));
        Assertions.assertTrue(S2PropertyCheck.hasProperty(Point.class, "x"));
        Assertions.assertFalse(S2PropertyCheck.hasProperty(Member.class, "nmae"));
    }

    @Test
    @DisplayName("평면 Map 의 점(.)이 든 키는 그대로 찾고, 중첩 Map 경로도 계속 동작한다")
    void flatMapKeyWithDot() {
        S2Validator<java.util.Map<String, Object>> validator = S2Validator.<java.util.Map<String, Object>>builder()
                .field("address.zip", "우편번호").rule(S2RuleType.ZIP)
                .build();
        java.util.Map<String, Object> flat = new java.util.HashMap<>();
        flat.put("address.zip", "abc");
        java.util.Map<String, Object> nested = new java.util.HashMap<>();
        nested.put("address", java.util.Map.of("zip", "12345"));
        List<String> invalid = new ArrayList<>();

        validator.validate(flat, e -> invalid.add(e.fieldName()));
        validator.validate(nested, e -> invalid.add("nested:" + e.fieldName()));

        Assertions.assertEquals(List.of("address.zip"), invalid);
    }

    @Test
    @DisplayName("규칙 JSON 은 <·>·& 를 이스케이프해 <script> 안에서도 태그가 닫히지 않고, 다시 읽으면 같은 값이다")
    void rulesJsonEscapesHtml() {
        S2Validator<Member> validator = S2Validator.<Member>builder().field("name", "</script><b>&").build();

        String json = validator.getRulesJson(Locale.KOREAN);

        Assertions.assertFalse(json.contains("<") || json.contains(">") || json.contains("&"), json);
        Assertions.assertEquals("</script><b>&", S2Validator.fromJson(json).getFields().get(0).getLabel());
    }

    @Test
    @DisplayName("공유 S2Rule.required() 는 메시지를 바꿀 수 없다")
    void sharedRequiredRuleIsImmutable() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> S2Rule.required().storeMessage("ko", "x"));
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

    public static class LazyDto {
        public String getEmail() {
            throw new IllegalStateException("could not initialize proxy - no Session");
        }
    }

    @Test
    @DisplayName("getter 가 예외를 던지면 빈 값으로 보고 통과시키지 않고 S2PropertyAccessException 을 전파한다")
    void getterFailureIsNotTreatedAsEmptyValue() {
        S2Validator<LazyDto> validator = S2Validator.<LazyDto>builder()
                .field("email", "이메일").rule(S2RuleType.EMAIL)
                .build();
        var e = Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> validator.validate(new LazyDto()));
        Assertions.assertEquals("email", e.getPropertyPath());
        Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> validator.validateAll(new LazyDto()));
    }

    @Test
    @DisplayName("검증기는 Serializable 이 아니다(람다 규칙 때문에 지킬 수 없는 계약) — static final 상수로 공유한다")
    void validatorIsNotSerializable() {
        Assertions.assertFalse(java.io.Serializable.class.isAssignableFrom(S2Validator.class));
        Assertions.assertFalse(java.io.Serializable.class.isAssignableFrom(S2Field.class));
        Assertions.assertFalse(java.io.Serializable.class.isAssignableFrom(S2Rule.class));
        Assertions.assertFalse(java.io.Serializable.class.isAssignableFrom(S2Condition.class));
    }
}
