package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;

import static ca.kieve.ssss.input.InputAction.A;
import static ca.kieve.ssss.input.InputAction.D;
import static ca.kieve.ssss.input.InputAction.S;
import static ca.kieve.ssss.input.InputAction.W;

public class WasdSystem extends System {
    public WasdSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void run() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(
            WasdController.class,
            Position.class
        );

        var optionalResult = searchResults.stream().findFirst();
        if (optionalResult.isEmpty()) {
            return;
        }

        var withResult = optionalResult.get();
        var wasdController = withResult.comp1();
        var pos = withResult.comp2().getPosition();

        var oldPos = pos.copy();
        var newPos = pos.copy();

        if (wasdController.consume(W)) {
            newPos.y++;
        }
        if (wasdController.consume(A)) {
            newPos.x--;
        }
        if (wasdController.consume(S)) {
            newPos.y--;
        }
        if (wasdController.consume(D)) {
            newPos.x++;
        }
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
}
