package ca.kieve.ssss.component;

import ca.kieve.ssss.util.FluidUtil;

import static ca.kieve.ssss.util.MathUtil.clampWarn;

public record FluidBarrier(int barrierHeight, int flowResistance) implements Component {
    public FluidBarrier {
        barrierHeight = clampWarn(barrierHeight, 0, FluidUtil.MAX_BARRIER_HEIGHT, "barrierHeight");
        flowResistance = clampWarn(flowResistance, 0, FluidUtil.MAX_RESISTANCE, "flowResistance");
    }
}
