package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.BlockGlyphResolver;
import ca.kieve.ssss.editor.MapSaver;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.ui.PanCanvas;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;

public class MapViewPanel extends BorderPane {
    private final EditorMapModel m_model;
    private final MapRenderer m_renderer;
    private final PanCanvas m_panCanvas;
    private final BlockPanel m_blockPanel;
    private final InfoBar m_infoBar;
    private final ZLevelOverlay m_zOverlay;

    private int m_currentZ;
    private String m_selectedBlockName;

    public MapViewPanel(
            ContentRegistry registry,
            MapDefinition mapDef,
            File mapFile
    ) {
        m_model = EditorMapModel.fromDefinition(mapDef, mapFile);

        var colorResolver = new BlockColorResolver(registry);
        m_renderer = new MapRenderer(
                colorResolver,
                new BlockGlyphResolver(registry));
        m_renderer.updateCharToType(m_model.buildCharToTypeMap());

        m_panCanvas = new PanCanvas();
        m_panCanvas.setOnRedraw(this::redraw);

        // Left: tool bar
        var toolBar = new EditorToolBar();

        // Right: block panel
        m_blockPanel = new BlockPanel(
                m_model, registry, colorResolver);
        m_blockPanel.setOnSelectionChanged(name -> {
            m_selectedBlockName = name;
        });

        // Pick a default selected block (first non-air)
        for (var entry : m_model.getBlocks().entrySet()) {
            if (!"air".equals(entry.getValue().type())) {
                m_selectedBlockName = entry.getKey();
                m_blockPanel.selectBlock(m_selectedBlockName);
                break;
            }
        }

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

        // Center: canvas with floating overlay
        var canvasStack = new StackPane(m_panCanvas, m_zOverlay);
        StackPane.setAlignment(
                m_zOverlay, Pos.TOP_RIGHT);
        StackPane.setMargin(
                m_zOverlay, new Insets(8, 8, 0, 0));

        setLeft(toolBar);
        setCenter(canvasStack);
        setRight(m_blockPanel);
        setBottom(m_infoBar);

        setupPainting();
        setupScrollZLevel();
        loadLayer(defaultZ);

        // Center on the map once at startup
        m_panCanvas.centerOn(
                m_renderer.getMapOriginX()
                        + m_renderer.getMapWidth() / 2.0,
                m_renderer.getMapOriginY()
                        + m_renderer.getMapHeight() / 2.0);
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
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save map");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    public void refreshCharToType() {
        m_renderer.updateCharToType(m_model.buildCharToTypeMap());
        m_panCanvas.requestRedraw();
    }

    private void setupScrollZLevel() {
        m_panCanvas.setOnScroll(e -> {
            if (e.getDeltaY() > 0) {
                m_zOverlay.step(1);
            } else if (e.getDeltaY() < 0) {
                m_zOverlay.step(-1);
            }
            e.consume();
        });
    }

    private void setupPainting() {
        m_panCanvas.addEventFilter(
                MouseEvent.MOUSE_PRESSED, this::handlePaint);
        m_panCanvas.addEventFilter(
                MouseEvent.MOUSE_DRAGGED, this::handlePaint);
    }

    private void handlePaint(MouseEvent e) {
        if (e.getButton() == MouseButton.MIDDLE) {
            return;
        }

        if (e.getButton() == MouseButton.PRIMARY) {
            paintAt(e.getX(), e.getY());
            e.consume();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            eraseAt(e.getX(), e.getY());
            e.consume();
        }
    }

    private void paintAt(double mouseX, double mouseY) {
        if (m_selectedBlockName == null) {
            return;
        }
        MapBlockDefinition block =
                m_model.getBlocks().get(m_selectedBlockName);
        if (block == null) {
            return;
        }
        int col = (int) Math.floor(
                (mouseX + m_panCanvas.getCameraX())
                        / MapRenderer.CELL_SIZE);
        int row = (int) Math.floor(
                (mouseY + m_panCanvas.getCameraY())
                        / MapRenderer.CELL_SIZE);

        if (!m_model.setCell(
                m_currentZ, row, col, block.layoutChar())) {
            return;
        }
        m_infoBar.setDimensions(
                m_renderer.getMapCols(),
                m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    private void eraseAt(double mouseX, double mouseY) {
        char airChar = '.';
        for (var entry : m_model.getBlocks().entrySet()) {
            if ("air".equals(entry.getValue().type())) {
                airChar = entry.getValue().layoutChar();
                break;
            }
        }

        int col = (int) Math.floor(
                (mouseX + m_panCanvas.getCameraX())
                        / MapRenderer.CELL_SIZE);
        int row = (int) Math.floor(
                (mouseY + m_panCanvas.getCameraY())
                        / MapRenderer.CELL_SIZE);

        if (!m_model.setCell(
                m_currentZ, row, col, airChar)) {
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
                m_panCanvas.getCameraY());
    }
}
