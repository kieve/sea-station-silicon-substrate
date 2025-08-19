package ca.kieve.ssss.context;

import com.badlogic.gdx.InputMultiplexer;
import dev.dominion.ecs.api.Dominion;

import ca.kieve.ssss.system.System;

import java.util.ArrayList;
import java.util.List;

public record GameContext(
    Dominion ecs,
    PositionContext pos,
    InputMultiplexer inputMux,
    List<System> updateSystems,
    List<System> renderSystems
) {
    public GameContext() {
        this(
            Dominion.create(),
            new PositionContext(),
            new InputMultiplexer(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }
}
