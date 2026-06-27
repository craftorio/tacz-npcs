package com.corrinedev.tacznpcs.common.entity;

import com.tacz.guns.init.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.AvoidEntity;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.StrafeTarget;
import com.corrinedev.tacznpcs.common.entity.behavior.NearestTargetOrRetaliate;
import com.corrinedev.tacznpcs.common.entity.behavior.TargetLock;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

import static com.corrinedev.tacznpcs.NPCS.MODID;


public class DutyEntity extends AbstractScavEntity {
    public static final EntityType<DutyEntity> DUTY;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    @javax.annotation.Nullable
    private UUID persistentAngerTarget;
    private LivingEntity currentAngerTarget;
    private static final int ALERT_RANGE_Y = 10;
    private static final UniformInt ALERT_INTERVAL = TimeUtil.rangeOfSeconds(4, 6);
    private static final int ALLY_ALERT_COOLDOWN_TICKS = 40;
    private int allyAlertCooldown = 0;
    private boolean angry = false;

    static {
        DUTY = EntityType.Builder.of(DutyEntity::new, MobCategory.MISC).sized(0.65f, 1.95f).build("duty");
    }

    protected DutyEntity(EntityType<? extends PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
        applySpawnLoadout(p_21684_, new ResourceLocation(MODID, "duty"));
        for (int i = 0; i < this.inventory.getContainerSize() - 1; i++) {
            if (inventory.getItem(i).getItem() instanceof PatchItem r) {
                this.setCustomName(Component.translatable("entity.tacz_npc.named",
                        Component.translatable(r.rank.getTranslationKey()), this.getName()));
                inventory.getItem(i).getOrCreateTag().putString("type", "duty");
                inventory.getItem(i).setHoverName(Component.translatable("item.tacz_npc.patch.duty",
                        Component.translatable(r.rank.getTranslationKey())));
                switch (r.rank) {
                    case ROOKIE -> {

                    }
                    case EXPERIENCED -> {

                    }
                    case EXPERT -> {

                    }
                    case VETERAN -> {

                    }
                }
            }
        }
    }

    @Override
    public boolean isSlim() {
        return false;
    }

    @Override
    public boolean allowInventory(Player player) {
        if (this.deadAsContainer) {
            return true;
        }
        return lastHurtByPlayer != player;
    }

    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (pSource.getEntity() instanceof DutyEntity) {
            return false;
        }
        this.panic = true;
        paniccooldown = 60;
        if (pSource.getEntity() instanceof LivingEntity entity) {
            this.currentAngerTarget = entity;
            if (isAttackableTarget(entity)) {
                TargetLock.retaliate(this, entity);
            }
            if (allyAlertCooldown <= 0) {
                allyAlertCooldown = ALLY_ALERT_COOLDOWN_TICKS;
                List<DutyEntity> entities = this.level().getEntitiesOfClass(DutyEntity.class, AABB.ofSize(this.position(), 64, 16, 64));
                for (DutyEntity duty : entities) {
                    if (duty == this || duty.currentAngerTarget == entity) {
                        continue;
                    }
                    if (duty.hasLineOfSight(entity) || BehaviorUtils.entityIsVisible(duty.getBrain(), entity)) {
                        duty.currentAngerTarget = entity;
                        if (isAttackableTarget(entity)) {
                            TargetLock.retaliate(duty, entity);
                        }
                    }
                }
            }
        }

        return super.hurt(pSource, pAmount);
    }

    @Override
    public List<ExtendedSensor<AbstractScavEntity>> getSensors() {
        return ObjectArrayList.of(
                new NearbyPlayersSensor<AbstractScavEntity>().setPredicate((p, e) -> e.attackers.contains(p)),
                new HurtBySensor<>(),
                new NearbyLivingEntitySensor<AbstractScavEntity>()
                        .setPredicate((target, entity) -> target == this.currentAngerTarget ||
                                (target instanceof BanditEntity bandit && !bandit.deadAsContainer) ||
                                (target instanceof Monster && !(target instanceof BanditEntity)) ||
                                target.getType().getCategory() == MobCategory.MONSTER));
    }

    @Override
    public void tick() {
        if (allyAlertCooldown > 0) {
            allyAlertCooldown--;
        }
        if (this.getTarget() == null && this.angry) {
            angry = false;
        }
        if (this.currentAngerTarget != null && this.currentAngerTarget instanceof AbstractScavEntity scav && scav.deadAsContainer) {
            this.setTarget(null);
            this.currentAngerTarget = null;
        }
        if (this.currentAngerTarget != null && !this.currentAngerTarget.isAlive()) {
            this.currentAngerTarget = null;
        }
        super.tick();
    }

    public void setTarget(@Nullable LivingEntity pLivingEntity) {
        if (this.getTarget() == null && pLivingEntity != null) {
            ALERT_INTERVAL.sample(this.random);
        }

        if (pLivingEntity instanceof Player) {
            this.setLastHurtByPlayer((Player) pLivingEntity);
        }

        super.setTarget(pLivingEntity);
    }

    @Override
    public BrainActivityGroup<AbstractScavEntity> getCoreTasks() {
        return BrainActivityGroup.coreTasks(new Behavior[]{
                //new StayNearHome<>(e -> (e instanceof DutyEntity d) ? d.homePos : null, HOME_RADIUS, 1.05f),

                // выбор цели — ближайшая видимая
                new NearestTargetOrRetaliate<DutyEntity>()
                        //.isAllyIf((e, l) -> l instanceof DutyEntity)
                        // союзники = любые НЕ монстры (включая игроков, жителей, големов, животных и т.д.)
                        .isAllyIf((e, l) -> l != null && l.getType().getCategory() != MobCategory.MONSTER)
                        .attackablePredicate(l -> l != null && this.hasLineOfSight(l))
                        .alertAlliesWhen((m, e) -> e != null && m.hasLineOfSight(e))
                        .runFor(e -> 999),

                // ЗАМЕНА Panic<>: «паническое» отступление при низком HP
                (new AvoidEntity<>())
                        .noCloserThan(16)
                        .avoiding(entity -> entity == this.getTarget())
                        .speedModifier(1.1f)
                        .startCondition(e -> this.getHealth() <= 5)
                        .whenStarting(e -> this.panic = true)
                        .whenStopping(e -> this.panic = false)
                        .stopIf(e -> this.getTarget() == null || !this.getTarget().hasLineOfSight(this))
                        .runFor(e -> 20),

                (new LookAtTarget<>())
                        .runFor(entity -> entity.getRandom().nextInt(40, 300)),

                new OneRandomBehaviour<>(new ExtendedBehaviour[]{
                        (new StrafeTarget<>())
                                .speedMod(0.85f)
                                .strafeDistance(14)
                                .stopStrafingWhen(entity ->
                                        this.getTarget() == null || !this.isUsingGun() || this.panic)
                                .startCondition(e -> this.isUsingGun() && !this.panic),
                        new MoveToWalkTarget<>()
                })
        });
    }

    private static boolean isAttackableTarget(LivingEntity entity) {
        return entity instanceof Player
                || entity instanceof BanditEntity
                || entity instanceof Monster
                || entity.getType().getCategory() == MobCategory.MONSTER;
    }
}
