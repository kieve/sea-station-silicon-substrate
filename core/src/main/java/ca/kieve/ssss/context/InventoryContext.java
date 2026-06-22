package ca.kieve.ssss.context;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.component.Item;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.PlayerUtil;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class InventoryContext {
    public enum PaneSide {
        LEFT,
        RIGHT
    }

    public enum PaneLocation {
        PLAYER_INVENTORY("Inventory", null),
        TILE_HERE("Tile (here)", Vec3i.ZERO),
        TILE_N("Tile (N)", Vec3i.NORTH),
        TILE_S("Tile (S)", Vec3i.SOUTH),
        TILE_W("Tile (W)", Vec3i.WEST),
        TILE_E("Tile (E)", Vec3i.EAST),
        TILE_NE("Tile (NE)", Vec3i.NORTHEAST),
        TILE_NW("Tile (NW)", Vec3i.NORTHWEST),
        TILE_SE("Tile (SE)", Vec3i.SOUTHEAST),
        TILE_SW("Tile (SW)", Vec3i.SOUTHWEST);

        private final String m_label;
        private final Vec3i m_offset;

        PaneLocation(String label, Vec3i offset) {
            m_label = label;
            m_offset = offset;
        }

        public String label() {
            return m_label;
        }

        /** Returns the tile offset from the player, or {@code null} for PLAYER_INVENTORY. */
        public Vec3i offset() {
            return m_offset;
        }
    }

    private static final Map<InputAction, PaneLocation> ACTION_TO_LOCATION;
    static {
        ACTION_TO_LOCATION = new EnumMap<>(InputAction.class);
        ACTION_TO_LOCATION.put(InputAction.SELF, PaneLocation.PLAYER_INVENTORY);
        ACTION_TO_LOCATION.put(InputAction.ORIGIN, PaneLocation.TILE_HERE);
        ACTION_TO_LOCATION.put(InputAction.N, PaneLocation.TILE_N);
        ACTION_TO_LOCATION.put(InputAction.S, PaneLocation.TILE_S);
        ACTION_TO_LOCATION.put(InputAction.W, PaneLocation.TILE_W);
        ACTION_TO_LOCATION.put(InputAction.E, PaneLocation.TILE_E);
        ACTION_TO_LOCATION.put(InputAction.NE, PaneLocation.TILE_NE);
        ACTION_TO_LOCATION.put(InputAction.NW, PaneLocation.TILE_NW);
        ACTION_TO_LOCATION.put(InputAction.SE, PaneLocation.TILE_SE);
        ACTION_TO_LOCATION.put(InputAction.SW, PaneLocation.TILE_SW);
    }

    private final EnumMap<PaneSide, PaneLocation> m_locations = new EnumMap<>(PaneSide.class);
    private final EnumMap<PaneSide, Integer> m_cursors = new EnumMap<>(PaneSide.class);

    private boolean m_active = false;
    private PaneSide m_activeSide = PaneSide.LEFT;

    public static PaneLocation locationFor(InputAction action) {
        return ACTION_TO_LOCATION.get(action);
    }

    public InventoryContext() {
        m_locations.put(PaneSide.LEFT, PaneLocation.PLAYER_INVENTORY);
        m_locations.put(PaneSide.RIGHT, PaneLocation.TILE_HERE);
        m_cursors.put(PaneSide.LEFT, 0);
        m_cursors.put(PaneSide.RIGHT, 0);
    }

    public boolean isActive() {
        return m_active;
    }

    public void enter() {
        m_active = true;
        m_activeSide = PaneSide.LEFT;
        m_locations.put(PaneSide.LEFT, PaneLocation.PLAYER_INVENTORY);
        m_locations.put(PaneSide.RIGHT, PaneLocation.TILE_HERE);
        m_cursors.put(PaneSide.LEFT, 0);
        m_cursors.put(PaneSide.RIGHT, 0);
    }

    public void exit() {
        m_active = false;
    }

    public PaneSide getActiveSide() {
        return m_activeSide;
    }

    public void swapActiveSide() {
        m_activeSide = (m_activeSide == PaneSide.LEFT) ? PaneSide.RIGHT : PaneSide.LEFT;
    }

    public PaneLocation getLocation(PaneSide side) {
        return m_locations.get(side);
    }

    public void setLocation(PaneSide side, PaneLocation location) {
        m_locations.put(side, location);
        m_cursors.put(side, 0);
    }

    public int getCursor(PaneSide side) {
        return m_cursors.get(side);
    }

    public void incrementCursor(PaneSide side, int count) {
        if (count <= 0) {
            return;
        }
        int next = (m_cursors.get(side) + 1) % count;
        m_cursors.put(side, next);
    }

    public void decrementCursor(PaneSide side, int count) {
        if (count <= 0) {
            return;
        }
        int next = (m_cursors.get(side) - 1 + count) % count;
        m_cursors.put(side, next);
    }

    public void clampCursor(PaneSide side, int count) {
        int current = m_cursors.get(side);
        if (count <= 0) {
            m_cursors.put(side, 0);
            return;
        }
        if (current >= count) {
            m_cursors.put(side, count - 1);
        } else if (current < 0) {
            m_cursors.put(side, 0);
        }
    }

    /**
     * Resolves the entities currently shown in the given pane. For
     * PLAYER_INVENTORY this is the player's Inventory items; for tile
     * locations this is the Item-component entities at the offset tile.
     */
    public List<Entity> getItemsAt(GameContext gameContext, PaneLocation location) {
        if (location == PaneLocation.PLAYER_INVENTORY) {
            var player = PlayerUtil.getPlayerEntity(gameContext.ecs());
            if (player == null) {
                return Collections.emptyList();
            }
            var inventory = player.get(Inventory.class);
            if (inventory == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(inventory.items());
        }

        var controlledPos = PlayerUtil.getControlledPosition(gameContext.ecs());
        if (controlledPos == null) {
            return Collections.emptyList();
        }
        var tilePos = controlledPos.add(location.offset());
        var atTile = gameContext.pos().getAt(tilePos);
        var items = new ArrayList<Entity>();
        for (var entity : atTile) {
            if (entity.get(Item.class) != null) {
                items.add(entity);
            }
        }
        return items;
    }

    /**
     * Resolves the world position represented by a tile pane. Returns
     * {@code null} for {@link PaneLocation#PLAYER_INVENTORY}.
     */
    public Vec3i resolveTilePos(GameContext gameContext, PaneLocation location) {
        if (location.offset() == null) {
            return null;
        }
        var controlledPos = PlayerUtil.getControlledPosition(gameContext.ecs());
        if (controlledPos == null) {
            return null;
        }
        return controlledPos.add(location.offset());
    }
}
