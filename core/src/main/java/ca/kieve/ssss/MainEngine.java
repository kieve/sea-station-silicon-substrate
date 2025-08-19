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
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.system.CameraSystem;
import ca.kieve.ssss.system.WasdSystem;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;

import static ca.kieve.ssss.input.InputAction.A;
import static ca.kieve.ssss.input.InputAction.D;
import static ca.kieve.ssss.input.InputAction.S;
import static ca.kieve.ssss.input.InputAction.W;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MainEngine extends ApplicationAdapter {
    private static boolean DEBUG_GRID = true;

    public static final int TILE_SIZE = 32;
    public static final float TILE_SCALE = (float) 1 / TILE_SIZE;

    private GameContext m_gameContext;

    private Viewport m_mainViewport;
    private Camera m_camera;

    private SpriteBatch m_spriteBatch;
    private ShapeRenderer m_shapeRenderer;

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
        ScreenUtils.clear(Color.BLACK);
        Gdx.graphics.getDeltaTime();

        m_gameContext.systems().forEach(Runnable::run);

        var entities = m_gameContext.ecs().findEntitiesWith(Position.class, TileGlyph.class);
        m_spriteBatch.setProjectionMatrix(m_camera.combined);
        m_spriteBatch.begin();
        entities.forEach(with -> {
            var position = with.comp1();
            var glyph = with.comp2();

            // Get all the other entities at the same position
            var stack = m_gameContext.pos().getAt(position.getPosition());
            if (stack.size() > 1) {
                boolean isPlayer = with.entity().has(CameraComp.class);
                if (!isPlayer) {
                    return;
                }
            }

            var pos = position.getPosition();
            var font = glyph.font();
            font.draw(m_spriteBatch, "" + glyph.glyph(),
                pos.x + glyph.dx(),
                pos.y + glyph.dy());
        });
        m_spriteBatch.end();

        m_shapeRenderer.setProjectionMatrix(m_camera.combined);
        m_shapeRenderer.begin(ShapeType.Line);
        m_shapeRenderer.setColor(Color.BLUE);
        m_shapeRenderer.rect(0, 0, 1, 1);
        m_shapeRenderer.rect(1, 1, 1, 1);
        m_shapeRenderer.end();
    }

    private void update(float delta) {

    }

    private void render(float delta) {

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        m_mainViewport.update(width, height, true);
    }

    private void createSystems() {
        m_gameContext.systems().addAll(List.of(
            new WasdSystem(m_gameContext),
            new CameraSystem(m_gameContext)
        ));
    }

    private void createEntities() {
        var mapModel = MapModelBuilder.build(64, 48, 16);
        // gets the center of the first room
        MapModelBuilder.Point firstRoomCenter = mapModel.rooms()[0].center();

        var playerPos = new Vec3i(firstRoomCenter.x(), firstRoomCenter.y(), 0);
        var player = PlayerBlueprint.create(m_gameContext, playerPos);

        var camera = player.get(CameraComp.class);
        m_mainViewport.setCamera(camera.gdx());
        m_camera = camera.gdx();

//        TileBlueprints.createWall(m_gameContext, new Vec3i(0, 0, 0));
//        TileBlueprints.createWall(m_gameContext, new Vec3i(1, 1, 0));

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
