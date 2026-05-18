package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import ca.kieve.ssss.component.Connector;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Minimal "create a connector" dialog. Asks for the connector's id only;
 * Position defaults to (0, 0, 0) and direction is left null. Further
 * editing happens inline in the properties panel.
 */
public class AddConnectorDialog extends Dialog<EditorEntity> {
    private final TextField m_idField;

    public AddConnectorDialog() {
        DialogUtil.style(this, "Add Connector");
        m_idField = new TextField();
        m_idField.setPromptText("Connector id (e.g. east_door)");
        m_idField.setPrefColumnCount(20);

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new EditorLabel("Id:"), 0, 0);
        grid.add(m_idField, 1, 0);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            String id = m_idField.getText().trim();
            if (id.isEmpty()) {
                return null;
            }
            var components = new ArrayList<ComponentDefinition>();
            components.add(new ComponentDefinition(Connector.class));
            var pos = new ComponentDefinition(Position.class);
            pos.setProperty("x", 0);
            pos.setProperty("y", 0);
            pos.setProperty("z", 0);
            components.add(pos);
            return new EditorEntity(id, components);
        });
    }

    public static Optional<EditorEntity> showAdd() {
        return new AddConnectorDialog().showAndWait();
    }
}
