package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.UiPosition;

public class StackLayout extends UiLayout {
    public StackLayout() {
        super();
    }

    @Override
    public void layout() {
        m_children.forEach(child -> {
            child.setPosition(UiPosition.ZERO);
            child.setSize(m_size);
        });
    }
}
