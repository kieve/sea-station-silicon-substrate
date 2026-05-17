package ca.kieve.ssss.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class DebugContext {
    public record Toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
    }

    private final List<Toggle> m_toggles;

    private boolean m_debugGrid = false;
    private int m_selectedIndex = 0;

    public DebugContext() {
        var toggles = new ArrayList<Toggle>();
        toggles.add(new Toggle("Debug Grid", () -> m_debugGrid, v -> m_debugGrid = v));
        m_toggles = Collections.unmodifiableList(toggles);
    }

    public boolean isDebugGrid() {
        return m_debugGrid;
    }

    public int getSelectedIndex() {
        return m_selectedIndex;
    }

    public void incrementSelectedIndex() {
        if (m_toggles.isEmpty()) {
            return;
        }
        m_selectedIndex = (m_selectedIndex + 1) % m_toggles.size();
    }

    public void decrementSelectedIndex() {
        if (m_toggles.isEmpty()) {
            return;
        }
        m_selectedIndex = (m_selectedIndex - 1 + m_toggles.size()) % m_toggles.size();
    }

    public List<Toggle> getToggles() {
        return m_toggles;
    }

    public void toggleSelected() {
        if (m_toggles.isEmpty()) {
            return;
        }
        var toggle = m_toggles.get(m_selectedIndex);
        toggle.setter().accept(!toggle.getter().getAsBoolean());
    }
}
