package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.GlyphDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.model.EditorMapModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class BlockPanel extends VBox {
    private final EditorMapModel m_model;
    private final BlockColorResolver m_colorResolver;
    private final ListView<String> m_blockList;
    private final List<String> m_blockTypes;
    private final Map<String, GlyphDefinition> m_glyphs;
    private Consumer<String> m_onSelectionChanged;

    public BlockPanel(
            EditorMapModel model,
            ContentRegistry registry,
            BlockColorResolver colorResolver
    ) {
        m_model = model;
        m_colorResolver = colorResolver;
        m_blockTypes = buildBlockTypeList(registry);
        m_glyphs = buildGlyphMap(registry);

        getStyleClass().add(EditorTheme.STYLE_BLOCK_PANEL);
        setPrefWidth(200);
        setMinWidth(160);

        var titleLabel = new Label("Blocks");
        titleLabel.getStyleClass().add(
                EditorTheme.STYLE_TOOLBAR_LABEL_BOLD);

        m_blockList = new ListView<>();
        m_blockList.setCellFactory(lv -> new BlockListCell());
        m_blockList.setFocusTraversable(false);
        VBox.setVgrow(m_blockList, Priority.ALWAYS);

        m_blockList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
            if (m_onSelectionChanged != null && newVal != null) {
                m_onSelectionChanged.accept(newVal);
            }
        });

        var addBtn = new Button("Add");
        addBtn.setFocusTraversable(false);
        addBtn.setOnAction(e -> onAdd());

        var editBtn = new Button("Edit");
        editBtn.setFocusTraversable(false);
        editBtn.setOnAction(e -> onEdit());

        var removeBtn = new Button("Remove");
        removeBtn.setFocusTraversable(false);
        removeBtn.setOnAction(e -> onRemove());

        var buttonBar = new HBox(4, addBtn, editBtn, removeBtn);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(titleLabel, m_blockList, buttonBar);

        refreshList();
    }

    public void setOnSelectionChanged(
            Consumer<String> callback) {
        m_onSelectionChanged = callback;
    }

    public void selectBlock(String name) {
        m_blockList.getSelectionModel().select(name);
    }

    public String getSelectedBlock() {
        return m_blockList.getSelectionModel().getSelectedItem();
    }

    private void refreshList() {
        String selected = getSelectedBlock();
        m_blockList.getItems().setAll(
                m_model.getBlocks().keySet().stream()
                        .sorted().toList());
        if (selected != null
                && m_blockList.getItems().contains(selected)) {
            m_blockList.getSelectionModel().select(selected);
        } else if (!m_blockList.getItems().isEmpty()) {
            m_blockList.getSelectionModel().selectFirst();
        }
    }

    private void onAdd() {
        BlockEditDialog.showAdd(m_blockTypes, m_glyphs)
                .ifPresent(result -> {
            m_model.addBlock(result.name(), result.blockDef());
            refreshList();
            m_blockList.getSelectionModel().select(result.name());
            fireSelectionChanged();
        });
    }

    private void onEdit() {
        String selected = getSelectedBlock();
        if (selected == null) {
            return;
        }
        MapBlockDefinition existing =
                m_model.getBlocks().get(selected);
        BlockEditDialog.showEdit(
                m_blockTypes, m_glyphs, selected, existing
        ).ifPresent(result -> {
            m_model.addBlock(result.name(), result.blockDef());
            refreshList();
            fireSelectionChanged();
        });
    }

    private void onRemove() {
        String selected = getSelectedBlock();
        if (selected == null) {
            return;
        }
        var alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Remove block definition '" + selected + "'?",
                ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                m_model.removeBlock(selected);
                refreshList();
                fireSelectionChanged();
            }
        });
    }

    private void fireSelectionChanged() {
        if (m_onSelectionChanged != null) {
            String sel = getSelectedBlock();
            if (sel != null) {
                m_onSelectionChanged.accept(sel);
            }
        }
    }

    private Map<String, GlyphDefinition> buildGlyphMap(
            ContentRegistry registry) {
        Map<String, GlyphDefinition> glyphs =
                new HashMap<>();
        for (String id : registry.getGlyphIds()) {
            glyphs.put(id,
                    registry.getGlyphDefinition(id));
        }
        return glyphs;
    }

    private List<String> buildBlockTypeList(
            ContentRegistry registry) {
        List<String> types = new ArrayList<>();
        types.add("air");
        for (String entityId : registry.getEntityIds()) {
            if (entityId.startsWith("block_")) {
                types.add(entityId.substring("block_".length()));
            }
        }
        types.sort(String::compareTo);
        return types;
    }

    private class BlockListCell extends ListCell<String> {
        @Override
        protected void updateItem(String name, boolean empty) {
            super.updateItem(name, empty);
            if (empty || name == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            MapBlockDefinition blockDef =
                    m_model.getBlocks().get(name);
            if (blockDef == null) {
                setText(name);
                setGraphic(null);
                return;
            }

            Color color =
                    m_colorResolver.resolve(blockDef.type());
            var swatch = new Rectangle(12, 12, color);
            swatch.setStroke(Color.gray(0.5));
            swatch.setStrokeWidth(0.5);

            var label = new Label(
                    name + " '" + blockDef.layoutChar() + "'");
            label.setPadding(new Insets(0, 0, 0, 6));

            var cell = new HBox(swatch, label);
            cell.setAlignment(Pos.CENTER_LEFT);
            setGraphic(cell);
            setText(null);
        }
    }
}
