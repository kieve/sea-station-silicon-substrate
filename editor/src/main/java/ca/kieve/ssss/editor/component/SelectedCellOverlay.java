package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.editor.EditorTheme;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class SelectedCellOverlay extends VBox {
    public enum ItemType { BLOCK, ENTITY }

    public record CellItem(
            String label, ItemType type, int index) {}

    public record EntityInfo(int index, String id) {}

    // language=css
    private static final String CSS =
            EditorTheme.OVERLAY_CSS + """
            .%1$s .list-cell {
                -fx-cell-size: 1.4em;
                -fx-padding: 1 4;
                -fx-cursor: hand;
            }
            .%1$s .list-cell:hover {
                -fx-background-color: -color-neutral-muted;
            }
            """.formatted(EditorTheme.STYLE_OVERLAY);

    private final Label m_header;
    private final ListView<CellItem> m_list;
    private Consumer<CellItem> m_onItemSelected;

    public SelectedCellOverlay() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(EditorTheme.STYLE_OVERLAY);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);
        setSpacing(2);

        m_header = new Label("No selection");
        m_header.setStyle("-fx-font-weight: bold;");

        m_list = new ListView<>();
        m_list.setFocusTraversable(false);
        m_list.setMaxHeight(150);
        m_list.setPrefHeight(USE_COMPUTED_SIZE);
        m_list.setCellFactory(lv -> new CellItemCell());
        VBox.setVgrow(m_list, Priority.NEVER);

        m_list.setOnMouseClicked(e -> {
            CellItem item = m_list.getSelectionModel()
                    .getSelectedItem();
            if (item != null
                    && m_onItemSelected != null) {
                m_onItemSelected.accept(item);
            }
        });

        getChildren().addAll(m_header, m_list);
        setVisible(false);
        setManaged(false);
    }

    public void setOnItemSelected(
            Consumer<CellItem> callback) {
        m_onItemSelected = callback;
    }

    public void update(
            String blockName,
            List<EntityInfo> entities) {
        setVisible(true);
        setManaged(true);

        m_list.getItems().clear();

        if (blockName != null) {
            m_list.getItems().add(new CellItem(
                    blockName, ItemType.BLOCK, 0));
        }

        for (var entity : entities) {
            m_list.getItems().add(new CellItem(
                    entity.index() + ": " + entity.id(),
                    ItemType.ENTITY,
                    entity.index()));
        }

        if (m_list.getItems().isEmpty()) {
            m_header.setText("Cell: (empty)");
        }
    }

    public void selectEntity(int entityIndex) {
        for (int i = 0; i < m_list.getItems().size(); i++) {
            var item = m_list.getItems().get(i);
            if (item.type() == ItemType.ENTITY
                    && item.index() == entityIndex) {
                m_list.getSelectionModel().select(i);
                return;
            }
        }
    }

    public void setHeaderText(String text) {
        m_header.setText(text);
    }

    public void clear() {
        m_header.setText("No selection");
        m_list.getItems().clear();
        setVisible(false);
        setManaged(false);
    }

    private static class CellItemCell
            extends ListCell<CellItem> {
        @Override
        protected void updateItem(
                CellItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            String prefix = item.type() == ItemType.BLOCK
                    ? "[B] " : "[E] ";
            setText(prefix + item.label());
        }
    }
}
