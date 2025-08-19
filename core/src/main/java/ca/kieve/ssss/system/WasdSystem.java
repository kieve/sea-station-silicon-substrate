package ca.kieve.ssss.system;

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

        if (wasdController.consume(W)) {
            pos.y++;
        }
        if (wasdController.consume(A)) {
            pos.x--;
        }
        if (wasdController.consume(S)) {
            pos.y--;
        }
        if (wasdController.consume(D)) {
            pos.x++;
        }
        m_gameContext.pos().move(withResult.entity(), oldPos, pos);
    }
}
