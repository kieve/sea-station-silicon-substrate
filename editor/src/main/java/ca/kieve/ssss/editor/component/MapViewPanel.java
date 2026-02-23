package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.EditorTheme;
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
import javafx.scene.paint.Color;
import ca.kieve.ssss.editor.util.DialogUtil;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private final SelectedCellOverlay m_selectedCellOverlay;
    private final EditorToolBar m_toolBar;
    private final ToolOptionsPanel m_toolOptionsPanel;
    private final TabPane m_tabPane;
    private final Tab m_blocksTab;
    private final Tab m_entitiesTab;

    private record GridCell(int row, int col) {}

    private record EntityPos(int x, int y, int z) {}

    private static EntityPos getEntityPos(
            MapEntityDefinition entity) {
        for (var comp : entity.components()) {
            if (comp.type() != Position.class) {
                continue;
            }
            Object xVal = comp.properties().get("x");
            Object yVal = comp.properties().get("y");
            Object zVal = comp.properties().get("z");
            if (xVal instanceof Number nx
                    && yVal instanceof Number ny
                    && zVal instanceof Number nz) {
                return new EntityPos(
                        nx.intValue(),
                        ny.intValue(),
                        nz.intValue());
            }
            return null;
        }
        return null;
    }

    private int m_currentZ;
    private String m_selectedBlockName;
    private Integer m_selectedEntityIndex;

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
        m_toolBar = new EditorToolBar();
        m_toolOptionsPanel = new ToolOptionsPanel();

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
            m_selectedEntityIndex = index;
            List<MapEntityDefinition> entities =
                    m_model.getEntities();
            if (index >= entities.size()) {
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
        m_blocksTab = new Tab("Blocks", m_blockPanel);
        m_blocksTab.setClosable(false);

        m_entitiesTab = new Tab(
                "Entities", m_entityPanel);
        m_entitiesTab.setClosable(false);

        m_tabPane = new TabPane(
                m_blocksTab, m_entitiesTab);
        m_tabPane.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {
            if (newTab == m_blocksTab) {
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

        // Floating selection overlay
        m_selectedCellOverlay = new SelectedCellOverlay();
        m_selectedCellOverlay.setOnItemSelected(item -> {
            if (item.type()
                    == SelectedCellOverlay.ItemType.BLOCK) {
                m_selectedEntityIndex = null;
                m_tabPane.getSelectionModel()
                        .select(m_blocksTab);
                m_blockPanel.selectBlock(item.label());
            } else {
                m_tabPane.getSelectionModel()
                        .select(m_entitiesTab);
                m_entityPanel.selectEntity(item.index());
            }
        });

        // Clear selection when switching to PAINT
        m_toolBar.activeToolProperty().addListener(
                (obs, oldTool, newTool) -> {
            if (newTool == EditorToolBar.Tool.PAINT) {
                clearSelection();
            }
            m_toolOptionsPanel.updateForTool(newTool);
        });

        // Center: canvas with floating overlays
        var rightOverlayBox = new VBox(
                4, m_zOverlay, m_zoomOverlay,
                m_selectedCellOverlay);
        rightOverlayBox.setAlignment(Pos.TOP_RIGHT);
        rightOverlayBox.setMaxWidth(Region.USE_PREF_SIZE);
        rightOverlayBox.setMaxHeight(Region.USE_PREF_SIZE);
        rightOverlayBox.setPickOnBounds(false);

        var leftOverlayBox = new VBox(
                4, m_toolOptionsPanel);
        leftOverlayBox.setAlignment(Pos.TOP_LEFT);
        leftOverlayBox.setMaxWidth(Region.USE_PREF_SIZE);
        leftOverlayBox.setMaxHeight(Region.USE_PREF_SIZE);
        leftOverlayBox.setPickOnBounds(false);

        var canvasStack = new StackPane(
                m_panCanvas,
                rightOverlayBox, leftOverlayBox);
        StackPane.setAlignment(
                rightOverlayBox, Pos.TOP_RIGHT);
        StackPane.setMargin(
                rightOverlayBox,
                new Insets(8, 8, 0, 0));
        StackPane.setAlignment(
                leftOverlayBox, Pos.TOP_LEFT);
        StackPane.setMargin(
                leftOverlayBox,
                new Insets(8, 0, 0, 8));

        var rightSplit = new SplitPane(
                m_tabPane, m_componentPanel);
        rightSplit.setOrientation(Orientation.VERTICAL);
        rightSplit.setDividerPositions(0.65);

        var mainSplit = new SplitPane(
                canvasStack, rightSplit);
        mainSplit.setDividerPositions(0.8);

        setLeft(m_toolBar);
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

        var tool = m_toolBar.getActiveTool();

        if (tool == EditorToolBar.Tool.SELECT) {
            if (e.getButton()
                    == MouseButton.PRIMARY) {
                selectAt(e.getX(), e.getY());
                e.consume();
            } else if (e.getButton()
                    == MouseButton.SECONDARY) {
                clearSelection();
                e.consume();
            }
            return;
        }

        if (tool == EditorToolBar.Tool.MOVE) {
            if (e.getButton()
                    == MouseButton.PRIMARY) {
                moveEntityTo(e.getX(), e.getY());
                e.consume();
            } else if (e.getButton()
                    == MouseButton.SECONDARY) {
                clearSelection();
                e.consume();
            }
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

    private GridCell mouseToGrid(
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
        row = m_renderer.visualRowToDataRow(row);
        return new GridCell(row, col);
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
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();

        boolean changed;
        if (m_toolOptionsPanel.isAllLayers()) {
            changed = setCellAllLayers(
                    row, col, m_selectedBlockName);
        } else {
            changed = m_model.setCell(
                    m_currentZ, row, col,
                    m_selectedBlockName);
        }
        if (!changed) {
            return;
        }
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private void eraseAt(
            double mouseX, double mouseY) {
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();

        boolean changed;
        if (m_toolOptionsPanel.isAllLayers()) {
            changed = setCellAllLayers(row, col, null);
        } else {
            changed = m_model.setCell(
                    m_currentZ, row, col, null);
        }
        if (!changed) {
            return;
        }
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private boolean setCellAllLayers(
            int row, int col, String blockName) {
        boolean anyChanged = false;
        for (int z : m_model.getZLevels()) {
            if (m_model.setCell(z, row, col, blockName)) {
                anyChanged = true;
            }
        }
        return anyChanged;
    }

    private void selectAt(
            double mouseX, double mouseY) {
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();
        m_renderer.setSelectedCell(row, col);

        String blockName =
                m_model.getCell(m_currentZ, row, col);

        var entityInfos =
                new ArrayList<SelectedCellOverlay.EntityInfo>();
        var entities = m_model.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            var entity = entities.get(i);
            var pos = getEntityPos(entity);
            if (pos != null
                    && pos.x() == col
                    && pos.y() == row
                    && pos.z() == m_currentZ) {
                entityInfos.add(
                        new SelectedCellOverlay
                                .EntityInfo(
                                i, entity.id()));
            }
        }

        m_selectedCellOverlay.setHeaderText(
                "Cell: (" + col + ", " + row + ")");
        m_selectedCellOverlay.update(
                blockName, entityInfos);
        m_panCanvas.requestRedraw();
    }

    private void moveEntityTo(
            double mouseX, double mouseY) {
        if (m_selectedEntityIndex == null) {
            return;
        }
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();
        if (!m_model.moveEntity(
                m_selectedEntityIndex,
                col, row, m_currentZ)) {
            return;
        }

        m_renderer.loadEntities(
                buildEntityMarkers(m_currentZ));
        m_renderer.setSelectedCell(row, col);

        var entities = m_model.getEntities();
        var entity = entities.get(m_selectedEntityIndex);
        m_componentPanel.showMapEntity(
                entity.id(), entity.components());
        m_panCanvas.requestRedraw();
    }

    private void clearSelection() {
        m_selectedEntityIndex = null;
        m_renderer.setSelectedCell(null, null);
        m_selectedCellOverlay.clear();
        m_panCanvas.requestRedraw();
    }

    private void loadLayer(int zLevel) {
        var cells = m_model.getLayer(zLevel);
        if (!m_renderer.loadLayer(cells)) {
            return;
        }

        clearSelection();
        m_renderer.loadEntities(
                buildEntityMarkers(zLevel));
        m_currentZ = zLevel;
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private List<MapRenderer.EntityMarker>
            buildEntityMarkers(int zLevel) {
        var entities = m_model.getEntities();

        BlockColorResolver colorResolver =
                EditorContext.getInstance()
                        .getColorResolver();
        var markers =
                new ArrayList<MapRenderer.EntityMarker>();
        for (var entity : entities) {
            var pos = getEntityPos(entity);
            if (pos == null || pos.z() != zLevel) {
                continue;
            }

            Color color =
                    colorResolver.resolveWithOverrides(
                            entity.id(),
                            entity.components());
            if (color.equals(Color.WHITE)) {
                color = EditorTheme.ENTITY_MARKER_COLOR;
            }
            markers.add(
                    new MapRenderer.EntityMarker(
                            pos.y(), pos.x(), color));
        }
        return markers;
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
