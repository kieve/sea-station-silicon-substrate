package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiSize;

import java.util.Objects;
import java.util.stream.Collectors;

public class HorizontalLayout extends UiLayout {
    public static class LayoutParams implements UiLayoutParams {
        public Integer width = null;

        public LayoutParams() {
        }

        public LayoutParams(Integer width) {
            this.width = width;
        }
    }

    public HorizontalLayout() {
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
        if (m_children.isEmpty()) return;

        int totalWidth = m_size.w();

        var stats = m_children.stream()
            .map(HorizontalLayout::getLayoutParams)
            .filter(Objects::nonNull)
            .filter(params -> params.width != null)
            .collect(Collectors.summarizingInt(v -> v.width));

        int numReserved = Math.toIntExact(stats.getCount());
        int reservedWidth = Math.toIntExact(stats.getSum());

        int dynamicWidth = totalWidth - reservedWidth;

        int numChildren = m_children.size();
        int numChildrenUnreserved = Math.max(numChildren - numReserved, 1);
        int widthPerChild = dynamicWidth / numChildrenUnreserved;
        int remainder = dynamicWidth % numChildrenUnreserved;

        int dynamicNodesSeen = 0;
        int x = 0;
        for (UiNode child : m_children) {
            var layoutParams = getLayoutParams(child);
            if (layoutParams != null && layoutParams.width != null) {
                child.setPosition(new UiPosition(x, 0));
                child.setSize(new UiSize(layoutParams.width, m_size.h()));
                x += layoutParams.width;
                continue;
            }

            boolean withinRoundedWidth = dynamicNodesSeen < remainder;
            if (withinRoundedWidth) {
                dynamicNodesSeen++;
            }

            child.setPosition(new UiPosition(x, 0));

            int width = withinRoundedWidth ? widthPerChild + 1 : widthPerChild;
            child.setSize(new UiSize(width, m_size.h()));

            x += width;
        }
    }
}
