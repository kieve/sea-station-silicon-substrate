package ca.kieve.ssss.editor.model;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EditorEntity {
    private String m_id;
    private final List<ComponentDefinition> m_components;

    public EditorEntity(
            String id,
            List<ComponentDefinition> components) {
        m_id = id;
        m_components = new ArrayList<>(components);
    }

    public static EditorEntity fromDefinition(
            MapEntityDefinition def) {
        return new EditorEntity(
                def.id(), def.components());
    }

    public MapEntityDefinition toDefinition() {
        return new MapEntityDefinition(
                m_id, List.copyOf(m_components));
    }

    public String id() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
    }

    public List<ComponentDefinition> components() {
        return Collections.unmodifiableList(m_components);
    }

    public ComponentDefinition getPositionComponent() {
        for (var comp : m_components) {
            if (comp.type() == Position.class) {
                return comp;
            }
        }
        return null;
    }

    public void setComponentOverride(
            ComponentDefinition comp) {
        String typeName = comp.type().getSimpleName();
        for (int i = 0; i < m_components.size(); i++) {
            if (m_components.get(i).type().getSimpleName()
                    .equals(typeName)) {
                m_components.set(i, comp);
                return;
            }
        }
        m_components.add(comp);
    }

    public void removeComponent(String typeName) {
        m_components.removeIf(comp ->
                comp.type().getSimpleName()
                        .equals(typeName));
    }

    public void movePosition(int x, int y, int z) {
        var posComp = getPositionComponent();
        if (posComp != null) {
            posComp.setProperty("x", x);
            posComp.setProperty("y", y);
            posComp.setProperty("z", z);
            return;
        }

        var newPos =
                new ComponentDefinition(Position.class);
        newPos.setProperty("x", x);
        newPos.setProperty("y", y);
        newPos.setProperty("z", z);
        m_components.add(newPos);
    }
}
