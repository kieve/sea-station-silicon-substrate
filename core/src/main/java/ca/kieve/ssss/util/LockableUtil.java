package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.component.LockId;
import ca.kieve.ssss.component.Lockable;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.context.GameContext;

public final class LockableUtil {
    private LockableUtil() {}

    public static boolean unlock(
            GameContext context, Entity player, Entity target) {
        var lockId = target.get(LockId.class);
        if (lockId == null) {
            context.log().log("It's locked, but has no keyhole.");
            return false;
        }

        var inventory = player.get(Inventory.class);
        if (inventory == null) {
            context.log().log("It's locked. You need a key.");
            return false;
        }

        Entity matchingKey = null;
        for (var item : inventory.items()) {
            var itemLockId = item.get(LockId.class);
            if (itemLockId != null
                    && itemLockId.lockId().equals(lockId.lockId())) {
                matchingKey = item;
                break;
            }
        }

        if (matchingKey == null) {
            context.log().log("It's locked. You need a key.");
            return false;
        }

        var lockable = target.get(Lockable.class);
        if (lockable != null) {
            lockable.isLocked = false;
        }

        var keyDesc = matchingKey.get(Descriptor.class);
        String keyName = keyDesc != null ? keyDesc.name() : "a key";
        context.log().log("You unlock it with " + keyName + ".");

        // Convenience: also open if the entity is openable
        if (target.has(Openable.class)) {
            OpenableUtil.open(context, target);
        }
        return true;
    }

    public static boolean lock(GameContext context, Entity target) {
        var lockable = target.get(Lockable.class);
        if (lockable == null) {
            return false;
        }

        // Convenience: close first if the entity is openable and open
        var openable = target.get(Openable.class);
        if (openable != null && openable.isOpen) {
            if (!OpenableUtil.tryClose(context, target)) {
                return false;
            }
        }

        lockable.isLocked = true;

        var descriptor = target.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "something";
        context.log().log("You lock the " + name + ".");
        return true;
    }
}
