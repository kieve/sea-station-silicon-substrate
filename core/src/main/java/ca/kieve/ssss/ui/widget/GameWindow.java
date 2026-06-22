package ca.kieve.ssss.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.util.TickStage;

public class GameWindow extends UiWindow {
    public static final int TILE_SIZE = 32;
    public static final float TILE_SCALE = (float) 1 / TILE_SIZE;

    private final GameContext m_gameContext;
    private FrameBuffer m_frameBuffer;
    private int m_lastWidth = 0;
    private int m_lastHeight = 0;

    public GameWindow(GameContext gameContext) {
        super(gameContext, false);
        m_gameContext = gameContext;
        m_viewport.setUnitsPerPixel(TILE_SCALE);

        m_gameContext.gameEngine().initializeRenderSystems(
            m_spriteBatch,
            m_shapeRenderer,
            m_camera
        );
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var clock = m_gameContext.clock();
        m_gameContext.updateSystems().forEach(Runnable::run);

        // Process all tick stages within a single frame so AI turns
        // don't stall at one stage per frame (~16.7ms each).
        while (clock.getTickStage() != TickStage.AWAIT_INPUT) {
            m_gameContext.updateSystems().forEach(Runnable::run);
        }
    }

    @Override
    public void render(UiRenderContext dnu, float delta) {
        // Use back buffer dimensions for proper HiDPI/Retina support
        int screenWidth = Gdx.graphics.getBackBufferWidth();
        int screenHeight = Gdx.graphics.getBackBufferHeight();

        // When the window is minimized the back buffer is 0x0. A FrameBuffer
        // can't be built at that size (OpenGL reports "incomplete attachment"),
        // and there's nothing to draw anyway. Skip the frame and keep the
        // existing cached FrameBuffer for when the window is restored.
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        // Recreate FrameBuffer if size changed
        if (m_frameBuffer == null
            || screenWidth != m_lastWidth
            || screenHeight != m_lastHeight) {
            if (m_frameBuffer != null) {
                m_frameBuffer.dispose();
            }
            m_frameBuffer = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                screenWidth,
                screenHeight,
                false
            );
            m_lastWidth = screenWidth;
            m_lastHeight = screenHeight;
            // Force redraw when FrameBuffer is recreated
            m_gameContext.render().markDirty();
        }

        // Re-render to FrameBuffer when dirty and in AWAIT_INPUT stage
        var isDirty = m_gameContext.render().isDirty();
        var isAwaitingInput = m_gameContext.clock().getTickStage() == TickStage.AWAIT_INPUT;
        if (isDirty && isAwaitingInput) {
            m_frameBuffer.begin();
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            m_viewport.apply();
            m_spriteBatch.setProjectionMatrix(m_camera.combined);
            m_shapeRenderer.setProjectionMatrix(m_camera.combined);

            m_gameContext.renderSystems().forEach(Runnable::run);

            m_shapeRenderer.begin(ShapeType.Line);
            m_shapeRenderer.setColor(Color.BLUE);
            var x = m_camera.position.x;
            var y = m_camera.position.y;
            var vpw = m_camera.viewportWidth;
            var vph = m_camera.viewportHeight;
            m_shapeRenderer.rect(
                x - vpw / 2 + 0.01f,
                y - vph / 2 + 0.01f,
                vpw - 0.01f,
                vph - 0.01f
            );
            m_shapeRenderer.end();

            m_frameBuffer.end();
            m_gameContext.render().clearDirty();
        }

        // Always draw the cached frame
        Texture cachedTexture = m_frameBuffer.getColorBufferTexture();
        m_spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, screenWidth, screenHeight);
        m_spriteBatch.begin();
        // FrameBuffer textures are Y-flipped, so flip V coordinates
        m_spriteBatch.draw(
            cachedTexture,
            0,
            0,
            screenWidth,
            screenHeight,
            0,
            0,
            cachedTexture.getWidth(),
            cachedTexture.getHeight(),
            false,
            true // flipX = false, flipY = true
        );
        m_spriteBatch.end();
    }
}
