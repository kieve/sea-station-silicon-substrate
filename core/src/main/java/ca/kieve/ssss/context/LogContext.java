package ca.kieve.ssss.context;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LogContext {
    private static final int MAX_MESSAGES = 30;

    private final Queue<String> m_messages = new LinkedList<>();

    public List<String> getMessages() {
        return m_messages.stream().toList();
    }

    public void log(String message) {
        if (m_messages.size() >= MAX_MESSAGES) {
            m_messages.poll();
        }
        m_messages.add(message);
    }
}
