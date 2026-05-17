package ca.kieve.ssss;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.input.InputActionController;
import ca.kieve.ssss.system.AttackSystem;
import ca.kieve.ssss.system.CameraSystem;
import ca.kieve.ssss.system.ClockSystem;
import ca.kieve.ssss.system.DebugMenuSystem;
import ca.kieve.ssss.system.DebugRectRenderSystem;
import ca.kieve.ssss.system.EjectSystem;
import ca.kieve.ssss.system.EventSystem;
import ca.kieve.ssss.system.ExamineSystem;
import ca.kieve.ssss.system.InteractMenuSystem;
import ca.kieve.ssss.system.InteractSystem;
import ca.kieve.ssss.system.MapInitSystem;
import ca.kieve.ssss.system.OpenSystem;
import ca.kieve.ssss.system.PathingSystem;
import ca.kieve.ssss.system.SanityCheckSystem;
import ca.kieve.ssss.system.ScurryInitSystem;
import ca.kieve.ssss.system.SocketSystem;
import ca.kieve.ssss.system.TileGlyphRenderSystem;
import ca.kieve.ssss.system.TileHighlightRenderSystem;
import ca.kieve.ssss.system.VelocitySystem;
import ca.kieve.ssss.system.VisionSystem;
import ca.kieve.ssss.system.WasdSystem;
import ca.kieve.ssss.system.ai.AiControllerSystem;
import ca.kieve.ssss.world.WorldEntityFactory;
import ca.kieve.ssss.world.WorldModel;

import java.util.List;

public class GameEngine {
    private GameContext m_gameContext;
    private WorldModel m_worldModel;

    public void init(GameContext gameContext) {
        m_gameContext = gameContext;
        m_worldModel = gameContext.mapGenerator().generate(gameContext.blockTypes());

        var inputActionController = new InputActionController(gameContext);
        gameContext.inputMux().addProcessor(0, inputActionController);

        createUpdateSystems();
        createEntities();
    }

    private void createUpdateSystems() {
        m_gameContext.updateSystems().addAll(
            List.of(
                new ClockSystem(m_gameContext),
                new DebugMenuSystem(m_gameContext),
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
                new VisionSystem(m_gameContext),
                new SanityCheckSystem(m_gameContext),
                new EventSystem(m_gameContext)
            )
        );
    }

    public void initializeRenderSystems(
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer,
        Camera camera
    ) {
        // Assign the libGDX camera to the entity with CameraComp
        var cameraResults = m_gameContext.ecs().findEntitiesWith(CameraComp.class);
        for (var result : cameraResults) {
            result.comp().setGdx(camera);
        }

        var tileGlyphRenderSystem = new TileGlyphRenderSystem(
            m_gameContext,
            spriteBatch,
            shapeRenderer
        );

        var debugRectRenderSystem = new DebugRectRenderSystem(m_gameContext, shapeRenderer);

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

        m_gameContext.renderSystems().addAll(
            List.of(tileGlyphRenderSystem, debugRectRenderSystem, tileHighlightRenderSystem)
        );
    }

    private void createEntities() {
        // Create block entities from the world model
        WorldEntityFactory.createEntities(m_gameContext, m_worldModel);

        // Create map entities from YAML definitions
        var factory = m_gameContext.entityFactory();
        for (MapEntityDefinition entityDef : m_gameContext.mapGenerator().getEntities()) {
            factory.createEntityWithOverrides(
                m_gameContext,
                entityDef.id(),
                entityDef.components()
            );
        }

        // Run map init systems for post-spawn processing. Standalone init
        // systems run first; any update system that also implements
        // MapInitSystem opts into the same one-shot post-spawn hook.
        List<MapInitSystem> mapInitSystems = List.of(new ScurryInitSystem());
        for (MapInitSystem initSystem : mapInitSystems) {
            initSystem.run(m_gameContext);
        }
        for (var system : m_gameContext.updateSystems()) {
            if (system instanceof MapInitSystem mapInit) {
                mapInit.run(m_gameContext);
            }
        }
    }
}
