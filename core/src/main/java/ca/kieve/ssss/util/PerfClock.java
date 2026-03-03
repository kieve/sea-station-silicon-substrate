package ca.kieve.ssss.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class PerfClock {
    private static class PerfEntry {
        long m_startTime;
        long m_totalNanos;
        int m_callCount;
    }

    private static final double NANOS_PER_MS = 1_000_000.0;
    private static final boolean DEFAULT_ENABLED = false;

    private final Map<String, PerfEntry> m_entries = new LinkedHashMap<>();

    private boolean m_enabled = DEFAULT_ENABLED;

    public boolean isEnabled() {
        return m_enabled;
    }

    public void setEnabled(boolean enabled) {
        m_enabled = enabled;
    }

    public void start(String key) {
        if (!m_enabled) {
            return;
        }
        var entry = m_entries.computeIfAbsent(key, k -> new PerfEntry());
        entry.m_startTime = System.nanoTime();
    }

    public void end(String key) {
        if (!m_enabled) {
            return;
        }
        var entry = m_entries.get(key);
        if (entry == null || entry.m_startTime == 0) {
            return;
        }
        long elapsed = System.nanoTime() - entry.m_startTime;
        entry.m_totalNanos += elapsed;
        entry.m_callCount++;
        entry.m_startTime = 0;
    }

    public void report() {
        if (!m_enabled || m_entries.isEmpty()) {
            return;
        }

        long grandTotal = 0;
        int maxKeyLen = 0;
        for (var e : m_entries.entrySet()) {
            maxKeyLen = Math.max(maxKeyLen, e.getKey().length());
            grandTotal += e.getValue().m_totalNanos;
        }

        for (var e : m_entries.entrySet()) {
            var key = e.getKey();
            var entry = e.getValue();
            double totalMs = entry.m_totalNanos / NANOS_PER_MS;
            double avgMs = entry.m_callCount > 0
                ? totalMs / entry.m_callCount
                : 0;
            IO.println(
                String.format(
                    "[PerfClock] %-" + maxKeyLen + "s  calls=%-4d avg=%.2fms"
                        + "  total=%.2fms",
                    key,
                    entry.m_callCount,
                    avgMs,
                    totalMs
                )
            );
        }

        IO.println(String.format("[PerfClock] TURN TOTAL: %.2fms", grandTotal / NANOS_PER_MS));

        m_entries.clear();
    }
}
