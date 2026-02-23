package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityDefinition;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.ui.CompactTreeTable;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentPanel
        extends CompactTreeTable<ComponentPanel.ComponentRow> {

    enum OverrideStatus { NONE, REPLACED, ADDED }

    record ComponentRow(
            String name,
            String value,
            OverrideStatus status,
            String baseValue
    ) {
        ComponentRow(String name, String value) {
            this(name, value, OverrideStatus.NONE, null);
        }
    }

    private static final String STYLE_COMPONENT_PANEL =
            "component-panel";
    private static final String STYLE_ROW_REPLACED =
            "component-row-replaced";
    private static final String STYLE_ROW_ADDED =
            "component-row-added";
    private static final int INDICATOR_WIDTH = 3;

    // language=css
    private static final String ROW_CSS = """
            .%1$s .tree-table-row-cell {
                -fx-padding: 0 0 0 %4$d;
            }
            .%1$s .%2$s {
                -fx-border-color: -color-danger-fg;
                -fx-border-width: 0 0 0 %4$d;
                -fx-padding: 0;
            }
            .%1$s .%3$s {
                -fx-border-color: -color-success-fg;
                -fx-border-width: 0 0 0 %4$d;
                -fx-padding: 0;
            }
            """.formatted(
            STYLE_COMPONENT_PANEL,
            STYLE_ROW_REPLACED,
            STYLE_ROW_ADDED,
            INDICATOR_WIDTH);

    private final ContentRegistry m_registry;

    public ComponentPanel() {
        super(ComponentRow::name, ComponentRow::value);
        m_registry =
                EditorContext.getInstance().getRegistry();
        getStyleClass().add(STYLE_COMPONENT_PANEL);
        getStylesheets().add(inline(ROW_CSS));
        setupRowFactory();
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

        setRoot(null);
        setRoot(root);
    }

    public void showMapEntity(
            String baseEntityId,
            List<ComponentDefinition> overrides) {
        if (baseEntityId == null
                || !m_registry.hasEntity(baseEntityId)) {
            clear();
            return;
        }

        EntityDefinition def =
                m_registry.getEntityDefinition(baseEntityId);
        List<ComponentDefinition> baseComponents =
                def.resolveComponents(m_registry);

        Map<String, ComponentDefinition> baseMap =
                new HashMap<>();
        for (ComponentDefinition comp : baseComponents) {
            baseMap.put(
                    comp.type().getSimpleName(), comp);
        }

        Map<String, ComponentDefinition> overrideMap =
                new HashMap<>();
        for (ComponentDefinition comp : overrides) {
            overrideMap.put(
                    comp.type().getSimpleName(), comp);
        }

        // Merge: base + overrides
        Map<String, ComponentDefinition> merged =
                new HashMap<>(baseMap);
        merged.putAll(overrideMap);

        // Determine status per component
        Map<String, OverrideStatus> statusMap =
                new HashMap<>();
        for (String typeName : merged.keySet()) {
            if (overrideMap.containsKey(typeName)) {
                if (baseMap.containsKey(typeName)) {
                    statusMap.put(typeName,
                            OverrideStatus.REPLACED);
                } else {
                    statusMap.put(typeName,
                            OverrideStatus.ADDED);
                }
            } else {
                statusMap.put(typeName,
                        OverrideStatus.NONE);
            }
        }

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

        for (var entry : merged.entrySet()) {
            String typeName = entry.getKey();
            ComponentDefinition comp = entry.getValue();
            OverrideStatus status = statusMap.get(typeName);
            ComponentDefinition baseComp =
                    baseMap.get(typeName);
            root.getChildren().add(
                    buildComponentItem(
                            comp, status, baseComp));
        }

        setRoot(null);
        setRoot(root);
    }

    public void clear() {
        var emptyRoot = new TreeItem<>(
                new ComponentRow("", ""));
        emptyRoot.getChildren().add(new TreeItem<>(
                new ComponentRow("Nothing selected", "")));
        setRoot(null);
        setRoot(emptyRoot);
    }

    private void setupRowFactory() {
        setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected void updateItem(
                    ComponentRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        STYLE_ROW_REPLACED,
                        STYLE_ROW_ADDED);
                setTooltip(null);

                if (empty || item == null) {
                    return;
                }

                switch (item.status()) {
                    case REPLACED ->
                        getStyleClass()
                                .add(STYLE_ROW_REPLACED);
                    case ADDED ->
                        getStyleClass()
                                .add(STYLE_ROW_ADDED);
                    default -> {}
                }

                if (item.baseValue() != null) {
                    setTooltip(new Tooltip(
                            "Base: " + item.baseValue()));
                }
            }
        });
    }

    private TreeItem<ComponentRow> buildComponentItem(
            ComponentDefinition comp) {
        return buildComponentItem(
                comp, OverrideStatus.NONE, null);
    }

    private TreeItem<ComponentRow> buildComponentItem(
            ComponentDefinition comp,
            OverrideStatus status,
            ComponentDefinition baseComp) {
        String name = comp.type().getSimpleName();
        var props = comp.properties();

        if (props.isEmpty()) {
            return new TreeItem<>(new ComponentRow(
                    name, "(marker)", status, null));
        }

        var item = new TreeItem<>(
                new ComponentRow(name, "", status, null));

        Map<String, Object> baseProps =
                baseComp != null
                        ? baseComp.properties()
                        : Map.of();

        for (var entry : props.entrySet()) {
            String propName = entry.getKey();
            String propValue =
                    String.valueOf(entry.getValue());

            String baseValue = null;
            if (status == OverrideStatus.REPLACED
                    && baseProps.containsKey(propName)) {
                String basePropValue = String.valueOf(
                        baseProps.get(propName));
                if (!basePropValue.equals(propValue)) {
                    baseValue = basePropValue;
                }
            }

            item.getChildren().add(new TreeItem<>(
                    new ComponentRow(
                            propName,
                            propValue,
                            status,
                            baseValue)));
        }
        item.setExpanded(false);
        return item;
    }
}
