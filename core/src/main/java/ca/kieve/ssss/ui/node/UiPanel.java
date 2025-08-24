package ca.kieve.ssss.ui.node;

import ca.kieve.ssss.ui.core.HasUiPosition;
import ca.kieve.ssss.ui.core.HasUiSize;
import ca.kieve.ssss.ui.core.UiOrigin;
import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiParent;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiRenderable;
import ca.kieve.ssss.ui.core.UiSize;

import java.util.ArrayList;
import java.util.List;

public class UiPanel implements
    UiNode,
    HasUiPosition,
    HasUiSize,
    UiRenderable,
    UiParent<UiPanel>
{
    private UiPosition m_position;
    private UiSize m_size;

    private List<UiNode> m_children = new ArrayList<>();

    public UiPanel() {
        this(UiPosition.ZERO, UiSize.ZERO);
    }

    public UiPanel(UiPosition position, UiSize size) {
        m_position = position;
        m_size = size;
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
    }

    @Override
    public UiPanel addChild(UiNode child) {
        m_children.add(child);
        return this;
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        m_children.forEach(child -> {
            if (child instanceof UiRenderable renderableChild) {
                renderableChild.update(renderContext, delta);
            }
        });
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        m_children.forEach(child -> {
            if (child instanceof UiRenderable renderableChild) {
                renderableChild.render(renderContext, delta);
            }
        });
    }
}
