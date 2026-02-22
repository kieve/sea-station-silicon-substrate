package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.component.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EditorModel {
    public interface Listener {
        void onModelChanged();
        void onLayerChanged();
        void onBlockSelectionChanged();
        void onDirtyChanged();
        void onEntitiesChanged();
        void onToolChanged();
    }

    public enum Tool { PAINT, SELECT, ERASE }

    public record CellKey(int col, int row) {}

    public record Bounds(int minCol, int minRow, int maxCol, int maxRow) {
        public int width() {
            return maxCol - minCol + 1;
        }

        public int height() {
            return maxRow - minRow + 1;
        }
    }

    public record EntityPosition(int x, int y, int z) {}

    private static final int DEFAULT_SIZE = 10;

    private final List<Listener> m_listeners = new ArrayList<>();

    private Map<String, MapBlockDefinition> m_blocks =
            new LinkedHashMap<>();
    private Map<Integer, HashMap<CellKey, Character>> m_layers =
            new HashMap<>();
    private List<MapEntityDefinition> m_entities = new ArrayList<>();
    private String m_floorGlyph = "interpunct";

    private int m_activeLayer = 0;
    private String m_activeBlockName = "";
    private Tool m_activeTool = Tool.PAINT;
    private boolean m_dirty = false;

    public void addListener(Listener listener) {
        m_listeners.add(listener);
    }

    public void newMap() {
        m_blocks.clear();
        m_blocks.put("floor", new MapBlockDefinition("wood", '+'));
        m_blocks.put("wall", new MapBlockDefinition("stone", '#'));
        m_blocks.put("air", new MapBlockDefinition("air", '.'));

        m_layers.clear();

        var layer0 = new HashMap<CellKey, Character>();
        var layer1 = new HashMap<CellKey, Character>();
        for (int row = 0; row < DEFAULT_SIZE; row++) {
            for (int col = 0; col < DEFAULT_SIZE; col++) {
                boolean border = row == 0
                        || row == DEFAULT_SIZE - 1
                        || col == 0
                        || col == DEFAULT_SIZE - 1;
                layer0.put(new CellKey(col, row),
                        border ? '#' : '+');
                layer1.put(new CellKey(col, row),
                        border ? '#' : '.');
            }
        }
        m_layers.put(0, layer0);
        m_layers.put(1, layer1);

        m_entities.clear();
        var playerPos = new ComponentDefinition(Position.class);
        playerPos.setProperty("x", 1);
        playerPos.setProperty("y", DEFAULT_SIZE - 2);
        playerPos.setProperty("z", 1);
        m_entities.add(new MapEntityDefinition(
                "player", List.of(playerPos)));

        m_floorGlyph = "interpunct";
        m_activeLayer = 0;
        m_activeBlockName = "wall";
        m_activeTool = Tool.PAINT;
        m_dirty = false;

        fireModelChanged();
        fireLayerChanged();
        fireBlockSelectionChanged();
        fireDirtyChanged();
        fireEntitiesChanged();
    }

    public void fromMapDefinition(MapDefinition def) {
        m_blocks = new LinkedHashMap<>(def.blocks());
        m_floorGlyph = def.floorGlyph() != null
                ? def.floorGlyph()
                : "interpunct";

        m_layers.clear();

        for (var entry : def.layers().entrySet()) {
            int z = Integer.parseInt(entry.getKey());
            String layerStr = entry.getValue();
            String[] rows = layerStr.split("\n");

            var cellMap = new HashMap<CellKey, Character>();
            for (int row = 0; row < rows.length; row++) {
                for (int col = 0; col < rows[row].length(); col++) {
                    char c = rows[row].charAt(col);
                    if (c != ' ') {
                        cellMap.put(new CellKey(col, row), c);
                    }
                }
            }
            m_layers.put(z, cellMap);
        }

        if (m_layers.isEmpty()) {
            m_layers.put(0, new HashMap<>());
        }

        m_entities = new ArrayList<>(def.entities());

        m_activeLayer = m_layers.keySet().stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);

        if (!m_blocks.isEmpty()) {
            m_activeBlockName = m_blocks.keySet().iterator().next();
        }
        m_dirty = false;

        fireModelChanged();
        fireLayerChanged();
        fireBlockSelectionChanged();
        fireDirtyChanged();
        fireEntitiesChanged();
    }

    public MapDefinition toMapDefinition() {
        Bounds bounds = getGlobalBounds();
        if (bounds == null) {
            return new MapDefinition(
                    new LinkedHashMap<>(m_blocks),
                    new LinkedHashMap<>(),
                    m_floorGlyph,
                    new ArrayList<>(m_entities));
        }

        Map<String, String> layers = new LinkedHashMap<>();
        var sortedKeys = m_layers.keySet().stream()
                .sorted()
                .toList();

        for (int z : sortedKeys) {
            var cellMap = m_layers.get(z);
            var sb = new StringBuilder();
            for (int row = bounds.minRow;
                    row <= bounds.maxRow; row++) {
                if (row > bounds.minRow) {
                    sb.append('\n');
                }
                int lastNonSpace = bounds.minCol - 1;
                for (int col = bounds.maxCol;
                        col >= bounds.minCol; col--) {
                    if (cellMap.containsKey(
                            new CellKey(col, row))) {
                        lastNonSpace = col;
                        break;
                    }
                }
                for (int col = bounds.minCol;
                        col <= lastNonSpace; col++) {
                    Character c = cellMap.get(
                            new CellKey(col, row));
                    sb.append(c != null ? c : ' ');
                }
            }
            sb.append('\n');
            layers.put(String.valueOf(z), sb.toString());
        }

        return new MapDefinition(
                new LinkedHashMap<>(m_blocks),
                layers,
                m_floorGlyph,
                new ArrayList<>(m_entities));
    }

    public Bounds getGlobalBounds() {
        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int maxRow = Integer.MIN_VALUE;
        boolean found = false;

        for (var cellMap : m_layers.values()) {
            for (var key : cellMap.keySet()) {
                minCol = Math.min(minCol, key.col());
                minRow = Math.min(minRow, key.row());
                maxCol = Math.max(maxCol, key.col());
                maxRow = Math.max(maxRow, key.row());
                found = true;
            }
        }
        if (!found) {
            return null;
        }
        return new Bounds(minCol, minRow, maxCol, maxRow);
    }

    public void paintCell(int col, int row, char layoutChar) {
        var cellMap = m_layers.get(m_activeLayer);
        if (cellMap == null) {
            return;
        }
        var key = new CellKey(col, row);
        Character existing = cellMap.get(key);
        if (existing != null && existing == layoutChar) {
            return;
        }
        cellMap.put(key, layoutChar);
        markDirty();
        fireModelChanged();
    }

    public void eraseCell(int col, int row) {
        var cellMap = m_layers.get(m_activeLayer);
        if (cellMap == null) {
            return;
        }
        var key = new CellKey(col, row);
        if (cellMap.remove(key) != null) {
            markDirty();
            fireModelChanged();
        }
    }

    public void addBlock(String name, MapBlockDefinition block) {
        m_blocks.put(name, block);
        markDirty();
        fireModelChanged();
    }

    public void removeBlock(String name) {
        m_blocks.remove(name);
        if (m_activeBlockName.equals(name)) {
            m_activeBlockName = m_blocks.isEmpty()
                    ? ""
                    : m_blocks.keySet().iterator().next();
            fireBlockSelectionChanged();
        }
        markDirty();
        fireModelChanged();
    }

    public void addLayer(int z) {
        if (m_layers.containsKey(z)) {
            return;
        }
        m_layers.put(z, new HashMap<>());
        m_activeLayer = z;
        markDirty();
        fireModelChanged();
        fireLayerChanged();
    }

    public void removeLayer(int z) {
        if (m_layers.size() <= 1) {
            return;
        }
        m_layers.remove(z);
        if (m_activeLayer == z) {
            m_activeLayer = m_layers.keySet().stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(0);
        }
        markDirty();
        fireModelChanged();
        fireLayerChanged();
    }

    // --- Entity management ---

    public List<MapEntityDefinition> getEntities() {
        return m_entities;
    }

    public void setEntities(List<MapEntityDefinition> entities) {
        m_entities = new ArrayList<>(entities);
        markDirty();
        fireEntitiesChanged();
    }

    public void addEntity(MapEntityDefinition entity) {
        m_entities.add(entity);
        markDirty();
        fireEntitiesChanged();
    }

    public void removeEntity(int index) {
        if (index < 0 || index >= m_entities.size()) {
            return;
        }
        m_entities.remove(index);
        markDirty();
        fireEntitiesChanged();
    }

    public void updateEntity(int index, MapEntityDefinition entity) {
        if (index < 0 || index >= m_entities.size()) {
            return;
        }
        m_entities.set(index, entity);
        markDirty();
        fireEntitiesChanged();
    }

    public EntityPosition getEntityPosition(
            MapEntityDefinition entity) {
        for (var comp : entity.components()) {
            if (comp.type() == Position.class) {
                var props = comp.properties();
                Object xObj = props.get("x");
                Object yObj = props.get("y");
                Object zObj = props.get("z");
                if (xObj == null || yObj == null || zObj == null) {
                    return null;
                }
                return new EntityPosition(
                        toInt(xObj), toInt(yObj), toInt(zObj));
            }
        }
        return null;
    }

    public List<Integer> getEntitiesAt(int col, int row, int z) {
        var result = new ArrayList<Integer>();
        for (int i = 0; i < m_entities.size(); i++) {
            var pos = getEntityPosition(m_entities.get(i));
            if (pos != null
                    && pos.x() == col
                    && pos.y() == row
                    && pos.z() == z) {
                result.add(i);
            }
        }
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(obj.toString());
    }

    // --- Tool ---

    public Tool getActiveTool() {
        return m_activeTool;
    }

    public void setActiveTool(Tool tool) {
        m_activeTool = tool;
        fireToolChanged();
    }

    // --- Existing getters/setters ---

    private void markDirty() {
        if (!m_dirty) {
            m_dirty = true;
            fireDirtyChanged();
        }
    }

    public void clearDirty() {
        m_dirty = false;
        fireDirtyChanged();
    }

    public Map<String, MapBlockDefinition> getBlocks() {
        return m_blocks;
    }

    public HashMap<CellKey, Character> getActiveCellMap() {
        return m_layers.get(m_activeLayer);
    }

    public int getActiveLayer() {
        return m_activeLayer;
    }

    public void setActiveLayer(int z) {
        if (!m_layers.containsKey(z)) {
            return;
        }
        m_activeLayer = z;
        fireLayerChanged();
    }

    public List<Integer> getSortedLayerKeys() {
        return m_layers.keySet().stream()
                .sorted()
                .toList();
    }

    public String getActiveBlockName() {
        return m_activeBlockName;
    }

    public void setActiveBlockName(String name) {
        m_activeBlockName = name;
        fireBlockSelectionChanged();
    }

    public MapBlockDefinition getActiveBlock() {
        return m_blocks.get(m_activeBlockName);
    }

    public String getFloorGlyph() {
        return m_floorGlyph;
    }

    public void setFloorGlyph(String glyph) {
        m_floorGlyph = glyph;
        markDirty();
    }

    public boolean isDirty() {
        return m_dirty;
    }

    public String getBlockNameForChar(char c) {
        for (var entry : m_blocks.entrySet()) {
            if (entry.getValue().layoutChar() == c) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void fireModelChanged() {
        for (var l : m_listeners) {
            l.onModelChanged();
        }
    }

    private void fireLayerChanged() {
        for (var l : m_listeners) {
            l.onLayerChanged();
        }
    }

    private void fireBlockSelectionChanged() {
        for (var l : m_listeners) {
            l.onBlockSelectionChanged();
        }
    }

    private void fireDirtyChanged() {
        for (var l : m_listeners) {
            l.onDirtyChanged();
        }
    }

    private void fireEntitiesChanged() {
        for (var l : m_listeners) {
            l.onEntitiesChanged();
        }
    }

    private void fireToolChanged() {
        for (var l : m_listeners) {
            l.onToolChanged();
        }
    }
}
