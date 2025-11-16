package ca.kieve.ssss.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InputContext.Mode;

public class EjectInputController extends InputAdapter {
    private final InputContext m_inputContext;

    private final KeyState m_ejectKey = new KeyState(Keys.Q);
    private final KeyState m_wKey = new KeyState(Keys.W);
    private final KeyState m_aKey = new KeyState(Keys.A);
    private final KeyState m_sKey = new KeyState(Keys.S);
    private final KeyState m_dKey = new KeyState(Keys.D);

    public EjectInputController(GameContext gameContext) {
        m_inputContext = gameContext.input();
    }

    public boolean consumeEjectKey() {
        var result = m_ejectKey.event;
        m_ejectKey.event = false;
        return result;
    }

    public boolean consumeW() {
        var result = m_wKey.event;
        m_wKey.event = false;
        return result;
    }

    public boolean consumeA() {
        var result = m_aKey.event;
        m_aKey.event = false;
        return result;
    }

    public boolean consumeS() {
        var result = m_sKey.event;
        m_sKey.event = false;
        return result;
    }

    public boolean consumeD() {
        var result = m_dKey.event;
        m_dKey.event = false;
        return result;
    }

    @Override
    public boolean keyDown(int keycode) {
        // Track Q key when in NORMAL mode (to enter eject mode)
        if (m_inputContext.isMode(Mode.NORMAL)) {
            if (m_ejectKey.handleKeyDown(keycode)) {
                return true;
            }
        }

        // Only consume other keys when in EJECT mode
        if (!m_inputContext.isMode(Mode.EJECT)) {
            return false;
        }

        // Track Q key to cancel eject mode
        if (m_ejectKey.handleKeyDown(keycode)) {
            return true;
        }

        // Consume WASD for direction selection
        if (m_wKey.handleKeyDown(keycode)) {
            return true;
        }
        if (m_aKey.handleKeyDown(keycode)) {
            return true;
        }
        if (m_sKey.handleKeyDown(keycode)) {
            return true;
        }
        if (m_dKey.handleKeyDown(keycode)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        m_ejectKey.handleKeyUp(keycode);
        m_wKey.handleKeyUp(keycode);
        m_aKey.handleKeyUp(keycode);
        m_sKey.handleKeyUp(keycode);
        m_dKey.handleKeyUp(keycode);
        return false;
    }
}
