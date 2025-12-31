package ca.kieve.ssss.ai.state;

import java.util.Map;

/**
 * State that logs a death announcement message.
 * Property: message (String) - the message to log.
 */
public class AnnounceDeathState extends AiState {
    private String m_message;

    @Override
    public void initialize(Map<String, Object> properties) {
        super.initialize(properties);
        m_message = (String) properties.get("message");
    }

    @Override
    public void execute(StateContext context) {
        context.gameContext().log().log(m_message);
    }
}
