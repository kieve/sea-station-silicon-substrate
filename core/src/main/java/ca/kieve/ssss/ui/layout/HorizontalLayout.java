package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiSize;

public class HorizontalLayout extends UiLayout {
    public HorizontalLayout() {
        super();
    }

    @Override
    public void layout() {
        if (m_children.isEmpty()) return;

        int totalWidth = m_size.w();
        int numChildren = m_children.size();
        int widthPerChild = totalWidth / numChildren;
        int remainder = totalWidth % numChildren;

        for (int i = 0; i < numChildren; i++) {
            UiNode child = m_children.get(i);

            boolean withinRoundedWidth = i < remainder;

            int x;
            if (withinRoundedWidth) {
                x = (widthPerChild + 1) * i;
            } else {
                x = widthPerChild + i + remainder;
            }
            child.setPosition(new UiPosition(x, 0));

            int width = withinRoundedWidth ? widthPerChild + 1 : widthPerChild;
            child.setSize(new UiSize(width, m_size.h()));
        }
    }
}
