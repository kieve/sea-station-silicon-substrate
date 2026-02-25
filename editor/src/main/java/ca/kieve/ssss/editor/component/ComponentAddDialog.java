package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ComponentTypeDeserializer;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;

public class ComponentAddDialog
        extends Dialog<ComponentAddDialog.Result> {
    public record Result(String componentTypeName) {}

    private final ComboBox<String> m_typeCombo;

    public ComponentAddDialog(List<String> typeNames) {
        DialogUtil.style(this, "Add Component Override");

        m_typeCombo = new ComboBox<>();
        m_typeCombo.getItems().addAll(typeNames);
        if (!typeNames.isEmpty()) {
            m_typeCombo.setValue(typeNames.getFirst());
        }

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new EditorLabel("Component:"), 0, 0);
        grid.add(m_typeCombo, 1, 0);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            String typeName = m_typeCombo.getValue();
            if (typeName == null
                    || typeName.isEmpty()) {
                return null;
            }
            return new Result(typeName);
        });
    }

    public static Optional<Result> showAdd(
            Set<String> excludeTypes) {
        List<String> available =
                ComponentTypeDeserializer
                        .getAllTypeNames().stream()
                        .filter(name ->
                                !excludeTypes.contains(
                                        name))
                        .toList();
        var dialog = new ComponentAddDialog(available);
        return dialog.showAndWait();
    }
}
