package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class VelocitySystem extends System {
    private final List<Vec3i> m_instantsToZero = new ArrayList<>();

    public VelocitySystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(
            Position.class,
            Velocity.class
        );

        var optionalResult = searchResults.stream().findFirst();
        if (optionalResult.isEmpty()) {
            return;
        }

        var withResult = optionalResult.get();
        var pos = withResult.comp1().getPosition();
        var velocity = withResult.comp2();
        var instantVelocity = velocity.instant();

        var oldPos = pos.copy();
        var newPos = pos.copy();

        newPos.addMut(instantVelocity);
        m_instantsToZero.add(instantVelocity);

        var entities = m_gameContext.pos().getAt(newPos);
        var solid = entities.stream().anyMatch(entity -> {
            var density = entity.get(Density.class);
            return density == Density.SOLID;
        });
        if (solid) {
            return;
        }

        pos.set(newPos);
        m_gameContext.pos().move(withResult.entity(), oldPos, pos);
    }

    @Override
    public void postTick() {
        m_instantsToZero.forEach(instant -> instant.set(Vec3i.ZERO));
        m_instantsToZero.clear();
    }
}
