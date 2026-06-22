package ca.kieve.ssss.context;

import com.badlogic.gdx.InputMultiplexer;
import dev.dominion.ecs.api.Dominion;

import ca.kieve.ssss.GameEngine;
import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityFactory;
import ca.kieve.ssss.content.map.MapGenerator;
import ca.kieve.ssss.system.System;
import ca.kieve.ssss.util.PerfClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record GameContext(
    Random random,
    Dominion ecs,
    ClockContext clock,
    PositionContext pos,
    InputMultiplexer inputMux,
    InputContext input,
    LogContext log,
    ExamineContext examine,
    EjectContext eject,
    InteractContext interact,
    InventoryContext inventory,
    EventContext events,
    RenderContext render,
    AiControllerContext aiController,
    PathingContext pathing,
    VisionContext vision,
    MapContext map,
    WorldContext world,
    ContentRegistry content,
    EntityFactory entityFactory,
    BlockTypeFactory blockTypes,
    List<System> updateSystems,
    List<System> renderSystems,
    GameEngine gameEngine,
    MapGenerator mapGenerator,
    PerfClock perf,
    DebugContext debug
) {
    public GameContext(ContentRegistry content, MapGenerator mapGenerator) {
        this(content, mapGenerator, false);
    }

    public GameContext(ContentRegistry content, MapGenerator mapGenerator, boolean headless) {
        this(
            new Random(),
            Dominion.create(),
            new ClockContext(),
            new PositionContext(),
            new InputMultiplexer(),
            new InputContext(),
            new LogContext(),
            new ExamineContext(),
            new EjectContext(),
            new InteractContext(),
            new InventoryContext(),
            new EventContext(),
            new RenderContext(),
            new AiControllerContext(),
            new PathingContext(),
            new VisionContext(),
            new MapContext(),
            new WorldContext(),
            content,
            new EntityFactory(content, headless),
            content.getBlockTypeFactory(),
            new ArrayList<>(),
            new ArrayList<>(),
            new GameEngine(),
            mapGenerator,
            new PerfClock(),
            new DebugContext()
        );
    }
}
