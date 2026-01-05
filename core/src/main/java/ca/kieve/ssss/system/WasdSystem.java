package ca.kieve.ssss.system;

import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.input.InputAction.WAIT;

public class WasdSystem extends System {
    private final InputContext m_input;

    public WasdSystem(GameContext gameContext) {
        super(gameContext);
        m_input = gameContext.input();
    }

    @Override
    public void awaitingUserInput() {
        if (!m_input.isMode(MODE_NORMAL)) {
            return;
        }

        var searchResults = m_gameContext.ecs().findEntitiesWith(
            PlayerController.class,
            Velocity.class,
            Speed.class
        );

        var optionalResult = searchResults.stream().findFirst();
        if (optionalResult.isEmpty()) {
            return;
        }

        var withResult = optionalResult.get();
        var velocity = withResult.comp2();
        var instantVelocity = velocity.instant();
        var speed = withResult.comp3().val;

        boolean anyInput = false;
        if (m_input.consume(UP)) {
            instantVelocity.y++;
            anyInput = true;
        }
        if (m_input.consume(LEFT)) {
            instantVelocity.x--;
            anyInput = true;
        }
        if (m_input.consume(DOWN)) {
            instantVelocity.y--;
            anyInput = true;
        }
        if (m_input.consume(RIGHT)) {
            instantVelocity.x++;
            anyInput = true;
        }
        if (m_input.consume(WAIT)) {
            anyInput = true;
        }

        if (anyInput) {
            m_clock.processPlayerActed(speed);
        }
    }
}
