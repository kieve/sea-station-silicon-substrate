package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.HasUiOrigin;
import ca.kieve.ssss.ui.core.HasUiSize;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiOrigin;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiRenderable;
import ca.kieve.ssss.ui.core.UiSize;

public class NodeLayout implements UiLayout<NodeLayout> {
    private UiPosition m_position;
    private UiSize m_size;
    private final UiOrigin m_origin;
    private UiNode m_child = null;

    public NodeLayout() {
        this(UiPosition.ZERO, UiSize.ZERO);
    }

    public NodeLayout(UiPosition position, UiSize size) {
        m_position = position;
        m_size = size;
        m_origin = new UiOrigin();
    }

    @Override
    public UiPosition getUiPosition() {
        return m_position;
    }

    @Override
    public void setUiPosition(UiPosition uiPosition) {
        m_position = uiPosition;
    }

    @Override
    public UiSize getUiSize() {
        return m_size;
    }

    @Override
    public void setUiSize(UiSize uiSize) {
        m_size = uiSize;
        // Relayout children...
        if (m_child instanceof HasUiSize hasSize) {
            hasSize.setUiSize(m_size);
        }
    }

    @Override
    public UiOrigin getOrigin() {
        return m_origin;
    }

    @Override
    public NodeLayout addChild(UiNode child) {
        m_child = child;
        return this;
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        if (m_child instanceof HasUiOrigin hasOrigin) {
            hasOrigin.getOrigin().setOrigin(m_position);
        }
        if (m_child instanceof UiRenderable renderable) {
            renderable.update(renderContext, delta);
        }
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        if (m_child instanceof UiRenderable renderable) {
            renderable.render(renderContext, delta);
        }
    }
}
