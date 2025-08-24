package ca.kieve.ssss.ui.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.layout.UiLayout;

public class UiWindow extends UiLayout {
    protected final ScreenViewport m_viewport;
    protected final Camera m_camera;
    protected final SpriteBatch m_spriteBatch;
    protected final ShapeRenderer m_shapeRenderer;

    // UiWindow owns the UiRenderContext for all it's children.
    // It ignores any UiRenderContext it's parent might have.
    private final UiRenderContext m_renderContext;

    public UiWindow(GameContext gameContext) {
        this(gameContext, true);
    }

    public UiWindow(GameContext gameContext, boolean yDown) {
        super();

        m_viewport = new ScreenViewport();
        m_camera = m_viewport.getCamera();
        m_spriteBatch = new SpriteBatch();
        m_shapeRenderer = new ShapeRenderer();

        m_renderContext = new UiRenderContext(
            gameContext,
            m_camera,
            m_spriteBatch,
            m_shapeRenderer
        );

        if (yDown) {
            m_camera.up.set(0, -1, 0);
            m_camera.direction.set(0, 0, 1);
        }
    }

    @Override
    public void setPosition(UiPosition uiPosition) {
        super.setPosition(uiPosition);
        updateViewport();
    }

    @Override
    public void setSize(UiSize uiSize) {
        super.setSize(uiSize);
        updateViewport();
    }

    @Override
    protected void updateChildOrigin(UiNode child) {
        // Do nothing; window children do not need to consider the windows offset
        // As, the window defines the 0,0 coordinate
    }

    private void updateViewport() {
        var pos = getScreenPosition();
        var x = pos.x();
        var y = pos.y();
        var w = m_size.w();
        var h = m_size.h();
        var upp = m_viewport.getUnitsPerPixel();

        m_viewport.setScreenBounds(x, y, w, h);
        m_viewport.setWorldSize(w * upp, h * upp);
    }

    public void applyViewport() {
        m_viewport.apply(true);
        m_spriteBatch.setProjectionMatrix(m_camera.combined);
        m_shapeRenderer.setProjectionMatrix(m_camera.combined);
    }

    @Override
    public void layout() {
        m_children.forEach(child -> {
            child.setPosition(UiPosition.ZERO);
            child.setSize(new UiSize(m_size.w(), m_size.h()));
        });
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        super.update(m_renderContext, delta);
    }

    @Override
    public void render(UiRenderContext renderContext, float delta) {
        applyViewport();
        super.render(m_renderContext, delta);
    }
}
