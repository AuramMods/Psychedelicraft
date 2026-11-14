/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import java.util.Random;

public final class CameraEffects {
    private static final Random NOISE = new Random();
    private static EntityPlayerSP owner;
    private static DriftingCamera drift = new DriftingCamera();
    private static CameraInertia inertia = new CameraInertia();
    private static ModelPose pose;

    private CameraEffects() {
    }

    static void reset(EntityPlayerSP player) {
        owner = player;
        drift = new DriftingCamera();
        inertia = new CameraInertia();
    }

    static void tick(EntityPlayerSP player, DrugProperties drugs) {
        drift.update(drugs.getModifier(Drug::weightlessness), player.ticksExisted, player.motionX, player.motionZ, player.getRNG());
        inertia.tick(drugs.getModifier(Drug::headMotionInertia), CameraInertia.sensitivity(Minecraft.getMinecraft().gameSettings.mouseSensitivity));
    }

    static void transformView(EntityViewRenderEvent.CameraSetup event) {
        Minecraft client = Minecraft.getMinecraft();
        EntityPlayerSP player = client.player;
        DrugProperties drugs = player == null ? null : DrugCapability.get(player);
        if (drugs == null) return;
        float partial = (float) event.getRenderPartialTicks();
        float tick = player.ticksExisted + partial;
        if (player == owner) {
            Vec3d position = drift.position(partial);
            Vec3d rotation = drift.rotation(partial);
            if (position.lengthSquared() > 0 || rotation.lengthSquared() > 0) {
                // Conjugate the world-space drift by the camera rotation Forge applies after this event.
                GlStateManager.rotate(event.getRoll(), 0, 0, 1);
                GlStateManager.rotate(event.getPitch(), 1, 0, 0);
                GlStateManager.rotate(event.getYaw(), 0, 1, 0);
                GlStateManager.translate(position.x, position.y, position.z);
                GlStateManager.rotate((float) Math.toDegrees(rotation.x), 1, 0, 0);
                GlStateManager.rotate((float) Math.toDegrees(rotation.y), 0, 1, 0);
                GlStateManager.rotate((float) Math.toDegrees(rotation.z), 0, 0, 1);
                GlStateManager.rotate(-event.getYaw(), 0, 1, 0);
                GlStateManager.rotate(-event.getPitch(), 1, 0, 0);
                GlStateManager.rotate(-event.getRoll(), 0, 0, 1);
            }
        }
        float wobble = Math.min(1F, drugs.getModifier(Drug::viewWobblyness));
        float amount = drugs.blendStrength(0F, Drug::viewTrembleStrength);
        Entity view = event.getEntity();
        boolean frontView = (wobble > 0F || amount > 0F) && client.gameSettings.thirdPersonView == 2
                && !(view instanceof EntityLivingBase && ((EntityLivingBase) view).isPlayerSleeping());
        // Forge has already turned the front-facing camera around. Keep wobble and tremor in screen axes.
        if (frontView) GlStateManager.rotate(-180F, 0F, 1F, 0F);
        wobble(tick, wobble);
        if (amount > 0F) GlStateManager.translate(noise(tick) * 0.05F * amount, verticalTremble(tick, amount), 0F);
        if (frontView) GlStateManager.rotate(180F, 0F, 1F, 0F);
    }

    private static void wobble(float tick, float strength) {
        if (strength <= 0F) return;
        float base = 5F / (strength * strength + 5F) - strength * 0.04F;
        float square = base * base;
        float angle = tick * 3F;
        GlStateManager.rotate(angle, 0, 1, 0);
        GlStateManager.rotate(angle, 0, 0, 1);
        GlStateManager.scale(axisScale(tick, 150F, strength, square), axisScale(tick, 170F, strength, square),
                axisScale(tick, 190F, strength, square));
        GlStateManager.rotate(-angle, 0, 0, 1);
        GlStateManager.rotate(-angle, 0, 1, 0);
    }

    private static float axisScale(float tick, float period, float strength, float square) {
        return 1F / (square + strength * MathHelper.sin(tick / period * (float) Math.PI) / 2F);
    }

    public static int bodyView(int vanillaView) {
        Minecraft client = Minecraft.getMinecraft();
        return vanillaView == 0 && owner != null && owner == client.player && owner == client.getRenderViewEntity()
                && drift.isOutOfBody() ? 1 : vanillaView;
    }

    public static void turn(EntityPlayerSP player, float x, float y, float partial) {
        Minecraft client = Minecraft.getMinecraft();
        DrugProperties drugs = player == owner && !client.isGamePaused() ? DrugCapability.get(player) : null;
        if (drugs != null && drugs.getModifier(Drug::headMotionInertia) > 0F) {
            float sensitivity = CameraInertia.sensitivity(client.gameSettings.mouseSensitivity);
            int invert = client.gameSettings.invertMouse ? -1 : 1;
            Vec2f change = inertia.sample(client.mouseHelper.deltaX * sensitivity,
                    client.mouseHelper.deltaY * sensitivity, partial);
            if (client.gameSettings.smoothCamera) {
                x += change.x;
                y += change.y * invert;
            } else {
                x = change.x;
                y = change.y * invert;
            }
        }
        player.turn(x, y);
    }

    public static void shiftHand(float partial) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        DrugProperties drugs = player == null ? null : DrugCapability.get(player);
        if (drugs == null) return;
        float hand = drugs.blendStrength(0F, Drug::handTrembleStrength);
        float view = drugs.blendStrength(0F, Drug::viewTrembleStrength);
        if (hand <= 0F && view <= 0F) return;
        float tick = player.ticksExisted + partial;
        GlStateManager.translate(noise(tick) * 0.015F * hand, verticalTremble(tick, view) * 0.3F, 0F);
    }

    public static void posePlayer(ModelBase base, EntityLivingBase entity, float partial) {
        if (!(base instanceof ModelBiped) || !(entity instanceof EntityPlayer)) return;
        DrugProperties drugs = DrugCapability.get((EntityPlayer) entity);
        if (drugs == null) return;
        float view = drugs.blendStrength(0F, Drug::viewTrembleStrength);
        float hand = drugs.blendStrength(0F, Drug::handTrembleStrength);
        float alcohol = (float) Math.min(1, drugs.getActiveValue(DrugType.ALCOHOL) + drugs.getActiveValue(DrugType.KAVA));
        if (view <= 0F && hand <= 0F && alcohol <= 0F) return;

        ModelBiped model = (ModelBiped) base;
        pose = new ModelPose(model, entity, pose);
        float tick = entity.ticksExisted + partial;
        float noise = noise(tick);
        float shift = verticalTremble(tick, view);
        model.bipedHead.rotateAngleX += noise * 0.05F * view;
        model.bipedHead.rotateAngleY += shift;
        model.bipedHead.rotateAngleZ = alcohol;
        model.bipedLeftArm.rotateAngleX += noise * 0.03F * hand;
        model.bipedRightArm.rotateAngleX -= noise * 0.03F * hand;
        model.bipedLeftArm.rotateAngleZ += shift * 0.3F;
        model.bipedRightArm.rotateAngleZ -= shift * 0.3F;
        copyOverlays(model);
    }

    public static void restorePose(ModelBase model, EntityLivingBase entity) {
        ModelPose saved = pose;
        if (saved == null || saved.model != model || saved.entity != entity) return;
        saved.restore();
        pose = saved.previous;
    }

    private static void copyOverlays(ModelBiped model) {
        ModelBase.copyModelAngles(model.bipedHead, model.bipedHeadwear);
        if (model instanceof ModelPlayer) {
            ModelPlayer player = (ModelPlayer) model;
            ModelBase.copyModelAngles(model.bipedLeftArm, player.bipedLeftArmwear);
            ModelBase.copyModelAngles(model.bipedRightArm, player.bipedRightArmwear);
        }
    }

    static float noise(float tick) {
        NOISE.setSeed((long) (tick * 1000F));
        return NOISE.nextFloat() - 0.5F;
    }

    static float verticalTremble(float tick, float strength) {
        float sine = MathHelper.sin(tick / 3F);
        return sine * sine * strength * 0.1F;
    }

    private static final class ModelPose {
        private final ModelBiped model;
        private final EntityLivingBase entity;
        private final ModelPose previous;
        private final float headX;
        private final float headY;
        private final float headZ;
        private final float leftX;
        private final float leftZ;
        private final float rightX;
        private final float rightZ;

        private ModelPose(ModelBiped model, EntityLivingBase entity, ModelPose previous) {
            this.model = model;
            this.entity = entity;
            this.previous = previous;
            headX = model.bipedHead.rotateAngleX;
            headY = model.bipedHead.rotateAngleY;
            headZ = model.bipedHead.rotateAngleZ;
            leftX = model.bipedLeftArm.rotateAngleX;
            leftZ = model.bipedLeftArm.rotateAngleZ;
            rightX = model.bipedRightArm.rotateAngleX;
            rightZ = model.bipedRightArm.rotateAngleZ;
        }

        private void restore() {
            model.bipedHead.rotateAngleX = headX;
            model.bipedHead.rotateAngleY = headY;
            model.bipedHead.rotateAngleZ = headZ;
            model.bipedLeftArm.rotateAngleX = leftX;
            model.bipedLeftArm.rotateAngleZ = leftZ;
            model.bipedRightArm.rotateAngleX = rightX;
            model.bipedRightArm.rotateAngleZ = rightZ;
            copyOverlays(model);
        }
    }
}
