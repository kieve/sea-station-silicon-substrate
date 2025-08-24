package ca.kieve.ssss.ui.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class UiWindow implements
    UiNode,
    HasUiPosition,
    HasUiSize,
    HasUiOrigin,
    UiRenderable,
    UiParent<UiWindow>
{
    private final ScreenViewport m_viewport;
    private final Camera m_camera;
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;

    // UiWindow owns the UiRenderContext for all it's children.
    // It ignores any UiRenderContext it's parent might have.
    private final UiRenderContext m_renderContext;

    private UiPosition m_position;
    private UiSize m_size;
    private final UiOrigin m_origin;

    private UiNode m_child;

    public UiWindow() {
        m_origin = new UiOrigin();

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
    public UiPosition getUiPosition() {
        return m_position;
    }

    @Override
    public void setUiPosition(UiPosition uiPosition) {
        m_position = uiPosition;
        m_viewport.setScreenPosition(uiPosition.x(), uiPosition.y());
    }

    @Override
    public UiSize getUiSize() {
        return m_size;
    }

    @Override
    public void setUiSize(UiSize uiSize) {
        m_size = uiSize;
        m_viewport.update(uiSize.w(), uiSize.h(), true);
    }

    @Override
    public UiPosition getOrigin() {
        return m_origin.getPosition();
    }

    @Override
    public void setOrigin(UiPosition origin) {
        m_origin.setPosition(origin);
    }

    @Override
    public UiWindow addChild(UiNode child) {
        m_child = child;
        return this;
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        if (m_child instanceof UiRenderable renderable) {
            renderable.update(m_renderContext, delta);
        }
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        m_spriteBatch.setProjectionMatrix(m_camera.combined);
        m_shapeRenderer.setProjectionMatrix(m_camera.combined);
        if (m_child instanceof UiRenderable renderable) {
            renderable.render(m_renderContext, delta);
        }
    }
}
