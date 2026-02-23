package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.MapBlockDefinition;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

public class BlockEditDialog extends Dialog<BlockEditDialog.Result> {
    public record Result(
            String name, MapBlockDefinition blockDef) {}

    private final TextField m_nameField;
    private final ComboBox<String> m_typeCombo;
    private final TextField m_charField;

    public BlockEditDialog(
            List<String> blockTypes,
            String existingName,
            MapBlockDefinition existingDef
    ) {
        setTitle(existingDef != null ? "Edit Block" : "Add Block");

        m_nameField = new TextField(
                existingName != null ? existingName : "");
        m_nameField.setPromptText("Block name");
        if (existingName != null) {
            m_nameField.setDisable(true);
        }

        m_typeCombo = new ComboBox<>();
        m_typeCombo.getItems().addAll(blockTypes);
        if (existingDef != null) {
            m_typeCombo.setValue(existingDef.type());
        } else if (!blockTypes.isEmpty()) {
            m_typeCombo.setValue(blockTypes.getFirst());
        }

        m_charField = new TextField(
                existingDef != null
                        ? String.valueOf(existingDef.layoutChar())
                        : "");
        m_charField.setPrefColumnCount(2);
        // Limit to a single character
        m_charField.textProperty().addListener(
                (obs, oldVal, newVal) -> {
            if (newVal.length() > 1) {
                m_charField.setText(
                        newVal.substring(0, 1));
            }
        });

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Name:"), 0, 0);
        grid.add(m_nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(m_typeCombo, 1, 1);
        grid.add(new Label("Layout Char:"), 0, 2);
        grid.add(m_charField, 1, 2);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            String name = m_nameField.getText().trim();
            String type = m_typeCombo.getValue();
            String charText = m_charField.getText();
            if (name.isEmpty() || type == null
                    || charText.isEmpty()) {
                return null;
            }
            return new Result(
                    name,
                    new MapBlockDefinition(
                            type, charText.charAt(0)));
        });
    }

    public static Optional<Result> showAdd(
            List<String> blockTypes) {
        var dialog = new BlockEditDialog(
                blockTypes, null, null);
        return dialog.showAndWait();
    }

    public static Optional<Result> showEdit(
            List<String> blockTypes,
            String name,
            MapBlockDefinition def
    ) {
        var dialog = new BlockEditDialog(
                blockTypes, name, def);
        return dialog.showAndWait();
    }
}
