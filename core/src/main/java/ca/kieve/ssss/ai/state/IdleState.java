package ca.kieve.ssss.ai.state;

/**
 * State that does nothing.
 */
public class IdleState extends AiState {
    @Override
    public void execute(StateContext context) {
        // Do nothing
    }
}
