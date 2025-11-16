package ca.kieve.ssss.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

import ca.kieve.ssss.context.InputContext;
import ca.kieve.ssss.context.InputContext.Mode;

public class ExamineInputController extends InputAdapter {
    private final InputContext m_inputContext;

    private final KeyState m_examineKey = new KeyState(Keys.E);
    private final KeyState m_enterKey = new KeyState(Keys.ENTER);
    private final KeyState m_escapeKey = new KeyState(Keys.ESCAPE);
    private final KeyState m_wKey = new KeyState(Keys.W);
    private final KeyState m_aKey = new KeyState(Keys.A);
    private final KeyState m_sKey = new KeyState(Keys.S);
    private final KeyState m_dKey = new KeyState(Keys.D);

    public ExamineInputController(InputContext inputContext) {
        m_inputContext = inputContext;
    }

    public boolean consumeExamineKey() {
        var result = m_examineKey.event;
        m_examineKey.event = false;
        return result;
    }

    public boolean consumeEnterKey() {
        var result = m_enterKey.event;
        m_enterKey.event = false;
        return result;
    }

    public boolean consumeEscapeKey() {
        var result = m_escapeKey.event;
        m_escapeKey.event = false;
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
        // Always track E key to toggle examine mode
        if (m_examineKey.handleKeyDown(keycode)) {
            return true;
        }

        // Only consume other keys when in EXAMINE mode
        if (!m_inputContext.isMode(Mode.EXAMINE)) {
            return false;
        }

        // Consume WASD to prevent player movement
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

        // Track Enter and Escape
        if (m_enterKey.handleKeyDown(keycode)) {
            return true;
        }
        if (m_escapeKey.handleKeyDown(keycode)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        m_examineKey.handleKeyUp(keycode);
        m_wKey.handleKeyUp(keycode);
        m_aKey.handleKeyUp(keycode);
        m_sKey.handleKeyUp(keycode);
        m_dKey.handleKeyUp(keycode);
        m_enterKey.handleKeyUp(keycode);
        m_escapeKey.handleKeyUp(keycode);
        return false;
    }
}
