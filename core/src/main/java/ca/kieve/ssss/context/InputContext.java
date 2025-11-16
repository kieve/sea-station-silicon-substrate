package ca.kieve.ssss.context;

import com.badlogic.gdx.Input.Keys;

import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.input.KeyState;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class InputContext {
    public enum Mode {
        NORMAL,
        EXAMINE,
        EJECT,
    }

    private final Map<InputAction, Integer> m_keyMappings = new EnumMap<>(InputAction.class);
    private final Map<InputAction, KeyState> m_keyStates = new EnumMap<>(InputAction.class);
    private final Map<Integer, InputAction> m_keycodeToAction = new HashMap<>();

    private Mode m_currentMode = Mode.NORMAL;

    public InputContext() {
        initializeDefaults();
    }

    private void initializeDefaults() {
        setKeyMapping(InputAction.UP, Keys.W);
        setKeyMapping(InputAction.DOWN, Keys.S);
        setKeyMapping(InputAction.LEFT, Keys.A);
        setKeyMapping(InputAction.RIGHT, Keys.D);
        setKeyMapping(InputAction.EXAMINE, Keys.E);
        setKeyMapping(InputAction.CONFIRM, Keys.ENTER);
        setKeyMapping(InputAction.CANCEL, Keys.ESCAPE);
        setKeyMapping(InputAction.EJECT, Keys.Q);
    }

    public void setKeyMapping(InputAction action, int keycode) {
        Integer oldKeycode = m_keyMappings.get(action);
        if (oldKeycode != null) {
            m_keycodeToAction.remove(oldKeycode);
        }

        m_keyMappings.put(action, keycode);
        m_keyStates.put(action, new KeyState(keycode));
        m_keycodeToAction.put(keycode, action);
    }

    public InputAction getActionForKeycode(int keycode) {
        return m_keycodeToAction.get(keycode);
    }

    public KeyState getKeyState(InputAction action) {
        return m_keyStates.get(action);
    }

    public boolean consume(InputAction action) {
        KeyState state = m_keyStates.get(action);
        if (state == null) {
            return false;
        }
        boolean result = state.event;
        state.event = false;
        return result;
    }

    public boolean isHeld(InputAction action) {
        KeyState state = m_keyStates.get(action);
        if (state == null) {
            return false;
        }
        return state.held;
    }

    public Mode getMode() {
        return m_currentMode;
    }

    public void setMode(Mode mode) {
        m_currentMode = mode;
    }

    public boolean isMode(Mode mode) {
        return m_currentMode == mode;
    }
}
