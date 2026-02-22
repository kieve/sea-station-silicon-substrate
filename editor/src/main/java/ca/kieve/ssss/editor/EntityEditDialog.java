package ca.kieve.ssss.editor;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityEditDialog extends JDialog {
    private final JTextField m_idField;
    private final JCheckBox m_posCheckBox;
    private final JSpinner m_xSpinner;
    private final JSpinner m_ySpinner;
    private final JSpinner m_zSpinner;
    private final DefaultListModel<String> m_compListModel;
    private final JList<String> m_compList;
    private final List<ComponentEntry> m_components =
            new ArrayList<>();
    private MapEntityDefinition m_result;

    private record ComponentEntry(
            String typeName,
            Map<String, Object> properties) {}

    public EntityEditDialog(
            Window owner,
            MapEntityDefinition existing) {
        super(owner, existing == null ? "Add Entity" : "Edit Entity",
                ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(8, 8));

        // --- Top: Entity ID ---
        var idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        idPanel.add(new JLabel("Entity ID:"));
        m_idField = new JTextField(20);
        idPanel.add(m_idField);
        add(idPanel, BorderLayout.NORTH);

        // --- Center: Position + Components ---
        var centerPanel = new JPanel();
        centerPanel.setLayout(
                new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Position section
        var posPanel = new JPanel(new GridBagLayout());
        posPanel.setBorder(
                BorderFactory.createTitledBorder("Position"));
        var gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);

        m_posCheckBox = new JCheckBox("Has Position", false);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        posPanel.add(m_posCheckBox, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        posPanel.add(new JLabel("X:"), gbc);
        m_xSpinner = new JSpinner(
                new SpinnerNumberModel(0, -999, 999, 1));
        gbc.gridx = 1;
        posPanel.add(m_xSpinner, gbc);

        gbc.gridx = 2;
        posPanel.add(new JLabel("Y:"), gbc);
        m_ySpinner = new JSpinner(
                new SpinnerNumberModel(0, -999, 999, 1));
        gbc.gridx = 3;
        posPanel.add(m_ySpinner, gbc);

        gbc.gridx = 4;
        posPanel.add(new JLabel("Z:"), gbc);
        m_zSpinner = new JSpinner(
                new SpinnerNumberModel(0, -99, 99, 1));
        gbc.gridx = 5;
        posPanel.add(m_zSpinner, gbc);

        m_posCheckBox.addActionListener(e -> {
            boolean enabled = m_posCheckBox.isSelected();
            m_xSpinner.setEnabled(enabled);
            m_ySpinner.setEnabled(enabled);
            m_zSpinner.setEnabled(enabled);
        });
        m_xSpinner.setEnabled(false);
        m_ySpinner.setEnabled(false);
        m_zSpinner.setEnabled(false);

        centerPanel.add(posPanel);

        // Component overrides section
        var compPanel = new JPanel(new BorderLayout(4, 4));
        compPanel.setBorder(BorderFactory.createTitledBorder(
                "Component Overrides"));

        m_compListModel = new DefaultListModel<>();
        m_compList = new JList<>(m_compListModel);
        m_compList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        compPanel.add(new JScrollPane(m_compList),
                BorderLayout.CENTER);

        var compButtons = new JPanel(
                new FlowLayout(FlowLayout.LEFT));
        var addCompBtn = new JButton("Add");
        addCompBtn.addActionListener(e -> addComponent());
        var editCompBtn = new JButton("Edit");
        editCompBtn.addActionListener(e -> editComponent());
        var removeCompBtn = new JButton("Remove");
        removeCompBtn.addActionListener(e -> removeComponent());
        compButtons.add(addCompBtn);
        compButtons.add(editCompBtn);
        compButtons.add(removeCompBtn);
        compPanel.add(compButtons, BorderLayout.SOUTH);
        compPanel.setPreferredSize(new Dimension(400, 200));

        centerPanel.add(compPanel);
        add(centerPanel, BorderLayout.CENTER);

        // --- Bottom: OK / Cancel ---
        var buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));
        var okButton = new JButton("OK");
        okButton.addActionListener(e -> onOk());
        var cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            m_result = null;
            dispose();
        });
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Populate from existing
        if (existing != null) {
            m_idField.setText(existing.id());
            for (var comp : existing.components()) {
                if (comp.type() == Position.class) {
                    m_posCheckBox.setSelected(true);
                    m_xSpinner.setEnabled(true);
                    m_ySpinner.setEnabled(true);
                    m_zSpinner.setEnabled(true);
                    var props = comp.properties();
                    setSpinnerValue(m_xSpinner, props.get("x"));
                    setSpinnerValue(m_ySpinner, props.get("y"));
                    setSpinnerValue(m_zSpinner, props.get("z"));
                } else {
                    m_components.add(new ComponentEntry(
                            comp.type().getSimpleName(),
                            new java.util.LinkedHashMap<>(
                                    comp.properties())));
                }
            }
            refreshCompList();
        }

        pack();
        setMinimumSize(new Dimension(450, 400));
        setLocationRelativeTo(owner);
    }

    public MapEntityDefinition getResult() {
        return m_result;
    }

    private void setSpinnerValue(JSpinner spinner, Object val) {
        if (val == null) {
            return;
        }
        if (val instanceof Number n) {
            spinner.setValue(n.intValue());
        } else {
            try {
                spinner.setValue(
                        Integer.parseInt(val.toString()));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void refreshCompList() {
        m_compListModel.clear();
        for (var entry : m_components) {
            if (entry.properties().isEmpty()) {
                m_compListModel.addElement(entry.typeName());
            } else {
                m_compListModel.addElement(
                        entry.typeName() + " "
                                + entry.properties());
            }
        }
    }

    private void addComponent() {
        String typeName = JOptionPane.showInputDialog(this,
                "Component type name (e.g. Speed, Health):",
                "Add Component", JOptionPane.PLAIN_MESSAGE);
        if (typeName == null || typeName.isBlank()) {
            return;
        }
        String propsStr = JOptionPane.showInputDialog(this,
                "Properties (key=value, comma-separated, "
                        + "or leave blank):",
                "Component Properties",
                JOptionPane.PLAIN_MESSAGE);
        var props = parseProperties(
                propsStr != null ? propsStr : "");
        m_components.add(new ComponentEntry(
                typeName.strip(), props));
        refreshCompList();
    }

    private void editComponent() {
        int idx = m_compList.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        var existing = m_components.get(idx);
        String typeName = JOptionPane.showInputDialog(this,
                "Component type name:",
                existing.typeName());
        if (typeName == null || typeName.isBlank()) {
            return;
        }
        String propsStr = JOptionPane.showInputDialog(this,
                "Properties (key=value, comma-separated):",
                formatProperties(existing.properties()));
        var props = parseProperties(
                propsStr != null ? propsStr : "");
        m_components.set(idx, new ComponentEntry(
                typeName.strip(), props));
        refreshCompList();
    }

    private void removeComponent() {
        int idx = m_compList.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        m_components.remove(idx);
        refreshCompList();
    }

    private Map<String, Object> parseProperties(String input) {
        var map = new java.util.LinkedHashMap<String, Object>();
        if (input.isBlank()) {
            return map;
        }
        for (String pair : input.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].strip();
                String val = kv[1].strip();
                map.put(key, parseValue(val));
            }
        }
        return map;
    }

    private Object parseValue(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e2) {
                if ("true".equalsIgnoreCase(val)
                        || "false".equalsIgnoreCase(val)) {
                    return Boolean.parseBoolean(val);
                }
                return val;
            }
        }
    }

    private String formatProperties(Map<String, Object> props) {
        var sb = new StringBuilder();
        boolean first = true;
        for (var entry : props.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    private void onOk() {
        String id = m_idField.getText().strip();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Entity ID cannot be empty.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        var compDefs = new ArrayList<ComponentDefinition>();

        // Add Position if checked
        if (m_posCheckBox.isSelected()) {
            var posDef = new ComponentDefinition(Position.class);
            posDef.setProperty("x",
                    (int) m_xSpinner.getValue());
            posDef.setProperty("y",
                    (int) m_ySpinner.getValue());
            posDef.setProperty("z",
                    (int) m_zSpinner.getValue());
            compDefs.add(posDef);
        }

        // Add other components
        for (var entry : m_components) {
            try {
                Class<?> clazz = resolveComponentType(
                        entry.typeName());
                var compDef = new ComponentDefinition(clazz);
                for (var prop : entry.properties().entrySet()) {
                    compDef.setProperty(
                            prop.getKey(), prop.getValue());
                }
                compDefs.add(compDef);
            } catch (ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this,
                        "Unknown component type: "
                                + entry.typeName(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        m_result = new MapEntityDefinition(id, compDefs);
        dispose();
    }

    private Class<?> resolveComponentType(String typeName)
            throws ClassNotFoundException {
        var packages = List.of(
                "ca.kieve.ssss.component.",
                "ca.kieve.ssss.ai.behavior."
        );
        for (String pkg : packages) {
            try {
                return Class.forName(pkg + typeName);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(typeName);
    }
}
