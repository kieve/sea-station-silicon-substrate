package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.TestUiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiSize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StackLayoutTest {
    @Test
    void layoutSetsChildrenPositionToZeroAndSizeToLayoutSize() {
        StackLayout layout = new StackLayout();
        UiSize layoutSize = new UiSize(128, 64);
        layout.setSize(layoutSize);

        TestUiNode firstChild = new TestUiNode();
        TestUiNode secondChild = new TestUiNode();

        firstChild.setPosition(new UiPosition(3, 4));
        firstChild.setSize(new UiSize(10, 5));
        secondChild.setPosition(new UiPosition(6, 7));
        secondChild.setSize(new UiSize(4, 4));

        layout.add(firstChild);
        layout.add(secondChild);

        assertEquals(UiPosition.ZERO, firstChild.getPosition());
        assertEquals(layoutSize, firstChild.getSize());
        assertEquals(UiPosition.ZERO, secondChild.getPosition());
        assertEquals(layoutSize, secondChild.getSize());
    }

    @Test
    void setSizeRelayoutsChildrenToMatchLayoutSize() {
        StackLayout layout = new StackLayout();
        TestUiNode child = new TestUiNode();

        layout.add(child);

        child.setPosition(new UiPosition(8, 9));
        child.setSize(new UiSize(12, 13));

        UiSize newLayoutSize = new UiSize(200, 120);
        layout.setSize(newLayoutSize);

        assertEquals(UiPosition.ZERO, child.getPosition());
        assertEquals(newLayoutSize, child.getSize());
    }

    @Test
    void setPositionRelayoutsChildrenAndUpdatesOrigin() {
        StackLayout layout = new StackLayout();
        UiPosition layoutOrigin = new UiPosition(1, 2);
        UiSize layoutSize = new UiSize(75, 45);
        TestUiNode child = new TestUiNode();

        layout.setOrigin(layoutOrigin);
        layout.setSize(layoutSize);
        layout.add(child);

        UiPosition newPosition = new UiPosition(6, 8);
        layout.setPosition(newPosition);

        assertEquals(UiPosition.ZERO, child.getPosition());
        assertEquals(new UiPosition(
            layoutOrigin.x() + newPosition.x(),
            layoutOrigin.y() + newPosition.y()
        ), child.getOrigin());
        assertEquals(layoutSize, child.getSize());
    }
}
