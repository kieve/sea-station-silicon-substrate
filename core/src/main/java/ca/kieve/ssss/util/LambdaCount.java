package ca.kieve.ssss.util;

/**
 * A mutable integer counter intended for use inside lambdas. Java
 * lambdas can only close over effectively-final locals, so a counter
 * has to live behind a reference. This is the obvious-name wrapper
 * around that pattern — no {@code int[1]} hacks and no atomic-
 * synchronisation overhead from {@link java.util.concurrent.atomic.AtomicInteger}.
 *
 * <p>Not thread-safe.
 */
public final class LambdaCount {
    private int m_value;

    public LambdaCount() {
        this(0);
    }

    public LambdaCount(int initial) {
        m_value = initial;
    }

    public int get() {
        return m_value;
    }

    public void increment() {
        m_value++;
    }

    public int incrementAndGet() {
        return ++m_value;
    }

    public int getAndIncrement() {
        return m_value++;
    }

    public void add(int delta) {
        m_value += delta;
    }
}
