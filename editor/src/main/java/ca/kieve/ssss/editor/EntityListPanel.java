package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.editor.EditorModel.EntityPosition;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;

public class EntityListPanel extends JPanel
        implements MapGridPanel.TileSelectionListener {

    private final EditorModel m_model;
    private final DefaultListModel<String> m_listModel;
    private final JList<String> m_list;
    private Consumer<List<MapEntityDefinition>> m_undoCallback;

    public EntityListPanel(EditorModel model) {
        m_model = model;
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Entities"));
        setPreferredSize(new Dimension(180, 200));

        m_listModel = new DefaultListModel<>();
        m_list = new JList<>(m_listModel);
        m_list.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(m_list), BorderLayout.CENTER);

        var buttonPanel = new JPanel(new GridLayout(1, 3, 4, 0));

        var addButton = new JButton("Add");
        addButton.addActionListener(e -> addEntity());

        var editButton = new JButton("Edit");
        editButton.addActionListener(e -> editEntity());

        var removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeEntity());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        m_list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            onEntityListSelection();
        });

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {}

            @Override
            public void onLayerChanged() {
                refreshList();
            }

            @Override
            public void onBlockSelectionChanged() {}

            @Override
            public void onDirtyChanged() {}

            @Override
            public void onEntitiesChanged() {
                refreshList();
            }

            @Override
            public void onToolChanged() {}
        });
    }

    public void setUndoCallback(
            Consumer<List<MapEntityDefinition>> callback) {
        m_undoCallback = callback;
    }

    public void refreshList() {
        int selected = m_list.getSelectedIndex();
        m_listModel.clear();
        var entities = m_model.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            var entity = entities.get(i);
            EntityPosition pos =
                    m_model.getEntityPosition(entity);
            String label;
            if (pos != null) {
                label = String.format("%s (%d, %d, z%d)",
                        entity.id(), pos.x(), pos.y(), pos.z());
            } else {
                label = entity.id() + " (no pos)";
            }
            m_listModel.addElement(label);
        }
        if (selected >= 0 && selected < m_listModel.size()) {
            m_list.setSelectedIndex(selected);
        }
    }

    @Override
    public void onTileSelected(int col, int row, int z) {
        var indices = m_model.getEntitiesAt(col, row, z);
        if (!indices.isEmpty()) {
            m_list.setSelectedIndex(indices.getFirst());
        }
    }

    @Override
    public void onSelectionCleared() {
        m_list.clearSelection();
    }

    private void onEntityListSelection() {
        int idx = m_list.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        var entity = m_model.getEntities().get(idx);
        EntityPosition pos = m_model.getEntityPosition(entity);
        if (pos == null) {
            return;
        }
        // Don't fire setSelection to avoid recursion;
        // just visually sync the grid
    }

    private List<MapEntityDefinition> snapshotEntities() {
        return List.copyOf(m_model.getEntities());
    }

    private void addEntity() {
        var dialog = new EntityEditDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                null);
        dialog.setVisible(true);
        var result = dialog.getResult();
        if (result == null) {
            return;
        }
        var before = snapshotEntities();
        m_model.addEntity(result);
        notifyUndo(before);
    }

    private void editEntity() {
        int idx = m_list.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        var existing = m_model.getEntities().get(idx);
        var dialog = new EntityEditDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this),
                existing);
        dialog.setVisible(true);
        var result = dialog.getResult();
        if (result == null) {
            return;
        }
        var before = snapshotEntities();
        m_model.updateEntity(idx, result);
        notifyUndo(before);
    }

    private void removeEntity() {
        int idx = m_list.getSelectedIndex();
        if (idx < 0) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove entity '"
                        + m_model.getEntities().get(idx).id()
                        + "'?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        var before = snapshotEntities();
        m_model.removeEntity(idx);
        notifyUndo(before);
    }

    private void notifyUndo(List<MapEntityDefinition> before) {
        if (m_undoCallback != null) {
            m_undoCallback.accept(before);
        }
    }
}
