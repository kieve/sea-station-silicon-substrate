package ca.kieve.ssss.component;

import com.badlogic.gdx.InputAdapter;

import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.input.KeyState;

import java.util.Map;

import static ca.kieve.ssss.input.InputAction.A;
import static ca.kieve.ssss.input.InputAction.D;
import static ca.kieve.ssss.input.InputAction.S;
import static ca.kieve.ssss.input.InputAction.W;

public class WasdController extends InputAdapter {
    private final Map<InputAction, KeyState> m_enumMap;

    public WasdController() {
        m_enumMap = Map.of(
            W, W.createKeyState(),
            A, A.createKeyState(),
            S, S.createKeyState(),
            D, D.createKeyState()
        );
    }

    public boolean consume(InputAction action) {
        var state = m_enumMap.get(action);
        if (state == null) {
            return false;
        }

        var result = state.event;
        state.event = false;
        return result;
    }

    @Override
    public boolean keyDown(int keycode) {
        return m_enumMap.values().stream()
            .anyMatch(state -> state.handleKeyDown(keycode));
    }

    @Override
    public boolean keyUp(int keycode) {
        return m_enumMap.values().stream()
            .anyMatch(state -> state.handleKeyUp(keycode));
    }
}
