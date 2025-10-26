package ca.kieve.ssss.blueprint;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Socket;
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
            color,

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
        var entity = context.ecs().createEntity(
            // Display
            GlyphRepo.M,
            new Descriptor("A Mech",
                "It might be dead, but I haven't implemented Dynamic descriptions yet tho."),
            color,

            // Control?
            new Socket(),

            // Physics
            new Position(pos),
            new Velocity(),
            COLLIDER,

            // Stats?
            new Health(100, 0)
        );
        context.pos().add(entity, pos);

        return entity;
    }
}
