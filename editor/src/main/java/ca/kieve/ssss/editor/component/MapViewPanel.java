package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.editor.MapSaver;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.ui.PanCanvas;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ca.kieve.ssss.editor.util.DialogUtil;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MapViewPanel extends BorderPane {
    private final EditorMapModel m_model;
    private final MapRenderer m_renderer;
    private final PanCanvas m_panCanvas;
    private final BlockPanel m_blockPanel;
    private final MapEntityPanel m_entityPanel;
    private final ComponentPanel m_componentPanel;
    private final InfoBar m_infoBar;
    private final ZLevelOverlay m_zOverlay;
    private final ZoomOverlay m_zoomOverlay;

    private int m_currentZ;
    private String m_selectedBlockName;

    public MapViewPanel(
            MapDefinition mapDef,
            File mapFile
    ) {
        m_model = EditorMapModel.fromDefinition(
                mapDef, mapFile);

        m_renderer = new MapRenderer();
        m_renderer.updateNameToBpId(
                m_model.buildNameToBpIdMap());

        m_panCanvas = new PanCanvas();
        m_panCanvas.setOnRedraw(this::redraw);

        // Left: tool bar
        var toolBar = new EditorToolBar();

        // Right: block panel, entity panel, component panel
        m_blockPanel = new BlockPanel(m_model);
        m_entityPanel = new MapEntityPanel(m_model);
        m_componentPanel = new ComponentPanel();

        m_blockPanel.setOnSelectionChanged(name -> {
            m_selectedBlockName = name;
            var blockDef = m_model.getBlocks().get(name);
            if (blockDef != null) {
                m_componentPanel.showEntity(
                        blockDef.bpId());
            } else {
                m_componentPanel.clear();
            }
        });
        m_blockPanel.setOnBlocksChanged(
                this::refreshBlockTypes);

        m_entityPanel.setOnSelectionChanged(index -> {
            List<MapEntityDefinition> entities =
                    m_model.getEntities();
            if (entities == null
                    || index >= entities.size()) {
                m_componentPanel.clear();
                return;
            }
            MapEntityDefinition entity =
                    entities.get(index);
            m_componentPanel.showMapEntity(
                    entity.id(), entity.components());
        });

        // Pick a default selected block (first non-air)
        for (var entry : m_model.getBlocks().entrySet()) {
            if (!"air".equals(entry.getValue().bpId())) {
                m_selectedBlockName = entry.getKey();
                m_blockPanel.selectBlock(
                        m_selectedBlockName);
                m_componentPanel.showEntity(
                        entry.getValue().bpId());
                break;
            }
        }

        // TabPane for blocks and entities
        var blocksTab = new Tab("Blocks", m_blockPanel);
        blocksTab.setClosable(false);

        var entitiesTab = new Tab(
                "Entities", m_entityPanel);
        entitiesTab.setClosable(false);

        var tabPane = new TabPane(
                blocksTab, entitiesTab);
        tabPane.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {
            if (newTab == blocksTab) {
                // Re-fire block selection
                String sel =
                        m_blockPanel.getSelectedBlock();
                if (sel != null) {
                    var blockDef =
                            m_model.getBlocks().get(sel);
                    if (blockDef != null) {
                        m_componentPanel.showEntity(
                                blockDef.bpId());
                    } else {
                        m_componentPanel.clear();
                    }
                } else {
                    m_componentPanel.clear();
                }
            } else {
                m_componentPanel.clear();
            }
        });

        // Bottom: info bar
        m_infoBar = new InfoBar();
        m_infoBar.setFileName(mapFile.getName());

        // Floating Z-level overlay
        m_zOverlay = new ZLevelOverlay();
        var zLevels = m_model.getZLevels();
        int defaultZ = zLevels.contains(1)
                ? 1 : zLevels.getFirst();
        m_zOverlay.setZLevels(zLevels, defaultZ);
        m_zOverlay.zLevelProperty().addListener(
                (obs, oldVal, newVal) ->
                        loadLayer(newVal.intValue()));

        // Floating zoom overlay
        m_zoomOverlay = new ZoomOverlay();
        m_zoomOverlay.bindZoom(
                m_panCanvas.zoomProperty());
        m_zoomOverlay.setOnReset(
                m_panCanvas::resetZoom);

        // Center: canvas with floating overlays
        var overlayBox = new VBox(
                4, m_zOverlay, m_zoomOverlay);
        overlayBox.setAlignment(Pos.TOP_RIGHT);
        overlayBox.setMaxWidth(Region.USE_PREF_SIZE);
        overlayBox.setMaxHeight(Region.USE_PREF_SIZE);
        overlayBox.setPickOnBounds(false);

        var canvasStack = new StackPane(
                m_panCanvas, overlayBox);
        StackPane.setAlignment(
                overlayBox, Pos.TOP_RIGHT);
        StackPane.setMargin(
                overlayBox, new Insets(8, 8, 0, 0));

        var rightSplit = new SplitPane(
                tabPane, m_componentPanel);
        rightSplit.setOrientation(Orientation.VERTICAL);
        rightSplit.setDividerPositions(0.35);

        var mainSplit = new SplitPane(
                canvasStack, rightSplit);
        mainSplit.setDividerPositions(0.8);

        setLeft(toolBar);
        setCenter(mainSplit);
        setBottom(m_infoBar);

        setupPainting();
        setupScrollZLevel();
        loadLayer(defaultZ);

        // Center on the map once at startup
        m_panCanvas.centerOn(
                m_renderer.getMapOriginX()
                        + m_renderer.getMapWidth() / 2.0,
                m_renderer.getMapOriginY()
                        + m_renderer.getMapHeight()
                                / 2.0);
    }

    public EditorMapModel getModel() {
        return m_model;
    }

    public int getCurrentZ() {
        return m_currentZ;
    }

    public void save() {
        File file = m_model.getFile();
        if (file == null) {
            return;
        }
        saveAs(file);
    }

    public void saveAs(File file) {
        try {
            MapSaver.save(m_model, file);
            m_infoBar.setFileName(file.getName());
        } catch (IOException ex) {
            var alert = new Alert(Alert.AlertType.ERROR);
            DialogUtil.style(
                    alert, "Failed to Save Map");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    public void refreshBlockTypes() {
        m_renderer.updateNameToBpId(
                m_model.buildNameToBpIdMap());
        m_panCanvas.requestRedraw();
    }

    private void setupScrollZLevel() {
        m_panCanvas.setOnScroll(e -> {
            if (e.isShiftDown()) {
                // Windows converts Shift+ScrollWheel
                // vertical into horizontal, so check
                // both axes.
                double delta = e.getDeltaY() != 0
                        ? e.getDeltaY() : e.getDeltaX();
                if (delta != 0) {
                    int dir = delta > 0 ? 1 : -1;
                    m_panCanvas.zoom(
                            dir, e.getX(), e.getY());
                }
            } else {
                if (e.getDeltaY() > 0) {
                    m_zOverlay.step(1);
                } else if (e.getDeltaY() < 0) {
                    m_zOverlay.step(-1);
                }
            }
            e.consume();
        });
    }

    private void setupPainting() {
        m_panCanvas.addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                this::handlePaint);
        m_panCanvas.addEventFilter(
                MouseEvent.MOUSE_DRAGGED,
                this::handlePaint);
    }

    private void handlePaint(MouseEvent e) {
        if (e.getButton() == MouseButton.MIDDLE) {
            return;
        }

        if (e.getButton() == MouseButton.PRIMARY) {
            paintAt(e.getX(), e.getY());
            e.consume();
        } else if (e.getButton()
                == MouseButton.SECONDARY) {
            eraseAt(e.getX(), e.getY());
            e.consume();
        }
    }

    private void paintAt(
            double mouseX, double mouseY) {
        if (m_selectedBlockName == null) {
            return;
        }
        if (!m_model.getBlocks()
                .containsKey(m_selectedBlockName)) {
            return;
        }
        double zoom = m_panCanvas.getZoom();
        int col = (int) Math.floor(
                (mouseX / zoom
                        + m_panCanvas.getCameraX())
                        / MapRenderer.CELL_SIZE);
        int row = (int) Math.floor(
                (mouseY / zoom
                        + m_panCanvas.getCameraY())
                        / MapRenderer.CELL_SIZE);

        if (!m_model.setCell(
                m_currentZ, row, col,
                m_selectedBlockName)) {
            return;
        }
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private void eraseAt(
            double mouseX, double mouseY) {
        double zoom = m_panCanvas.getZoom();
        int col = (int) Math.floor(
                (mouseX / zoom
                        + m_panCanvas.getCameraX())
                        / MapRenderer.CELL_SIZE);
        int row = (int) Math.floor(
                (mouseY / zoom
                        + m_panCanvas.getCameraY())
                        / MapRenderer.CELL_SIZE);

        if (!m_model.setCell(
                m_currentZ, row, col, null)) {
            return;
        }
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private void loadLayer(int zLevel) {
        var cells = m_model.getLayer(zLevel);
        if (!m_renderer.loadLayer(cells)) {
            return;
        }

        m_currentZ = zLevel;
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private void redraw() {
        var canvas = m_panCanvas.getCanvas();
        m_renderer.render(
                canvas.getGraphicsContext2D(),
                canvas.getWidth(),
                canvas.getHeight(),
                m_panCanvas.getCameraX(),
                m_panCanvas.getCameraY(),
                m_panCanvas.getZoom());
    }
}
