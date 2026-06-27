package com.corrinedev.tacznpcs.common;

import com.corrinedev.tacznpcs.Config;
import com.corrinedev.tacznpcs.common.entity.AbstractScavEntity;
import com.corrinedev.tacznpcs.common.entity.behavior.TargetLock;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber
public class Events {
    @SubscribeEvent
    public static void onHitByScav(EntityHurtByGunEvent.Pre event) {
        if(event.getAttacker() instanceof AbstractScavEntity && event.getHurtEntity() instanceof Player) {
            event.setBaseAmount((float) (event.getBaseAmount() * Config.NPCDAMAGEPLAYER.get()));
        } else if (event.getAttacker() instanceof AbstractScavEntity) {
            event.setBaseAmount((float) (event.getBaseAmount() * Config.NPCDAMAGE.get()));
        }
    }

    @SubscribeEvent
    public static void onMonsterShotByScav(EntityHurtByGunEvent.Post event) {
        if (event.getHurtEntity() instanceof Monster monster) {
            retaliateMonster(monster, event.getAttacker());
        }
    }

    @SubscribeEvent
    public static void onMonsterHurtByScav(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof Monster monster)) {
            return;
        }
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker) {
            retaliateMonster(monster, attacker);
        }
    }

    private static void retaliateMonster(Monster monster, LivingEntity attacker) {
        if (attacker instanceof AbstractScavEntity scav && scav.isAlive() && !scav.deadAsContainer) {
            TargetLock.retaliate(monster, scav);
        }
    }
    @SubscribeEvent
    public static void openDeadScav(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        List<AbstractScavEntity> scavs = event.getLevel().getEntitiesOfClass(AbstractScavEntity.class, AABB.ofSize(event.getHitVec().getLocation(), 1, 1, 1));
        if (scavs.isEmpty()) {
            return;
        }
        for (AbstractScavEntity entity : scavs) {
            if (entity.deadAsContainer || entity.isDeadOrDying()) {
                entity.openCustomInventoryScreen(event.getEntity());
            }
        }
    }
}
