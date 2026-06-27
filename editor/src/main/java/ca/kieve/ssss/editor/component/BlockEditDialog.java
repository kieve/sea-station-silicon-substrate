package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Spinner;
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

    private static final int MAX_SOURCE_DEPTH = 9999;

    private final TextField m_nameField;
    private final ComboBox<String> m_typeCombo;
    private final Spinner<Double> m_waterFillSpinner;
    private final Spinner<Integer> m_waterDepthSpinner;

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

        double existingFill = (existingDef != null && existingDef.waterFill() != null)
            ? existingDef.waterFill()
            : 0.0;
        m_waterFillSpinner = new Spinner<>(0.0, 1.0, existingFill, 0.1);

        int existingDepth = (existingDef != null && existingDef.waterDepth() != null)
            ? existingDef.waterDepth()
            : 0;
        m_waterDepthSpinner = new Spinner<>(0, MAX_SOURCE_DEPTH, existingDepth);

        m_waterFillSpinner.setEditable(true);
        m_waterDepthSpinner.setEditable(true);
        m_waterFillSpinner.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                commitFill();
            }
        });
        m_waterDepthSpinner.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                commitDepth();
            }
        });

        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(16));
        grid.add(new EditorLabel("Name:"), 0, 0);
        grid.add(m_nameField, 1, 0);
        grid.add(new EditorLabel("Blueprint:"), 0, 1);
        grid.add(m_typeCombo, 1, 1);
        grid.add(new EditorLabel("Water fill (0-1):"), 0, 2);
        grid.add(m_waterFillSpinner, 1, 2);
        grid.add(new EditorLabel("Water depth (source):"), 0, 3);
        grid.add(m_waterDepthSpinner, 1, 3);

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
            commitFill();
            commitDepth();
            double fill = m_waterFillSpinner.getValue();
            Double waterFill = fill > 0 ? fill : null;
            int depth = m_waterDepthSpinner.getValue();
            Integer waterDepth = depth > 0 ? depth : null;
            return new Result(name, new MapBlockDefinition(bpId, ' ', waterFill, waterDepth));
        });
    }

    private void commitFill() {
        Double parsed = parseDouble(m_waterFillSpinner.getEditor().getText());
        double value = parsed != null
            ? Math.max(0.0, Math.min(1.0, parsed))
            : m_waterFillSpinner.getValue();
        m_waterFillSpinner.getValueFactory().setValue(value);
    }

    private void commitDepth() {
        Integer parsed = parseInt(m_waterDepthSpinner.getEditor().getText());
        int value = parsed != null
            ? Math.max(0, Math.min(MAX_SOURCE_DEPTH, parsed))
            : m_waterDepthSpinner.getValue();
        m_waterDepthSpinner.getValueFactory().setValue(value);
    }

    private static Integer parseInt(String text) {
        String trimmed = text.trim();
        return trimmed.matches("-?\\d+") ? Integer.valueOf(trimmed) : null;
    }

    private static Double parseDouble(String text) {
        String trimmed = text.trim();
        return trimmed.matches("-?\\d*\\.?\\d+") ? Double.valueOf(trimmed) : null;
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
