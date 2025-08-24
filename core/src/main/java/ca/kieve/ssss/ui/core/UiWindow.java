package ca.kieve.ssss.ui.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class UiWindow extends UiNode {
    private final ScreenViewport m_viewport;
    private final Camera m_camera;
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;

    // UiWindow owns the UiRenderContext for all it's children.
    // It ignores any UiRenderContext it's parent might have.
    private final UiRenderContext m_renderContext;

    private UiNode m_child;

    public UiWindow() {
        super();

        m_viewport = new ScreenViewport();
        m_camera = m_viewport.getCamera();
        m_spriteBatch = new SpriteBatch();
        m_shapeRenderer = new ShapeRenderer();

        m_renderContext = new UiRenderContext(
            m_camera,
            m_spriteBatch,
            m_shapeRenderer
        );
    }

    public ScreenViewport getViewport() {
        return m_viewport;
    }

    public Camera getCamera() {
        return m_viewport.getCamera();
    }

    public UiRenderContext getRenderContext() {
        return m_renderContext;
    }

    @Override
    public void setPosition(UiPosition uiPosition) {
        super.setPosition(uiPosition);
        m_viewport.setScreenPosition(uiPosition.x(), uiPosition.y());
    }

    @Override
    public void setSize(UiSize uiSize) {
        super.setSize(uiSize);
        m_viewport.update(uiSize.w(), uiSize.h(), true);
    }

    public void addChild(UiNode child) {
        m_child = child;
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        if (m_child == null) return;
        m_child.update(m_renderContext, delta);
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        if (m_child == null) return;
        m_spriteBatch.setProjectionMatrix(m_camera.combined);
        m_shapeRenderer.setProjectionMatrix(m_camera.combined);
        m_child.render(m_renderContext, delta);
    }
}
