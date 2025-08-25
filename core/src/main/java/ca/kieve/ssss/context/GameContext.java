package ca.kieve.ssss.context;

import com.badlogic.gdx.InputMultiplexer;
import dev.dominion.ecs.api.Dominion;

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
    LogContext log,
    List<System> updateSystems,
    List<System> renderSystems
) {
    public GameContext() {
        this(
            new Random(),
            Dominion.create(),
            new ClockContext(),
            new PositionContext(),
            new InputMultiplexer(),
            new LogContext(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
}
