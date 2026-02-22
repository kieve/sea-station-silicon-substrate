package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.ui.PanCanvas;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.File;

public class MapViewPanel extends BorderPane {
    private final MapDefinition m_mapDef;
    private final MapRenderer m_renderer;
    private final PanCanvas m_panCanvas;
    private final Label m_dimensionsLabel;

    public MapViewPanel(
            ContentRegistry registry,
            MapDefinition mapDef,
            File mapFile
    ) {
        m_mapDef = mapDef;
        m_renderer = new MapRenderer(
                new BlockColorResolver(registry), mapDef);
        m_panCanvas = new PanCanvas();
        m_panCanvas.setOnRedraw(this::redraw);

        var fileLabel = new Label(mapFile.getName());
        fileLabel.setStyle(
                "-fx-font-weight: bold; -fx-padding: 0 8 0 4;");

        var zLabel = new Label("Z-Level:");
        zLabel.setStyle("-fx-padding: 0 4 0 8;");

        var zLevels = mapDef.layers().keySet().stream()
                .map(Integer::parseInt)
                .sorted()
                .toList();

        int defaultZ = zLevels.contains(1) ? 1 : zLevels.getFirst();
        var zSpinner = new Spinner<Integer>();
        zSpinner.setValueFactory(
                new SpinnerValueFactory.ListSpinnerValueFactory<>(
                        javafx.collections.FXCollections
                                .observableArrayList(zLevels)));
        zSpinner.getValueFactory().setValue(defaultZ);
        zSpinner.setPrefWidth(80);
        zSpinner.valueProperty().addListener(
                (obs, oldVal, newVal) -> loadLayer(newVal));

        m_dimensionsLabel = new Label();
        m_dimensionsLabel.setStyle("-fx-padding: 0 4 0 8;");

        var toolbar = new HBox(
                8, fileLabel, zLabel, zSpinner, m_dimensionsLabel);
        toolbar.setStyle(
                "-fx-padding: 8; -fx-alignment: center-left;");

        setTop(toolbar);
        setCenter(m_panCanvas);

        loadLayer(defaultZ);
    }

    private void loadLayer(int zLevel) {
        if (!m_renderer.loadLayer(m_mapDef, zLevel)) {
            return;
        }

        m_dimensionsLabel.setText(
                m_renderer.getMapCols()
                        + " x " + m_renderer.getMapRows());

        m_panCanvas.centerOn(
                m_renderer.getMapWidth() / 2.0,
                m_renderer.getMapHeight() / 2.0);
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
