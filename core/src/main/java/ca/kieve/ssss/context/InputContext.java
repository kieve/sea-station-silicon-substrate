package ca.kieve.ssss.context;

public class InputContext {
    public enum Mode {
        NORMAL,
        EXAMINE,
    }

    private Mode m_currentMode = Mode.NORMAL;

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
