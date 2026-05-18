package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Submap;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Minimal "create a submap" dialog. Asks for the submap's id and ref;
 * defaults to offset mode with Position (0, 0, 0). Further editing —
 * including switching to connector mode by removing Position and adding
 * localConnector / remoteConnector — happens inline in the properties
 * panel.
 */
public class AddSubmapDialog extends Dialog<EditorEntity> {
    private final TextField m_idField;
    private final TextField m_refField;

    public AddSubmapDialog() {
        DialogUtil.style(this, "Add Submap");
        m_idField = new TextField();
        m_idField.setPromptText("Region id (e.g. maintenance_east)");
        m_idField.setPrefColumnCount(24);

        m_refField = new TextField();
        m_refField.setPromptText("./neighbor.yaml or /maps/absolute.yaml");
        m_refField.setPrefColumnCount(24);

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new EditorLabel("Id:"), 0, 0);
        grid.add(m_idField, 1, 0);
        grid.add(new EditorLabel("Ref:"), 0, 1);
        grid.add(m_refField, 1, 1);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            String id = m_idField.getText().trim();
            String ref = m_refField.getText().trim();
            if (id.isEmpty() || ref.isEmpty()) {
                return null;
            }
            var components = new ArrayList<ComponentDefinition>();
            var submapComp = new ComponentDefinition(Submap.class);
            submapComp.setProperty("ref", ref);
            components.add(submapComp);
            var pos = new ComponentDefinition(Position.class);
            pos.setProperty("x", 0);
            pos.setProperty("y", 0);
            pos.setProperty("z", 0);
            components.add(pos);
            return new EditorEntity(id, components);
        });
    }

    public static Optional<EditorEntity> showAdd() {
        return new AddSubmapDialog().showAndWait();
    }
}
