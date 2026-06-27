package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.FluidBarrier;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.PositionContext;

public final class FluidUtil {
    public static final int MAX_RESISTANCE = 10;
    public static final int MAX_BARRIER_HEIGHT = 10;

    public static final FluidBarrier OPEN = new FluidBarrier(0, 0);
    public static final FluidBarrier SOLID = new FluidBarrier(MAX_BARRIER_HEIGHT, MAX_RESISTANCE);

    private FluidUtil() {
    }

    public static FluidBarrier barrierInto(GameContext context, Vec3i cell) {
        return barrierInto(context.pos(), cell);
    }

    public static FluidBarrier barrierInto(PositionContext positions, Vec3i cell) {
        boolean anySolid = false;
        for (Entity entity : positions.getAt(cell)) {
            FluidBarrier barrier = entity.get(FluidBarrier.class);
            if (barrier != null && isBarrierActive(entity)) {
                return barrier;
            }
            if (SolidUtil.isSolid(entity)) {
                anySolid = true;
            }
        }
        return anySolid ? SOLID : OPEN;
    }

    private static boolean isBarrierActive(Entity entity) {
        Openable openable = entity.get(Openable.class);
        return openable == null || !openable.isOpen;
    }

    public static boolean blocksAllFlow(FluidBarrier barrier) {
        return barrier.barrierHeight() >= MAX_BARRIER_HEIGHT
            && barrier.flowResistance() >= MAX_RESISTANCE;
    }

    public static double verticalConductance(FluidBarrier into) {
        return seepConductance(into);
    }

    public static double lateralConductance(double srcMass, FluidBarrier into) {
        if (blocksAllFlow(into)) {
            return 0;
        }
        if (srcMass > overflowMass(into.barrierHeight())) {
            return 1.0;
        }
        return seepConductance(into);
    }

    private static double overflowMass(int barrierHeight) {
        if (barrierHeight >= MAX_BARRIER_HEIGHT) {
            return Double.POSITIVE_INFINITY;
        }
        return (double) barrierHeight / MAX_BARRIER_HEIGHT * FluidContext.FULL;
    }

    private static double seepConductance(FluidBarrier into) {
        int resistance = into.flowResistance();
        if (resistance >= MAX_RESISTANCE) {
            return 0;
        }
        return 1.0 - (double) resistance / MAX_RESISTANCE;
    }
}
