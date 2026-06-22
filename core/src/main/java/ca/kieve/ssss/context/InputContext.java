package ca.kieve.ssss.context;

import com.badlogic.gdx.Input.Keys;

import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.input.KeyState;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ca.kieve.ssss.input.InputAction.CANCEL;
import static ca.kieve.ssss.input.InputAction.CONFIRM;
import static ca.kieve.ssss.input.InputAction.DEBUG_MENU;
import static ca.kieve.ssss.input.InputAction.DOWN;
import static ca.kieve.ssss.input.InputAction.E;
import static ca.kieve.ssss.input.InputAction.EJECT;
import static ca.kieve.ssss.input.InputAction.EXAMINE;
import static ca.kieve.ssss.input.InputAction.EXIT_GAME;
import static ca.kieve.ssss.input.InputAction.INTERACT;
import static ca.kieve.ssss.input.InputAction.INVENTORY;
import static ca.kieve.ssss.input.InputAction.LEFT;
import static ca.kieve.ssss.input.InputAction.N;
import static ca.kieve.ssss.input.InputAction.NE;
import static ca.kieve.ssss.input.InputAction.NW;
import static ca.kieve.ssss.input.InputAction.ORIGIN;
import static ca.kieve.ssss.input.InputAction.RIGHT;
import static ca.kieve.ssss.input.InputAction.S;
import static ca.kieve.ssss.input.InputAction.SE;
import static ca.kieve.ssss.input.InputAction.SELF;
import static ca.kieve.ssss.input.InputAction.SW;
import static ca.kieve.ssss.input.InputAction.SWITCH_PANE;
import static ca.kieve.ssss.input.InputAction.UP;
import static ca.kieve.ssss.input.InputAction.W;
import static ca.kieve.ssss.input.InputAction.WAIT;

public class InputContext {
    public enum Mode {
        MODE_NORMAL(DIRECTIONS, List.of(EXAMINE, EJECT, INTERACT, WAIT, INVENTORY)),
        MODE_EXAMINE(DIRECTIONS, PROMPT, List.of(EXAMINE)),
        MODE_EJECT(DIRECTIONS, List.of(EJECT)),
        MODE_INTERACT(DIRECTIONS, PROMPT, List.of(INTERACT, WAIT)),
        MODE_INVENTORY(DIRECTIONS, PROMPT, LOCATIONS, List.of(SWITCH_PANE)),
        MODE_DEBUG_MENU(DIRECTIONS, PROMPT);

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
    private static final Set<InputAction> LOCATIONS = EnumSet.of(
        N,
        S,
        E,
        W,
        NE,
        NW,
        SE,
        SW,
        ORIGIN,
        SELF
    );
    private static final Set<InputAction> GLOBAL_ACTIONS = EnumSet.of(EXIT_GAME, DEBUG_MENU);

    private final Map<InputAction, Set<Integer>> m_keyMappings = new EnumMap<>(InputAction.class);
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
        setKeyMapping(INVENTORY, Keys.I);
        setKeyMapping(SWITCH_PANE, Keys.TAB);

        // Spatial location picker, laid out like a numpad:
        //   NW N  NE       7 8 9
        //   W  O  E   <=>  4 5 6   (O = ORIGIN, the tile underfoot)
        //   SW S  SE       1 2 3
        //      SELF          0     (SELF = the player's own inventory)
        setKeyMapping(NW, Keys.NUMPAD_7);
        setKeyMapping(N, Keys.NUMPAD_8);
        setKeyMapping(NE, Keys.NUMPAD_9);
        setKeyMapping(W, Keys.NUMPAD_4);
        setKeyMapping(ORIGIN, Keys.NUMPAD_5);
        setKeyMapping(E, Keys.NUMPAD_6);
        setKeyMapping(SW, Keys.NUMPAD_1);
        setKeyMapping(S, Keys.NUMPAD_2);
        setKeyMapping(SE, Keys.NUMPAD_3);
        setKeyMapping(SELF, Keys.NUMPAD_0);

        setKeyMapping(EXIT_GAME, Keys.ESCAPE);
        setKeyMapping(DEBUG_MENU, Keys.GRAVE);

        // Numpad-less keyboards: a second binding for the picker, using a
        // 3x3 cluster on the main keyboard that lines up under i-o-p.
        //   7 8 9        8 9 0
        //   4 5 6   <=>  i o p
        //   1 2 3        k l ;
        //     0            ,
        addKeyMapping(NW, Keys.NUM_8);
        addKeyMapping(N, Keys.NUM_9);
        addKeyMapping(NE, Keys.NUM_0);
        addKeyMapping(W, Keys.I);
        addKeyMapping(ORIGIN, Keys.O);
        addKeyMapping(E, Keys.P);
        addKeyMapping(SW, Keys.K);
        addKeyMapping(S, Keys.L);
        addKeyMapping(SE, Keys.SEMICOLON);
        addKeyMapping(SELF, Keys.COMMA);
    }

    public void setKeyMapping(InputAction action, int keycode) {
        Set<Integer> oldKeycodes = m_keyMappings.get(action);
        if (oldKeycodes != null) {
            for (Integer oldKeycode : oldKeycodes) {
                m_keycodeToAction.remove(oldKeycode);
            }
        }

        Set<Integer> keycodes = new HashSet<>();
        keycodes.add(keycode);
        m_keyMappings.put(action, keycodes);
        m_keyStates.put(action, new KeyState(keycode));
        m_keycodeToAction.put(keycode, action);
    }

    /**
     * Adds an additional keycode that triggers the given action, on top of
     * any binding set by {@link #setKeyMapping}. Use this for alternate
     * hotkeys (e.g. a numpad-less layout that mirrors the numpad).
     */
    public void addKeyMapping(InputAction action, int keycode) {
        Set<Integer> keycodes = m_keyMappings.get(action);
        KeyState state = m_keyStates.get(action);
        if (keycodes == null || state == null) {
            setKeyMapping(action, keycode);
            return;
        }
        keycodes.add(keycode);
        state.addKeycode(keycode);
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
                Set<Integer> keycodes = m_keyMappings.get(action);
                if (keycodes == null) {
                    continue;
                }

                for (Integer keycode : keycodes) {
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
}
