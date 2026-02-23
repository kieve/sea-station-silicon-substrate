package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.editor.model.EditorMapModel;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class MapEntityPanel extends VBox {
    private static final String STYLE_ENTITY_PANEL =
            "editor-entity-panel";
    private static final String STYLE_TOOLBAR_LABEL_BOLD =
            "editor-toolbar-label-bold";

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
            """.formatted(
            STYLE_ENTITY_PANEL,
            STYLE_TOOLBAR_LABEL_BOLD);

    private final EditorMapModel m_model;
    private final ListView<Integer> m_entityList;
    private Consumer<Integer> m_onSelectionChanged;

    public MapEntityPanel(EditorMapModel model) {
        m_model = model;

        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_ENTITY_PANEL);
        setPrefWidth(200);
        setMinWidth(160);

        var titleLabel = new Label("Entities");
        titleLabel.getStyleClass()
                .add(STYLE_TOOLBAR_LABEL_BOLD);

        m_entityList = new ListView<>();
        m_entityList.setCellFactory(
                lv -> new EntityListCell());
        m_entityList.setFocusTraversable(false);
        VBox.setVgrow(m_entityList, Priority.ALWAYS);

        m_entityList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
            if (m_onSelectionChanged != null
                    && newVal != null) {
                m_onSelectionChanged.accept(newVal);
            }
        });

        getChildren().addAll(titleLabel, m_entityList);
        refreshList();
    }

    public void setOnSelectionChanged(
            Consumer<Integer> callback) {
        m_onSelectionChanged = callback;
    }

    private void refreshList() {
        List<MapEntityDefinition> entities =
                m_model.getEntities();
        m_entityList.getItems().clear();
        if (entities == null) {
            return;
        }
        for (int i = 0; i < entities.size(); i++) {
            m_entityList.getItems().add(i);
        }
    }

    private class EntityListCell
            extends ListCell<Integer> {
        @Override
        protected void updateItem(
                Integer index, boolean empty) {
            super.updateItem(index, empty);
            if (empty || index == null) {
                setText(null);
                return;
            }

            List<MapEntityDefinition> entities =
                    m_model.getEntities();
            if (entities == null
                    || index >= entities.size()) {
                setText(null);
                return;
            }

            MapEntityDefinition entity =
                    entities.get(index);
            setText(index + ": " + entity.id());
        }
    }
}
