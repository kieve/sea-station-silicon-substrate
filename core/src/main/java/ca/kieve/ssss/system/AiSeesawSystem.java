package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.ai.AiSeesawController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

public class AiSeesawSystem extends System {
    public AiSeesawSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void preTick() {
        var searchResults = m_gameContext.ecs().findEntitiesWith(
            AiSeesawController.class,
            Position.class,
            Velocity.class,
            Speed.class
        );

        searchResults.forEach(with -> {
            var controller = with.comp1();
            var pos = with.comp2().getPosition();
            var velocity = with.comp3();
            var speed = with.comp4();

            if (!speed.canAct) {
                return;
            }

            // Figure out what direction it should go
            var ip = controller.m_initialPos;
            var dv = Vec3i.Y.copy();
            if (controller.goUp && pos.y >= ip.y + 5
                || !controller.goUp && pos.y <= ip.y - 5)
            {
                // Swap directions
                controller.goUp = !controller.goUp;
            }

            if (!controller.goUp) {
                // Swap the vector to go down
                dv.productMut(-1);
            }

            velocity.instant().set(dv);
        });
    }
}
