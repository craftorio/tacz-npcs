package com.corrinedev.tacznpcs.common.entity.behavior;

import com.corrinedev.tacznpcs.common.ScavPlayer;
import com.corrinedev.tacznpcs.common.entity.AbstractScavEntity;
import com.mojang.datafixers.util.Pair;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.item.ModernKineticGunItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.util.BrainUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TaczShootAttack<E extends LivingEntity & SmartBrainOwner<E>> extends ExtendedBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS;
    protected float attackRadius;
    protected @Nullable LivingEntity target = null;

    static {
        MEMORY_REQUIREMENTS = ObjectArrayList.of(
                Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                Pair.of(MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT)
        );
    }

    public TaczShootAttack() {
        this(32);
    }

    public TaczShootAttack(int attackRadius) {
        super();
        this.attackRadius = attackRadius;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E owner) {
        this.target = BrainUtils.getTargetOfEntity(owner);
        return this.target != null && BrainUtils.canSee(owner, this.target);
    }

    @Override
    protected void start(E owner) {
        LivingEntity tgt = BrainUtils.getTargetOfEntity(owner);
        if (tgt == null) return;

        if (BehaviorUtils.entityIsVisible(owner.getBrain(), tgt) && owner.hasLineOfSight(tgt)) {
            owner.lookAt(EntityAnchorArgument.Anchor.EYES, tgt.getEyePosition(1f));
            BehaviorUtils.lookAtEntity(owner, tgt);

            if (owner.getMainHandItem().getItem() instanceof ModernKineticGunItem) {
                IGunOperator op = IGunOperator.fromLivingEntity(owner);

                op.aim(true);
                ShootResult result = op.shoot(() -> owner.getViewXRot(1f), () -> owner.getViewYRot(1f));

                if (result == ShootResult.NEED_BOLT) {
                    op.bolt();
                } else if (result == ShootResult.SUCCESS) {
                    if (owner instanceof AbstractScavEntity a) {
                        a.firing = true;
                        a.collectiveShots++;
                    } else if (owner instanceof ScavPlayer.InternalPathfinder pf) {
                        pf.firing = true;
                        pf.collectiveShots++;
                    }
                }

                BrainUtils.setForgettableMemory(owner, MemoryModuleType.ATTACK_COOLING_DOWN, true, 1);
            }
        }
    }

    @Override
    public List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }
}
