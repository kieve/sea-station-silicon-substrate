package ca.kieve.ssss.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListUtilTest {
    @Test
    void flattenReturnsEmptyArrayWhenNoArguments() {
        Object[] result = ListUtil.flatten();

        assertEquals(0, result.length);
    }

    @Test
    void flattenFlattensNestedListsRecursively() {
        Object[] result = ListUtil.flatten(
            1,
            List.of(2, List.of(3, List.of(4))),
            List.of(),
            5
        );

        assertArrayEquals(new Object[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void flattenPreservesOrderOfMixedValues() {
        Object[] result = ListUtil.flatten(
            "alpha",
            List.of("beta", List.of("gamma", 42)),
            3.14
        );

        assertArrayEquals(new Object[]{"alpha", "beta", "gamma", 42, 3.14}, result);
    }
}
