package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.UiNode;
import ca.kieve.ssss.ui.core.UiPosition;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiSize;
import ca.kieve.ssss.ui.core.UiWindow;

import java.util.ArrayList;
import java.util.List;

public abstract class UiLayout extends UiNode {
    private UiWindow m_parentWindow = null;
    protected List<UiNode> m_children = new ArrayList<>();

    abstract public void layout();

    public void setParentWindow(UiWindow parentWindow) {
        m_parentWindow = parentWindow;
    }

    public void add(UiNode child, UiLayoutParams layoutParams) {
        child.setLayoutParams(layoutParams);
        add(child);
    }

    public void add(UiNode child) {
        m_children.add(child);
        updateChildOrigin(child);
        layout();
    }

    @Override
    public void setPosition(UiPosition position) {
        super.setPosition(position);
        updateChildrenOrigin();
        layout();
    }

    @Override
    public void setSize(UiSize size) {
        super.setSize(size);
        layout();
    }

    @Override
    public void setOrigin(UiPosition origin) {
        super.setOrigin(origin);
        updateChildrenOrigin();
        layout();
    }

    protected void updateChildrenOrigin() {
        m_children.forEach(this::updateChildOrigin);
    }

    protected void updateChildOrigin(UiNode child) {
        var ox = m_origin.x();
        var oy = m_origin.y();
        child.setOrigin(new UiPosition(
            ox + m_position.x(),
            oy + m_position.y()
        ));
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        m_children.forEach(child -> child.update(renderContext, delta));
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        m_children.forEach(child -> {
            child.render(renderContext, delta);
            if (m_parentWindow != null && child instanceof UiWindow) {
                // UiWindows will apply their own viewport.
                // So, we have to reset the viewport back.
                m_parentWindow.applyViewport();
            }
        });
    }
}
