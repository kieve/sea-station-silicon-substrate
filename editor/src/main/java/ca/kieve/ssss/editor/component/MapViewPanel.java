package ca.kieve.ssss.editor.component;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Submap;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.MapLoader;
import ca.kieve.ssss.editor.MapSaver;
import ca.kieve.ssss.editor.handler.EntityOverrideHandler;
import ca.kieve.ssss.editor.handler.MapToolHandler;
import ca.kieve.ssss.editor.model.ComposedWorld;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.model.SparseGrid;
import ca.kieve.ssss.editor.ui.PanCanvas;
import ca.kieve.ssss.editor.ui.fx.EditorButton;
import ca.kieve.ssss.editor.util.DialogUtil;
import ca.kieve.ssss.editor.util.MapPathUtil;
import ca.kieve.ssss.util.Vec3i;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

public class MapViewPanel extends BorderPane {
    private static final int OVERLAY_SPACING = 4;
    private static final int OVERLAY_MARGIN = 8;
    private static final double RIGHT_SPLIT_POS = 0.65;
    private static final double MAIN_SPLIT_POS = 0.8;

    private final EditorMapModel m_model;
    private final MapRenderer m_renderer;
    private final PanCanvas m_panCanvas;
    private final BlockPanel m_blockPanel;
    private final EntityPanel m_entityPanel;
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
    private final Tab m_submapsTab;
    private final SubmapPanel m_submapPanel;
    private final Button m_addOverrideBtn;
    private final EntityOverrideHandler m_overrideHandler;
    private final MapToolHandler m_toolHandler;

    private int m_currentZ;
    private String m_selectedBlockName;
    private Integer m_selectedEntityIndex;
    private Integer m_selectedConnectorIndex;
    private Integer m_selectedSubmapIndex;
    private ComposedWorld m_composedWorld;

    public MapViewPanel(MapDefinition mapDef, File mapFile) {
        m_model = EditorMapModel.fromDefinition(mapDef, mapFile);

        m_renderer = new MapRenderer();
        m_renderer.updateBlocks(m_model.getBlocks());

        m_panCanvas = new PanCanvas();
        m_panCanvas.setOnRedraw(this::redraw);

        // Left: tool bar
        m_toolBar = new EditorToolBar();
        m_toolOptionsPanel = new ToolOptionsPanel();

        // Right: block panel, entity panel, component panel
        m_blockPanel = new BlockPanel(m_model);
        m_entityPanel = new EntityPanel(m_model);
        m_componentPanel = new ComponentPanel();

        m_blockPanel.setOnSelectionChanged(name -> {
            m_componentPanel.commitPendingEdit();
            m_selectedBlockName = name;
            m_selectedConnectorIndex = null;
            m_selectedSubmapIndex = null;
            var blockDef = m_model.getBlocks().get(name);
            if (blockDef != null) {
                m_componentPanel.showEntity(blockDef.bpId());
            } else {
                m_componentPanel.clear();
            }
        });
        m_blockPanel.setOnBlocksChanged(this::refreshBlockTypes);

        m_entityPanel.setOnSelectionChanged(index -> {
            m_componentPanel.commitPendingEdit();
            m_selectedConnectorIndex = null;
            m_selectedSubmapIndex = null;
            m_selectedEntityIndex = index;
            List<EditorEntity> entities = m_model.getEntities();
            if (index >= entities.size()) {
                m_componentPanel.clear();
                setAddOverrideVisible(false);
                return;
            }
            EditorEntity entity = entities.get(index);
            m_componentPanel.showMapEntity(entity.id(), entity.components());
            setAddOverrideVisible(true);
            focusEntity(entity);
        });

        m_entityPanel.setOnEntitiesChanged(() -> {
            m_componentPanel.commitPendingEdit();
            m_selectedEntityIndex = null;
            m_componentPanel.clear();
            setAddOverrideVisible(false);
            m_renderer.loadEntities(buildEntityMarkers(m_currentZ));
            m_panCanvas.requestRedraw();
        });

        m_overrideHandler = new EntityOverrideHandler(m_model);
        m_overrideHandler.setRefresher(this::refreshEntityView);

        m_toolHandler = new MapToolHandler(m_model, m_renderer, m_panCanvas);
        m_toolHandler.setViewUpdater(new MapToolHandler.ViewUpdater() {
            @Override
            public void onCellChanged(int mapCols, int mapRows) {
                m_infoBar.setDimensions(mapCols, mapRows);
            }

            @Override
            public void onCellSelected(
                int row,
                int col,
                String blockName,
                List<SelectedCellOverlay.EntityInfo> entityInfos,
                List<SelectedCellOverlay.ConnectorInfo> connectorInfos
            ) {
                m_selectedCellOverlay.setHeaderText("Cell: (" + col + ", " + row + ")");
                m_selectedCellOverlay.update(blockName, entityInfos, connectorInfos);
            }

            @Override
            public void onEntityMoved(EditorEntity entity, int row, int col) {
                m_renderer.loadEntities(buildEntityMarkers(m_currentZ));
                m_renderer.setSelectedCell(row, col);
                m_componentPanel.showMapEntity(entity.id(), entity.components());
            }

            @Override
            public void onSelectionCleared() {
                m_selectedEntityIndex = null;
                m_selectedCellOverlay.clear();
            }
        });

        m_componentPanel.setOnPropertyEdited(this::routePropertyEdit);
        var overrideCallback = new ComponentPanel.ComponentOverrideCallback() {
            @Override
            public void onOverrideAdded(String componentTypeName) {
                m_overrideHandler.handleOverrideAdded(m_selectedEntityIndex, componentTypeName);
            }

            @Override
            public void onOverrideRemoved(String componentTypeName) {
                m_overrideHandler.handleOverrideRemoved(m_selectedEntityIndex, componentTypeName);
            }

            @Override
            public void onPropertyReverted(String componentTypeName, String propertyName) {
                m_overrideHandler.handlePropertyReverted(
                    m_selectedEntityIndex,
                    componentTypeName,
                    propertyName
                );
            }
        };
        m_componentPanel.setOnComponentOverride(overrideCallback);

        m_addOverrideBtn = new EditorButton("Add Override");
        m_addOverrideBtn.setMaxWidth(Double.MAX_VALUE);
        m_addOverrideBtn.setVisible(false);
        m_addOverrideBtn.setManaged(false);
        m_addOverrideBtn.setOnAction(
            e -> m_overrideHandler.showAddOverrideDialog(m_selectedEntityIndex)
        );

        // Pick a default selected block (first non-air)
        for (var entry : m_model.getBlocks().entrySet()) {
            if ("air".equals(entry.getValue().bpId())) {
                continue;
            }
            m_selectedBlockName = entry.getKey();
            m_blockPanel.selectBlock(m_selectedBlockName);
            m_componentPanel.showEntity(entry.getValue().bpId());
            break;
        }

        // TabPane for blocks, entities, and submaps
        m_blocksTab = new Tab("Blocks", m_blockPanel);
        m_blocksTab.setClosable(false);

        m_entitiesTab = new Tab("Entities", m_entityPanel);
        m_entitiesTab.setClosable(false);

        m_submapPanel = new SubmapPanel(m_model);
        m_submapsTab = new Tab("Submaps", m_submapPanel);
        m_submapsTab.setClosable(false);

        m_tabPane = new TabPane(m_blocksTab, m_entitiesTab, m_submapsTab);
        m_tabPane.getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldTab, newTab) -> onTabChanged(newTab));

        // Bottom: info bar
        m_infoBar = new InfoBar();
        m_infoBar.setFileName(MapPathUtil.getRelativePath(mapFile));

        // Floating Z-level overlay
        m_zOverlay = new ZLevelOverlay();
        // Build the composed world up-front so the dropdown can union
        // local + submap layers from the start.
        m_composedWorld = buildComposedWorld();
        var zLevels = allZLevels();
        int defaultZ = zLevels.contains(1) ? 1 : zLevels.getFirst();
        m_zOverlay.setZLevels(zLevels, defaultZ);
        m_zOverlay.setOnZLevelRequested(this::setZLevel);
        m_zOverlay.setOnAddLayerRequested(this::addZLayer);

        // Hook the SubmapPanel callbacks now that all dependencies
        // (renderer, z-overlay, canvas) are initialized.
        m_submapPanel.setOnChanged(this::refreshAfterSubmapChange);
        m_submapPanel.setOnConnectorSelected(idx -> selectConnector(idx));
        m_submapPanel.setOnSubmapSelected(idx -> selectSubmap(idx));

        // Floating zoom overlay
        m_zoomOverlay = new ZoomOverlay();
        m_zoomOverlay.bindZoom(m_panCanvas.zoomProperty());
        m_zoomOverlay.setOnReset(m_panCanvas::resetZoom);

        // Floating selection overlay
        m_selectedCellOverlay = new SelectedCellOverlay();
        m_selectedCellOverlay.setOnItemSelected(item -> {
            switch (item.type()) {
            case ENTITY -> {
                m_tabPane.getSelectionModel().select(m_entitiesTab);
                m_entityPanel.selectEntity(item.index());
            }
            case CONNECTOR -> {
                m_tabPane.getSelectionModel().select(m_submapsTab);
                m_submapPanel.selectConnector(item.index());
            }
            case BLOCK -> {
                clearSectionSelection();
                m_tabPane.getSelectionModel().select(m_blocksTab);
                m_blockPanel.selectBlock(item.label());
            }
            }
        });

        // Clear selection when switching to PAINT
        m_toolBar.activeToolProperty().addListener(
            (obs, oldTool, newTool) -> onToolChanged(newTool)
        );

        // Composed-mode toggle from the toolbar
        m_toolBar.composedModeProperty().addListener(
            (obs, oldVal, newVal) -> setComposedMode(newVal)
        );

        // Center: canvas with floating overlays
        var rightOverlayBox = new VBox(
            OVERLAY_SPACING,
            m_zOverlay,
            m_zoomOverlay,
            m_selectedCellOverlay
        );
        rightOverlayBox.setAlignment(Pos.TOP_RIGHT);
        rightOverlayBox.setMaxWidth(Region.USE_PREF_SIZE);
        rightOverlayBox.setMaxHeight(Region.USE_PREF_SIZE);
        rightOverlayBox.setPickOnBounds(false);

        var leftOverlayBox = new VBox(OVERLAY_SPACING, m_toolOptionsPanel);
        leftOverlayBox.setAlignment(Pos.TOP_LEFT);
        leftOverlayBox.setMaxWidth(Region.USE_PREF_SIZE);
        leftOverlayBox.setMaxHeight(Region.USE_PREF_SIZE);
        leftOverlayBox.setPickOnBounds(false);

        var canvasStack = new StackPane(m_panCanvas, rightOverlayBox, leftOverlayBox);
        StackPane.setAlignment(rightOverlayBox, Pos.TOP_RIGHT);
        StackPane.setMargin(rightOverlayBox, new Insets(OVERLAY_MARGIN, OVERLAY_MARGIN, 0, 0));
        StackPane.setAlignment(leftOverlayBox, Pos.TOP_LEFT);
        StackPane.setMargin(leftOverlayBox, new Insets(OVERLAY_MARGIN, 0, 0, OVERLAY_MARGIN));

        var componentBox = new VBox(m_componentPanel, m_addOverrideBtn);
        VBox.setVgrow(m_componentPanel, Priority.ALWAYS);

        var rightSplit = new SplitPane(m_tabPane, componentBox);
        rightSplit.setOrientation(Orientation.VERTICAL);
        rightSplit.setDividerPositions(RIGHT_SPLIT_POS);

        var mainSplit = new SplitPane(canvasStack, rightSplit);
        mainSplit.setDividerPositions(MAIN_SPLIT_POS);

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
                    / 2.0
        );
    }

    public EditorMapModel getModel() {
        return m_model;
    }

    /**
     * Routes a SubmapPanel connector selection: tracks which inline
     * entity is active so property edits route correctly, displays its
     * components in the {@link ComponentPanel}, and focuses the canvas
     * on the connector's cell.
     */
    private void selectConnector(int index) {
        if (index < 0 || index >= m_model.getConnectors().size()) {
            return;
        }
        m_selectedEntityIndex = null;
        m_selectedSubmapIndex = null;
        m_selectedConnectorIndex = index;
        EditorEntity c = m_model.getConnectors().get(index);
        m_componentPanel.showInlineEntity(c.id(), c.components());
        setAddOverrideVisible(false);
        focusOnInlineEntity(c);
    }

    private void selectSubmap(int index) {
        if (index < 0 || index >= m_model.getSubmaps().size()) {
            return;
        }
        m_selectedEntityIndex = null;
        m_selectedConnectorIndex = null;
        m_selectedSubmapIndex = index;
        EditorEntity s = m_model.getSubmaps().get(index);
        m_componentPanel.showInlineEntity(s.id(), s.components());
        setAddOverrideVisible(false);
        focusOnInlineEntity(s);
    }

    private void clearSectionSelection() {
        m_selectedEntityIndex = null;
        m_selectedConnectorIndex = null;
        m_selectedSubmapIndex = null;
    }

    private void focusOnInlineEntity(EditorEntity entity) {
        var pos = entity.getEntityPos();
        if (pos == null) {
            return;
        }
        if (pos.z() != m_currentZ && allZLevels().contains(pos.z())) {
            setZLevel(pos.z());
        }
        m_renderer.setSelectedCell(pos.y(), pos.x());
        m_panCanvas.requestRedraw();
    }

    /**
     * Returns whichever {@link EditorEntity} the move tool should target,
     * given the current selection. Order of precedence: connector,
     * submap, entity. Returns {@code null} if no movable target is
     * selected.
     */
    private EditorEntity currentMoveTarget() {
        if (m_selectedConnectorIndex != null) {
            int idx = m_selectedConnectorIndex;
            var list = m_model.getConnectors();
            return idx >= 0 && idx < list.size() ? list.get(idx) : null;
        }
        if (m_selectedSubmapIndex != null) {
            int idx = m_selectedSubmapIndex;
            var list = m_model.getSubmaps();
            return idx >= 0 && idx < list.size() ? list.get(idx) : null;
        }
        if (m_selectedEntityIndex != null) {
            int idx = m_selectedEntityIndex;
            var list = m_model.getEntities();
            return idx >= 0 && idx < list.size() ? list.get(idx) : null;
        }
        return null;
    }

    /**
     * After moving a connector or submap, refresh the inline view + all
     * overlays. For regular entities, the existing
     * {@link MapToolHandler.ViewUpdater#onEntityMoved} hook already
     * handles redraw.
     */
    private void onMoveCompleted(EditorEntity target) {
        if (m_selectedConnectorIndex == null && m_selectedSubmapIndex == null) {
            return;
        }
        m_componentPanel.showInlineEntity(target.id(), target.components());
        m_submapPanel.refreshLists();
        refreshAfterSubmapChange();
    }

    private void refreshAfterSubmapChange() {
        m_composedWorld = buildComposedWorld();
        m_renderer.setSubmapOverlay(buildSubmapOverlay());
        m_renderer.setSubmapCells(submapCellsAt(m_currentZ));
        m_renderer.setSubmapEntities(submapEntityMarkersAt(m_currentZ));
        m_renderer.setConnectorMarkers(buildConnectorMarkers(m_currentZ));
        m_renderer.setSubmapConnectorMarkers(buildSubmapConnectorMarkers(m_currentZ));
        if (m_renderer.isComposedMode()) {
            m_renderer.setComposedWorld(m_composedWorld, m_currentZ);
        }
        m_zOverlay.setZLevels(allZLevels(), m_currentZ);
        m_panCanvas.requestRedraw();
    }

    /**
     * Dispatches a {@link ComponentPanel} property edit to the
     * appropriate target list based on what's currently selected.
     * Entities go through the {@link EntityOverrideHandler}; connectors
     * and submaps are simple in-place property updates (no blueprint
     * merging, no override status).
     */
    private void routePropertyEdit(String componentType, String propertyName, String newValue) {
        if (m_selectedConnectorIndex != null) {
            applyInlineEdit(
                m_model.getConnectors(),
                m_selectedConnectorIndex,
                componentType,
                propertyName,
                newValue,
                SubmapPanel.SectionKind.CONNECTOR
            );
            return;
        }
        if (m_selectedSubmapIndex != null) {
            applyInlineEdit(
                m_model.getSubmaps(),
                m_selectedSubmapIndex,
                componentType,
                propertyName,
                newValue,
                SubmapPanel.SectionKind.SUBMAP
            );
            return;
        }
        m_overrideHandler.onPropertyEdited(
            m_selectedEntityIndex,
            componentType,
            propertyName,
            newValue
        );
    }

    private void applyInlineEdit(
        List<EditorEntity> list,
        int index,
        String componentType,
        String propertyName,
        String newValue,
        SubmapPanel.SectionKind kind
    ) {
        if (index < 0 || index >= list.size()) {
            return;
        }
        EditorEntity entity = list.get(index);

        if (ComponentPanel.INLINE_ID_MARKER.equals(componentType)) {
            // The id row is a virtual property on the entity itself,
            // not on any component. setId is a no-op for empty input
            // (an id is required), so trim and bail out if blank.
            String trimmed = newValue == null ? "" : newValue.trim();
            if (trimmed.isEmpty() || trimmed.equals(entity.id())) {
                return;
            }
            entity.setId(trimmed);
        } else {
            ComponentDefinition comp = null;
            for (ComponentDefinition c : entity.components()) {
                if (c.type().getSimpleName().equals(componentType)) {
                    comp = c;
                    break;
                }
            }
            if (comp == null) {
                return;
            }
            Object oldValue = comp.properties().get(propertyName);
            Object parsed = EntityOverrideHandler.parseValue(newValue, oldValue);
            comp.setProperty(propertyName, parsed);
        }
        m_model.markModified();

        m_componentPanel.showInlineEntity(entity.id(), entity.components());
        // Refresh all dependent overlays — the edit may have moved the
        // connector/submap, changed direction, etc.
        m_submapPanel.refreshLists();
        m_composedWorld = buildComposedWorld();
        m_renderer.setSubmapOverlay(buildSubmapOverlay());
        m_renderer.setSubmapCells(submapCellsAt(m_currentZ));
        m_renderer.setSubmapEntities(submapEntityMarkersAt(m_currentZ));
        m_renderer.setConnectorMarkers(buildConnectorMarkers(m_currentZ));
        m_renderer.setSubmapConnectorMarkers(buildSubmapConnectorMarkers(m_currentZ));
        if (m_renderer.isComposedMode()) {
            m_renderer.setComposedWorld(m_composedWorld, m_currentZ);
        }
        m_zOverlay.setZLevels(allZLevels(), m_currentZ);
        m_panCanvas.requestRedraw();
    }

    public void setOnOpenSubmap(Consumer<File> handler) {
        m_submapPanel.setOnOpenReferenced(handler);
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
            m_infoBar.setFileName(MapPathUtil.getRelativePath(file));
        } catch (IOException ex) {
            var alert = new Alert(Alert.AlertType.ERROR);
            DialogUtil.style(alert, "Failed to Save Map");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private void onToolChanged(EditorToolBar.Tool newTool) {
        if (newTool == EditorToolBar.Tool.PAINT) {
            m_toolHandler.clearSelection();
        }
        m_toolOptionsPanel.updateForTool(newTool);
    }

    private void onTabChanged(Tab newTab) {
        if (newTab != m_blocksTab) {
            setAddOverrideVisible(m_selectedEntityIndex != null);
            m_componentPanel.clear();
            return;
        }

        setAddOverrideVisible(false);
        String sel = m_blockPanel.getSelectedBlock();
        if (sel == null) {
            m_componentPanel.clear();
            return;
        }

        var blockDef = m_model.getBlocks().get(sel);
        if (blockDef == null) {
            m_componentPanel.clear();
            return;
        }

        m_componentPanel.showEntity(blockDef.bpId());
    }

    public void refreshBlockTypes() {
        m_renderer.updateBlocks(m_model.getBlocks());
        m_panCanvas.requestRedraw();
    }

    private void setupScrollZLevel() {
        m_panCanvas.setOnScroll(e -> {
            if (e.isShiftDown()) {
                // Windows converts Shift+ScrollWheel
                // vertical into horizontal, so check
                // both axes.
                double delta = e.getDeltaY() != 0
                    ? e.getDeltaY()
                    : e.getDeltaX();
                if (delta != 0) {
                    int dir = delta > 0 ? 1 : -1;
                    m_panCanvas.zoom(dir, e.getX(), e.getY());
                }
            } else {
                if (e.getDeltaY() > 0) {
                    stepZ(1);
                } else if (e.getDeltaY() < 0) {
                    stepZ(-1);
                }
            }
            e.consume();
        });
    }

    private void setupPainting() {
        m_panCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handlePaint);
        m_panCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handlePaint);
    }

    private void handlePaint(MouseEvent e) {
        if (e.getButton() == MouseButton.MIDDLE) {
            return;
        }
        if (m_renderer.isComposedMode()) {
            return;
        }

        var tool = m_toolBar.getActiveTool();

        if (tool == EditorToolBar.Tool.SELECT) {
            if (e.getButton() == MouseButton.PRIMARY) {
                m_toolHandler.selectAt(e.getX(), e.getY(), m_currentZ);
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                m_toolHandler.clearSelection();
                e.consume();
            }
            return;
        }

        if (tool == EditorToolBar.Tool.MOVE) {
            if (e.getButton() == MouseButton.PRIMARY) {
                EditorEntity target = currentMoveTarget();
                if (target != null) {
                    m_toolHandler.moveTargetTo(target, e.getX(), e.getY(), m_currentZ);
                    onMoveCompleted(target);
                }
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                m_toolHandler.clearSelection();
                e.consume();
            }
            return;
        }

        boolean allLayers = m_toolOptionsPanel.isAllLayers();
        if (e.getButton() == MouseButton.PRIMARY) {
            m_toolHandler.paintAt(e.getX(), e.getY(), m_selectedBlockName, m_currentZ, allLayers);
            e.consume();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            m_toolHandler.eraseAt(e.getX(), e.getY(), m_currentZ, allLayers);
            e.consume();
        }
    }

    private void setAddOverrideVisible(boolean visible) {
        m_addOverrideBtn.setVisible(visible);
        m_addOverrideBtn.setManaged(visible);
    }

    private void refreshEntityView(EditorEntity entity) {
        m_renderer.loadEntities(buildEntityMarkers(m_currentZ));
        m_componentPanel.showMapEntity(entity.id(), entity.components());
        m_entityPanel.refreshCells();
        m_panCanvas.requestRedraw();
    }

    private void focusEntity(EditorEntity entity) {
        var pos = entity.getEntityPos();
        if (pos == null) {
            return;
        }

        // Switch Z-level if needed
        if (pos.z() != m_currentZ) {
            var zLevels = m_model.getZLevels();
            if (!zLevels.contains(pos.z())) {
                return;
            }
            setZLevel(pos.z());
        }

        int row = pos.y();
        int col = pos.x();
        m_renderer.setSelectedCell(row, col);

        String blockName = m_model.getCell(m_currentZ, row, col);
        var entityInfos = new ArrayList<SelectedCellOverlay.EntityInfo>();
        var entities = m_model.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            var e = entities.get(i);
            var ePos = e.getEntityPos();
            if (ePos != null
                && ePos.x() == col
                && ePos.y() == row
                && ePos.z() == m_currentZ) {
                entityInfos.add(new SelectedCellOverlay.EntityInfo(i, e.id()));
            }
        }
        var connectorInfos = new ArrayList<SelectedCellOverlay.ConnectorInfo>();
        var connectors = m_model.getConnectors();
        for (int i = 0; i < connectors.size(); i++) {
            var c = connectors.get(i);
            var cPos = c.getEntityPos();
            if (cPos != null
                && cPos.x() == col
                && cPos.y() == row
                && cPos.z() == m_currentZ) {
                connectorInfos.add(new SelectedCellOverlay.ConnectorInfo(i, c.id()));
            }
        }

        m_selectedCellOverlay.setHeaderText("Cell: (" + col + ", " + row + ")");
        m_selectedCellOverlay.update(blockName, entityInfos, connectorInfos);
        if (m_selectedEntityIndex != null) {
            m_selectedCellOverlay.selectEntity(m_selectedEntityIndex);
        }

        // Center map on the entity's cell
        double worldX = col * MapRenderer.CELL_SIZE
            + MapRenderer.CELL_SIZE / 2.0;
        double worldY = -row * MapRenderer.CELL_SIZE
            + MapRenderer.CELL_SIZE / 2.0;
        m_panCanvas.centerOn(worldX, worldY);
        m_panCanvas.requestRedraw();
    }

    private void addZLayer() {
        int newZ = m_model.addZLayer();
        m_zOverlay.setZLevels(allZLevels(), newZ);
        loadLayer(newZ);
    }

    private void setZLevel(int z) {
        loadLayer(z);
        m_zOverlay.displayZLevel(z);
    }

    private void stepZ(int direction) {
        var zLevels = allZLevels();
        if (zLevels.isEmpty()) {
            return;
        }
        int idx = zLevels.indexOf(m_currentZ);
        if (idx < 0) {
            setZLevel(zLevels.getFirst());
            return;
        }
        int next = (idx + direction + zLevels.size())
            % zLevels.size();
        setZLevel(zLevels.get(next));
    }

    private void loadLayer(int zLevel) {
        var cells = m_model.getLayer(zLevel);
        if (cells == null) {
            // No local layer at this z — show an empty editable canvas
            // so submap-only z-levels still render via the ghost overlay.
            cells = new SparseGrid();
        }
        if (!m_renderer.loadLayer(cells)) {
            return;
        }

        // Clear visual cell selection but preserve entity
        // selection so entities can be moved between layers.
        m_renderer.setSelectedCell(null, null);
        m_selectedCellOverlay.clear();

        m_composedWorld = buildComposedWorld();
        m_renderer.loadEntities(buildEntityMarkers(zLevel));
        m_renderer.setSubmapCells(submapCellsAt(zLevel));
        m_renderer.setSubmapEntities(submapEntityMarkersAt(zLevel));
        m_renderer.setSubmapOverlay(buildSubmapOverlay());
        m_renderer.setConnectorMarkers(buildConnectorMarkers(zLevel));
        if (m_renderer.isComposedMode()) {
            m_renderer.setComposedWorld(m_composedWorld, zLevel);
        }
        m_currentZ = zLevel;
        m_infoBar.setDimensions(m_renderer.getMapCols(), m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    public ComposedWorld buildComposedWorld() {
        MapDefinition def = m_model.toDefinition();
        return ComposedWorld.flatten(def, m_model.getFile());
    }

    /**
     * Union of local layers and every submap's declared z-levels (translated
     * into the parent's world Z). Ascending order. Drives the Z-level
     * dropdown so a composition-only file still shows its submaps' layers.
     */
    private List<Integer> allZLevels() {
        var set = new TreeSet<>(m_model.getZLevels());
        if (m_composedWorld != null) {
            set.addAll(m_composedWorld.allDeclaredZLevels());
        }
        if (set.isEmpty()) {
            set.add(0);
        }
        return List.copyOf(set);
    }

    private List<ComposedWorld.Cell> submapCellsAt(int z) {
        if (m_composedWorld == null) {
            return List.of();
        }
        var out = new ArrayList<ComposedWorld.Cell>();
        for (ComposedWorld.Cell cell : m_composedWorld.cellsAt(z)) {
            if (!ComposedWorld.ROOT_REGION_ID.equals(cell.regionId())) {
                out.add(cell);
            }
        }
        return out;
    }

    private List<MapRenderer.EntityMarker> submapEntityMarkersAt(int z) {
        if (m_composedWorld == null) {
            return List.of();
        }
        BlockColorResolver colorResolver = EditorContext.getInstance().getColorResolver();
        var out = new ArrayList<MapRenderer.EntityMarker>();
        for (ComposedWorld.Entity entity : m_composedWorld.entitiesAt(z)) {
            if (ComposedWorld.ROOT_REGION_ID.equals(entity.regionId())) {
                continue;
            }
            Color color = colorResolver.resolveWithOverrides(entity.id(), entity.components());
            if (color.equals(Color.WHITE)) {
                color = EditorTheme.ENTITY_MARKER_COLOR;
            }
            out.add(new MapRenderer.EntityMarker(entity.row(), entity.col(), color));
        }
        return out;
    }

    public void setComposedMode(boolean enabled) {
        m_renderer.setComposedMode(enabled);
        if (enabled) {
            m_renderer.setComposedWorld(m_composedWorld, m_currentZ);
        }
        m_panCanvas.requestRedraw();
    }

    public boolean isComposedMode() {
        return m_renderer.isComposedMode();
    }

    public List<MapRenderer.SubmapGhost> buildSubmapOverlay() {
        var ghosts = new ArrayList<MapRenderer.SubmapGhost>();
        for (EditorEntity submap : m_model.getSubmaps()) {
            MapRenderer.SubmapGhost ghost = buildGhostFor(submap);
            if (ghost != null) {
                ghosts.add(ghost);
            }
        }
        return ghosts;
    }

    private MapRenderer.SubmapGhost buildGhostFor(EditorEntity submap) {
        String refStr = readSubmapField(submap, "ref");
        if (refStr == null) {
            return null;
        }
        File ref = MapPathUtil.resolveSubmapRef(m_model.getFile(), refStr);
        if (ref == null || !ref.isFile()) {
            return null;
        }
        MapDefinition childDef;
        try {
            childDef = MapLoader.load(ref);
        } catch (IOException ex) {
            return null;
        }
        Vec3i bounds = computeBounds(childDef);
        if (bounds.x == 0 || bounds.y == 0) {
            return null;
        }
        Vec3i offset = resolveOffset(submap, childDef);
        if (offset == null) {
            return null;
        }
        String label = submap.id() != null ? submap.id() : refStr;
        return new MapRenderer.SubmapGhost(label, offset.y, offset.x, bounds.y, bounds.x);
    }

    private Vec3i resolveOffset(EditorEntity submap, MapDefinition childDef) {
        Vec3i posOffset = readVec3i(submap);
        if (posOffset != null) {
            return posOffset;
        }
        String localId = readSubmapField(submap, "localConnector");
        String remoteId = readSubmapField(submap, "remoteConnector");
        if (localId == null || remoteId == null) {
            return null;
        }
        var parentConnectorDefs = m_model.getConnectors().stream()
            .map(EditorEntity::toDefinition)
            .toList();
        return ComposedWorld.computeConnectorOffset(
            parentConnectorDefs,
            childDef.connectors(),
            localId,
            remoteId
        );
    }

    private static String readSubmapField(EditorEntity entity, String fieldName) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() != Submap.class) {
                continue;
            }
            Object value = comp.properties().get(fieldName);
            return value == null ? null : value.toString();
        }
        return null;
    }

    private static Vec3i readVec3i(EditorEntity entity) {
        var pos = entity.getEntityPos();
        return pos == null ? null : new Vec3i(pos.x(), pos.y(), pos.z());
    }

    private static Vec3i computeBounds(MapDefinition def) {
        int width = 0;
        int height = 0;
        int depth = def.layers().size();
        for (String layerData : def.layers().values()) {
            String[] lines = layerData.split("\n");
            height = Math.max(height, lines.length);
            for (String line : lines) {
                width = Math.max(width, line.length());
            }
        }
        return new Vec3i(width, height, depth);
    }

    public List<MapRenderer.ConnectorMarker> buildConnectorMarkers(int zLevel) {
        var markers = new ArrayList<MapRenderer.ConnectorMarker>();
        for (EditorEntity c : m_model.getConnectors()) {
            var pos = c.getEntityPos();
            if (pos == null || pos.z() != zLevel) {
                continue;
            }
            markers.add(new MapRenderer.ConnectorMarker(c.id(), pos.y(), pos.x(), pos.z()));
        }
        return markers;
    }

    /**
     * Connectors owned by submaps (non-root regions), translated into
     * world coords by each region's offset and filtered to the current
     * z. Drawn dimmed by the renderer so a parent/child connector pair
     * lined up around an edge is visible at a glance.
     */
    public List<MapRenderer.ConnectorMarker> buildSubmapConnectorMarkers(int zLevel) {
        if (m_composedWorld == null) {
            return List.of();
        }
        var out = new ArrayList<MapRenderer.ConnectorMarker>();
        for (ComposedWorld.RegionInfo region : m_composedWorld.regions()) {
            if (ComposedWorld.ROOT_REGION_ID.equals(region.id())) {
                continue;
            }
            Vec3i offset = region.offset();
            for (MapEntityDefinition conn : region.connectors()) {
                Vec3i pos = Position.readFromDefinition(conn);
                if (pos == null) {
                    continue;
                }
                int worldZ = pos.z + offset.z;
                if (worldZ != zLevel) {
                    continue;
                }
                out.add(
                    new MapRenderer.ConnectorMarker(
                        conn.id(),
                        pos.y + offset.y,
                        pos.x + offset.x,
                        worldZ
                    )
                );
            }
        }
        return out;
    }

    private List<MapRenderer.EntityMarker> buildEntityMarkers(int zLevel) {
        var entities = m_model.getEntities();

        BlockColorResolver colorResolver = EditorContext.getInstance()
            .getColorResolver();
        var markers = new ArrayList<MapRenderer.EntityMarker>();
        for (EditorEntity entity : entities) {
            var pos = entity.getEntityPos();
            if (pos == null || pos.z() != zLevel) {
                continue;
            }

            Color color = colorResolver.resolveWithOverrides(entity.id(), entity.components());
            if (color.equals(Color.WHITE)) {
                color = EditorTheme.ENTITY_MARKER_COLOR;
            }
            markers.add(new MapRenderer.EntityMarker(pos.y(), pos.x(), color));
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
            m_panCanvas.getZoom()
        );
    }
}
