package com.corrinedev.tacznpcs.common.entity.behavior;

import com.corrinedev.tacznpcs.common.ScavPlayer;
import com.corrinedev.tacznpcs.common.entity.behavior.TargetLock;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Picks the nearest visible attackable entity. Unlike TargetOrRetaliate, does not
 * prefer HURT_BY over closer targets.
 */
public class NearestTargetOrRetaliate<E extends Mob> extends ExtendedBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = ObjectArrayList.of(
            Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT)
    );

    protected Predicate<LivingEntity> canAttackPredicate = LivingEntity::isAlive;
    protected BiPredicate<E, LivingEntity> allyPredicate = (mob, other) ->
            mob.getClass().isAssignableFrom(other.getClass())
                    && BrainUtils.getTargetOfEntity(other) == null;
    protected BiPredicate<E, LivingEntity> alertAlliesPredicate = (mob, target) -> false;
    protected LivingEntity toTarget;

    public NearestTargetOrRetaliate<E> attackablePredicate(Predicate<LivingEntity> predicate) {
        this.canAttackPredicate = predicate;
        return this;
    }

    public NearestTargetOrRetaliate<E> isAllyIf(BiPredicate<E, LivingEntity> predicate) {
        this.allyPredicate = predicate;
        return this;
    }

    public NearestTargetOrRetaliate<E> alertAlliesWhen(BiPredicate<E, LivingEntity> predicate) {
        this.alertAlliesPredicate = predicate;
        return this;
    }

    @Override
    public List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E owner) {
        NearestVisibleLivingEntities visible = BrainUtils.getMemory(owner, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visible == null) {
            return false;
        }

        this.toTarget = visible.findClosest(this.canAttackPredicate).orElse(null);
        if (this.toTarget == null) {
            return false;
        }

        if (this.alertAlliesPredicate.test(owner, this.toTarget)) {
            alertAllies(level, owner);
        }
        return true;
    }

    @Override
    protected void start(E owner) {
        BrainUtils.setTargetOfEntity(owner, this.toTarget);
        BrainUtils.clearMemory(owner, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        TargetLock.refreshLock(owner);
        this.toTarget = null;
    }

    private void alertAllies(ServerLevel level, E owner) {
        double range = owner.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        var allies = level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(range, 10, range),
                entity -> entity != owner
                        && entity instanceof Mob mob
                        && this.allyPredicate.test(owner, mob));
        for (LivingEntity ally : allies) {
            BrainUtils.setTargetOfEntity(ally, this.toTarget);
            TargetLock.refreshLock(ally);
        }
    }
}
