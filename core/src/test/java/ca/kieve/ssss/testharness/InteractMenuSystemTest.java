package ca.kieve.ssss.testharness;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.context.InputContext.Mode;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.PlayerUtil;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for {@link ca.kieve.ssss.system.InteractMenuSystem}: the F-mode
 * interaction menu. These tests focus on the two edge cases called out in
 * {@code TODO.txt}:
 * <ul>
 * <li>Pressing F when standing on the only interactable picks it up directly,
 *     without requiring a follow-up direction press.</li>
 * <li>Pressing F when nothing nearby is interactable short-circuits with the
 *     standard "nothing to interact with" message instead of silently entering
 *     interact mode.</li>
 * </ul>
 */
class InteractMenuSystemTest {
    private static final Vec3i PLAYER_START = new Vec3i(4, 4, 1);

    @Test
    void f_overOnlyItem_picksItUpImmediately() {
        TestEngine engine = TestEngine.createEmpty();
        var ctx = engine.context();
        var item = ctx.entityFactory().createEntity(ctx, "note", PLAYER_START);

        engine.pressAction(InputAction.INTERACT);
        engine.step();

        var player = PlayerUtil.getPlayerEntity(ctx.ecs());
        var inventory = player.get(Inventory.class);
        assertTrue(
            inventory.items().contains(item),
            "F alone should pick up the item underneath when nothing else is interactable"
        );
        assertEquals(Mode.MODE_NORMAL, ctx.input().getMode(), "should return to normal mode");
        assertFalse(ctx.interact().isActive(), "interact context should be inactive");
    }

    @Test
    void f_withNothingNearby_shortCircuitsWithMessage() {
        TestEngine engine = TestEngine.createEmpty();
        var ctx = engine.context();

        engine.pressAction(InputAction.INTERACT);
        engine.step();

        assertEquals(
            Mode.MODE_NORMAL,
            ctx.input().getMode(),
            "F with no interactables should not enter interact mode"
        );
        assertFalse(ctx.interact().isActive(), "interact context should stay inactive");
        assertTrue(
            engine.log().stream().anyMatch(e -> e.message().contains("Nothing to interact with")),
            "should log the 'nothing to interact with' message"
        );
    }

    @Test
    void f_withSingleAdjacentItem_picksItUpImmediately() {
        TestEngine engine = TestEngine.createEmpty();
        var ctx = engine.context();
        Vec3i north = PLAYER_START.add(Vec3i.NORTH);
        var item = ctx.entityFactory().createEntity(ctx, "note", north);

        engine.pressAction(InputAction.INTERACT);
        engine.step();

        var player = PlayerUtil.getPlayerEntity(ctx.ecs());
        var inventory = player.get(Inventory.class);
        assertTrue(
            inventory.items().contains(item),
            "F alone should pick up the single adjacent item without a direction press"
        );
        assertEquals(Mode.MODE_NORMAL, ctx.input().getMode(), "should return to normal mode");
        assertFalse(ctx.interact().isActive(), "interact context should be inactive");
    }

    @Test
    void f_withMultipleAdjacentItems_thenDirection_picksUpChosenItem() {
        TestEngine engine = TestEngine.createEmpty();
        var ctx = engine.context();
        var northItem = ctx.entityFactory()
            .createEntity(ctx, "note", PLAYER_START.add(Vec3i.NORTH));
        var eastItem = ctx.entityFactory()
            .createEntity(ctx, "greenNote", PLAYER_START.add(Vec3i.EAST));

        engine.pressAction(InputAction.INTERACT);
        engine.step();
        assertEquals(
            Mode.MODE_INTERACT,
            ctx.input().getMode(),
            "two valid directions must require an explicit direction choice"
        );

        engine.pressAction(InputAction.UP);
        engine.step();

        var player = PlayerUtil.getPlayerEntity(ctx.ecs());
        var inventory = player.get(Inventory.class);
        assertTrue(inventory.items().contains(northItem), "UP should pick up the north item");
        assertFalse(inventory.items().contains(eastItem), "the east item should be left alone");
        assertEquals(Mode.MODE_NORMAL, ctx.input().getMode());
    }

    @Test
    void f_withItemUnderneathAndAdjacent_entersDirectionSelect() {
        TestEngine engine = TestEngine.createEmpty();
        var ctx = engine.context();
        ctx.entityFactory().createEntity(ctx, "note", PLAYER_START);
        ctx.entityFactory().createEntity(ctx, "greenNote", PLAYER_START.add(Vec3i.EAST));

        engine.pressAction(InputAction.INTERACT);
        engine.step();

        assertEquals(
            Mode.MODE_INTERACT,
            ctx.input().getMode(),
            "F should enter interact mode when more than one tile has interactables"
        );
        assertTrue(ctx.interact().isActive(), "interact context should be active");

        var player = PlayerUtil.getPlayerEntity(ctx.ecs());
        assertTrue(
            player.get(Inventory.class).items().isEmpty(),
            "nothing should have been auto-picked-up"
        );
    }
}
