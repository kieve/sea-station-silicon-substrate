package ca.kieve.ssss.editor;

import ca.kieve.ssss.component.Identifier;
import ca.kieve.ssss.content.EntityDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BlockPalettePanel extends JPanel {
    private static final String BLOCKS_DIR =
            "content/entities/blocks/";
    private static final String FILE_LIST = "file_list.txt";
    private static final String BLOCK_PREFIX = "block_";

    private final EditorModel m_model;
    private final DefaultListModel<String> m_listModel;
    private final JList<String> m_list;
    private final List<String> m_blockTypeIds;

    public BlockPalettePanel(EditorModel model) {
        m_model = model;
        m_blockTypeIds = loadBlockTypeIds();
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

            @Override
            public void onEntitiesChanged() {
            }

            @Override
            public void onToolChanged() {
            }
        });
    }

    private static List<String> loadBlockTypeIds() {
        List<String> typeIds = new ArrayList<>();
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        for (String filename : readFileIndex()) {
            String resource = BLOCKS_DIR + filename;
            try (InputStream in = BlockPalettePanel.class
                    .getClassLoader()
                    .getResourceAsStream(resource)) {
                if (in == null) {
                    continue;
                }
                var iterator = mapper.readValues(
                        mapper.getFactory()
                                .createParser(in),
                        EntityDefinition.class);
                while (iterator.hasNext()) {
                    EntityDefinition def = iterator.next();
                    String entityId = extractEntityId(def);
                    if (entityId == null) {
                        continue;
                    }
                    String typeId = entityIdToTypeId(entityId);
                    if (typeId != null) {
                        typeIds.add(typeId);
                    }
                }
            } catch (IOException e) {
                // Skip files that can't be parsed
            }
        }

        typeIds.sort(String::compareTo);
        return typeIds;
    }

    private static List<String> readFileIndex() {
        List<String> files = new ArrayList<>();
        String resource = BLOCKS_DIR + FILE_LIST;
        try (InputStream in = BlockPalettePanel.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            if (in == null) {
                return files;
            }
            var reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                files.add(trimmed);
            }
        } catch (IOException e) {
            // Return whatever we have
        }
        return files;
    }

    private static String extractEntityId(EntityDefinition def) {
        for (var comp : def.components()) {
            if (comp.type() == Identifier.class) {
                Object key = comp.properties().get("key");
                if (key != null) {
                    return key.toString();
                }
            }
        }
        return null;
    }

    private static String entityIdToTypeId(String entityId) {
        if ("air".equals(entityId)) {
            return "air";
        }
        if (entityId.startsWith(BLOCK_PREFIX)) {
            return entityId.substring(BLOCK_PREFIX.length());
        }
        return null;
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
        var owner = (Frame) SwingUtilities
                .getWindowAncestor(this);
        var dialog = new JDialog(owner, "Add Block", true);
        dialog.setLayout(new BorderLayout(8, 8));

        var formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(
                BorderFactory.createEmptyBorder(8, 8, 0, 8));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Block name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Name:"), gbc);

        var nameField = new JTextField(15);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);

        // Block type dropdown
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Type:"), gbc);

        var typeCombo = new JComboBox<>(
                m_blockTypeIds.toArray(new String[0]));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(typeCombo, gbc);

        // Layout character
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Layout char:"), gbc);

        var charField = new JTextField(3);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(charField, gbc);

        dialog.add(formPanel, BorderLayout.CENTER);

        // Buttons
        var buttonPanel = new JPanel();
        var okButton = new JButton("OK");
        var cancelButton = new JButton("Cancel");

        boolean[] accepted = {false};

        okButton.addActionListener(e -> {
            String name = nameField.getText().strip();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Block name is required.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (m_model.getBlocks().containsKey(name)) {
                JOptionPane.showMessageDialog(dialog,
                        "Block '" + name + "' already exists.",
                        "Duplicate Block",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            Object typeObj = typeCombo.getSelectedItem();
            String typeId = typeObj != null
                    ? typeObj.toString().strip() : "";
            if (typeId.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Block type is required.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String charStr = charField.getText();
            if (charStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Layout character is required.",
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            accepted[0] = true;
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().setDefaultButton(okButton);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);

        if (!accepted[0]) {
            return;
        }

        String name = nameField.getText().strip();
        String typeId = typeCombo.getSelectedItem()
                .toString().strip();
        char layoutChar = charField.getText().charAt(0);

        m_model.addBlock(name,
                new MapBlockDefinition(typeId, layoutChar));
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
