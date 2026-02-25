package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;

public class EntityAddDialog
        extends Dialog<EntityAddDialog.Result> {
    public record Result(String entityId) {}

    private final ComboBox<String> m_entityCombo;

    public EntityAddDialog(List<String> entityIds) {
        DialogUtil.style(this, "Add Entity");

        m_entityCombo = new ComboBox<>();
        m_entityCombo.getItems().addAll(entityIds);
        if (!entityIds.isEmpty()) {
            m_entityCombo.setValue(entityIds.getFirst());
        }

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new EditorLabel("Entity:"), 0, 0);
        grid.add(m_entityCombo, 1, 0);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            String entityId = m_entityCombo.getValue();
            if (entityId == null
                    || entityId.isEmpty()) {
                return null;
            }
            return new Result(entityId);
        });
    }

    public static Optional<Result> showAdd() {
        var registry =
                EditorContext.getInstance().getRegistry();
        List<String> entityIds =
                registry.getEntityIds().stream()
                        .sorted()
                        .toList();
        var dialog = new EntityAddDialog(entityIds);
        return dialog.showAndWait();
    }
}
