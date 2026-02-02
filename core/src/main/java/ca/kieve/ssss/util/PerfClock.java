package ca.kieve.ssss.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class PerfClock {
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
        entry.startTime = java.lang.System.nanoTime();
    }

    public void end(String key) {
        if (!m_enabled) {
            return;
        }
        var entry = m_entries.get(key);
        if (entry == null || entry.startTime == 0) {
            return;
        }
        long elapsed = java.lang.System.nanoTime() - entry.startTime;
        entry.totalNanos += elapsed;
        entry.callCount++;
        entry.startTime = 0;
    }

    public void report() {
        if (!m_enabled || m_entries.isEmpty()) {
            return;
        }

        long grandTotal = 0;
        int maxKeyLen = 0;
        for (var e : m_entries.entrySet()) {
            maxKeyLen = Math.max(maxKeyLen, e.getKey().length());
            grandTotal += e.getValue().totalNanos;
        }

        for (var e : m_entries.entrySet()) {
            var key = e.getKey();
            var entry = e.getValue();
            double totalMs = entry.totalNanos / NANOS_PER_MS;
            double avgMs = entry.callCount > 0
                ? totalMs / entry.callCount
                : 0;
            IO.println(String.format(
                "[PerfClock] %-" + maxKeyLen + "s  calls=%-4d avg=%.2fms"
                    + "  total=%.2fms",
                key,
                entry.callCount,
                avgMs,
                totalMs));
        }

        IO.println(String.format(
            "[PerfClock] TURN TOTAL: %.2fms",
            grandTotal / NANOS_PER_MS));

        m_entries.clear();
    }

    private static class PerfEntry {
        long startTime;
        long totalNanos;
        int callCount;
    }
}
