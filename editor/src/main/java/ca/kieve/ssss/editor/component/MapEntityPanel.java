package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
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
    private Runnable m_onEntitiesChanged;

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

        var addBtn = new Button("Add");
        addBtn.setFocusTraversable(false);
        addBtn.setOnAction(e -> onAdd());

        var removeBtn = new Button("Remove");
        removeBtn.setFocusTraversable(false);
        removeBtn.setOnAction(e -> onRemove());

        var buttonBar = new HBox(4, addBtn, removeBtn);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(4, 0, 0, 0));

        getChildren().addAll(
                titleLabel, m_entityList, buttonBar);
        refreshList();
    }

    public void setOnSelectionChanged(
            Consumer<Integer> callback) {
        m_onSelectionChanged = callback;
    }

    public void setOnEntitiesChanged(Runnable callback) {
        m_onEntitiesChanged = callback;
    }

    public void selectEntity(int index) {
        m_entityList.getSelectionModel().select(index);
    }

    public void refreshList() {
        List<EditorEntity> entities =
                m_model.getEntities();
        m_entityList.getItems().clear();
        for (int i = 0; i < entities.size(); i++) {
            m_entityList.getItems().add(i);
        }
    }

    private void onAdd() {
        EntityAddDialog.showAdd().ifPresent(result -> {
            var entity = new EditorEntity(
                    result.entityId(), List.of());
            m_model.addEntity(entity);
            refreshList();
            int newIndex =
                    m_model.getEntities().size() - 1;
            m_entityList.getSelectionModel()
                    .select(newIndex);
            fireSelectionChanged();
            fireEntitiesChanged();
        });
    }

    private void onRemove() {
        Integer selected = m_entityList
                .getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        m_model.removeEntity(selected);
        refreshList();
        fireEntitiesChanged();
    }

    private void fireSelectionChanged() {
        if (m_onSelectionChanged != null) {
            Integer sel = m_entityList
                    .getSelectionModel().getSelectedItem();
            if (sel != null) {
                m_onSelectionChanged.accept(sel);
            }
        }
    }

    private void fireEntitiesChanged() {
        if (m_onEntitiesChanged != null) {
            m_onEntitiesChanged.run();
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

            List<EditorEntity> entities =
                    m_model.getEntities();
            if (index >= entities.size()) {
                setText(null);
                return;
            }

            EditorEntity entity = entities.get(index);
            setText(index + ": " + entity.id());
        }
    }
}
