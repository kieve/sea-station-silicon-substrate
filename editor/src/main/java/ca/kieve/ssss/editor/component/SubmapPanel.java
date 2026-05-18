package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import ca.kieve.ssss.component.Connector;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Submap;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.ui.fx.EditorButton;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.MapPathUtil;

import java.io.File;
import java.util.function.Consumer;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

/**
 * Tab content for connectors and submaps. Both lists hold
 * {@link EditorEntity} instances so the rest of the editor's
 * entity-shaped infrastructure (component panel, move tool, color
 * resolution) works for them by default.
 *
 * <p>Selecting a row fires {@link #setOnConnectorSelected} or
 * {@link #setOnSubmapSelected}; the parent panel is responsible for
 * routing the selection to {@link ComponentPanel#showInlineEntity}.
 */
public class SubmapPanel extends VBox {
    public enum SectionKind {
        CONNECTOR,
        SUBMAP
    }

    private class ConnectorListCell extends ListCell<Integer> {
        @Override
        protected void updateItem(Integer index, boolean empty) {
            super.updateItem(index, empty);
            if (empty || index == null || index >= m_model.getConnectors().size()) {
                setText(null);
                setGraphic(null);
                return;
            }
            EditorEntity c = m_model.getConnectors().get(index);
            ComponentDefinition pos = findComponent(c, Position.class);
            ComponentDefinition conn = findComponent(c, Connector.class);
            String posStr = pos == null
                ? "(?, ?, ?)"
                : "(" + pos.properties().get("x") + ", "
                    + pos.properties().get("y") + ", "
                    + pos.properties().get("z") + ")";
            String dir = conn == null || conn.properties().get("direction") == null
                ? ""
                : " " + conn.properties().get("direction");
            setText(c.id() + " @ " + posStr + dir);
        }
    }

    private class SubmapListCell extends ListCell<Integer> {
        @Override
        protected void updateItem(Integer index, boolean empty) {
            super.updateItem(index, empty);
            if (empty || index == null || index >= m_model.getSubmaps().size()) {
                setText(null);
                setGraphic(null);
                return;
            }
            EditorEntity s = m_model.getSubmaps().get(index);
            ComponentDefinition sm = findComponent(s, Submap.class);
            ComponentDefinition pos = findComponent(s, Position.class);
            String ref = sm == null || sm.properties().get("ref") == null
                ? "(no ref)"
                : sm.properties().get("ref").toString();
            String placement;
            if (pos != null) {
                placement = "offset (" + pos.properties().get("x") + ", "
                    + pos.properties().get("y") + ", "
                    + pos.properties().get("z") + ")";
            } else if (sm != null
                && sm.properties().get("localConnector") != null
                && sm.properties().get("remoteConnector") != null) {
                placement = sm.properties().get("localConnector") + " <-> "
                    + sm.properties().get("remoteConnector");
            } else {
                placement = "(no placement)";
            }
            setText(s.id() + " " + ref + " " + placement);
        }
    }

    private static final int PREF_WIDTH = 220;
    private static final int MIN_WIDTH = 180;
    private static final int BUTTON_SPACING = 4;
    private static final int SECTION_SPACING = 8;

    private static final String STYLE_SUBMAP_PANEL = "editor-submap-panel";
    private static final String STYLE_TOOLBAR_LABEL_BOLD = "editor-toolbar-label-bold";

    // language=css
    private static final String CSS = """
        .%1$s {
            -fx-background-color: -color-bg-subtle;
            -fx-padding: 8;
            -fx-spacing: 8;
        }
        .%1$s .list-cell {
            -fx-cell-size: 1.5em;
            -fx-padding: 0.125em 0.583em;
        }
        .%2$s {
            -fx-font-weight: bold;
            -fx-padding: 0 8 0 4;
        }
        """.formatted(STYLE_SUBMAP_PANEL, STYLE_TOOLBAR_LABEL_BOLD);

    private final EditorMapModel m_model;
    private final ListView<Integer> m_connectorList;
    private final ListView<Integer> m_submapList;

    private Consumer<File> m_onOpenReferenced;
    private Runnable m_onChanged;
    private Consumer<Integer> m_onConnectorSelected;
    private Consumer<Integer> m_onSubmapSelected;

    public SubmapPanel(EditorMapModel model) {
        m_model = model;

        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_SUBMAP_PANEL);
        setPrefWidth(PREF_WIDTH);
        setMinWidth(MIN_WIDTH);

        var connectorTitle = new EditorLabel("Connectors");
        connectorTitle.getStyleClass().add(STYLE_TOOLBAR_LABEL_BOLD);

        m_connectorList = new ListView<>();
        m_connectorList.setCellFactory(lv -> new ConnectorListCell());
        m_connectorList.setFocusTraversable(false);
        m_connectorList.setPrefHeight(140);
        VBox.setVgrow(m_connectorList, Priority.SOMETIMES);

        var connectorAdd = new EditorButton("Add");
        connectorAdd.setFocusTraversable(false);
        connectorAdd.setOnAction(e -> onConnectorAdd());

        var connectorRemove = new EditorButton("Remove");
        connectorRemove.setFocusTraversable(false);
        connectorRemove.setOnAction(e -> onConnectorRemove());

        var connectorButtons = new HBox(BUTTON_SPACING, connectorAdd, connectorRemove);
        connectorButtons.setAlignment(Pos.CENTER_LEFT);

        var submapTitle = new EditorLabel("Submaps");
        submapTitle.getStyleClass().add(STYLE_TOOLBAR_LABEL_BOLD);
        VBox.setMargin(submapTitle, new Insets(SECTION_SPACING, 0, 0, 0));

        m_submapList = new ListView<>();
        m_submapList.setCellFactory(lv -> new SubmapListCell());
        m_submapList.setFocusTraversable(false);
        VBox.setVgrow(m_submapList, Priority.ALWAYS);

        // Wire selection listeners after both lists exist — each clears
        // the other so a connector and submap can't be selected
        // simultaneously.
        m_connectorList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal == null) {
                    return;
                }
                m_submapList.getSelectionModel().clearSelection();
                if (m_onConnectorSelected != null) {
                    m_onConnectorSelected.accept(newVal);
                }
            }
        );
        m_submapList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal == null) {
                    return;
                }
                m_connectorList.getSelectionModel().clearSelection();
                if (m_onSubmapSelected != null) {
                    m_onSubmapSelected.accept(newVal);
                }
            }
        );

        var submapAdd = new EditorButton("Add");
        submapAdd.setFocusTraversable(false);
        submapAdd.setOnAction(e -> onSubmapAdd());

        var submapRemove = new EditorButton("Remove");
        submapRemove.setFocusTraversable(false);
        submapRemove.setOnAction(e -> onSubmapRemove());

        var submapOpen = new EditorButton("Open");
        submapOpen.setFocusTraversable(false);
        submapOpen.setOnAction(e -> onSubmapOpen());

        var submapButtons = new HBox(BUTTON_SPACING, submapAdd, submapRemove, submapOpen);
        submapButtons.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
            connectorTitle,
            m_connectorList,
            connectorButtons,
            submapTitle,
            m_submapList,
            submapButtons
        );

        refreshConnectorList();
        refreshSubmapList();
    }

    public void setOnOpenReferenced(Consumer<File> handler) {
        m_onOpenReferenced = handler;
    }

    public void setOnChanged(Runnable handler) {
        m_onChanged = handler;
    }

    public void setOnConnectorSelected(Consumer<Integer> handler) {
        m_onConnectorSelected = handler;
    }

    public void setOnSubmapSelected(Consumer<Integer> handler) {
        m_onSubmapSelected = handler;
    }

    public void selectConnector(int index) {
        if (index < 0 || index >= m_model.getConnectors().size()) {
            return;
        }
        m_connectorList.getSelectionModel().select(Integer.valueOf(index));
        m_connectorList.scrollTo(index);
    }

    public void selectSubmap(int index) {
        if (index < 0 || index >= m_model.getSubmaps().size()) {
            return;
        }
        m_submapList.getSelectionModel().select(Integer.valueOf(index));
        m_submapList.scrollTo(index);
    }

    public void refreshConnectorList() {
        m_connectorList.getItems().clear();
        for (int i = 0; i < m_model.getConnectors().size(); i++) {
            m_connectorList.getItems().add(i);
        }
    }

    public void refreshSubmapList() {
        m_submapList.getItems().clear();
        for (int i = 0; i < m_model.getSubmaps().size(); i++) {
            m_submapList.getItems().add(i);
        }
    }

    public void refreshLists() {
        m_connectorList.refresh();
        m_submapList.refresh();
    }

    private void onConnectorAdd() {
        AddConnectorDialog.showAdd().ifPresent(entity -> {
            m_model.addConnector(entity);
            refreshConnectorList();
            int newIndex = m_model.getConnectors().size() - 1;
            m_connectorList.getSelectionModel().select(Integer.valueOf(newIndex));
            fireChanged();
        });
    }

    private void onConnectorRemove() {
        Integer index = m_connectorList.getSelectionModel().getSelectedItem();
        if (index == null) {
            return;
        }
        m_model.removeConnector(index);
        refreshConnectorList();
        fireChanged();
    }

    private void onSubmapAdd() {
        AddSubmapDialog.showAdd().ifPresent(entity -> {
            m_model.addSubmap(entity);
            refreshSubmapList();
            int newIndex = m_model.getSubmaps().size() - 1;
            m_submapList.getSelectionModel().select(Integer.valueOf(newIndex));
            fireChanged();
        });
    }

    private void onSubmapRemove() {
        Integer index = m_submapList.getSelectionModel().getSelectedItem();
        if (index == null) {
            return;
        }
        m_model.removeSubmap(index);
        refreshSubmapList();
        fireChanged();
    }

    private void onSubmapOpen() {
        if (m_onOpenReferenced == null) {
            return;
        }
        Integer index = m_submapList.getSelectionModel().getSelectedItem();
        if (index == null) {
            return;
        }
        EditorEntity submap = m_model.getSubmaps().get(index);
        String ref = readSubmapRef(submap);
        if (ref == null) {
            return;
        }
        File target = MapPathUtil.resolveSubmapRef(m_model.getFile(), ref);
        if (target == null || !target.isFile()) {
            return;
        }
        m_onOpenReferenced.accept(target);
    }

    private void fireChanged() {
        if (m_onChanged != null) {
            m_onChanged.run();
        }
    }

    private static String readSubmapRef(EditorEntity entity) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() == Submap.class) {
                Object ref = comp.properties().get("ref");
                return ref == null ? null : ref.toString();
            }
        }
        return null;
    }

    private static ComponentDefinition findComponent(EditorEntity entity, Class<?> type) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() == type) {
                return comp;
            }
        }
        return null;
    }
}
