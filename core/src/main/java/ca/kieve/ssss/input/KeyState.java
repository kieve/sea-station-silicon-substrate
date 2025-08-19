package ca.kieve.ssss.input;

public class KeyState {
    public final int keycode;

    public boolean event = false;
    public boolean held = false;

    public KeyState(int keycode) {
        this.keycode = keycode;
    }

    public boolean handleKeyDown(int inKey) {
        if (inKey != keycode) return false;
        event = true;
        held = true;
        return true;
    }

    public boolean handleKeyUp(int inKey) {
        if (inKey != keycode) return false;
        held = false;
        return true;
    }

    public void consume() {
        event = false;
    }
}
