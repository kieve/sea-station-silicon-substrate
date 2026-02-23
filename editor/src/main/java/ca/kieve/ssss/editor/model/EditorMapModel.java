package ca.kieve.ssss.editor.model;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EditorMapModel {
    private final Map<String, MapBlockDefinition> m_blocks =
            new HashMap<>();
    private final TreeMap<Integer, SparseGrid> m_layers =
            new TreeMap<>();
    private String m_floorGlyph;
    private List<MapEntityDefinition> m_entities;
    private final BooleanProperty m_modified =
            new SimpleBooleanProperty(false);
    private File m_file;

    public static EditorMapModel fromDefinition(
            MapDefinition def, File file) {
        var model = new EditorMapModel();
        model.m_file = file;
        model.m_floorGlyph = def.floorGlyph();
        model.m_entities = def.entities();
        model.m_blocks.putAll(def.blocks());

        // Build a reverse map: layoutChar -> block name
        Map<Character, String> charToName = new HashMap<>();
        for (var entry : def.blocks().entrySet()) {
            charToName.put(
                    entry.getValue().layoutChar(),
                    entry.getKey());
        }

        for (var entry : def.layers().entrySet()) {
            int z = Integer.parseInt(entry.getKey());
            String[] rows = entry.getValue().split("\n");
            var grid = new SparseGrid();
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length(); c++) {
                    char ch = rows[r].charAt(c);
                    String blockName = charToName.get(ch);
                    if (blockName != null) {
                        grid.setCell(r, c, blockName);
                    }
                }
            }
            model.m_layers.put(z, grid);
        }

        return model;
    }

    public MapDefinition toDefinition() {
        Map<String, MapBlockDefinition> blocks =
                new HashMap<>(m_blocks);

        // Find the bounding box across all layers
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int maxCol = Integer.MIN_VALUE;
        for (var grid : m_layers.values()) {
            if (grid.isEmpty()) {
                continue;
            }
            minRow = Math.min(minRow, grid.getMinRow());
            maxRow = Math.max(maxRow, grid.getMaxRow());
            minCol = Math.min(minCol, grid.getMinCol());
            maxCol = Math.max(maxCol, grid.getMaxCol());
        }

        if (minRow > maxRow) {
            minRow = 0;
            minCol = 0;
            maxRow = 0;
            maxCol = 0;
        }

        Map<String, String> layers = new HashMap<>();
        for (var entry : m_layers.entrySet()) {
            var grid = entry.getValue();
            var sb = new StringBuilder();
            for (int r = minRow; r <= maxRow; r++) {
                if (r > minRow) {
                    sb.append('\n');
                }
                // Find the last occupied column in this row
                int rowEnd = minCol - 1;
                for (int c = minCol; c <= maxCol; c++) {
                    if (grid.getCell(r, c) != null) {
                        rowEnd = c;
                    }
                }
                for (int c = minCol; c <= rowEnd; c++) {
                    String blockName =
                            grid.getCell(r, c);
                    if (blockName != null) {
                        var blockDef =
                                m_blocks.get(blockName);
                        sb.append(blockDef != null
                                ? blockDef.layoutChar()
                                : ' ');
                    } else {
                        sb.append(' ');
                    }
                }
            }
            layers.put(
                    String.valueOf(entry.getKey()),
                    sb.toString());
        }

        // Adjust entity positions so they are relative to the
        // bounding-box origin
        List<MapEntityDefinition> entities =
                adjustEntityPositions(
                        m_entities, -minCol, -minRow);

        return new MapDefinition(
                blocks, layers, m_floorGlyph, entities);
    }

    private static List<MapEntityDefinition>
            adjustEntityPositions(
                    List<MapEntityDefinition> entities,
                    int xAdj, int yAdj) {
        if ((xAdj == 0 && yAdj == 0)
                || entities.isEmpty()) {
            return entities;
        }
        return entities.stream().map(e -> {
            var adjusted =
                    e.components().stream().map(comp -> {
                if (!"Position".equals(
                        comp.type().getSimpleName())) {
                    return comp;
                }
                var copy =
                        new ComponentDefinition(comp.type());
                for (var p : comp.properties().entrySet()) {
                    copy.setProperty(
                            p.getKey(), p.getValue());
                }
                Object xVal = copy.properties().get("x");
                Object yVal = copy.properties().get("y");
                if (xVal instanceof Number n) {
                    copy.setProperty(
                            "x", n.intValue() + xAdj);
                }
                if (yVal instanceof Number n) {
                    copy.setProperty(
                            "y", n.intValue() + yAdj);
                }
                return copy;
            }).toList();
            return new MapEntityDefinition(
                    e.id(), adjusted);
        }).toList();
    }

    public String getCell(int z, int row, int col) {
        var grid = m_layers.get(z);
        if (grid == null) {
            return null;
        }
        return grid.getCell(row, col);
    }

    public boolean setCell(
            int z, int row, int col, String blockName) {
        var grid = m_layers.get(z);
        if (grid == null) {
            return false;
        }
        if (!grid.setCell(row, col, blockName)) {
            return false;
        }
        m_modified.set(true);
        return true;
    }

    public SparseGrid getLayer(int z) {
        return m_layers.get(z);
    }

    public List<Integer> getZLevels() {
        return List.copyOf(m_layers.keySet());
    }

    public Map<String, MapBlockDefinition> getBlocks() {
        return m_blocks;
    }

    public void addBlock(
            String name, MapBlockDefinition block) {
        m_blocks.put(name, block);
        m_modified.set(true);
    }

    public void removeBlock(String name) {
        m_blocks.remove(name);
        m_modified.set(true);
    }

    public boolean isModified() {
        return m_modified.get();
    }

    public BooleanProperty modifiedProperty() {
        return m_modified;
    }

    public void setModified(boolean modified) {
        m_modified.set(modified);
    }

    public File getFile() {
        return m_file;
    }

    public void setFile(File file) {
        m_file = file;
    }

    public String getFloorGlyph() {
        return m_floorGlyph;
    }

    /**
     * Build a mapping from block name to block type for
     * rendering.
     */
    public Map<String, String> buildBlockNameToTypeMap() {
        Map<String, String> map = new HashMap<>();
        for (var entry : m_blocks.entrySet()) {
            MapBlockDefinition blockDef = entry.getValue();
            map.put(entry.getKey(), blockDef.type());
        }
        return map;
    }
}
