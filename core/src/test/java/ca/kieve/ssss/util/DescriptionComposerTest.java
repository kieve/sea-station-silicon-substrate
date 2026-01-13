package ca.kieve.ssss.util;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Material;
import ca.kieve.ssss.component.Socket;
import dev.dominion.ecs.api.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DescriptionComposerTest {
    @Test
    void composeReturnsEmptyStringWhenNoDescriptor() {
        Entity entity = mock(Entity.class);
        when(entity.get(Descriptor.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("", result);
    }

    @Test
    void composeReturnsBaseDescriptionWhenOnlyDescriptor() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Crab", "A small crustacean.");
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(null);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Crab. A small crustacean.", result);
    }

    @Test
    void composeIncludesHealthyStatus() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Crab", "A small crustacean.");
        var health = new Health(100, 100);
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Crab. A small crustacean. It looks healthy.", result);
    }

    @Test
    void composeIncludesSlightlyDamagedStatus() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Crab", "A small crustacean.");
        var health = new Health(100, 80);
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Crab. A small crustacean. It looks slightly damaged.", result);
    }

    @Test
    void composeIncludesNearlyDeadStatus() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Crab", "A small crustacean.");
        var health = new Health(100, 30);
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Crab. A small crustacean. It looks nearly dead.", result);
    }

    @Test
    void composeIncludesDeadStatus() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Crab", "A small crustacean.");
        var health = new Health(100, 0);
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Crab. A small crustacean. It's dead.", result);
    }

    @Test
    void composeIncludesMaterialDescription() {
        Entity entity = mock(Entity.class);
        Entity materialEntity = mock(Entity.class);
        var descriptor = new Descriptor("Door", "A sturdy barrier.");
        var materialDescriptor = new Descriptor("Wood", "A natural, organic material");
        when(materialEntity.get(Descriptor.class)).thenReturn(materialDescriptor);
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(null);
        when(entity.get(Material.class)).thenReturn(new Material(materialEntity));
        when(entity.get(Socket.class)).thenReturn(null);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Door. A sturdy barrier. It's made from Wood.", result);
    }

    @Test
    void composeIncludesSocketAliveMessage() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Robot", "A mechanical construct.");
        var health = new Health(100, 50);
        var socket = new Socket();
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(socket);

        String result = DescriptionComposer.compose(entity);

        assertEquals(
            "It's a Robot. A mechanical construct. It looks nearly dead. "
                + "Could be hijacked, if defeated.",
            result
        );
    }

    @Test
    void composeIncludesSocketDeadMessage() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Robot", "A mechanical construct.");
        var health = new Health(100, 0);
        var socket = new Socket();
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(socket);

        String result = DescriptionComposer.compose(entity);

        assertEquals(
            "It's a Robot. A mechanical construct. It's dead. Is prime to be hijacked.",
            result
        );
    }

    @Test
    void composeSocketWithoutHealthDoesNotIncludeSocketMessage() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Robot", "A mechanical construct.");
        var socket = new Socket();
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(null);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(socket);

        String result = DescriptionComposer.compose(entity);

        assertEquals("It's a Robot. A mechanical construct.", result);
    }

    @Test
    void composeIncludesControllingMessageWhenPlayerSocketed() {
        Entity entity = mock(Entity.class);
        Entity playerEntity = mock(Entity.class);
        var descriptor = new Descriptor("Robot", "A mechanical construct.");
        var health = new Health(100, 100);
        var socket = new Socket();
        socket.socketedEntity = playerEntity;
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(socket);

        String result = DescriptionComposer.compose(entity);

        assertEquals(
            "It's a Robot. A mechanical construct. It looks healthy. You're controlling it.",
            result
        );
    }

    @Test
    void composeIncludesAllComponentsInCorrectOrder() {
        Entity entity = mock(Entity.class);
        Entity materialEntity = mock(Entity.class);
        var descriptor = new Descriptor("Mech", "A giant robot.");
        var materialDescriptor = new Descriptor("Steel", "A strong, refined metal alloy");
        var health = new Health(200, 200);
        var socket = new Socket();
        when(materialEntity.get(Descriptor.class)).thenReturn(materialDescriptor);
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(new Material(materialEntity));
        when(entity.get(Socket.class)).thenReturn(socket);

        String result = DescriptionComposer.compose(entity);

        assertEquals(
            "It's a Mech. A giant robot. It looks healthy. It's made from Steel. "
                + "Could be hijacked, if defeated.",
            result
        );
    }

    @Test
    void composeIncludesDestroyedMessageWhenSocketDestroyed() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Robot", "A mechanical construct.");
        var health = new Health(100, 0);
        var socket = new Socket();
        socket.destroyed = true;
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Health.class)).thenReturn(health);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(socket);

        String result = DescriptionComposer.compose(entity);

        assertEquals(
            "It's a Robot. A mechanical construct. It's dead. It's destroyed and unusable.",
            result
        );
    }

    @Test
    void composeHandlesBoundaryHealthPercentages() {
        Entity entity = mock(Entity.class);
        var descriptor = new Descriptor("Target", "Test target.");
        when(entity.get(Descriptor.class)).thenReturn(descriptor);
        when(entity.get(Material.class)).thenReturn(null);
        when(entity.get(Socket.class)).thenReturn(null);

        // Test exactly 75%
        var health75 = new Health(100, 75);
        when(entity.get(Health.class)).thenReturn(health75);
        String result75 = DescriptionComposer.compose(entity);
        assertEquals("It's a Target. Test target. It looks slightly damaged.", result75);

        // Test exactly 25%
        var health25 = new Health(100, 25);
        when(entity.get(Health.class)).thenReturn(health25);
        String result25 = DescriptionComposer.compose(entity);
        assertEquals("It's a Target. Test target. It looks nearly dead.", result25);

        // Test just below 75%
        var health74 = new Health(100, 74);
        when(entity.get(Health.class)).thenReturn(health74);
        String result74 = DescriptionComposer.compose(entity);
        assertEquals("It's a Target. Test target. It looks nearly dead.", result74);
    }
}
