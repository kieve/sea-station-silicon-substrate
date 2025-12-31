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

    private Vec3i m_initialPos;
    private boolean m_goingUp = true;

    @Override
    public void onEnter(StateContext context) {
        if (m_initialPos != null) {
            return;
        }
        var posComp = context.entity().get(Position.class);
        if (posComp != null) {
            m_initialPos = posComp.getPosition().copy();
        }
    }

    @Override
    public void execute(StateContext context) {
        var posComp = context.entity().get(Position.class);
        var velocity = context.entity().get(Velocity.class);

        if (posComp == null || velocity == null) {
            return;
        }

        Vec3i pos = posComp.getPosition();
        if (m_initialPos == null) {
            return;
        }

        int range = DEFAULT_RANGE;
        if (m_properties != null && m_properties.containsKey("range")) {
            range = ((Number) m_properties.get("range")).intValue();
        }

        // Check bounds and potentially reverse direction
        if (m_goingUp && pos.y >= m_initialPos.y + range
                || !m_goingUp && pos.y <= m_initialPos.y - range) {
            m_goingUp = !m_goingUp;
        }

        Vec3i dv = Vec3i.Y.copy();
        if (!m_goingUp) {
            dv.productMut(-1);
        }

        velocity.instant().set(dv);
    }
}
