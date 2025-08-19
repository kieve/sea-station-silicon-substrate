package ca.kieve.ssss.input;

import com.badlogic.gdx.Input.Keys;

public enum InputAction {
    W(Keys.W),
    A(Keys.A),
    S(Keys.S),
    D(Keys.D),
    ;

    private final int m_keycode;

    InputAction(int keycode) {
        m_keycode = keycode;
    }

    public KeyState createKeyState() {
        return new KeyState(m_keycode);
    }
}
