package ca.kieve.ssss;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import ca.kieve.ssss.REPLACE.MapModelBuilder;
import ca.kieve.ssss.blueprint.PlayerBlueprint;
import ca.kieve.ssss.blueprint.TileBlueprints;
import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.system.CameraSystem;
import ca.kieve.ssss.system.TileGlyphRenderSystem;
import ca.kieve.ssss.system.WasdSystem;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainEngine extends ApplicationAdapter {
    private static boolean DEBUG_GRID = false;

    private static final float TARGET_FPS = 60f;
    private static final float TARGET_FRAME_TIME_MS = 1000f / TARGET_FPS;

    public static final int TILE_SIZE = 32;
    public static final float TILE_SCALE = (float) 1 / TILE_SIZE;

    private GameContext m_gameContext;

    private Viewport m_mainViewport;
    private Camera m_camera;

    private SpriteBatch m_spriteBatch;
    private ShapeRenderer m_shapeRenderer;

    private float m_frameDeltaAccumulator = 0f;

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_INFO);

        var mainViewport = new ScreenViewport();
        mainViewport.setUnitsPerPixel(TILE_SCALE);
        m_mainViewport = mainViewport;

        m_gameContext = new GameContext();
        m_spriteBatch = new SpriteBatch();
        m_shapeRenderer = new ShapeRenderer();

        Gdx.input.setInputProcessor(m_gameContext.inputMux());

        createSystems();
        createEntities();
    }

    @Override
    public void render() {
        float deltaSeconds = Gdx.graphics.getDeltaTime();
        float deltaMs = deltaSeconds * 1000.0f;
        m_frameDeltaAccumulator += deltaMs;
        if (m_frameDeltaAccumulator < TARGET_FRAME_TIME_MS) {
            return;
        }
        m_frameDeltaAccumulator -= TARGET_FRAME_TIME_MS;

        update(TARGET_FRAME_TIME_MS);

        m_spriteBatch.setProjectionMatrix(m_camera.combined);
        m_shapeRenderer.setProjectionMatrix(m_camera.combined);
        render(TARGET_FRAME_TIME_MS);
    }

    private void update(float delta) {
        m_gameContext.updateSystems().forEach(Runnable::run);
    }

    private void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        m_gameContext.renderSystems().forEach(Runnable::run);

        m_shapeRenderer.setProjectionMatrix(m_camera.combined);
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
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        m_mainViewport.update(width, height, true);
    }

    private void createSystems() {
        m_gameContext.updateSystems().addAll(List.of(
            new WasdSystem(m_gameContext),
            new CameraSystem(m_gameContext)
        ));

        var tileGlyphRenderSystem =
            new TileGlyphRenderSystem(m_gameContext, m_spriteBatch, m_shapeRenderer);
        tileGlyphRenderSystem.setDebugGrid(DEBUG_GRID);

        m_gameContext.renderSystems().addAll(List.of(
            tileGlyphRenderSystem
        ));
    }

    private void createEntities() {
        var mapModel = MapModelBuilder.build(64, 48, 16);
        MapModelBuilder.Point firstRoomCenter = mapModel.rooms()[0].center();

        var playerPos = new Vec3i(firstRoomCenter.x(), firstRoomCenter.y(), 0);
        var player = PlayerBlueprint.create(m_gameContext, playerPos);

        var camera = player.get(CameraComp.class);
        m_mainViewport.setCamera(camera.gdx());
        m_camera = camera.gdx();

        for (int y = 0; y < mapModel.height(); y++) {
            for (int x = 0; x < mapModel.width(); x++) {
                var pos = new Vec3i(x, y, 0);
                switch(mapModel.map()[y][x]) {
                    case WALL -> TileBlueprints.createWall(m_gameContext, pos);
                    case FLOOR ->  TileBlueprints.createFloor(m_gameContext, pos);
                }
            }
        }
    }
}
