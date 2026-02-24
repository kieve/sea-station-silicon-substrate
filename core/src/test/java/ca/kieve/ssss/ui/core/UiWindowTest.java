package ca.kieve.ssss.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import ca.kieve.ssss.context.GameContext;

class UiWindowTest {
    @BeforeEach
    void setUp() {
        Gdx.graphics = mock(Graphics.class);
        when(Gdx.graphics.getHeight()).thenReturn(600);
    }

    @AfterEach
    void tearDown() {
        Gdx.graphics = null;
    }
    private static class TestNode extends UiNode {
        UiRenderContext m_lastUpdateContext;
        float m_lastUpdateDelta;
        UiRenderContext m_lastRenderContext;
        float m_lastRenderDelta;

        @Override
        public void update(UiRenderContext renderContext, float delta) {
            m_lastUpdateContext = renderContext;
            m_lastUpdateDelta = delta;
        }

        @Override
        public void render(UiRenderContext renderContext, float delta) {
            m_lastRenderContext = renderContext;
            m_lastRenderDelta = delta;
        }
    }

    private record TestWindowContext(
        UiWindow window,
        GameContext gameContext,
        ScreenViewport viewport,
        OrthographicCamera camera,
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer
    ) {}

    @Test
    void constructorWithYDownTrueFlipsCameraOrientation() {
        TestWindowContext ctx = createWindow(true, 1f);

        assertEquals(0f, ctx.camera().up.x, 1e-6f);
        assertEquals(-1f, ctx.camera().up.y, 1e-6f);
        assertEquals(0f, ctx.camera().up.z, 1e-6f);

        assertEquals(0f, ctx.camera().direction.x, 1e-6f);
        assertEquals(0f, ctx.camera().direction.y, 1e-6f);
        assertEquals(1f, ctx.camera().direction.z, 1e-6f);
    }

    @Test
    void constructorWithDefaultYDownTrueFlipsCameraOrientation() {
        TestWindowContext ctx = createWindow(1f);

        assertEquals(0f, ctx.camera().up.x, 1e-6f);
        assertEquals(-1f, ctx.camera().up.y, 1e-6f);
        assertEquals(0f, ctx.camera().up.z, 1e-6f);

        assertEquals(0f, ctx.camera().direction.x, 1e-6f);
        assertEquals(0f, ctx.camera().direction.y, 1e-6f);
        assertEquals(1f, ctx.camera().direction.z, 1e-6f);
    }

    @Test
    void constructorWithYDownFalseKeepsCameraOrientation() {
        OrthographicCamera baseline = new OrthographicCamera();
        TestWindowContext ctx = createWindow(false, 1f);

        assertEquals(baseline.up.x, ctx.camera().up.x, 1e-6f);
        assertEquals(baseline.up.y, ctx.camera().up.y, 1e-6f);
        assertEquals(baseline.up.z, ctx.camera().up.z, 1e-6f);

        assertEquals(baseline.direction.x, ctx.camera().direction.x, 1e-6f);
        assertEquals(baseline.direction.y, ctx.camera().direction.y, 1e-6f);
        assertEquals(baseline.direction.z, ctx.camera().direction.z, 1e-6f);
    }

    @Test
    void applyViewportSetsProjectionMatrices() {
        TestWindowContext ctx = createWindow(true, 1f);
        ctx.camera().update(false);

        clearInvocations(ctx.viewport(), ctx.spriteBatch(), ctx.shapeRenderer());

        ctx.window().applyViewport();

        verify(ctx.viewport()).apply(true);
        verify(ctx.spriteBatch()).setProjectionMatrix(ctx.camera().combined);
        verify(ctx.shapeRenderer()).setProjectionMatrix(ctx.camera().combined);
    }

    @Test
    void setPositionAndSizeUpdateViewport() {
        float unitsPerPixel = 1.5f;
        TestWindowContext ctx = createWindow(true, unitsPerPixel);
        UiWindow window = ctx.window();

        window.setSize(new UiSize(40, 30));
        clearInvocations(ctx.viewport());

        window.setPosition(new UiPosition(10, 20));

        // Y coordinate is converted from Y-down UI coords to Y-up libGDX coords
        // libGdxY = screenHeight - y - h = 600 - 20 - 30 = 550
        verify(ctx.viewport()).setScreenBounds(10, 550, 40, 30);
        verify(ctx.viewport()).setWorldSize(40 * unitsPerPixel, 30 * unitsPerPixel);
    }

    @Test
    void layoutAssignsChildBounds() {
        TestWindowContext ctx = createWindow(true, 1f);
        UiWindow window = ctx.window();
        window.setSize(new UiSize(80, 50));

        TestNode child = new TestNode();
        window.add(child);

        assertEquals(UiPosition.ZERO, child.getPosition());
        assertEquals(new UiSize(80, 50), child.getSize());
    }

    @Test
    void updateUsesOwnedRenderContext() {
        TestWindowContext ctx = createWindow(true, 1f);
        UiWindow window = ctx.window();

        TestNode child = new TestNode();
        window.add(child);

        UiRenderContext external = new UiRenderContext(
            mock(GameContext.class),
            mock(Camera.class),
            mock(SpriteBatch.class),
            mock(ShapeRenderer.class)
        );

        window.update(external, 0.5f);

        assertSame(ctx.gameContext(), child.m_lastUpdateContext.gameContext());
        assertSame(ctx.camera(), child.m_lastUpdateContext.camera());
        assertSame(ctx.spriteBatch(), child.m_lastUpdateContext.spriteBatch());
        assertSame(ctx.shapeRenderer(), child.m_lastUpdateContext.shapeRenderer());
        assertEquals(0.5f, child.m_lastUpdateDelta, 1e-6f);
    }

    @Test
    void renderUsesOwnedRenderContextAndAppliesViewport() {
        TestWindowContext ctx = createWindow(true, 1f);
        UiWindow window = ctx.window();

        TestNode child = new TestNode();
        window.add(child);

        UiRenderContext external = new UiRenderContext(
            mock(GameContext.class),
            mock(Camera.class),
            mock(SpriteBatch.class),
            mock(ShapeRenderer.class)
        );

        clearInvocations(ctx.viewport(), ctx.spriteBatch(), ctx.shapeRenderer());

        window.render(external, 0.25f);

        verify(ctx.viewport()).apply(true);
        verify(ctx.spriteBatch()).setProjectionMatrix(ctx.camera().combined);
        verify(ctx.shapeRenderer()).setProjectionMatrix(ctx.camera().combined);

        assertSame(ctx.gameContext(), child.m_lastRenderContext.gameContext());
        assertSame(ctx.camera(), child.m_lastRenderContext.camera());
        assertSame(ctx.spriteBatch(), child.m_lastRenderContext.spriteBatch());
        assertSame(ctx.shapeRenderer(), child.m_lastRenderContext.shapeRenderer());
        assertEquals(0.25f, child.m_lastRenderDelta, 1e-6f);
    }

    private TestWindowContext createWindow(
            boolean yDown, float unitsPerPixel) {
        GameContext gameContext = mock(GameContext.class);
        OrthographicCamera camera = new OrthographicCamera();

        try (var viewportConstruction =
                 mockConstruction(ScreenViewport.class,
                     (viewport, context) -> {
                         when(viewport.getCamera())
                             .thenReturn(camera);
                         when(viewport.getUnitsPerPixel())
                             .thenReturn(unitsPerPixel);
                     });
             var spriteBatchConstruction =
                 mockConstruction(SpriteBatch.class);
             var shapeRendererConstruction =
                 mockConstruction(ShapeRenderer.class)) {

            var window = new UiWindow(gameContext, yDown);

            var viewport =
                viewportConstruction.constructed().get(0);
            var spriteBatch =
                spriteBatchConstruction.constructed().get(0);
            var shapeRenderer =
                shapeRendererConstruction.constructed().get(0);

            return new TestWindowContext(
                window, gameContext, viewport,
                camera, spriteBatch, shapeRenderer);
        }
    }

    private TestWindowContext createWindow(
            float unitsPerPixel) {
        GameContext gameContext = mock(GameContext.class);
        OrthographicCamera camera = new OrthographicCamera();

        try (var viewportConstruction =
                 mockConstruction(ScreenViewport.class,
                     (viewport, context) -> {
                         when(viewport.getCamera())
                             .thenReturn(camera);
                         when(viewport.getUnitsPerPixel())
                             .thenReturn(unitsPerPixel);
                     });
             var spriteBatchConstruction =
                 mockConstruction(SpriteBatch.class);
             var shapeRendererConstruction =
                 mockConstruction(ShapeRenderer.class)) {

            var window = new UiWindow(gameContext);

            var viewport =
                viewportConstruction.constructed().get(0);
            var spriteBatch =
                spriteBatchConstruction.constructed().get(0);
            var shapeRenderer =
                shapeRendererConstruction.constructed().get(0);

            return new TestWindowContext(
                window, gameContext, viewport,
                camera, spriteBatch, shapeRenderer);
        }
    }
}
