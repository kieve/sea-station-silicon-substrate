package ca.kieve.ssss.ui.core;

import ca.kieve.ssss.ui.TestUiNode;
import ca.kieve.ssss.ui.layout.UiLayoutParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UiNodeTest {
    private static class TestLayoutParams implements UiLayoutParams {
    }

    @Test
    void constructorInitializesZeroValues() {
        TestUiNode node = new TestUiNode();

        assertEquals(new UiPosition(0, 0), node.getPosition());
        assertEquals(new UiSize(0, 0), node.getSize());
        assertEquals(new UiPosition(0, 0), node.getOrigin());
    }

    @Test
    void settersStoreProvidedReferences() {
        TestUiNode node = new TestUiNode();
        UiPosition position = new UiPosition(10, 15);
        UiSize size = new UiSize(50, 60);
        UiPosition origin = new UiPosition(1, 2);
        TestLayoutParams layoutParams = new TestLayoutParams();

        node.setPosition(position);
        node.setSize(size);
        node.setOrigin(origin);
        node.setLayoutParams(layoutParams);

        assertSame(position, node.getPosition());
        assertSame(size, node.getSize());
        assertSame(origin, node.getOrigin());
        assertSame(layoutParams, node.getLayoutParams());
    }

    @Test
    void getScreenPositionCombinesOriginAndPosition() {
        TestUiNode node = new TestUiNode();
        node.setOrigin(new UiPosition(5, 9));
        node.setPosition(new UiPosition(3, 4));

        UiPosition screenPosition = node.getScreenPosition();

        assertEquals(new UiPosition(8, 13), screenPosition);
    }
}
