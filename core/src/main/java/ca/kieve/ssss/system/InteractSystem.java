package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;

import java.util.Objects;

public class InteractSystem extends System {
    public InteractSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void preTick() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(
            Position.class,
            Velocity.class,
            Speed.class,
            WasdController.class
        );

        var optionalResult = searchResults.stream().findFirst();
        if (optionalResult.isEmpty()) {
            return;
        }

        var withResult = optionalResult.get();

        var speed = withResult.comp3();
        if (!speed.canAct) {
            return;
        }

        var pos = withResult.comp1().getPosition();
        var velocity = withResult.comp2();
        var instantVelocity = velocity.instant();

        var newPos = pos.add(instantVelocity);
        var log = m_gameContext.log();

        var entities = m_gameContext.pos().getAt(newPos);
        entities.stream()
            .map(entity -> entity.get(Descriptor.class))
            .filter(Objects::nonNull)
            .map(Descriptor::description)
            .forEach(log::log);
    }
}
