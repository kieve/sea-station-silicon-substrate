package ca.kieve.ssss.editor.handler;

import ca.kieve.ssss.editor.component.MapRenderer;
import ca.kieve.ssss.editor.component.SelectedCellOverlay;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;
import ca.kieve.ssss.editor.model.GridCell;
import ca.kieve.ssss.editor.ui.PanCanvas;

import java.util.ArrayList;
import java.util.List;

public class MapToolHandler {
    public interface ViewUpdater {
        void onCellChanged(int mapCols, int mapRows);
        void onCellSelected(
            int row,
            int col,
            String blockName,
            List<SelectedCellOverlay.EntityInfo> entityInfos,
            List<SelectedCellOverlay.ConnectorInfo> connectorInfos
        );
        void onEntityMoved(EditorEntity entity, int row, int col);
        void onSelectionCleared();
    }

    private final EditorMapModel m_model;
    private final MapRenderer m_renderer;
    private final PanCanvas m_panCanvas;

    private ViewUpdater m_viewUpdater;

    public MapToolHandler(EditorMapModel model, MapRenderer renderer, PanCanvas panCanvas) {
        m_model = model;
        m_renderer = renderer;
        m_panCanvas = panCanvas;
    }

    public void setViewUpdater(ViewUpdater viewUpdater) {
        m_viewUpdater = viewUpdater;
    }

    public GridCell mouseToGrid(double mouseX, double mouseY) {
        double zoom = m_panCanvas.getZoom();
        int col = (int) Math.floor(
            (mouseX / zoom + m_panCanvas.getCameraX())
                / MapRenderer.CELL_SIZE
        );
        int row = (int) Math.floor(
            (mouseY / zoom + m_panCanvas.getCameraY())
                / MapRenderer.CELL_SIZE
        );
        row = m_renderer.visualRowToDataRow(row);
        return new GridCell(row, col);
    }

    public void paintAt(
        double mouseX,
        double mouseY,
        String blockName,
        int currentZ,
        boolean allLayers
    ) {
        if (blockName == null) {
            return;
        }
        if (!m_model.getBlocks().containsKey(blockName)) {
            return;
        }
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();

        boolean changed;
        if (allLayers) {
            changed = setCellAllLayers(row, col, blockName);
        } else {
            changed = m_model.setCell(currentZ, row, col, blockName);
        }
        if (!changed) {
            return;
        }
        m_viewUpdater.onCellChanged(m_renderer.getMapCols(), m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    public void eraseAt(double mouseX, double mouseY, int currentZ, boolean allLayers) {
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();

        boolean changed;
        if (allLayers) {
            changed = setCellAllLayers(row, col, null);
        } else {
            changed = m_model.setCell(currentZ, row, col, null);
        }
        if (!changed) {
            return;
        }
        m_viewUpdater.onCellChanged(m_renderer.getMapCols(), m_renderer.getMapRows());
        m_panCanvas.requestRedraw();
    }

    public void selectAt(double mouseX, double mouseY, int currentZ) {
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();
        m_renderer.setSelectedCell(row, col);

        String blockName = m_model.getCell(currentZ, row, col);

        var entityInfos = new ArrayList<SelectedCellOverlay.EntityInfo>();
        var entities = m_model.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            var entity = entities.get(i);
            var pos = entity.getEntityPos();
            if (pos != null
                && pos.x() == col
                && pos.y() == row
                && pos.z() == currentZ) {
                entityInfos.add(new SelectedCellOverlay.EntityInfo(i, entity.id()));
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
                && cPos.z() == currentZ) {
                connectorInfos.add(new SelectedCellOverlay.ConnectorInfo(i, c.id()));
            }
        }

        m_viewUpdater.onCellSelected(row, col, blockName, entityInfos, connectorInfos);
        m_panCanvas.requestRedraw();
    }

    public void moveEntityTo(double mouseX, double mouseY, Integer entityIndex, int currentZ) {
        if (entityIndex == null) {
            return;
        }
        var entities = m_model.getEntities();
        if (entityIndex < 0 || entityIndex >= entities.size()) {
            return;
        }
        moveTargetTo(entities.get(entityIndex), mouseX, mouseY, currentZ);
    }

    /**
     * Moves any {@link EditorEntity} (regular entity, connector, or
     * submap) to the cell under the cursor. Skips silently if the entity
     * has no Position component, since a moving-without-position is
     * meaningless (a connector-mode submap, e.g.).
     */
    public void moveTargetTo(EditorEntity entity, double mouseX, double mouseY, int currentZ) {
        if (entity.getPositionComponent() == null) {
            return;
        }
        var cell = mouseToGrid(mouseX, mouseY);
        int row = cell.row();
        int col = cell.col();

        entity.movePosition(col, row, currentZ);
        m_model.markModified();

        m_viewUpdater.onEntityMoved(entity, row, col);
        m_panCanvas.requestRedraw();
    }

    public void clearSelection() {
        m_renderer.setSelectedCell(null, null);
        m_viewUpdater.onSelectionCleared();
        m_panCanvas.requestRedraw();
    }

    private boolean setCellAllLayers(int row, int col, String blockName) {
        boolean anyChanged = false;
        for (int z : m_model.getZLevels()) {
            if (m_model.setCell(z, row, col, blockName)) {
                anyChanged = true;
            }
        }
        return anyChanged;
    }
}
