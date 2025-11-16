package ca.kieve.ssss.input;

import com.badlogic.gdx.InputAdapter;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.InputContext;

public class InputActionController extends InputAdapter {
    private final InputContext m_inputContext;

    public InputActionController(GameContext gameContext) {
        m_inputContext = gameContext.input();
    }

    @Override
    public boolean keyDown(int keycode) {
        InputAction action = m_inputContext.getActionForKeycode(keycode);
        if (action == null) {
            return false;
        }

        KeyState state = m_inputContext.getKeyState(action);
        if (state == null) {
            return false;
        }

        return state.handleKeyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        InputAction action = m_inputContext.getActionForKeycode(keycode);
        if (action == null) {
            return false;
        }

        KeyState state = m_inputContext.getKeyState(action);
        if (state == null) {
            return false;
        }

        return state.handleKeyUp(keycode);
    }
}
