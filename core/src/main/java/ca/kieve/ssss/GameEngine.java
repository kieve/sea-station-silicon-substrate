package ca.kieve.ssss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.input.InputActionController;
import ca.kieve.ssss.system.AttackSystem;
import ca.kieve.ssss.system.CameraSystem;
import ca.kieve.ssss.system.ClockSystem;
import ca.kieve.ssss.system.DebugRectRenderSystem;
import ca.kieve.ssss.system.EjectSystem;
import ca.kieve.ssss.system.EventSystem;
import ca.kieve.ssss.system.ExamineSystem;
import ca.kieve.ssss.system.InteractMenuSystem;
import ca.kieve.ssss.system.InteractSystem;
import ca.kieve.ssss.system.OpenSystem;
import ca.kieve.ssss.system.PathingSystem;
import ca.kieve.ssss.system.SanityCheckSystem;
import ca.kieve.ssss.system.SocketSystem;
import ca.kieve.ssss.system.TileGlyphRenderSystem;
import ca.kieve.ssss.system.TileHighlightRenderSystem;
import ca.kieve.ssss.system.VelocitySystem;
import ca.kieve.ssss.system.WasdSystem;
import ca.kieve.ssss.system.ai.AiControllerSystem;
import ca.kieve.ssss.world.MapGenerator;
import ca.kieve.ssss.world.StaticTestMapGenerator;
import ca.kieve.ssss.world.WorldEntityFactory;
import ca.kieve.ssss.world.WorldModel;

import java.util.List;

public class GameEngine {
    public static final boolean DEBUG_GRID = false;

    private GameContext m_gameContext;
    private MapGenerator m_mapGenerator;
    private WorldModel m_worldModel;

    public void init(GameContext gameContext) {
        m_gameContext = gameContext;
        m_mapGenerator = new StaticTestMapGenerator();
        m_worldModel = m_mapGenerator.generate(gameContext.blockTypes());

        var inputActionController = new InputActionController(gameContext);
        gameContext.inputMux().addProcessor(0, inputActionController);

        createUpdateSystems();
        createEntities();
    }

    private void createUpdateSystems() {
        m_gameContext.updateSystems().addAll(List.of(
            new ClockSystem(m_gameContext),
            new InteractSystem(m_gameContext),
            new OpenSystem(m_gameContext),
            new SocketSystem(m_gameContext),
            new ExamineSystem(m_gameContext),
            new EjectSystem(m_gameContext),
            new InteractMenuSystem(m_gameContext),
            new WasdSystem(m_gameContext),
            new PathingSystem(m_gameContext),
            new AiControllerSystem(m_gameContext),
            new AttackSystem(m_gameContext),
            new VelocitySystem(m_gameContext),
            new CameraSystem(m_gameContext),
            new SanityCheckSystem(m_gameContext),
            new EventSystem(m_gameContext)
        ));
    }

    public void initializeRenderSystems(
            SpriteBatch spriteBatch,
            ShapeRenderer shapeRenderer,
            Camera camera) {
        // Assign the libGDX camera to the entity with CameraComp
        var cameraResults = m_gameContext.ecs().findEntitiesWith(CameraComp.class);
        for (var result : cameraResults) {
            result.comp().setGdx(camera);
        }

        var tileGlyphRenderSystem = new TileGlyphRenderSystem(
            m_gameContext,
            spriteBatch,
            shapeRenderer,
            m_mapGenerator.getFloorGlyphId()
        );
        tileGlyphRenderSystem.setDebugGrid(DEBUG_GRID);

        var debugRectRenderSystem = new DebugRectRenderSystem(
            m_gameContext,
            shapeRenderer
        );

        var highlightProviders = List.of(
            m_gameContext.examine(),
            m_gameContext.eject(),
            m_gameContext.interact()
        );
        var tileHighlightRenderSystem = new TileHighlightRenderSystem(
            m_gameContext,
            shapeRenderer,
            highlightProviders
        );

        m_gameContext.renderSystems().addAll(List.of(
            tileGlyphRenderSystem,
            debugRectRenderSystem,
            tileHighlightRenderSystem
        ));
    }

    private void createEntities() {
        var playerSpawn = m_mapGenerator.getPlayerSpawn();

        // Create block entities from the world model
        WorldEntityFactory.createEntities(m_gameContext, m_worldModel);

        // Create player at the spawn position
        var factory = m_gameContext.entityFactory();
        factory.createEntity(m_gameContext, "player", playerSpawn);

        // Delegate entity creation to the map generator
        m_mapGenerator.createEntities(m_gameContext, playerSpawn);
    }

    public MapGenerator getMapGenerator() {
        return m_mapGenerator;
    }
}
