package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.ui.fx.EditorButton;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class EntityPanel extends VBox {
    private static final int PREF_WIDTH = 200;
    private static final int MIN_WIDTH = 160;
    private static final int BUTTON_SPACING = 4;
    private static final int BUTTON_BAR_TOP_PADDING = 4;
    private static final int INDICATOR_TOP = -4;
    private static final int INDICATOR_RIGHT = -10;
    private static final int INDICATOR_BOTTOM = -4;
    private static final int INDICATOR_LEFT = 4;

    private static final String STYLE_ENTITY_PANEL =
            "editor-entity-panel";
    private static final String STYLE_TOOLBAR_LABEL_BOLD =
            "editor-toolbar-label-bold";
    private static final String STYLE_NO_POSITION =
            "editor-entity-no-position";

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
            .%3$s {
                -fx-background-color: -color-danger-emphasis;
                -fx-background-radius: 0;
                -fx-min-width: 6;
                -fx-max-width: 6;
            }
            """.formatted(
            STYLE_ENTITY_PANEL,
            STYLE_TOOLBAR_LABEL_BOLD,
            STYLE_NO_POSITION);

    private final EditorMapModel m_model;
    private final ListView<Integer> m_entityList;
    private Consumer<Integer> m_onSelectionChanged;
    private Runnable m_onEntitiesChanged;

    public EntityPanel(EditorMapModel model) {
        m_model = model;

        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_ENTITY_PANEL);
        setPrefWidth(PREF_WIDTH);
        setMinWidth(MIN_WIDTH);

        var titleLabel = new EditorLabel("Entities");
        titleLabel.getStyleClass()
                .add(STYLE_TOOLBAR_LABEL_BOLD);

        m_entityList = new ListView<>();
        m_entityList.setCellFactory(
                lv -> new EntityListCell());
        m_entityList.setFocusTraversable(false);
        VBox.setVgrow(m_entityList, Priority.ALWAYS);

        m_entityList.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (m_onSelectionChanged != null
                                    && newVal != null) {
                                m_onSelectionChanged
                                        .accept(newVal);
                            }
                        });

        var addBtn = new EditorButton("Add");
        addBtn.setFocusTraversable(false);
        addBtn.setOnAction(e -> onAdd());

        var removeBtn = new EditorButton("Remove");
        removeBtn.setFocusTraversable(false);
        removeBtn.setOnAction(e -> onRemove());

        var buttonBar = new HBox(
                BUTTON_SPACING, addBtn, removeBtn);
        buttonBar.setAlignment(Pos.CENTER_LEFT);
        buttonBar.setPadding(new Insets(
                BUTTON_BAR_TOP_PADDING, 0, 0, 0));

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

    public void refreshCells() {
        m_entityList.refresh();
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
            fireEntitiesChanged();
            int newIndex =
                    m_model.getEntities().size() - 1;
            m_entityList.getSelectionModel()
                    .select(newIndex);
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

    private void fireEntitiesChanged() {
        if (m_onEntitiesChanged != null) {
            m_onEntitiesChanged.run();
        }
    }

    private class EntityListCell
            extends ListCell<Integer> {
        private final HBox m_root = new HBox();
        private final Label m_nameLabel = new EditorLabel();
        private final Region m_indicator = new Region();
        private final Region m_spacer = new Region();

        EntityListCell() {
            HBox.setHgrow(m_spacer, Priority.ALWAYS);
            m_indicator.getStyleClass()
                    .add(STYLE_NO_POSITION);
            Tooltip.install(m_indicator,
                    new Tooltip("No Position component"));
            HBox.setMargin(m_indicator,
                    new Insets(
                            INDICATOR_TOP,
                            INDICATOR_RIGHT,
                            INDICATOR_BOTTOM,
                            INDICATOR_LEFT));
            m_root.setAlignment(Pos.CENTER_LEFT);
            m_root.setFillHeight(true);
            m_root.getChildren().addAll(
                    m_nameLabel, m_spacer, m_indicator);
        }

        @Override
        protected void updateItem(
                Integer index, boolean empty) {
            super.updateItem(index, empty);
            if (empty || index == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            List<EditorEntity> entities =
                    m_model.getEntities();
            if (index >= entities.size()) {
                setText(null);
                setGraphic(null);
                return;
            }

            EditorEntity entity = entities.get(index);
            m_nameLabel.setText(
                    index + ": " + entity.id());
            boolean hasPosition =
                    entity.getPositionComponent() != null;
            m_indicator.setVisible(!hasPosition);
            m_indicator.setManaged(!hasPosition);
            setText(null);
            setGraphic(m_root);
        }
    }
}
