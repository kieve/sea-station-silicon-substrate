package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.util.Vec3i;

/**
 * State that oscillates up/down within a range.
 * Logic ported from AiSeesawSystem.
 */
public class WanderState extends AiState {
    private static final int DEFAULT_RANGE = 5;

    @Override
    public void onEnter(StateContext context) {
        // Capture initial position when first entering this state
        var controller = context.controller();
        if (controller.getWanderInitialPos() == null) {
            var posComp = context.entity().get(Position.class);
            if (posComp != null) {
                controller.setWanderInitialPos(posComp.getPosition());
            }
        }
    }

    @Override
    public void execute(StateContext context) {
        var controller = context.controller();
        var posComp = context.entity().get(Position.class);
        var velocity = context.entity().get(Velocity.class);

        if (posComp == null || velocity == null) {
            return;
        }

        Vec3i pos = posComp.getPosition();
        Vec3i initialPos = controller.getWanderInitialPos();
        if (initialPos == null) {
            return;
        }

        int range = DEFAULT_RANGE;
        if (m_properties != null && m_properties.containsKey("range")) {
            range = ((Number) m_properties.get("range")).intValue();
        }

        // Check bounds and potentially reverse direction
        boolean goingUp = controller.isWanderGoingUp();
        if (goingUp && pos.y >= initialPos.y + range
                || !goingUp && pos.y <= initialPos.y - range) {
            goingUp = !goingUp;
            controller.setWanderGoingUp(goingUp);
        }

        Vec3i dv = Vec3i.Y.copy();
        if (!goingUp) {
            dv.productMut(-1);
        }

        velocity.instant().set(dv);
    }
}
