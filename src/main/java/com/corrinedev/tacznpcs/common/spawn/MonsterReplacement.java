package com.corrinedev.tacznpcs.common.spawn;

import com.corrinedev.tacznpcs.NPCS;
import com.corrinedev.tacznpcs.common.registry.EntityTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = NPCS.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MonsterReplacement {
    private static final String NBT_FLAG = NPCS.MODID + ":replaced";

    private static final Map<ResourceLocation, Supplier<EntityType<?>>> REPLACEMENTS = new HashMap<>();
    static {
        // vanilla
        REPLACEMENTS.put(new ResourceLocation("minecraft", "pillager"),
                () -> EntityTypeRegistry.BANDIT.get());

        // guardvillagers
        REPLACEMENTS.put(new ResourceLocation("guardvillagers", "guard"),
                () -> EntityTypeRegistry.DUTY.get());
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // работаем только на сервере
        if (event.getLevel().isClientSide()) return;

        final Entity original = event.getEntity();
        final ServerLevel level = (ServerLevel) event.getLevel();

        // уже заменяли — выходим
        if (original.getPersistentData().getBoolean(NBT_FLAG)) return;

        // не трогаем наших
        if (original.getType() == EntityTypeRegistry.BANDIT.get()
                || original.getType() == EntityTypeRegistry.DUTY.get()) return;

        final ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(original.getType());
        if (key == null) return;

        final Supplier<EntityType<?>> sup = REPLACEMENTS.get(key);
        if (sup == null) return;

        // ⚠️ КРИТИЧЕСКОЕ: отменяем добавление исходника — клиент не получит spawn-пакет
        if (event.isCancelable()) event.setCanceled(true);

        final EntityType<?> replType = sup.get();
        final Entity replacement = replType.create(level);
        if (replacement == null) return;

        // переносим базовые свойства/позицию/угол/имя
        replacement.moveTo(original.getX(), original.getY(), original.getZ(),
                original.getYRot(), original.getXRot());
        if (original.hasCustomName()) {
            replacement.setCustomName(original.getCustomName());
            replacement.setCustomNameVisible(original.isCustomNameVisible());
        }

        // если оба — LivingEntity: копируем экипировку, огонь и т.д.
//        if (original instanceof LivingEntity src && replacement instanceof LivingEntity dst) {
//            // экипировка
//            for (EquipmentSlot slot : EquipmentSlot.values()) {
//                ItemStack stack = src.getItemBySlot(slot);
//                if (!stack.isEmpty()) dst.setItemSlot(slot, stack.copy());
//            }
//            // огонь
//            dst.setSecondsOnFire(src.getRemainingFireTicks() / 20);
//        }

        // ВАЖНО: persistence только для Mob
//        if (replacement instanceof Mob dstMob) {
//            // если исходник был мобом и был «постоянным», перенесём флаг
//            if (original instanceof Mob srcMob && srcMob.isPersistenceRequired()) {
//                dstMob.setPersistenceRequired(); // 1.19.2 — без аргументов
//            } else {
//                // или просто всегда делаем постоянным, если так нужно
//                dstMob.setPersistenceRequired();
//            }
//        }

        // помечаем, чтобы наш обработчик не трогал нашу же замену
        replacement.getPersistentData().putBoolean(NBT_FLAG, true);

        level.addFreshEntity(replacement);
    }
}
