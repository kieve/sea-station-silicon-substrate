package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.MapBlockDefinition;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

public class BlockPalettePanel extends JPanel {
    private final EditorModel m_model;
    private final DefaultListModel<String> m_listModel;
    private final JList<String> m_list;

    public BlockPalettePanel(EditorModel model) {
        m_model = model;
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Blocks"));
        setPreferredSize(new Dimension(180, 0));

        m_listModel = new DefaultListModel<>();
        m_list = new JList<>(m_listModel);
        m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_list.setCellRenderer(new BlockCellRenderer());
        m_list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            String selected = m_list.getSelectedValue();
            if (selected != null) {
                m_model.setActiveBlockName(selected);
            }
        });

        add(new JScrollPane(m_list), BorderLayout.CENTER);

        var buttonPanel = new JPanel(new java.awt.GridLayout(1, 2, 4, 0));

        var addButton = new JButton("Add");
        addButton.addActionListener(e -> addBlock());

        var removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeBlock());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {
                refreshList();
            }

            @Override
            public void onLayerChanged() {
            }

            @Override
            public void onBlockSelectionChanged() {
                String active = m_model.getActiveBlockName();
                if (!active.equals(m_list.getSelectedValue())) {
                    m_list.setSelectedValue(active, true);
                }
            }

            @Override
            public void onDirtyChanged() {
            }
        });
    }

    public void refreshList() {
        String selected = m_list.getSelectedValue();
        m_listModel.clear();
        for (String name : m_model.getBlocks().keySet()) {
            m_listModel.addElement(name);
        }
        if (selected != null && m_listModel.contains(selected)) {
            m_list.setSelectedValue(selected, true);
        } else if (!m_listModel.isEmpty()) {
            m_list.setSelectedIndex(0);
        }
    }

    private void addBlock() {
        String name = JOptionPane.showInputDialog(
                this, "Block name:", "Add Block",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        name = name.strip();

        if (m_model.getBlocks().containsKey(name)) {
            JOptionPane.showMessageDialog(this,
                    "Block '" + name + "' already exists.",
                    "Duplicate Block",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String typeId = JOptionPane.showInputDialog(
                this, "Block type ID (e.g. wood, stone, air):",
                "Block Type", JOptionPane.PLAIN_MESSAGE);
        if (typeId == null || typeId.isBlank()) {
            return;
        }

        String charStr = JOptionPane.showInputDialog(
                this, "Layout character:", "Layout Char",
                JOptionPane.PLAIN_MESSAGE);
        if (charStr == null || charStr.isEmpty()) {
            return;
        }

        char layoutChar = charStr.charAt(0);
        m_model.addBlock(name,
                new MapBlockDefinition(typeId.strip(), layoutChar));
        m_model.setActiveBlockName(name);
    }

    private void removeBlock() {
        String selected = m_list.getSelectedValue();
        if (selected == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove block '" + selected + "'?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            m_model.removeBlock(selected);
        }
    }

    private class BlockCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            String name = (String) value;
            var block = m_model.getBlocks().get(name);
            if (block != null) {
                setText(String.format("[%c] %s (%s)",
                        block.layoutChar(), name, block.type()));
                if (!isSelected) {
                    setBackground(new Color(245, 245, 245));
                }
            }
            return this;
        }
    }
}
