package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityDefinition;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.ui.CompactTreeTable;
import javafx.scene.control.TreeItem;

import java.util.List;

public class ComponentPanel
        extends CompactTreeTable<ComponentPanel.ComponentRow> {
    record ComponentRow(String name, String value) {}

    private final ContentRegistry m_registry;

    public ComponentPanel() {
        super(ComponentRow::name, ComponentRow::value);
        m_registry =
                EditorContext.getInstance().getRegistry();
        clear();
    }

    public void showEntity(String entityId) {
        if (entityId == null
                || !m_registry.hasEntity(entityId)) {
            clear();
            return;
        }

        EntityDefinition def =
                m_registry.getEntityDefinition(entityId);

        var root = new TreeItem<>(
                new ComponentRow("", ""));

        if (!def.parents().isEmpty()) {
            var parentsItem = new TreeItem<>(
                    new ComponentRow("Parents", ""));
            for (String parent : def.parents()) {
                parentsItem.getChildren().add(
                        new TreeItem<>(
                                new ComponentRow(
                                        "", parent)));
            }
            parentsItem.setExpanded(false);
            root.getChildren().add(parentsItem);
        }

        List<ComponentDefinition> resolved =
                def.resolveComponents(m_registry);
        for (ComponentDefinition comp : resolved) {
            root.getChildren().add(
                    buildComponentItem(comp));
        }

        setRoot(root);
    }

    public void clear() {
        var emptyRoot = new TreeItem<>(
                new ComponentRow("", ""));
        emptyRoot.getChildren().add(new TreeItem<>(
                new ComponentRow("Nothing selected", "")));
        setRoot(emptyRoot);
    }

    private TreeItem<ComponentRow> buildComponentItem(
            ComponentDefinition comp) {
        String name = comp.type().getSimpleName();
        var props = comp.properties();

        if (props.isEmpty()) {
            return new TreeItem<>(
                    new ComponentRow(name, "(marker)"));
        }

        var item = new TreeItem<>(
                new ComponentRow(name, ""));
        for (var entry : props.entrySet()) {
            item.getChildren().add(new TreeItem<>(
                    new ComponentRow(
                            entry.getKey(),
                            String.valueOf(
                                    entry.getValue()))));
        }
        item.setExpanded(false);
        return item;
    }
}
