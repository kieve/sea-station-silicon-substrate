package ca.kieve.ssss.system;

import ca.kieve.ssss.component.FluidBarrier;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.util.FluidUtil;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

public final class FluidSimulator {
    @FunctionalInterface
    public interface BarrierResolver {
        FluidBarrier barrierInto(Vec3i cell);
    }

    private static final Vec3i[] LATERAL = {
        Vec3i.NORTH, Vec3i.SOUTH, Vec3i.EAST, Vec3i.WEST
    };

    private static final double FULL = FluidContext.FULL;
    private static final double MIN_MASS = FluidContext.MIN_MASS;
    private static final double MAX_COMPRESS = 0.02;
    private static final double MAX_FLOW = 1.0;
    private static final double MIN_FLOW = 1e-4;
    private static final double LATERAL_RELAX = 1.0 / (2 * LATERAL.length);
    private static final double RETAIN = 0.10;

    private FluidSimulator() {
    }

    public static void step(FluidContext fluid, BarrierResolver barriers) {
        for (Vec3i source : fluid.sourceCells()) {
            fluid.setMass(source, fluid.sourceDepth(source) * FULL);
        }

        Map<Vec3i, Double> deltas = new HashMap<>();
        for (Vec3i cell : fluid.waterCells()) {
            spread(fluid, barriers, cell, deltas);
        }

        for (Map.Entry<Vec3i, Double> entry : deltas.entrySet()) {
            fluid.addMass(entry.getKey(), entry.getValue());
        }
        for (Vec3i cell : deltas.keySet()) {
            if (!fluid.isSource(cell) && fluid.getMass(cell) < MIN_MASS) {
                fluid.setMass(cell, 0);
            }
        }
    }

    private static void spread(
        FluidContext fluid,
        BarrierResolver barriers,
        Vec3i cell,
        Map<Vec3i, Double> deltas
    ) {
        double remaining = fluid.getMass(cell);
        if (remaining <= MIN_MASS) {
            return;
        }
        remaining = flowDown(fluid, barriers, cell, remaining, deltas);
        remaining = flowLateral(fluid, barriers, cell, remaining, deltas);
        flowUp(fluid, barriers, cell, remaining, deltas);
    }

    private static double flowDown(
        FluidContext fluid,
        BarrierResolver barriers,
        Vec3i cell,
        double remaining,
        Map<Vec3i, Double> deltas
    ) {
        Vec3i below = cell.add(Vec3i.DOWN);
        if (!fluid.inBounds(below)) {
            return remaining;
        }
        double conductance = FluidUtil.verticalConductance(barriers.barrierInto(below));
        if (conductance <= 0) {
            return remaining;
        }
        double massBelow = fluid.getMass(below);
        double flow = stableState(remaining + massBelow) - massBelow;
        flow = clampFlow(flow, remaining) * conductance;
        return move(deltas, cell, below, flow, remaining);
    }

    private static double flowLateral(
        FluidContext fluid,
        BarrierResolver barriers,
        Vec3i cell,
        double remaining,
        Map<Vec3i, Double> deltas
    ) {
        double[] want = new double[LATERAL.length];
        double totalWant = 0;
        for (int i = 0; i < LATERAL.length; i++) {
            Vec3i neighbor = cell.add(LATERAL[i]);
            if (!fluid.inBounds(neighbor)) {
                continue;
            }
            double conductance = FluidUtil
                .lateralConductance(remaining, barriers.barrierInto(neighbor));
            if (conductance <= 0) {
                continue;
            }
            double diff = remaining - fluid.getMass(neighbor);
            if (diff <= 0) {
                continue;
            }
            double w = Math.min(diff * LATERAL_RELAX, MAX_FLOW) * conductance;
            want[i] = w;
            totalWant += w;
        }
        if (totalWant <= 0) {
            return remaining;
        }
        double sendable = Math.max(0, remaining - RETAIN);
        double scale = totalWant > sendable ? sendable / totalWant : 1.0;
        for (int i = 0; i < LATERAL.length; i++) {
            if (want[i] <= 0) {
                continue;
            }
            remaining = move(deltas, cell, cell.add(LATERAL[i]), want[i] * scale, remaining);
        }
        return remaining;
    }

    private static void flowUp(
        FluidContext fluid,
        BarrierResolver barriers,
        Vec3i cell,
        double remaining,
        Map<Vec3i, Double> deltas
    ) {
        Vec3i above = cell.add(Vec3i.UP);
        if (!fluid.inBounds(above)) {
            return;
        }
        double conductance = FluidUtil.verticalConductance(barriers.barrierInto(above));
        if (conductance <= 0) {
            return;
        }
        double massAbove = fluid.getMass(above);
        double flow = remaining - stableState(remaining + massAbove);
        flow = clampFlow(flow, remaining) * conductance;
        move(deltas, cell, above, flow, remaining);
    }

    private static double move(
        Map<Vec3i, Double> deltas,
        Vec3i from,
        Vec3i to,
        double amount,
        double remaining
    ) {
        if (amount < MIN_FLOW) {
            return remaining;
        }
        deltas.merge(from, -amount, Double::sum);
        deltas.merge(to, amount, Double::sum);
        return remaining - amount;
    }

    private static double clampFlow(double flow, double remaining) {
        if (flow <= 0) {
            return 0;
        }
        return Math.min(flow, Math.min(MAX_FLOW, remaining));
    }

    private static double stableState(double total) {
        if (total <= FULL) {
            return FULL;
        }
        if (total < 2 * FULL + MAX_COMPRESS) {
            return (FULL * FULL + total * MAX_COMPRESS) / (FULL + MAX_COMPRESS);
        }
        return (total + MAX_COMPRESS) / 2;
    }
}
