package ca.kieve.ssss.context;

import com.badlogic.gdx.Input.Keys;

import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.input.KeyState;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.EJECT;
import static ca.kieve.ssss.input.InputAction.EXAMINE;
import static ca.kieve.ssss.input.InputAction.EXIT_GAME;
import static ca.kieve.ssss.input.InputAction.INTERACT;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.input.InputAction.WAIT;

public class InputContext {
    public enum Mode {
        MODE_NORMAL(DIRECTIONS, List.of(EXAMINE, EJECT, INTERACT, WAIT)),
        MODE_EXAMINE(DIRECTIONS, PROMPT, List.of(EXAMINE)),
        MODE_EJECT(DIRECTIONS, List.of(EJECT)),
        MODE_INTERACT(DIRECTIONS, PROMPT, List.of(INTERACT, WAIT));

        private final Set<InputAction> m_activeActions;

        @SafeVarargs
        Mode(Collection<InputAction>... actionGroups) {
            Set<InputAction> actions = EnumSet.noneOf(InputAction.class);
            for (Collection<InputAction> group : actionGroups) {
                actions.addAll(group);
            }
            m_activeActions = actions;
        }

        public boolean isActionActive(InputAction action) {
            return m_activeActions.contains(action);
        }

        public Set<InputAction> getActiveActions() {
            return Collections.unmodifiableSet(m_activeActions);
        }
    }

    private static final Set<InputAction> DIRECTIONS = EnumSet.of(UP, DOWN, LEFT, RIGHT);
    private static final Set<InputAction> PROMPT = EnumSet.of(CANCEL, CONFIRM);
    private static final Set<InputAction> GLOBAL_ACTIONS = EnumSet.of(EXIT_GAME);

    private final Map<InputAction, Integer> m_keyMappings = new EnumMap<>(InputAction.class);
    private final Map<InputAction, KeyState> m_keyStates = new EnumMap<>(InputAction.class);
    private final Map<Integer, InputAction> m_keycodeToAction = new HashMap<>();

    private Mode m_currentMode = Mode.MODE_NORMAL;

    public InputContext() {
        initializeDefaults();
        validateNoOverlapsWithinModes();
    }

    private void initializeDefaults() {
        setKeyMapping(UP, Keys.W);
        setKeyMapping(DOWN, Keys.S);
        setKeyMapping(LEFT, Keys.A);
        setKeyMapping(RIGHT, Keys.D);
        setKeyMapping(EXAMINE, Keys.E);
        setKeyMapping(CONFIRM, Keys.ENTER);
        setKeyMapping(CANCEL, Keys.Q);
        setKeyMapping(EJECT, Keys.Q);
        setKeyMapping(INTERACT, Keys.F);
        setKeyMapping(WAIT, Keys.SPACE);
        setKeyMapping(EXIT_GAME, Keys.ESCAPE);
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
        if (m_currentMode == mode) {
            return;
        }
        m_currentMode = mode;
        clearInactiveEvents();
    }

    public boolean isMode(Mode mode) {
        return m_currentMode == mode;
    }

    public boolean isActionActiveInCurrentMode(InputAction action) {
        if (GLOBAL_ACTIONS.contains(action)) {
            return true;
        }
        return m_currentMode.isActionActive(action);
    }

    private void clearInactiveEvents() {
        for (var entry : m_keyStates.entrySet()) {
            InputAction action = entry.getKey();
            if (!isActionActiveInCurrentMode(action)) {
                entry.getValue().event = false;
            }
        }
    }

    private void validateNoOverlapsWithinModes() {
        for (Mode mode : Mode.values()) {
            Map<Integer, InputAction> keysInMode = new HashMap<>();
            for (InputAction action : mode.getActiveActions()) {
                Integer keycode = m_keyMappings.get(action);
                if (keycode == null) {
                    continue;
                }

                InputAction existing = keysInMode.get(keycode);
                if (existing != null) {
                    throw new IllegalStateException(
                        "Hotkey conflict in " + mode + ": " + existing + " and " + action
                            + " both mapped to key " + keycode
                    );
                }
                keysInMode.put(keycode, action);
            }
        }
    }
}
