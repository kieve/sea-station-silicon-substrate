package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.BlockGlyphResolver;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.ui.fx.EditorButton;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class BlockPanel extends VBox {
    private class BlockListCell extends ListCell<String> {
        @Override
        protected void updateItem(String name, boolean empty) {
            super.updateItem(name, empty);
            if (empty || name == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            MapBlockDefinition blockDef = m_model.getBlocks().get(name);
            if (blockDef == null) {
                setText(name);
                setGraphic(null);
                return;
            }

            Color color = m_colorResolver.resolve(blockDef);
            var swatch = new Rectangle(12, 12, color);
            swatch.setStroke(Color.gray(0.5));
            swatch.setStrokeWidth(0.5);

            char glyph = m_glyphResolver.resolve(blockDef);

            var label = new EditorLabel(name + " (");
            label.setPadding(new Insets(0, 0, 0, 6));

            var glyphLabel = new EditorLabel(String.valueOf(glyph));
            glyphLabel.setTextFill(color);
            glyphLabel.setStyle("-fx-font-weight: bold;");

            var closeLabel = new EditorLabel(")");

            var cell = new HBox(swatch, label, glyphLabel, closeLabel);
            cell.setAlignment(Pos.CENTER_LEFT);
            setGraphic(cell);
            setText(null);
        }
    }

    private static final String STYLE_BLOCK_PANEL = "editor-block-panel";
    private static final String STYLE_TOOLBAR_LABEL_BOLD = "editor-toolbar-label-bold";

    // language=css
    private static final String CSS = """
        .%1$s {
            -fx-background-color: -color-bg-subtle;
            -fx-padding: 8;
            -fx-spacing: 4;
        }
        .%1$s .list-cell {
            -fx-cell-size: 1.5em;
            -fx-padding: 0.125em 0.583em;
        }
        .%2$s {
            -fx-font-weight: bold;
            -fx-padding: 0 8 0 4;
        }
        """.formatted(STYLE_BLOCK_PANEL, STYLE_TOOLBAR_LABEL_BOLD);

    private final EditorMapModel m_model;
    private final BlockColorResolver m_colorResolver;
    private final BlockGlyphResolver m_glyphResolver;
    private final ListView<String> m_blockList;
    private final List<String> m_blockTypes;

    private Consumer<String> m_onSelectionChanged;
    private Runnable m_onBlocksChanged;

    public BlockPanel(EditorMapModel model) {
        var ctx = EditorContext.getInstance();
        m_model = model;
        m_colorResolver = ctx.getColorResolver();
        m_glyphResolver = ctx.getGlyphResolver();
        m_blockTypes = buildBlockTypeList();

        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_BLOCK_PANEL);
        setPrefWidth(200);
        setMinWidth(160);

        var titleLabel = new EditorLabel("Blocks");
        titleLabel.getStyleClass().add(STYLE_TOOLBAR_LABEL_BOLD);

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

        var addBtn = new EditorButton("Add");
        addBtn.setFocusTraversable(false);
        addBtn.setOnAction(e -> onAdd());

        var editBtn = new EditorButton("Edit");
        editBtn.setFocusTraversable(false);
        editBtn.setOnAction(e -> onEdit());

        var removeBtn = new EditorButton("Remove");
        removeBtn.setFocusTraversable(false);
        removeBtn.setOnAction(e -> onRemove());

        var buttonBar = new HBox(4, addBtn, editBtn, removeBtn);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(titleLabel, m_blockList, buttonBar);

        refreshList();
    }

    public void setOnSelectionChanged(Consumer<String> callback) {
        m_onSelectionChanged = callback;
    }

    public void setOnBlocksChanged(Runnable callback) {
        m_onBlocksChanged = callback;
    }

    public void selectBlock(String name) {
        m_blockList.getSelectionModel().select(name);
    }

    public String getSelectedBlock() {
        return m_blockList.getSelectionModel().getSelectedItem();
    }

    private void refreshList() {
        String selected = getSelectedBlock();
        m_blockList.getItems().setAll(m_model.getBlocks().keySet().stream().sorted().toList());
        if (selected != null
            && m_blockList.getItems().contains(selected)) {
            m_blockList.getSelectionModel().select(selected);
        } else if (!m_blockList.getItems().isEmpty()) {
            m_blockList.getSelectionModel().selectFirst();
        }
    }

    private void onAdd() {
        BlockEditDialog.showAdd(m_blockTypes)
            .ifPresent(result -> {
                m_model.addBlock(result.name(), result.blockDef());
                refreshList();
                m_blockList.getSelectionModel().select(result.name());
                fireSelectionChanged();
                fireBlocksChanged();
            });
    }

    private void onEdit() {
        String selected = getSelectedBlock();
        if (selected == null) {
            return;
        }
        MapBlockDefinition existing = m_model.getBlocks().get(selected);
        BlockEditDialog.showEdit(m_blockTypes, selected, existing).ifPresent(result -> {
            m_model.addBlock(result.name(), result.blockDef());
            refreshList();
            fireSelectionChanged();
            fireBlocksChanged();
        });
    }

    private void onRemove() {
        String selected = getSelectedBlock();
        if (selected == null) {
            return;
        }

        if (!m_model.isBlockInUse(selected)) {
            m_model.removeBlock(selected);
            refreshList();
            fireSelectionChanged();
            fireBlocksChanged();
            return;
        }

        var replaceType = new ButtonType("Replace");
        var deleteType = new ButtonType("Delete");
        var ignoreType = new ButtonType("Ignore");

        var alert = new Alert(
            Alert.AlertType.WARNING,
            "Block '" + selected + "' is currently used in the map.",
            replaceType,
            deleteType,
            ignoreType,
            ButtonType.CANCEL
        );
        DialogUtil.style(alert, "Block In Use");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == replaceType) {
                onRemoveReplace(selected);
            } else if (btn == deleteType) {
                m_model.deleteBlockFromLayers(selected);
                m_model.removeBlock(selected);
                refreshList();
                fireSelectionChanged();
                fireBlocksChanged();
            } else if (btn == ignoreType) {
                m_model.removeBlock(selected);
                refreshList();
                fireSelectionChanged();
                fireBlocksChanged();
            }
        });
    }

    private void onRemoveReplace(String selected) {
        List<String> remaining = m_model.getBlocks().keySet().stream()
            .filter(n -> !n.equals(selected))
            .sorted()
            .toList();
        if (remaining.isEmpty()) {
            var err = new Alert(Alert.AlertType.WARNING, "No other blocks to replace with.");
            DialogUtil.style(err, "No Replacement Available");
            err.showAndWait();
            return;
        }

        var choice = new ChoiceDialog<>(remaining.getFirst(), remaining);
        DialogUtil.style(choice, "Replace Block");
        choice.setContentText("Replace '" + selected + "' with:");
        choice.showAndWait().ifPresent(replacement -> {
            m_model.replaceBlockInLayers(selected, replacement);
            m_model.removeBlock(selected);
            refreshList();
            fireSelectionChanged();
            fireBlocksChanged();
        });
    }

    private void fireSelectionChanged() {
        if (m_onSelectionChanged == null) {
            return;
        }
        String sel = getSelectedBlock();
        if (sel != null) {
            m_onSelectionChanged.accept(sel);
        }
    }

    private void fireBlocksChanged() {
        if (m_onBlocksChanged != null) {
            m_onBlocksChanged.run();
        }
    }

    private List<String> buildBlockTypeList() {
        var registry = EditorContext.getInstance().getRegistry();
        List<String> types = new ArrayList<>();
        types.add("air");
        for (String entityId : registry.getEntityIds()) {
            if (entityId.startsWith("block_")) {
                types.add(entityId);
            }
        }
        types.sort(String::compareTo);
        return types;
    }
}
