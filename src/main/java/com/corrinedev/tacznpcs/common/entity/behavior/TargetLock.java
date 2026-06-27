package com.corrinedev.tacznpcs.common.entity.behavior;

import com.corrinedev.tacznpcs.common.ScavPlayer;
import com.corrinedev.tacznpcs.common.entity.AbstractScavEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.tslat.smartbrainlib.util.BrainUtils;

/**
 * Shared target-lock duration for scav NPCs.
 */
public final class TargetLock {
    public static final int LOCK_TICKS = 40;

    private TargetLock() {
    }

    public static boolean isLocked(LivingEntity entity) {
        if (entity instanceof AbstractScavEntity scav) {
            return scav.isTargetLocked();
        }
        if (entity instanceof ScavPlayer.InternalPathfinder pathfinder) {
            return pathfinder.isTargetLocked();
        }
        return false;
    }

    /** Switch to attacker and hold that target for {@link #LOCK_TICKS} ticks. */
    public static void retaliate(LivingEntity entity, LivingEntity attacker) {
        if (attacker == null || !attacker.isAlive() || attacker == entity) {
            return;
        }
        BrainUtils.setTargetOfEntity(entity, attacker);
        if (entity instanceof Mob mob) {
            mob.setTarget(attacker);
        }
        BrainUtils.clearMemory(entity, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        refreshLock(entity);
    }

    public static void refreshLock(LivingEntity entity) {
        if (entity instanceof AbstractScavEntity scav) {
            scav.lockTarget();
        } else if (entity instanceof ScavPlayer.InternalPathfinder pathfinder) {
            pathfinder.lockTarget();
        }
    }

    public static boolean shouldInvalidate(LivingEntity entity, LivingEntity target) {
        if (isLocked(entity)) {
            return false;
        }
        if (target instanceof Player player && player.getAbilities().invulnerable) {
            return true;
        }
        if (entity.getAttributes().hasAttribute(Attributes.FOLLOW_RANGE)) {
            double range = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
            return entity.distanceToSqr(target) > range * range;
        }
        return false;
    }
}
