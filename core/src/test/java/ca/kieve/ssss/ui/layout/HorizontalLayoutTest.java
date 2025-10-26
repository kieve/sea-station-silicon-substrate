package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.TestUiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiSize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HorizontalLayoutTest {
    @Test
    void layoutWithNoChildrenDoesNotThrow() {
        HorizontalLayout layout = new HorizontalLayout();

        layout.setSize(new UiSize(42, 7));

        assertDoesNotThrow(layout::layout);
    }

    @Test
    void layoutDistributesWidthAcrossReservedAndDynamicChildren() {
        HorizontalLayout layout = new HorizontalLayout();
        UiSize layoutSize = new UiSize(10, 5);
        layout.setSize(layoutSize);

        TestUiNode reservedChild = new TestUiNode();
        reservedChild.setPosition(new UiPosition(100, 100));
        reservedChild.setSize(new UiSize(100, 100));

        TestUiNode dynamicChildWithRemainder = new TestUiNode();
        dynamicChildWithRemainder.setPosition(new UiPosition(200, 200));
        dynamicChildWithRemainder.setSize(new UiSize(200, 200));

        TestUiNode dynamicChildWithNullWidth = new TestUiNode();
        dynamicChildWithNullWidth.setPosition(new UiPosition(300, 300));
        dynamicChildWithNullWidth.setSize(new UiSize(300, 300));

        TestUiNode plainDynamicChild = new TestUiNode();
        plainDynamicChild.setPosition(new UiPosition(400, 400));
        plainDynamicChild.setSize(new UiSize(400, 400));

        layout.add(reservedChild, new HorizontalLayout.LayoutParams(3));
        layout.add(dynamicChildWithRemainder);
        layout.add(dynamicChildWithNullWidth, new HorizontalLayout.LayoutParams());
        layout.add(plainDynamicChild);

        layout.layout();

        assertEquals(new UiPosition(0, 0), reservedChild.getPosition());
        assertEquals(new UiSize(3, layoutSize.h()), reservedChild.getSize());

        assertEquals(new UiPosition(3, 0), dynamicChildWithRemainder.getPosition());
        assertEquals(new UiSize(3, layoutSize.h()), dynamicChildWithRemainder.getSize());

        assertEquals(new UiPosition(6, 0), dynamicChildWithNullWidth.getPosition());
        assertEquals(new UiSize(2, layoutSize.h()), dynamicChildWithNullWidth.getSize());

        assertEquals(new UiPosition(8, 0), plainDynamicChild.getPosition());
        assertEquals(new UiSize(2, layoutSize.h()), plainDynamicChild.getSize());
    }
}
