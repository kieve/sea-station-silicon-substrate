package ca.kieve.ssss.input;

import java.util.HashSet;
import java.util.Set;

public class KeyState {
    public boolean event = false;
    public boolean held = false;

    private final Set<Integer> m_keycodes = new HashSet<>();

    public KeyState(int keycode) {
        m_keycodes.add(keycode);
    }

    public void addKeycode(int keycode) {
        m_keycodes.add(keycode);
    }

    public boolean matches(int keycode) {
        return m_keycodes.contains(keycode);
    }

    public boolean handleKeyDown(int inKey) {
        if (!matches(inKey)) {
            return false;
        }
        event = true;
        held = true;
        return true;
    }

    public boolean handleKeyUp(int inKey) {
        if (!matches(inKey)) {
            return false;
        }
        held = false;
        return true;
    }

    public void consume() {
        event = false;
    }
}
