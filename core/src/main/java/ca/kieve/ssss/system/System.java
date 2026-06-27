package ca.kieve.ssss.system;

import ca.kieve.ssss.context.ClockContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.PerfClock;

public abstract class System implements Runnable {
    protected final GameContext m_gameContext;
    protected final ClockContext m_clock;
    private final PerfClock m_perf;
    private final String m_perfName;

    public System(GameContext gameContext) {
        m_gameContext = gameContext;
        m_clock = gameContext.clock();
        m_perf = gameContext.perf();
        m_perfName = getClass().getSimpleName();
    }

    public void awaitingUserInput() {
    }

    public void preTick() {
    }

    public void tick() {
    }

    public void postTick() {
    }

    public void reportResults() {
    }

    @Override
    public void run() {
        var stage = m_clock.getTickStage();
        String key = m_perfName + "-" + stage.name();
        m_perf.start(key);
        switch (stage) {
        case AWAIT_INPUT -> awaitingUserInput();
        case PRE_TICK -> preTick();
        case TICK -> tick();
        case POST_TICK -> postTick();
        case REPORT_RESULTS -> reportResults();
        }
        m_perf.end(key);
    }
}
