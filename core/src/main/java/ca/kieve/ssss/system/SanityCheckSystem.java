package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

public class SanityCheckSystem extends System {
    public SanityCheckSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void run() {
        var z = Vec3i.ZERO;
        if (z.x != 0 || z.y != 0 || z.z != 0) {
            throw new RuntimeException("Vec3i.ZERO is no longer zero");
        }
    }
}
