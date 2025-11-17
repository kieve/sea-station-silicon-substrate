package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.ComponentRepo;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.BlockType;

import static ca.kieve.ssss.util.ListUtil.flatten;

/**
 * Factory for creating block entities in the 3D voxel world.
 * Each block type has its own visual representation and material properties.
 *
 * Visual representation is determined by block type and Z-level:
 * - Blocks at current Z-level: '#' (wall glyph)
 * - Blocks at Z-1 (floor): '·' (floor glyph)
 * The TileRenderSystem handles this based on camera position.
 */
public class BlockBlueprints {

    private BlockBlueprints() {
        // Do not instantiate
    }

    /**
     * Creates a block entity based on the given block type.
     *
     * @param context   The game context
     * @param pos       The 3D position of the block
     * @param blockType The type of block to create
     * @return The created entity, or null if blockType is AIR
     */
    public static Entity createBlock(GameContext context, Vec3i pos, BlockType blockType) {
        return switch (blockType) {
            case AIR -> null; // Air blocks don't have entities
            case STONE -> createStoneBlock(context, pos);
            case WOOD -> createWoodBlock(context, pos);
            case STEEL -> createSteelBlock(context, pos);
        };
    }

    /**
     * Creates a stone block (typically used for walls).
     */
    public static Entity createStoneBlock(GameContext context, Vec3i pos) {
        return createBlockEntity(
            context,
            pos,
            GlyphRepo.POUND,
            new Position(pos),
            ComponentRepo.BLOCK,
            ComponentRepo.WALL_HINT,
            ComponentRepo.EXAMINABLE,
            MaterialBlueprint.createStoneComponents()
        );
    }

    /**
     * Creates a wood block (typically used for floors).
     */
    public static Entity createWoodBlock(GameContext context, Vec3i pos) {
        return createBlockEntity(
            context,
            pos,
            GlyphRepo.POUND,
            new Position(pos),
            ComponentRepo.BLOCK,
            ComponentRepo.WALL_HINT,
            ComponentRepo.EXAMINABLE,
            MaterialBlueprint.createWoodComponents()
        );
    }

    /**
     * Creates a steel block (reinforced walls).
     */
    public static Entity createSteelBlock(GameContext context, Vec3i pos) {
        return createBlockEntity(
            context,
            pos,
            GlyphRepo.SOLID,
            new Position(pos),
            ComponentRepo.REINFORCED_BLOCK,
            ComponentRepo.WALL_HINT,
            ComponentRepo.EXAMINABLE,
            MaterialBlueprint.createSteelComponents()
        );
    }

    private static Entity createBlockEntity(
            GameContext context,
            Vec3i pos,
            Object... components) {
        var entity = context.ecs().createEntity(flatten(components));
        context.pos().add(entity, pos);
        return entity;
    }
}
