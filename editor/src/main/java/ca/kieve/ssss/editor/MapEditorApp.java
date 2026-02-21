package ca.kieve.ssss.editor;

import ca.kieve.ssss.editor.EditorModel.CellKey;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapEditorApp {
    private final JFrame m_frame;
    private final EditorModel m_model;
    private final MapFileIO m_fileIO;
    private final MapGridPanel m_gridPanel;
    private final BlockPalettePanel m_palettePanel;
    private final LayerControlPanel m_layerPanel;
    private final SpawnTool m_spawnTool;

    private final List<UndoCommand> m_undoStack = new ArrayList<>();
    private final List<UndoCommand> m_redoStack = new ArrayList<>();
    private HashMap<CellKey, Character> m_snapshotBefore;

    public MapEditorApp() {
        m_model = new EditorModel();
        m_fileIO = new MapFileIO();
        m_gridPanel = new MapGridPanel(m_model);
        m_palettePanel = new BlockPalettePanel(m_model);
        m_layerPanel = new LayerControlPanel(m_model);
        m_spawnTool = new SpawnTool(m_model, m_gridPanel);

        m_frame = new JFrame("Map Editor");
        m_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        m_frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        setupMenuBar();
        setupLayout();
        setupUndoTracking();

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {
            }

            @Override
            public void onLayerChanged() {
            }

            @Override
            public void onBlockSelectionChanged() {
            }

            @Override
            public void onDirtyChanged() {
                updateTitle();
            }
        });

        m_model.newMap();

        m_frame.setSize(1000, 700);
        m_frame.setLocationRelativeTo(null);
        m_frame.setVisible(true);

        SwingUtilities.invokeLater(m_gridPanel::centerOnContent);
    }

    private void setupMenuBar() {
        var menuBar = new JMenuBar();

        var fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        var newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> newMap());

        var openItem = new JMenuItem("Open...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openMap());

        var saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveMap());

        var saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK
                        | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveMapAs());

        var exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> exit());

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        var editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        var undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> undo());

        var redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK
                        | InputEvent.SHIFT_DOWN_MASK));
        redoItem.addActionListener(e -> redo());

        editMenu.add(undoItem);
        editMenu.add(redoItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        m_frame.setJMenuBar(menuBar);
    }

    private void setupLayout() {
        var rightPanel = new JPanel();
        rightPanel.setLayout(
                new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(m_palettePanel);
        rightPanel.add(m_spawnTool);

        var topPanel = new JPanel(new BorderLayout());
        topPanel.add(m_layerPanel, BorderLayout.NORTH);
        topPanel.add(m_gridPanel, BorderLayout.CENTER);

        var splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, topPanel, rightPanel);
        splitPane.setResizeWeight(1.0);

        m_frame.getContentPane().add(
                splitPane, BorderLayout.CENTER);
    }

    private void setupUndoTracking() {
        m_gridPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)
                        || SwingUtilities.isRightMouseButton(e)) {
                    m_snapshotBefore = snapshotCellMap();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if ((SwingUtilities.isLeftMouseButton(e)
                        || SwingUtilities.isRightMouseButton(e))
                        && m_snapshotBefore != null) {
                    var after = snapshotCellMap();
                    if (!m_snapshotBefore.equals(after)) {
                        m_undoStack.add(new UndoCommand(
                                m_model.getActiveLayer(),
                                m_snapshotBefore, after));
                        m_redoStack.clear();
                    }
                    m_snapshotBefore = null;
                }
            }
        });
    }

    private HashMap<CellKey, Character> snapshotCellMap() {
        var cellMap = m_model.getActiveCellMap();
        if (cellMap == null) {
            return new HashMap<>();
        }
        return new HashMap<>(cellMap);
    }

    private void undo() {
        if (m_undoStack.isEmpty()) {
            return;
        }
        var cmd = m_undoStack.removeLast();
        cmd.undo(m_model);
        m_redoStack.add(cmd);
        m_gridPanel.repaint();
    }

    private void redo() {
        if (m_redoStack.isEmpty()) {
            return;
        }
        var cmd = m_redoStack.removeLast();
        cmd.redo(m_model);
        m_undoStack.add(cmd);
        m_gridPanel.repaint();
    }

    private void newMap() {
        if (!confirmDiscardChanges()) {
            return;
        }
        m_fileIO.clearCurrentFile();
        m_model.newMap();
        m_undoStack.clear();
        m_redoStack.clear();
        updateTitle();
        SwingUtilities.invokeLater(m_gridPanel::centerOnContent);
    }

    private void openMap() {
        if (!confirmDiscardChanges()) {
            return;
        }
        var def = m_fileIO.load(m_frame);
        if (def == null) {
            return;
        }
        m_model.fromMapDefinition(def);
        m_palettePanel.refreshList();
        m_layerPanel.refreshCombo();
        m_spawnTool.refreshFields();
        m_undoStack.clear();
        m_redoStack.clear();
        updateTitle();
        SwingUtilities.invokeLater(m_gridPanel::centerOnContent);
    }

    private void saveMap() {
        m_fileIO.save(m_frame, m_model);
        updateTitle();
    }

    private void saveMapAs() {
        m_fileIO.saveAs(m_frame, m_model);
        updateTitle();
    }

    private void exit() {
        if (!confirmDiscardChanges()) {
            return;
        }
        m_frame.dispose();
    }

    private boolean confirmDiscardChanges() {
        if (!m_model.isDirty()) {
            return true;
        }
        int result = javax.swing.JOptionPane.showConfirmDialog(
                m_frame,
                "You have unsaved changes. "
                        + "Save before continuing?",
                "Unsaved Changes",
                javax.swing.JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == javax.swing.JOptionPane.YES_OPTION) {
            return m_fileIO.save(m_frame, m_model);
        }
        return result == javax.swing.JOptionPane.NO_OPTION;
    }

    private void updateTitle() {
        var sb = new StringBuilder("Map Editor");
        var file = m_fileIO.getCurrentFile();
        if (file != null) {
            sb.append(" - ").append(file.getName());
        }
        if (m_model.isDirty()) {
            sb.append(" *");
        }
        m_frame.setTitle(sb.toString());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(MapEditorApp::new);
    }

    private record UndoCommand(
            int layer,
            HashMap<CellKey, Character> before,
            HashMap<CellKey, Character> after) {
        void undo(EditorModel model) {
            model.setActiveLayer(layer);
            var cellMap = model.getActiveCellMap();
            cellMap.clear();
            cellMap.putAll(before);
        }

        void redo(EditorModel model) {
            model.setActiveLayer(layer);
            var cellMap = model.getActiveCellMap();
            cellMap.clear();
            cellMap.putAll(after);
        }
    }
}
