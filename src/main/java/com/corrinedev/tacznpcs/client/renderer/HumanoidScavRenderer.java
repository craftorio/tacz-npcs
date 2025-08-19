package com.corrinedev.tacznpcs.client.renderer;

import com.corrinedev.tacznpcs.common.entity.AbstractScavEntity;
import com.corrinedev.tacznpcs.common.entity.DutyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class HumanoidScavRenderer extends HumanoidMobRenderer<AbstractScavEntity, HumanoidModel<AbstractScavEntity>> {

    public static final ResourceLocation banditTexture1 = new ResourceLocation("tacz_npc", "textures/entity/bandit_1.png");
    public static final ResourceLocation banditTexture2 = new ResourceLocation("tacz_npc", "textures/entity/bandit_2.png");
    public static final ResourceLocation banditTexture3 = new ResourceLocation("tacz_npc", "textures/entity/bandit_3.png");
    public static final ResourceLocation banditTexture4 = new ResourceLocation("tacz_npc", "textures/entity/bandit_4.png");

    public static final ResourceLocation dutyTexture1 = new ResourceLocation("tacz_npc", "textures/entity/duty_1.png");
    public static final ResourceLocation dutyTexture2 = new ResourceLocation("tacz_npc", "textures/entity/duty_2.png");
    public static final ResourceLocation dutyTexture3 = new ResourceLocation("tacz_npc", "textures/entity/duty_3.png");
    public static final ResourceLocation dutyTexture4 = new ResourceLocation("tacz_npc", "textures/entity/duty_4.png");

    private static final ResourceLocation[] BANDIT_SKINS = {
            banditTexture1, banditTexture2, banditTexture3, banditTexture4
    };
    private static final ResourceLocation[] DUTY_SKINS = {
            dutyTexture1, dutyTexture2, dutyTexture3, dutyTexture4
    };

    public final HumanoidArmorLayer<AbstractScavEntity, HumanoidModel<AbstractScavEntity>, HumanoidModel<AbstractScavEntity>> slimLayer;
    public final HumanoidArmorLayer<AbstractScavEntity, HumanoidModel<AbstractScavEntity>, HumanoidModel<AbstractScavEntity>> thickLayer;

    public HumanoidScavRenderer(EntityRendererProvider.Context pContext, HumanoidModel<AbstractScavEntity> pModel) {
        super(pContext, pModel, 0.5f);

        thickLayer = new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(pContext.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidArmorModel<>(pContext.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), pContext.getModelManager());
        slimLayer = new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(pContext.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)), new HumanoidArmorModel<>(pContext.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)), pContext.getModelManager());
    }

    @Override
    public void render(AbstractScavEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (pEntity.isSlim() && !layers.contains(slimLayer) && !layers.contains(thickLayer)) {
            addLayer(slimLayer);
        } else if (!layers.contains(thickLayer) && !layers.contains(slimLayer)) {
            addLayer(thickLayer);
        }
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractScavEntity entity) {
        ResourceLocation[] pool = (entity instanceof DutyEntity) ? DUTY_SKINS : BANDIT_SKINS;
        long seed = entity.getUUID().getLeastSignificantBits();
        return pool[Math.floorMod(seed, pool.length)];
    }
}
