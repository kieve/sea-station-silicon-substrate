package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

public class SanityCheckSystem extends System {
    public SanityCheckSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void run() {
        verifyStaticVec3i("Vec3i.ZERO", Vec3i.ZERO, 0, 0, 0);
        verifyStaticVec3i("Vec3i.X", Vec3i.X, 1, 0, 0);
        verifyStaticVec3i("Vec3i.Y", Vec3i.Y, 0, 1, 0);
    }

    private void verifyStaticVec3i(String label, Vec3i v, int x, int y, int z) {
        if (v.x != x || v.y != y || v.z != z) {
            throw new RuntimeException(label + " is no longer zero");
        }
    }
}
