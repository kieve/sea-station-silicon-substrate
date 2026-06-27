package ca.kieve.ssss.system;

import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.FluidUtil;

public class FluidSystem extends System {
    private static final long FLOW_PERIOD = 10;

    private final FluidContext m_fluid;
    private long m_nextFlowTime = FLOW_PERIOD;

    public FluidSystem(GameContext gameContext) {
        super(gameContext);
        m_fluid = gameContext.fluid();
    }

    @Override
    public void tick() {
        long now = m_clock.getCurrentTime();
        while (now >= m_nextFlowTime) {
            FluidSimulator.step(m_fluid, cell -> FluidUtil.barrierInto(m_gameContext, cell));
            m_nextFlowTime += FLOW_PERIOD;
        }
    }
}
