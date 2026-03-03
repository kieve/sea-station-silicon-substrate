package ca.kieve.ssss.input;

import com.badlogic.gdx.Gdx;
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
        if (handleGlobalActions(keycode)) {
            return true;
        }

        boolean handled = false;
        for (InputAction action : InputAction.values()) {
            if (!m_inputContext.isActionActiveInCurrentMode(action)) {
                continue;
            }
            KeyState state = m_inputContext.getKeyState(action);
            if (state != null && state.handleKeyDown(keycode)) {
                handled = true;
            }
        }
        return handled;
    }

    @Override
    public boolean keyUp(int keycode) {
        // Update all KeyStates for keyUp (held flag persists across mode changes)
        boolean handled = false;
        for (InputAction action : InputAction.values()) {
            KeyState state = m_inputContext.getKeyState(action);
            if (state != null && state.handleKeyUp(keycode)) {
                handled = true;
            }
        }
        return handled;
    }

    private boolean handleGlobalActions(int keycode) {
        KeyState exitState = m_inputContext.getKeyState(InputAction.EXIT_GAME);
        if (exitState != null && exitState.keycode == keycode) {
            Gdx.app.exit();
            return true;
        }
        return false;
    }
}
