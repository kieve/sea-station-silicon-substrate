package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import java.util.List;
import java.util.Optional;

public class BlockEditDialog
    extends
    Dialog<BlockEditDialog.Result> {
    public record Result(String name, MapBlockDefinition blockDef) {
    }

    private final TextField m_nameField;
    private final ComboBox<String> m_typeCombo;

    public BlockEditDialog(
        List<String> blockTypes,
        String existingName,
        MapBlockDefinition existingDef
    ) {
        DialogUtil.style(this, existingDef != null ? "Edit Block" : "Add Block");

        m_nameField = new TextField(existingName != null ? existingName : "");
        m_nameField.setPromptText("Block name");
        if (existingName != null) {
            m_nameField.setDisable(true);
        }

        m_typeCombo = new ComboBox<>();
        m_typeCombo.getItems().addAll(blockTypes);
        if (existingDef != null) {
            m_typeCombo.setValue(existingDef.bpId());
        } else if (!blockTypes.isEmpty()) {
            m_typeCombo.setValue(blockTypes.getFirst());
        }

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new EditorLabel("Name:"), 0, 0);
        grid.add(m_nameField, 1, 0);
        grid.add(new EditorLabel("Blueprint:"), 0, 1);
        grid.add(m_typeCombo, 1, 1);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(btn -> {
            if (btn != ButtonType.OK) {
                return null;
            }
            String name = m_nameField.getText().trim();
            String bpId = m_typeCombo.getValue();
            if (name.isEmpty() || bpId == null) {
                return null;
            }
            return new Result(name, new MapBlockDefinition(bpId, ' '));
        });
    }

    public static Optional<Result> showAdd(List<String> blockTypes) {
        var dialog = new BlockEditDialog(blockTypes, null, null);
        return dialog.showAndWait();
    }

    public static Optional<Result> showEdit(
        List<String> blockTypes,
        String name,
        MapBlockDefinition def
    ) {
        var dialog = new BlockEditDialog(blockTypes, name, def);
        return dialog.showAndWait();
    }
}
