package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MapViewPanel extends BorderPane {
    private static final int CELL_SIZE = 24;
    private static final Color GRID_COLOR = Color.gray(0.85);

    private final BlockColorResolver m_colorResolver;
    private final Map<Character, String> m_charToType = new HashMap<>();
    private final MapDefinition m_mapDef;
    private final Canvas m_canvas;
    private final Label m_dimensionsLabel;

    public MapViewPanel(
            ContentRegistry registry,
            MapDefinition mapDef,
            File mapFile
    ) {
        m_colorResolver = new BlockColorResolver(registry);
        m_mapDef = mapDef;
        m_canvas = new Canvas();

        for (var entry : mapDef.blocks().entrySet()) {
            MapBlockDefinition blockDef = entry.getValue();
            m_charToType.put(blockDef.layoutChar(), blockDef.type());
        }

        var fileLabel = new Label(mapFile.getName());
        fileLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 8 0 4;");

        var zLabel = new Label("Z-Level:");
        zLabel.setStyle("-fx-padding: 0 4 0 8;");

        var zLevels = mapDef.layers().keySet().stream()
                .map(Integer::parseInt)
                .sorted()
                .toList();

        int defaultZ = zLevels.contains(1) ? 1 : zLevels.getFirst();
        var zSpinner = new Spinner<Integer>();
        zSpinner.setValueFactory(new SpinnerValueFactory.ListSpinnerValueFactory<>(
                javafx.collections.FXCollections.observableArrayList(zLevels)));
        zSpinner.getValueFactory().setValue(defaultZ);
        zSpinner.setPrefWidth(80);
        zSpinner.valueProperty().addListener((obs, oldVal, newVal) -> renderLayer(newVal));

        m_dimensionsLabel = new Label();
        m_dimensionsLabel.setStyle("-fx-padding: 0 4 0 8;");

        var toolbar = new HBox(8, fileLabel, zLabel, zSpinner, m_dimensionsLabel);
        toolbar.setStyle("-fx-padding: 8; -fx-alignment: center-left;");
        setTop(toolbar);

        var scrollPane = new ScrollPane(m_canvas);
        scrollPane.setPannable(true);
        setCenter(scrollPane);

        renderLayer(defaultZ);
    }

    private void renderLayer(int zLevel) {
        String layerKey = String.valueOf(zLevel);
        String layerText = m_mapDef.layers().get(layerKey);
        if (layerText == null) {
            return;
        }

        String[] rows = layerText.split("\n");
        int numRows = rows.length;
        int numCols = 0;
        for (String row : rows) {
            numCols = Math.max(numCols, row.length());
        }

        m_dimensionsLabel.setText(numCols + " x " + numRows);

        double canvasWidth = numCols * CELL_SIZE;
        double canvasHeight = numRows * CELL_SIZE;
        m_canvas.setWidth(canvasWidth);
        m_canvas.setHeight(canvasHeight);

        GraphicsContext gc = m_canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        // The map YAML uses Y-down (first row = top), which matches
        // screen coordinates directly.
        for (int row = 0; row < numRows; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch == ' ') {
                    continue;
                }

                String typeId = m_charToType.get(ch);
                Color color;
                if (typeId != null) {
                    color = m_colorResolver.resolve(typeId);
                } else {
                    color = Color.MAGENTA;
                }

                double x = col * CELL_SIZE;
                double y = row * CELL_SIZE;

                gc.setFill(color);
                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                gc.setStroke(GRID_COLOR);
                gc.setLineWidth(0.5);
                gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }
}
