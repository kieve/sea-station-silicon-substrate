package ca.kieve.ssss.blueprint;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Attackable;
import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.ai.AiSeesawController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;

import static ca.kieve.ssss.repository.ComponentRepo.COLLIDER;

public class ActorBlueprint {
    private ActorBlueprint() {
        // Do not instantiate
    }

    public static Entity createDebugMover(
        GameContext context,
        Vec3i pos,
        int speed,
        Color color
    ) {
        if (color == null) {
            color = Color.WHITE;
        }
        var entity = context.ecs().createEntity(
            // Display
            GlyphRepo.S,
            new Descriptor("Moving Sign", "Perhaps, there's many of them?"),
            new ColorComp(color),
            new RenderingHint(1),

            // Control
            new AiSeesawController(pos),
            new Speed(speed),

            // Physics
            new Position(pos),
            new Velocity(),
            COLLIDER
        );
        context.pos().add(entity, pos);

        return entity;
    }

    public static Entity createDeadMech(
        GameContext context,
        Vec3i pos,
        Color color
    ) {
        if (color == null) {
            color = Color.WHITE;
        }
        var powerFist = WeaponBlueprints.createPowerFist(context);
        var entity = context.ecs().createEntity(
            // Display
            GlyphRepo.M,
            new Descriptor("Mech", "Beep Boop."),
            new ColorComp(color),
            new RenderingHint(1),

            // Control?
            new Socket(),
            new Socketable(),

            // Physics
            new Position(pos),
            new Velocity(),
            COLLIDER,

            // Stats?
            new Health(100, 0),
            new Equipment(powerFist)
        );
        context.pos().add(entity, pos);

        return entity;
    }

    public static Entity createTrainingDummy(
        GameContext context,
        Vec3i pos,
        Color color
    ) {
        if (color == null) {
            color = Color.WHITE;
        }
        var entity = context.ecs().createEntity(
            // Display
            GlyphRepo.T,
            new Descriptor("Training Dummy", "Go on, hit me."),
            new ColorComp(color),
            new RenderingHint(1),

            // Interaction
            new Attackable(),
            new Socketable(),

            // Physics
            new Position(pos),
            COLLIDER,

            // Stats
            new Health(20),
            new Socket()
        );
        context.pos().add(entity, pos);

        return entity;
    }
}
