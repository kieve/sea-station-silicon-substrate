package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Opaque;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.GhostEntity;
import ca.kieve.ssss.context.GhostTile;
import ca.kieve.ssss.context.PositionContext;
import ca.kieve.ssss.context.VisionContext;
import ca.kieve.ssss.util.DescriptionComposer;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.util.VisionUtil;
import ca.kieve.ssss.util.WaterExamine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.CEILING;
import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.FLOOR;
import static ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType.MAIN;

/**
 * Recalculates field of view and snapshots ghost tiles. Runs on POST_TICK so
 * positions are settled; implements MapInitSystem to seed FOV once after map
 * generation, so the first frame already has correct visibility before any
 * turn fires.
 */
public class VisionSystem extends System implements MapInitSystem {
    private final Dominion m_ecs;
    private final VisionContext m_vision;
    private final PositionContext m_pos;
    private final FluidContext m_fluid;
    private final TileGlyph m_floorGlyph;

    public VisionSystem(GameContext gameContext) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_vision = gameContext.vision();
        m_pos = gameContext.pos();
        m_fluid = gameContext.fluid();
        var floorGlyphId = gameContext.mapGenerator().getFloorGlyphId();
        m_floorGlyph = gameContext.entityFactory().getGlyphFactory().getGlyph(floorGlyphId);
    }

    @Override
    public void run(GameContext context) {
        recalculate();
    }

    @Override
    public void postTick() {
        recalculate();
    }

    private void recalculate() {
        var playerBody = getPlayerBody();
        var playerPos = getPlayerPosition(playerBody);
        if (playerPos == null) {
            return;
        }
        int cameraZ = playerPos.z;

        Set<String> opaqueKeys = new HashSet<>();
        m_ecs.findEntitiesWith(Opaque.class, Position.class).forEach(result -> {
            var pos = result.comp2().getPosition();
            if (pos.z == cameraZ && VisionUtil.blocksVision(result.entity())) {
                opaqueKeys.add(pos.x + "," + pos.y);
            }
        });

        m_vision.recalculate(
            playerPos.x,
            playerPos.y,
            cameraZ,
            (x, y) -> opaqueKeys.contains(x + "," + y)
        );

        snapshotGhosts(playerBody, cameraZ);
    }

    private void snapshotGhosts(Entity playerBody, int cameraZ) {
        var entities = m_ecs.findEntitiesWith(Position.class, TileGlyph.class);

        Map<String, Entity> topEntities = new HashMap<>();
        Map<String, Integer> topPriorities = new HashMap<>();
        Map<String, Integer> topZ = new HashMap<>();

        entities.forEach(with -> {
            var pos = with.comp1().getPosition();
            var entity = with.entity();

            if (entity == playerBody || entity.has(Hidden.class)) {
                return;
            }
            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                return;
            }
            var hint = entity.get(RenderingHint.class);
            int zIndex = (hint != null) ? hint.zIndex : 0;
            if (zIndex == -1) {
                return;
            }
            int priority = relativeZ * 100 + zIndex;
            String key = pos.x + "," + pos.y;
            var current = topPriorities.get(key);
            if (current != null && priority <= current) {
                return;
            }
            topEntities.put(key, entity);
            topPriorities.put(key, priority);
            topZ.put(key, pos.z);
        });

        for (var entry : topEntities.entrySet()) {
            var key = entry.getKey();
            var entity = entry.getValue();
            var pos = entity.get(Position.class).getPosition();
            if (!m_vision.isVisible(pos.x, pos.y)) {
                continue;
            }
            var glyph = entity.get(TileGlyph.class);
            if (glyph == null) {
                continue;
            }
            int relativeZ = topZ.get(key) - cameraZ;
            TileGlyph renderGlyph = (relativeZ == -1) ? m_floorGlyph : glyph;
            var color = Color.WHITE;
            var colorComp = entity.get(ColorComp.class);
            if (colorComp != null) {
                color = colorComp.color;
            }
            snapshotGhost(pos.x, pos.y, cameraZ, renderGlyph, color, playerBody);
        }
    }

    private void snapshotGhost(
        int x,
        int y,
        int cameraZ,
        TileGlyph renderGlyph,
        Color color,
        Entity playerBody
    ) {
        var ghostEntities = new ArrayList<GhostEntity>();
        addGhostEntities(ghostEntities, new Vec3i(x, y, cameraZ), MAIN, playerBody);
        addGhostEntities(ghostEntities, new Vec3i(x, y, cameraZ + 1), CEILING, playerBody);
        addGhostEntities(ghostEntities, new Vec3i(x, y, cameraZ - 1), FLOOR, playerBody);

        var ghost = new GhostTile(
            renderGlyph.glyph(),
            renderGlyph.font(),
            renderGlyph.dx(),
            renderGlyph.dy(),
            color,
            ghostEntities
        );
        m_vision.setGhost(x, y, cameraZ, ghost);
    }

    private void addGhostEntities(
        ArrayList<GhostEntity> ghostEntities,
        Vec3i pos,
        ExamineSystem.ExamineItem.ItemType type,
        Entity playerBody
    ) {
        var entities = ExamineContext.sortEntitiesByZIndex(m_pos.getAt(pos));
        for (var entity : entities) {
            if (entity == playerBody || entity.has(Hidden.class)) {
                continue;
            }
            var descriptor = entity.get(Descriptor.class);
            if (descriptor == null) {
                continue;
            }
            ghostEntities.add(
                new GhostEntity(descriptor.name(), DescriptionComposer.compose(entity), type)
            );
        }
        if (type != MAIN || !m_fluid.hasWater(pos)) {
            return;
        }
        ghostEntities.add(
            new GhostEntity(
                WaterExamine.name(m_fluid, pos),
                WaterExamine.description(m_fluid, pos),
                type
            )
        );
    }

    private Entity getPlayerBody() {
        var playerResults = m_ecs.findEntitiesWith(Player.class, CameraComp.class);
        var playerResult = playerResults.stream().findFirst();
        if (playerResult.isEmpty()) {
            return null;
        }
        var playerEntity = playerResult.get().entity();
        var socketPlug = playerEntity.get(SocketPlug.class);
        if (socketPlug != null && socketPlug.currentBody != null) {
            return socketPlug.currentBody;
        }
        return playerEntity;
    }

    private Vec3i getPlayerPosition(Entity body) {
        if (body == null) {
            return null;
        }
        var pos = body.get(Position.class);
        return pos != null ? pos.getPosition() : null;
    }
}
