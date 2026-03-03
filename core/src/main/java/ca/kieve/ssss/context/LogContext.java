package ca.kieve.ssss.context;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LogContext {
    public record LogEntry(String message, int count) {
    }

    private static final int MAX_MESSAGES = 30;

    private final Queue<LogEntry> m_messages = new LinkedList<>();

    private int m_totalMessages = 0;

    public List<LogEntry> getMessages() {
        return m_messages.stream().toList();
    }

    public int getTotalMessageCount() {
        return m_totalMessages;
    }

    public int getFirstMessageNumber() {
        return m_totalMessages - m_messages.size() + 1;
    }

    public void log(String message) {
        IO.println("[LOG] " + message);

        // Check if the last message is the same
        if (!m_messages.isEmpty()) {
            var last = ((LinkedList<LogEntry>) m_messages).getLast();
            if (last.message().equals(message)) {
                // Replace with incremented count
                ((LinkedList<LogEntry>) m_messages).removeLast();
                m_messages.add(new LogEntry(message, last.count() + 1));
                return;
            }
        }

        if (m_messages.size() >= MAX_MESSAGES) {
            m_messages.poll();
        }
        m_messages.add(new LogEntry(message, 1));
        m_totalMessages++;
    }
}
