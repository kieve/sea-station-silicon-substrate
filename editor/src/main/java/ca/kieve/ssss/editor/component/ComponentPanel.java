package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.EntityDefinition;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.component.ComponentIntrospector.FieldInfo;
import ca.kieve.ssss.editor.ui.CompactTreeTable;
import ca.kieve.ssss.editor.ui.fx.EditorButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ComponentPanel
        extends CompactTreeTable<ComponentPanel.ComponentRow> {

    enum OverrideStatus { NONE, REPLACED, ADDED }

    record ComponentRow(
            String name,
            String value,
            OverrideStatus status,
            String baseValue,
            String componentType,
            String propertyName,
            boolean editable,
            Object rawValue,
            Class<?> fieldType
    ) {
        ComponentRow(String name, String value) {
            this(name, value, OverrideStatus.NONE,
                    null, null, null, false, null, null);
        }
    }

    @FunctionalInterface
    public interface PropertyEditCallback {
        void onPropertyEdited(
                String componentTypeName,
                String propertyName,
                String newValue);
    }

    public interface ComponentOverrideCallback {
        void onOverrideAdded(String componentTypeName);
        void onOverrideRemoved(String componentTypeName);
        void onPropertyReverted(String componentTypeName, String propertyName);
    }

    private static final String STYLE_COMPONENT_PANEL = "component-panel";
    private static final String STYLE_ROW_REPLACED = "component-row-replaced";
    private static final String STYLE_ROW_ADDED = "component-row-added";
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
    private PropertyEditCallback m_onPropertyEdited;
    private ComponentOverrideCallback m_onComponentOverride;
    private EditableValueCell m_activeEditCell;
    private String m_baseEntityId;

    public ComponentPanel() {
        super(ComponentRow::name, ComponentRow::value);
        m_registry = EditorContext.getInstance().getRegistry();
        getStyleClass().add(STYLE_COMPONENT_PANEL);
        getStylesheets().add(inline(ROW_CSS));
        setupRowFactory();
        setupEditableCells();
        clear();
    }

    public void setOnPropertyEdited(PropertyEditCallback callback) {
        m_onPropertyEdited = callback;
    }

    public void setOnComponentOverride(ComponentOverrideCallback callback) {
        m_onComponentOverride = callback;
    }

    public String getBaseEntityId() {
        return m_baseEntityId;
    }

    public void showEntity(String entityId) {
        commitPendingEdit();
        m_baseEntityId = null;
        if (entityId == null || !m_registry.hasEntity(entityId)) {
            clear();
            return;
        }

        EntityDefinition def = m_registry.getEntityDefinition(entityId);
        var root = new TreeItem<>(new ComponentRow("", ""));

        if (!def.parents().isEmpty()) {
            var parentsItem = new TreeItem<>(new ComponentRow("Parents", ""));
            for (String parent : def.parents()) {
                parentsItem.getChildren().add(
                        new TreeItem<>(new ComponentRow("", parent)));
            }
            parentsItem.setExpanded(false);
            root.getChildren().add(parentsItem);
        }

        List<ComponentDefinition> resolved = def.resolveComponents(m_registry);
        for (ComponentDefinition comp : resolved) {
            root.getChildren().add(buildComponentItem(comp, false));
        }

        setRoot(null);
        setRoot(root);
    }

    public void showMapEntity(
            String baseEntityId, List<ComponentDefinition> overrides) {
        commitPendingEdit();
        m_baseEntityId = baseEntityId;
        if (baseEntityId == null || !m_registry.hasEntity(baseEntityId)) {
            clear();
            return;
        }

        Set<String> expanded = getExpandedNames();

        EntityDefinition def = m_registry.getEntityDefinition(baseEntityId);
        List<ComponentDefinition> baseComponents =
                def.resolveComponents(m_registry);

        Map<String, ComponentDefinition> baseMap = new HashMap<>();
        for (ComponentDefinition comp : baseComponents) {
            baseMap.put(comp.type().getSimpleName(), comp);
        }

        Map<String, ComponentDefinition> overrideMap = new HashMap<>();
        for (ComponentDefinition comp : overrides) {
            overrideMap.put(comp.type().getSimpleName(), comp);
        }

        // Merge: base + overrides
        Map<String, ComponentDefinition> merged = new HashMap<>(baseMap);
        merged.putAll(overrideMap);

        // Determine status per component
        Map<String, OverrideStatus> statusMap = new HashMap<>();
        for (String typeName : merged.keySet()) {
            if (overrideMap.containsKey(typeName)) {
                if (baseMap.containsKey(typeName)) {
                    statusMap.put(typeName, OverrideStatus.REPLACED);
                } else {
                    statusMap.put(typeName, OverrideStatus.ADDED);
                }
            } else {
                statusMap.put(typeName, OverrideStatus.NONE);
            }
        }

        var root = new TreeItem<>(new ComponentRow("", ""));

        if (!def.parents().isEmpty()) {
            var parentsItem = new TreeItem<>(new ComponentRow("Parents", ""));
            for (String parent : def.parents()) {
                parentsItem.getChildren().add(
                        new TreeItem<>(new ComponentRow("", parent)));
            }
            parentsItem.setExpanded(expanded.contains("Parents"));
            root.getChildren().add(parentsItem);
        }

        for (var entry : merged.entrySet()) {
            String typeName = entry.getKey();
            ComponentDefinition comp = entry.getValue();
            OverrideStatus status = statusMap.get(typeName);
            ComponentDefinition baseComp = baseMap.get(typeName);
            var item = buildComponentItem(comp, status, baseComp, true);
            if (expanded.contains(typeName)) {
                item.setExpanded(true);
            }
            root.getChildren().add(item);
        }

        setRoot(null);
        setRoot(root);
    }

    public void clear() {
        commitPendingEdit();
        m_baseEntityId = null;
        var emptyRoot = new TreeItem<>(new ComponentRow("", ""));
        emptyRoot.getChildren().add(
                new TreeItem<>(new ComponentRow("Nothing selected", "")));
        setRoot(null);
        setRoot(emptyRoot);
    }

    private void setupRowFactory() {
        setRowFactory(tv -> new TreeTableRow<>() {
            @Override
            protected void updateItem(ComponentRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(STYLE_ROW_REPLACED, STYLE_ROW_ADDED);
                setTooltip(null);
                setContextMenu(null);

                if (empty || item == null) {
                    return;
                }

                switch (item.status()) {
                    case REPLACED ->
                            getStyleClass().add(STYLE_ROW_REPLACED);
                    case ADDED ->
                            getStyleClass().add(STYLE_ROW_ADDED);
                    default -> {}
                }

                if (item.baseValue() != null) {
                    setTooltip(new Tooltip("Base: " + item.baseValue()));
                }

                setContextMenu(buildContextMenu(item));
            }
        });
    }

    private ContextMenu buildContextMenu(ComponentRow item) {
        if (m_onComponentOverride == null) {
            return null;
        }

        boolean isHeader = item.propertyName() == null;

        if (isHeader) {
            // Component header row
            if (item.status() == OverrideStatus.ADDED) {
                var deleteItem = new MenuItem("Delete");
                deleteItem.setOnAction(e ->
                        m_onComponentOverride.onOverrideRemoved(
                                item.componentType()));
                return new ContextMenu(deleteItem);
            }
            if (item.status() == OverrideStatus.REPLACED) {
                var revertItem = new MenuItem("Revert");
                revertItem.setOnAction(e ->
                        m_onComponentOverride.onOverrideRemoved(
                                item.componentType()));
                return new ContextMenu(revertItem);
            }
            return null;
        }

        // Property row — only on editable map entities
        if (!item.editable()) {
            return null;
        }
        if (item.status() == OverrideStatus.REPLACED
                && item.baseValue() != null) {
            var revertItem = new MenuItem("Revert");
            revertItem.setOnAction(e ->
                    m_onComponentOverride.onPropertyReverted(
                            item.componentType(), item.propertyName()));
            return new ContextMenu(revertItem);
        }
        return null;
    }

    private void setupEditableCells() {
        getRightColumn().setCellFactory(col -> new EditableValueCell());
    }

    public void commitPendingEdit() {
        if (m_activeEditCell != null) {
            m_activeEditCell.commitFromExternal();
        }
    }

    private Set<String> getExpandedNames() {
        Set<String> expanded = new HashSet<>();
        var root = getRoot();
        if (root == null) {
            return expanded;
        }
        for (var child : root.getChildren()) {
            if (child.isExpanded()) {
                var row = child.getValue();
                if (row != null && row.name() != null && !row.name().isEmpty()) {
                    expanded.add(row.name());
                }
            }
        }
        return expanded;
    }

    private TreeItem<ComponentRow> buildComponentItem(
            ComponentDefinition comp, boolean editable) {
        return buildComponentItem(comp, OverrideStatus.NONE, null, editable);
    }

    private TreeItem<ComponentRow> buildComponentItem(
            ComponentDefinition comp,
            OverrideStatus status,
            ComponentDefinition baseComp,
            boolean editable) {
        String name = comp.type().getSimpleName();
        Class<?> compClass = comp.type();
        var props = comp.properties();

        List<FieldInfo> fields =
                ComponentIntrospector.getEditableFields(compClass);
        boolean isEnum = compClass.isEnum();

        if (fields.isEmpty() && !isEnum) {
            return new TreeItem<>(new ComponentRow(
                    name, "(marker)", status, null,
                    name, null, false, null, null));
        }

        var item = new TreeItem<>(new ComponentRow(
                name, "", status, null,
                name, null, false, null, null));

        Map<String, Object> baseProps =
                baseComp != null ? baseComp.properties() : Map.of();

        if (isEnum) {
            // Enum components: show YAML properties as-is
            for (var entry : props.entrySet()) {
                String propName = entry.getKey();
                Object rawValue = entry.getValue();
                String propValue = String.valueOf(rawValue);

                String baseValue = null;
                if (status == OverrideStatus.REPLACED
                        && baseProps.containsKey(propName)) {
                    String basePropValue =
                            String.valueOf(baseProps.get(propName));
                    if (!basePropValue.equals(propValue)) {
                        baseValue = basePropValue;
                    }
                }

                item.getChildren().add(new TreeItem<>(new ComponentRow(
                        propName, propValue, status, baseValue,
                        name, propName, editable, rawValue, null)));
            }
        } else {
            // Regular components: iterate introspected fields
            for (FieldInfo field : fields) {
                Object rawValue = props.get(field.name());
                String propValue = rawValue != null
                        ? String.valueOf(rawValue) : "";

                String baseValue = null;
                if (status == OverrideStatus.REPLACED
                        && baseProps.containsKey(field.name())) {
                    String basePropValue =
                            String.valueOf(baseProps.get(field.name()));
                    if (!basePropValue.equals(propValue)) {
                        baseValue = basePropValue;
                    }
                }

                item.getChildren().add(new TreeItem<>(new ComponentRow(
                        field.name(), propValue, status, baseValue,
                        name, field.name(), editable,
                        rawValue, field.type())));
            }
        }
        item.setExpanded(false);
        return item;
    }

    private class EditableValueCell
            extends TreeTableCell<ComponentRow, String> {
        private TextField m_textField;
        private boolean m_editing;
        private ComponentRow m_editRowData;

        EditableValueCell() {
            setOnMouseClicked(e -> {
                if (m_editing) {
                    return;
                }
                var row = getTreeTableRow();
                if (row == null) {
                    return;
                }
                var treeItem = row.getTreeItem();
                if (treeItem == null) {
                    return;
                }
                ComponentRow rowData = treeItem.getValue();
                if (rowData == null
                        || !rowData.editable()
                        || rowData.propertyName() == null) {
                    return;
                }
                enterEditMode(rowData);
                e.consume();
            });
        }

        void commitFromExternal() {
            if (!m_editing || m_editRowData == null) {
                return;
            }
            commitEdit(m_editRowData);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (m_editing) {
                return;
            }
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(item);
            setGraphic(null);
        }

        private void enterEditMode(ComponentRow rowData) {
            m_editing = true;
            m_editRowData = rowData;
            m_activeEditCell = this;

            String currentValue = rowData.value();
            m_textField = new TextField(currentValue);
            m_textField.setMaxHeight(18);
            m_textField.setPrefHeight(18);
            m_textField.setStyle("-fx-font-size: 11;");

            applyTypeFilter(m_textField, rowData.rawValue(), rowData.fieldType());

            var confirmBtn = new EditorButton("\u2713");
            confirmBtn.setStyle("-fx-font-size: 9; -fx-padding: 0 3;");
            confirmBtn.setFocusTraversable(false);
            confirmBtn.setOnAction(e -> commitEdit(rowData));

            var cancelBtn = new EditorButton("\u2717");
            cancelBtn.setStyle("-fx-font-size: 9; -fx-padding: 0 3;");
            cancelBtn.setFocusTraversable(false);
            // Prevent focus transfer so the blur-commit listener doesn't fire
            cancelBtn.addEventFilter(
                    javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
                        exitEditMode(currentValue);
                        e.consume();
                    });

            m_textField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    commitEdit(rowData);
                    e.consume();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    exitEditMode(currentValue);
                    e.consume();
                }
            });

            m_textField.focusedProperty().addListener(
                    (obs, wasFocused, isFocused) -> {
                if (!isFocused && m_editing) {
                    commitEdit(rowData);
                }
            });

            var editBox = new HBox(2, m_textField, confirmBtn, cancelBtn);
            editBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(m_textField, Priority.ALWAYS);

            setText(null);
            setGraphic(editBox);
            m_textField.requestFocus();
            m_textField.selectAll();
        }

        private void commitEdit(ComponentRow rowData) {
            String newValue = m_textField.getText().trim();
            m_editing = false;
            m_editRowData = null;
            if (m_activeEditCell == this) {
                m_activeEditCell = null;
            }
            setText(newValue);
            setGraphic(null);

            if (!newValue.equals(rowData.value()) && m_onPropertyEdited != null) {
                m_onPropertyEdited.onPropertyEdited(
                        rowData.componentType(), rowData.propertyName(),
                        newValue);
            }
        }

        private void exitEditMode(String original) {
            m_editing = false;
            m_editRowData = null;
            if (m_activeEditCell == this) {
                m_activeEditCell = null;
            }
            setText(original);
            setGraphic(null);
        }

        private void applyTypeFilter(
                TextField field, Object rawValue, Class<?> fieldType) {
            if (isIntegerType(rawValue, fieldType)) {
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (newText.isEmpty() || newText.equals("-")) {
                        return change;
                    }
                    try {
                        Long.parseLong(newText);
                        return change;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                };
                field.setTextFormatter(new TextFormatter<>(filter));
            } else if (isFloatType(rawValue, fieldType)) {
                UnaryOperator<TextFormatter.Change> filter = change -> {
                    String newText = change.getControlNewText();
                    if (newText.isEmpty()
                            || newText.equals("-")
                            || newText.equals(".")) {
                        return change;
                    }
                    try {
                        Double.parseDouble(newText);
                        return change;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                };
                field.setTextFormatter(new TextFormatter<>(filter));
            }
        }

        private static boolean isIntegerType(Object rawValue, Class<?> fieldType) {
            if (rawValue instanceof Integer || rawValue instanceof Long) {
                return true;
            }
            return fieldType == int.class
                    || fieldType == long.class
                    || fieldType == Integer.class
                    || fieldType == Long.class;
        }

        private static boolean isFloatType(Object rawValue, Class<?> fieldType) {
            if (rawValue instanceof Double || rawValue instanceof Float) {
                return true;
            }
            return fieldType == double.class
                    || fieldType == float.class
                    || fieldType == Double.class
                    || fieldType == Float.class;
        }
    }
}
