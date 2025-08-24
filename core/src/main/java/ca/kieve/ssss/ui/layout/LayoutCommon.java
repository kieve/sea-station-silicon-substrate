package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.HasUiOrigin;
import ca.kieve.ssss.ui.core.HasUiSize;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiOrigin;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiSize;

import java.util.List;

public class LayoutCommon {
    private LayoutCommon() {
        // Do not instantiate
    }

    public static void updateChildOrigin(
        UiOrigin origin,
        UiPosition position,
        List<UiNode> children
    ) {
        children.forEach(child -> updateChildOrigin(origin, position, child));
    }

    public static void updateChildOrigin(
        UiOrigin origin,
        UiPosition position,
        UiNode child
    ) {
        if (child instanceof HasUiOrigin hasOrigin) {
            var originPosition = origin.getPosition();
            var ox = originPosition.x();
            var oy = originPosition.y();
            hasOrigin.setOrigin(new UiPosition(
                ox + position.x(),
                oy + position.y()
            ));
        }
    }

    public static void updateChildSize(UiSize size, UiNode child) {
        if (child instanceof HasUiSize hasSize) {
            hasSize.setUiSize(size);
        }
    }
}
