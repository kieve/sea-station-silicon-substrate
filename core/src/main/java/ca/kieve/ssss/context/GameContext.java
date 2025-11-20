package ca.kieve.ssss.context;

import com.badlogic.gdx.InputMultiplexer;
import dev.dominion.ecs.api.Dominion;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityFactory;
import ca.kieve.ssss.system.System;

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
    PlayerContext player,
    ExamineContext examine,
    EjectContext eject,
    EventContext events,
    RenderContext render,
    ContentRegistry content,
    EntityFactory entityFactory,
    BlockTypeFactory blockTypes,
    List<System> updateSystems,
    List<System> renderSystems
) {
    public GameContext(ContentRegistry content) {
        this(
            new Random(),
            Dominion.create(),
            new ClockContext(),
            new PositionContext(),
            new InputMultiplexer(),
            new InputContext(),
            new LogContext(),
            new PlayerContext(),
            new ExamineContext(),
            new EjectContext(),
            new EventContext(),
            new RenderContext(),
            content,
            new EntityFactory(content),
            content.getBlockTypeFactory(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
}
