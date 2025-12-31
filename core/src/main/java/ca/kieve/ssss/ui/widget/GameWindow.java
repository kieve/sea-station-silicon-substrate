package ca.kieve.ssss.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.content.EntityFactory;
import ca.kieve.ssss.world.MapGenerator;
import ca.kieve.ssss.world.StaticTestMapGenerator;
import ca.kieve.ssss.world.WorldEntityFactory;
import ca.kieve.ssss.world.WorldModel;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.input.InputActionController;
import ca.kieve.ssss.system.ai.AiControllerSystem;
import ca.kieve.ssss.system.AttackSystem;
import ca.kieve.ssss.system.CameraSystem;
import ca.kieve.ssss.system.ClockSystem;
import ca.kieve.ssss.system.DebugRectRenderSystem;
import ca.kieve.ssss.system.EjectHighlightRenderSystem;
import ca.kieve.ssss.system.EjectSystem;
import ca.kieve.ssss.system.EventSystem;
import ca.kieve.ssss.system.ExamineCrosshairRenderSystem;
import ca.kieve.ssss.system.ExamineSystem;
import ca.kieve.ssss.system.InteractSystem;
import ca.kieve.ssss.system.SanityCheckSystem;
import ca.kieve.ssss.system.SocketSystem;
import ca.kieve.ssss.system.TileGlyphRenderSystem;
import ca.kieve.ssss.system.VelocitySystem;
import ca.kieve.ssss.system.WasdSystem;
import ca.kieve.ssss.ui.core.UiRenderContext;
import ca.kieve.ssss.ui.core.UiWindow;
import ca.kieve.ssss.util.TickStage;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;

import static ca.kieve.ssss.MainEngine.DEBUG_GRID;

public class GameWindow extends UiWindow {
    public static final int TILE_SIZE = 32;
    public static final float TILE_SCALE = (float) 1 / TILE_SIZE;

    private final GameContext m_gameContext;
    private final MapGenerator m_mapGenerator;
    private final WorldModel m_worldModel;
    private FrameBuffer m_frameBuffer;
    private int m_lastWidth = 0;
    private int m_lastHeight = 0;

    public GameWindow(GameContext gameContext) {
        super(gameContext, false);
        m_gameContext = gameContext;
        m_viewport.setUnitsPerPixel(TILE_SCALE);

        // TODO: All this setup should be moved somewhere
        m_mapGenerator = new StaticTestMapGenerator();
        m_worldModel = m_mapGenerator.generate(m_gameContext.blockTypes());
        createSystems();
        createEntities();
    }

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        m_gameContext.updateSystems().forEach(Runnable::run);
    }

    @Override
    public void render(UiRenderContext dnu, float delta) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

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
            0, 0,
            screenWidth, screenHeight,
            0, 0,
            cachedTexture.getWidth(), cachedTexture.getHeight(),
            false, true  // flipX = false, flipY = true
        );
        m_spriteBatch.end();
    }

    private void createSystems() {
        // Add input action controller to handle all key input
        var inputActionController = new InputActionController(m_gameContext);
        m_gameContext.inputMux().addProcessor(0, inputActionController);

        m_gameContext.updateSystems().addAll(List.of(
            new ClockSystem(m_gameContext),
            new InteractSystem(m_gameContext),
            new SocketSystem(m_gameContext),
            new ExamineSystem(m_gameContext),
            new EjectSystem(m_gameContext),
            new WasdSystem(m_gameContext),
            new AiControllerSystem(m_gameContext),
            new AttackSystem(m_gameContext),
            new VelocitySystem(m_gameContext),
            new CameraSystem(m_gameContext),
            new SanityCheckSystem(m_gameContext),
            new EventSystem(m_gameContext)
        ));

        var tileGlyphRenderSystem = new TileGlyphRenderSystem(
            m_gameContext,
            m_spriteBatch,
            m_shapeRenderer,
            m_mapGenerator.getFloorGlyphId()
        );
        tileGlyphRenderSystem.setDebugGrid(DEBUG_GRID);

        var debugRectRenderSystem = new DebugRectRenderSystem(
            m_gameContext,
            m_shapeRenderer
        );

        var examineCrosshairRenderSystem = new ExamineCrosshairRenderSystem(
            m_gameContext,
            m_shapeRenderer
        );

        var ejectHighlightRenderSystem = new EjectHighlightRenderSystem(
            m_gameContext,
            m_shapeRenderer
        );

        m_gameContext.renderSystems().addAll(List.of(
            tileGlyphRenderSystem,
            debugRectRenderSystem,
            examineCrosshairRenderSystem,
            ejectHighlightRenderSystem
        ));
    }

    private void createEntities() {
        var playerSpawn = m_mapGenerator.getPlayerSpawn();

        // Create block entities from the world model
        WorldEntityFactory.createEntities(m_gameContext, m_worldModel);

        // Create player at the spawn position (Z=1, standing on Z=0 blocks)
        EntityFactory factory = m_gameContext.entityFactory();
        var player = factory.createEntity(m_gameContext, "player", playerSpawn);

        var camera = player.get(CameraComp.class);
        camera.setGdx(m_camera);

        // Let's place down some debug entities
        // Adjust Z to 1 since entities now exist at Z=1

        factory.createDebugMover(m_gameContext,
            // Move left 2 spaces
            playerSpawn.add(Vec3i.X.product(-2)),
            50,
            Color.BLUE
        );

        factory.createDebugMover(m_gameContext,
            // Move right 2 spaces
            playerSpawn.add(Vec3i.X.product(2)),
            100,
            Color.WHITE
        );

        factory.createDebugMover(m_gameContext,
            // Move right 4 spaces
            playerSpawn.add(Vec3i.X.product(4)),
            200,
            Color.RED
        );

        // Test socket for taking over dead entities
        factory.createEntity(m_gameContext,
            "deadMech",
            new Vec3i(5, 5, 1),
            Color.GOLD
        );

        // Training dummy for combat testing (in room 2)
        factory.createEntity(m_gameContext,
            "trainingDummy",
            new Vec3i(23, 7, 1),
            Color.PINK
        );

        // Attacker AI test - now uses Behavior component from YAML
        factory.createEntity(m_gameContext,
            "aiAttacker",
            playerSpawn.add(new Vec3i(3, 0, 0)),
            Color.SCARLET
        );
    }
}
