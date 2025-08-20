package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Velocity;
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
            Velocity.class
        );

        var optionalResult = searchResults.stream().findFirst();
        if (optionalResult.isEmpty()) {
            return;
        }

        var withResult = optionalResult.get();
        var wasdController = withResult.comp1();
        var velocity = withResult.comp2();
        var instantVelocity = velocity.instant();

        if (wasdController.consume(W)) {
            instantVelocity.y++;
        }
        if (wasdController.consume(A)) {
            instantVelocity.x--;
        }
        if (wasdController.consume(S)) {
            instantVelocity.y--;
        }
        if (wasdController.consume(D)) {
            instantVelocity.x++;
        }
    }
}
