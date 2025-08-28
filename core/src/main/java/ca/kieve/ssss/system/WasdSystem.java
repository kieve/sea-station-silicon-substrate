package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Speed;
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
    public void awaitingUserInput() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(
            WasdController.class,
            Velocity.class,
            Speed.class
        );

        var optionalResult = searchResults.stream().findFirst();
        if (optionalResult.isEmpty()) {
            return;
        }

        var withResult = optionalResult.get();
        var wasdController = withResult.comp1();
        var velocity = withResult.comp2();
        var instantVelocity = velocity.instant();
        var speed = withResult.comp3().val;

        boolean anyInput = false;
        if (wasdController.consume(W)) {
            instantVelocity.y++;
            anyInput = true;
        }
        if (wasdController.consume(A)) {
            instantVelocity.x--;
            anyInput = true;
        }
        if (wasdController.consume(S)) {
            instantVelocity.y--;
            anyInput = true;
        }
        if (wasdController.consume(D)) {
            instantVelocity.x++;
            anyInput = true;
        }

        if (anyInput) {
            m_clock.setUserInputRegistered(true);
            m_clock.setTargetTime(
                m_clock.getCurrentTime()
                + ClockSystem.getTicksToAct(speed)
            );
        }
    }
}
