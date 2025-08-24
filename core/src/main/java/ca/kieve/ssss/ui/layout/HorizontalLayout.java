package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.HasUiPosition;
import ca.kieve.ssss.ui.core.HasUiSize;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiOrigin;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiRenderable;
import ca.kieve.ssss.ui.core.UiSize;

import java.util.ArrayList;
import java.util.List;

public class HorizontalLayout implements UiLayout<HorizontalLayout> {
    private UiPosition m_position;
    private UiSize m_size;

    private final UiOrigin m_origin;
    private final List<UiNode> m_children;

    public HorizontalLayout() {
        this(UiPosition.ZERO, UiSize.ZERO);
    }

    public HorizontalLayout(UiPosition position, UiSize size) {
        m_position = position;
        m_size = size;
        m_origin = new UiOrigin();
        m_children = new ArrayList<>();
    }

    @Override
    public UiPosition getUiPosition() {
        return m_position;
    }

    @Override
    public void setUiPosition(UiPosition uiPosition) {
        m_position = uiPosition;
        LayoutCommon.updateChildOrigin(m_origin, m_position, m_children);
    }

    @Override
    public UiSize getUiSize() {
        return m_size;
    }

    @Override
    public void setUiSize(UiSize uiSize) {
        m_size = uiSize;
        layout();
    }

    @Override
    public UiPosition getOrigin() {
        return m_origin.getPosition();
    }

    @Override
    public void setOrigin(UiPosition origin) {
        m_origin.setPosition(origin);
        LayoutCommon.updateChildOrigin(m_origin, m_position, m_children);
    }

    @Override
    public HorizontalLayout addChild(UiNode child) {
        m_children.add(child);
        layout();
        return this;
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        m_children.forEach(child -> {
            if (child instanceof UiRenderable renderable) {
                renderable.update(renderContext, delta);
            }
        });
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        m_children.forEach(child -> {
            if (child instanceof UiRenderable renderable) {
                renderable.render(renderContext, delta);
            }
        });
    }

    private void layout() {
        if (m_children.isEmpty()) return;

        int totalWidth = m_size.w();
        int numChildren = m_children.size();
        int widthPerChild = totalWidth / numChildren;
        int remainder = totalWidth % numChildren;

        for (int i = 0; i < numChildren; i++) {
            boolean withinRoundedWidth = i < remainder;
            UiNode child = m_children.get(i);
            if (child instanceof HasUiPosition hasPos) {
                int x;
                if (withinRoundedWidth) {
                    x = (widthPerChild + 1) * i;
                } else {
                    x = widthPerChild + i + remainder;
                }
                hasPos.setUiPosition(new UiPosition(x, 0));
            }
            if (child instanceof HasUiSize hasSize) {
                int width = withinRoundedWidth ? widthPerChild + 1 : widthPerChild;
                hasSize.setUiSize(new UiSize(width, m_size.h()));
            }
        }
    }
}
