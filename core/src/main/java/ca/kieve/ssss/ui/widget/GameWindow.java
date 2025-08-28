package ca.kieve.ssss.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.REPLACE.MapModelBuilder;
import ca.kieve.ssss.blueprint.ActorBlueprint;
import ca.kieve.ssss.blueprint.PlayerBlueprint;
import ca.kieve.ssss.blueprint.TileBlueprints;
import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.system.AiSeesawSystem;
import ca.kieve.ssss.system.CameraSystem;
import ca.kieve.ssss.system.ClockSystem;
import ca.kieve.ssss.system.InteractSystem;
import ca.kieve.ssss.system.SanityCheckSystem;
import ca.kieve.ssss.system.TileGlyphRenderSystem;
import ca.kieve.ssss.system.VelocitySystem;
import ca.kieve.ssss.system.WasdSystem;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;

import static ca.kieve.ssss.MainEngine.DEBUG_GRID;

public class GameWindow extends UiWindow {
    public static final int TILE_SIZE = 32;
    public static final float TILE_SCALE = (float) 1 / TILE_SIZE;

    private final GameContext m_gameContext;

    public GameWindow(GameContext gameContext) {
        super(gameContext, false);
        m_gameContext = gameContext;
        m_viewport.setUnitsPerPixel(TILE_SCALE);

        createSystems();
        createEntities();
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        m_gameContext.updateSystems().forEach(Runnable::run);
    }

    @Override
    public void render(UiRenderContext dnu, float delta) {
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
    }

    private void createSystems() {
        m_gameContext.updateSystems().addAll(List.of(
            new ClockSystem(m_gameContext),
            new WasdSystem(m_gameContext),
            new AiSeesawSystem(m_gameContext),
            new VelocitySystem(m_gameContext),
            new CameraSystem(m_gameContext),
            new InteractSystem(m_gameContext),
            new SanityCheckSystem(m_gameContext)
        ));

        var tileGlyphRenderSystem = new TileGlyphRenderSystem(
            m_gameContext,
            m_spriteBatch,
            m_shapeRenderer
        );
        tileGlyphRenderSystem.setDebugGrid(DEBUG_GRID);

        m_gameContext.renderSystems().addAll(List.of(
            tileGlyphRenderSystem
        ));
    }

    private void createEntities() {
//        var mapModel = MapModelBuilder.build(64, 48, 16);
        var mapModel = MapModelBuilder.build(24, 24, 1);
        MapModelBuilder.Point firstRoomCenter = mapModel.rooms()[0].center();

        var playerPos = new Vec3i(firstRoomCenter.x(), firstRoomCenter.y(), 0);
        var player = PlayerBlueprint.create(m_gameContext, playerPos);

        var camera = player.get(CameraComp.class);
        camera.setGdx(m_camera);

        var rand = m_gameContext.random();

        for (int y = 0; y < mapModel.height(); y++) {
            for (int x = 0; x < mapModel.width(); x++) {
                var pos = new Vec3i(x, y, 0);
                switch(mapModel.map()[y][x]) {
                case WALL -> {
                    if (rand.nextInt(100) < 10) {
                        TileBlueprints.createSteelWall(m_gameContext, pos);
                    } else {
                        TileBlueprints.createWall(m_gameContext, pos);
                    }
                }
                case FLOOR ->  TileBlueprints.createFloor(m_gameContext, pos);
                case null -> {
                    // Do nothing
                }
                }
            }
        }

        // Let's place down some debug entities

        ActorBlueprint.createDebugMover(m_gameContext,
            // Move left 2 spaces
            playerPos.add(Vec3i.X.product(-2)),
            50,
            Color.BLUE
        );

        ActorBlueprint.createDebugMover(m_gameContext,
            // Move right 2 spaces
            playerPos.add(Vec3i.X.product(2)),
            100,
            Color.WHITE
        );

        ActorBlueprint.createDebugMover(m_gameContext,
            // Move right 4 spaces
            playerPos.add(Vec3i.X.product(4)),
            200,
            Color.RED
        );
    }
}
