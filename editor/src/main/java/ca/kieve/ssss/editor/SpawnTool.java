package ca.kieve.ssss.editor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;

public class SpawnTool extends JPanel {
    private final EditorModel m_model;
    private final MapGridPanel m_gridPanel;
    private final JTextField m_colField;
    private final JTextField m_rowField;
    private final JTextField m_zField;

    public SpawnTool(EditorModel model, MapGridPanel gridPanel) {
        m_model = model;
        m_gridPanel = gridPanel;
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        setBorder(BorderFactory.createTitledBorder("Player Spawn"));

        add(new JLabel("Col:"));
        m_colField = new JTextField(3);
        add(m_colField);

        add(new JLabel("Row:"));
        m_rowField = new JTextField(3);
        add(m_rowField);

        add(new JLabel("Z:"));
        m_zField = new JTextField(3);
        add(m_zField);

        var applyButton = new JButton("Set");
        applyButton.addActionListener(e -> applyFromFields());
        add(applyButton);

        var clickButton = new JButton("Click to Place");
        clickButton.addActionListener(e -> {
            m_gridPanel.setSpawnMode(true);
        });
        add(clickButton);

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {
                refreshFields();
            }

            @Override
            public void onLayerChanged() {
            }

            @Override
            public void onBlockSelectionChanged() {
            }

            @Override
            public void onDirtyChanged() {
            }
        });
    }

    public void refreshFields() {
        m_colField.setText(String.valueOf(m_model.getSpawnCol()));
        m_rowField.setText(String.valueOf(m_model.getSpawnRow()));
        m_zField.setText(String.valueOf(m_model.getSpawnZ()));
    }

    private void applyFromFields() {
        try {
            int col = Integer.parseInt(m_colField.getText().strip());
            int row = Integer.parseInt(m_rowField.getText().strip());
            int z = Integer.parseInt(m_zField.getText().strip());
            m_model.setPlayerSpawn(col, row);
        } catch (NumberFormatException ignored) {
        }
    }
}
