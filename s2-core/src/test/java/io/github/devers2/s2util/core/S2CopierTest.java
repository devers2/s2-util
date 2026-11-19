package io.github.devers2.s2util.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * S2Copier 시험: 상속 필드, 빈 값 유지, 깊은 복사(JDK 값 타입·배열·Date·정렬 컬렉션·protected 생성자).
 */
class S2CopierTest {

    public static class Base {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class Dto extends Base {
        public static String SHARED = "static";
        private String name;
        private String memo;
        private List<String> tags;
        private LocalDate birth;
        private BigDecimal amount;
        private int[] scores;
        private Date created;
        private TreeSet<String> sorted;
        private TreeMap<String, Integer> ranking;
        private Child child;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMemo() {
            return memo;
        }

        public void setMemo(String memo) {
            this.memo = memo;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public LocalDate getBirth() {
            return birth;
        }

        public void setBirth(LocalDate birth) {
            this.birth = birth;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public int[] getScores() {
            return scores;
        }

        public void setScores(int[] scores) {
            this.scores = scores;
        }

        public Date getCreated() {
            return created;
        }

        public void setCreated(Date created) {
            this.created = created;
        }

        public TreeSet<String> getSorted() {
            return sorted;
        }

        public void setSorted(TreeSet<String> sorted) {
            this.sorted = sorted;
        }

        public TreeMap<String, Integer> getRanking() {
            return ranking;
        }

        public void setRanking(TreeMap<String, Integer> ranking) {
            this.ranking = ranking;
        }

        public Child getChild() {
            return child;
        }

        public void setChild(Child child) {
            this.child = child;
        }
    }

    /** JPA 엔티티처럼 protected 기본 생성자만 있는 클래스 */
    public static class Child {
        private String value;

        protected Child() {
        }

        Child(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static Dto sample() {
        Dto d = new Dto();
        d.setId(7L);
        d.setName("n");
        d.setMemo("");
        d.setTags(new ArrayList<>());
        d.setBirth(LocalDate.of(2000, 1, 2));
        d.setAmount(new BigDecimal("12.50"));
        d.setScores(new int[] { 1, 2 });
        d.setCreated(new Date(1_000L));
        TreeSet<String> sorted = new TreeSet<>(Comparator.reverseOrder());
        sorted.add("a");
        sorted.add("b");
        d.setSorted(sorted);
        TreeMap<String, Integer> ranking = new TreeMap<>(Comparator.reverseOrder());
        ranking.put("x", 1);
        ranking.put("y", 2);
        d.setRanking(ranking);
        d.setChild(new Child("c"));
        return d;
    }

    @Test
    void copiesInheritedFieldsAndKeepsEmptyValues() {
        Dto copy = S2Copier.from(sample()).to(Dto.class);

        assertEquals(7L, copy.getId());
        assertEquals("", copy.getMemo());
        assertEquals(List.of(), copy.getTags());
        assertEquals("n", copy.getName());
    }

    @Test
    void mapTargetIncludesInheritedFieldsButNotStaticOnes() {
        Map<String, Object> map = S2Copier.from(sample()).to(new HashMap<>());

        assertEquals(7L, map.get("id"));
        assertEquals("", map.get("memo"));
        assertFalse(map.containsKey("SHARED"));
    }

    @Test
    void deepCopyKeepsJdkValueTypesAndCopiesMutableOnes() {
        Dto source = sample();
        Dto copy = S2Copier.from(source).deep().to(Dto.class);

        assertEquals(LocalDate.of(2000, 1, 2), copy.getBirth());
        assertEquals(new BigDecimal("12.50"), copy.getAmount());

        assertNotSame(source.getScores(), copy.getScores());
        assertArrayEquals(new int[] { 1, 2 }, copy.getScores());

        assertNotSame(source.getCreated(), copy.getCreated());
        assertEquals(source.getCreated(), copy.getCreated());

        assertNotSame(source.getChild(), copy.getChild());
        assertEquals("c", copy.getChild().getValue());
    }

    @Test
    void deepCopyKeepsSortedCollectionsSorted() {
        Dto copy = S2Copier.from(sample()).deep().to(Dto.class);

        assertInstanceOf(TreeSet.class, copy.getSorted());
        assertEquals(List.of("b", "a"), new ArrayList<>(copy.getSorted()));
        assertEquals(List.of("y", "x"), new ArrayList<>(copy.getRanking().keySet()));
    }

    @Test
    void shallowCopySharesReferences() {
        Dto source = sample();
        Dto copy = S2Copier.from(source).to(Dto.class);

        assertSame(source.getScores(), copy.getScores());
        assertSame(source.getChild(), copy.getChild());
    }

    @Test
    void copiesFromMapSource() {
        Map<String, Object> source = new HashMap<>();
        source.put("name", "m");
        source.put("memo", "");

        Dto dto = S2Copier.from(source).to(Dto.class);
        Map<String, Object> map = S2Copier.from(source).to(new HashMap<>());

        assertEquals("m", dto.getName());
        assertEquals("", dto.getMemo());
        assertEquals(source, map);
        assertTrue(map.containsKey("memo"));
    }
}
