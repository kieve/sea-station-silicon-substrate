package ca.kieve.ssss.editor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class LayerControlPanel extends JPanel {
    private final EditorModel m_model;
    private final JComboBox<String> m_layerCombo;
    private final JLabel m_boundsLabel;
    private boolean m_updatingCombo = false;

    public LayerControlPanel(EditorModel model) {
        m_model = model;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Layers"));

        var leftPanel = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 8, 4));

        leftPanel.add(new JLabel("Z-Layer:"));
        m_layerCombo = new JComboBox<>();
        m_layerCombo.addActionListener(e -> {
            if (m_updatingCombo) {
                return;
            }
            String selected =
                    (String) m_layerCombo.getSelectedItem();
            if (selected != null) {
                m_model.setActiveLayer(
                        Integer.parseInt(selected));
            }
        });
        leftPanel.add(m_layerCombo);

        var addButton = new JButton("Add");
        addButton.addActionListener(e -> addLayer());
        leftPanel.add(addButton);

        var removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeLayer());
        leftPanel.add(removeButton);

        m_boundsLabel = new JLabel();
        leftPanel.add(m_boundsLabel);

        add(leftPanel, BorderLayout.CENTER);

        var rightPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 4, 4));

        var prevButton = new JButton("<");
        prevButton.setToolTipText("Previous layer");
        prevButton.addActionListener(e -> prevLayer());
        rightPanel.add(prevButton);

        var nextButton = new JButton(">");
        nextButton.setToolTipText("Next layer");
        nextButton.addActionListener(e -> nextLayer());
        rightPanel.add(nextButton);

        add(rightPanel, BorderLayout.EAST);

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {
                updateBoundsLabel();
            }

            @Override
            public void onLayerChanged() {
                refreshCombo();
            }

            @Override
            public void onBlockSelectionChanged() {
            }

            @Override
            public void onDirtyChanged() {
            }
        });
    }

    public void refreshCombo() {
        m_updatingCombo = true;
        m_layerCombo.removeAllItems();
        for (int z : m_model.getSortedLayerKeys()) {
            m_layerCombo.addItem(String.valueOf(z));
        }
        m_layerCombo.setSelectedItem(
                String.valueOf(m_model.getActiveLayer()));
        m_updatingCombo = false;
        updateBoundsLabel();
    }

    private void updateBoundsLabel() {
        var bounds = m_model.getGlobalBounds();
        if (bounds == null) {
            m_boundsLabel.setText("  (empty)");
            return;
        }
        m_boundsLabel.setText(String.format(
                "  %dx%d", bounds.width(), bounds.height()));
    }

    private void addLayer() {
        String input = JOptionPane.showInputDialog(this,
                "Z-level for new layer:", "Add Layer",
                JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.isBlank()) {
            return;
        }
        try {
            int z = Integer.parseInt(input.strip());
            m_model.addLayer(z);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid number.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void prevLayer() {
        var keys = m_model.getSortedLayerKeys();
        int current = m_model.getActiveLayer();
        int idx = keys.indexOf(current);
        int prev = idx <= 0 ? keys.size() - 1 : idx - 1;
        m_model.setActiveLayer(keys.get(prev));
    }

    private void nextLayer() {
        var keys = m_model.getSortedLayerKeys();
        int current = m_model.getActiveLayer();
        int idx = keys.indexOf(current);
        int next = idx >= keys.size() - 1 ? 0 : idx + 1;
        m_model.setActiveLayer(keys.get(next));
    }

    private void removeLayer() {
        int z = m_model.getActiveLayer();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove layer " + z + "?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            m_model.removeLayer(z);
        }
    }
}
