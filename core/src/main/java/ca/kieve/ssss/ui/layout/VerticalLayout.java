package ca.kieve.ssss.ui.layout;

import java.util.Objects;
import java.util.stream.Collectors;

import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiSize;

public class VerticalLayout extends UiLayout {
    public static class LayoutParams implements UiLayoutParams {
        public Integer height = null;

        public LayoutParams() {
        }

        public LayoutParams(Integer height) {
            this.height = height;
        }
    }

    public VerticalLayout() {
        super();
    }

    private static LayoutParams getLayoutParams(UiNode child) {
        var layoutParams = child.getLayoutParams();
        if (layoutParams instanceof LayoutParams typed) {
            return typed;
        }
        return null;
    }

    @Override
    public void layout() {
        if (m_children.isEmpty()) {
            return;
        }

        int totalHeight = m_size.h();

        var stats = m_children.stream()
            .map(VerticalLayout::getLayoutParams)
            .filter(Objects::nonNull)
            .filter(params -> params.height != null)
            .collect(Collectors.summarizingInt(v -> v.height));

        int numReserved = Math.toIntExact(stats.getCount());
        int reservedHeight = Math.toIntExact(stats.getSum());

        int dynamicHeight = totalHeight - reservedHeight;

        int numChildren = m_children.size();
        int numChildrenUnreserved = Math.max(numChildren - numReserved, 1);
        int heightPerChild = dynamicHeight / numChildrenUnreserved;
        int remainder = dynamicHeight % numChildrenUnreserved;

        // First pass: calculate heights for all children
        int[] heights = new int[numChildren];
        int dynamicNodesSeen = 0;
        for (int i = 0; i < numChildren; i++) {
            var child = m_children.get(i);
            var layoutParams = getLayoutParams(child);
            if (layoutParams != null && layoutParams.height != null) {
                heights[i] = layoutParams.height;
                continue;
            }

            boolean withinRoundedHeight = dynamicNodesSeen < remainder;
            if (withinRoundedHeight) {
                dynamicNodesSeen++;
            }
            heights[i] = withinRoundedHeight ? heightPerChild + 1 : heightPerChild;
        }

        // Second pass: position children from top to bottom (Y-down coordinate system)
        // First child at top (y=0), subsequent children below
        int y = 0;
        for (int i = 0; i < numChildren; i++) {
            var child = m_children.get(i);
            child.setPosition(new UiPosition(0, y));
            child.setSize(new UiSize(m_size.w(), heights[i]));
            y += heights[i];
        }
    }
}
