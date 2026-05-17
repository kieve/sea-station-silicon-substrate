package ca.kieve.ssss.system;

import ca.kieve.ssss.context.DebugContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.RenderContext;

import static ca.kieve.ssss.context.InputContext.Mode.MODE_DEBUG_MENU;
import static ca.kieve.ssss.context.InputContext.Mode.MODE_NORMAL;
import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;

public class DebugMenuSystem extends System {
    private final InputContext m_input;
    private final DebugContext m_debug;
    private final RenderContext m_render;

    public DebugMenuSystem(GameContext gameContext) {
        super(gameContext);
        m_input = gameContext.input();
        m_debug = gameContext.debug();
        m_render = gameContext.render();
    }

    @Override
    public void awaitingUserInput() {
        if (!m_input.isMode(MODE_DEBUG_MENU)) {
            return;
        }

        if (m_input.consume(UP)) {
            m_debug.decrementSelectedIndex();
        }
        if (m_input.consume(DOWN)) {
            m_debug.incrementSelectedIndex();
        }
        m_input.consume(LEFT);
        m_input.consume(RIGHT);

        if (m_input.consume(CONFIRM)) {
            m_debug.toggleSelected();
            m_render.markDirty();
        }

        if (!m_input.consume(CANCEL)) {
            return;
        }
        m_input.setMode(MODE_NORMAL);
        m_render.markDirty();
    }
}
