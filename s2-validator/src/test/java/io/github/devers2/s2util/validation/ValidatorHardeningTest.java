package io.github.devers2.s2util.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 검증기 견고성 시험: build() 뒤 잠금, 목록(EACH) 요소의 순환 참조.
 */
class ValidatorHardeningTest {

    public static class Node {
        private String name;
        private List<Node> children = new ArrayList<>();
        private Node[] array = new Node[0];

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Node> getChildren() {
            return children;
        }

        public Node[] getArray() {
            return array;
        }

        public void setArray(Node[] array) {
            this.array = array;
        }
    }

    @Test
    void builtValidatorCannotBeChangedThroughTheChain() {
        var chain = S2Validator.<Node>builder().field("name", "이름").rule(S2RuleType.REQUIRED);
        S2Validator<Node> validator = chain.build();

        assertThrows(IllegalStateException.class, () -> chain.field("other"));
        assertThrows(IllegalStateException.class, () -> chain.rule(S2RuleType.MAX_LENGTH, 3));
        assertEquals(1, validator.getFields().size());
        assertThrows(UnsupportedOperationException.class, () -> validator.getFields().clear());

        // 같은 체인에서 build() 를 다시 불러도 같은 검증기(잠금 상태 유지)
        assertTrue(chain.build() == validator);
    }

    @Test
    void eachElementThatIsAnAncestorIsReportedAsCircular() {
        S2Validator<Object> item = S2Validator.builder().field("name", "이름").rule(S2RuleType.REQUIRED).build();
        S2Validator<Node> validator = S2Validator.<Node>builder()
                .field("children", "자식").rule(S2RuleType.EACH, item)
                .field("array", "배열").rule(S2RuleType.EACH, item)
                .build();

        Node root = new Node();
        root.setName("root");
        root.getChildren().add(root); // 자기 자신을 자식으로
        root.setArray(new Node[] { root });

        S2ValidationResult result = validator.validateAll(root);

        assertFalse(result.isValid());
        assertTrue(result.toMap().containsKey("children[0]"), result.toString());
        assertTrue(result.toMap().containsKey("array[0]"), result.toString());
    }

    @Test
    void eachWithoutCycleStillValidatesElements() {
        S2Validator<Object> item = S2Validator.builder().field("name", "이름").rule(S2RuleType.REQUIRED).build();
        S2Validator<Node> validator = S2Validator.<Node>builder()
                .field("children", "자식").rule(S2RuleType.EACH, item)
                .build();

        Node root = new Node();
        root.setName("root");
        Node child = new Node();
        root.getChildren().add(child); // 이름 없음 → 오류

        S2ValidationResult result = validator.validateAll(root);

        assertTrue(result.toMap().containsKey("children[0].name"), result.toString());
    }
}
