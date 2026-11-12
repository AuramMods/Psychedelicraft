/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

final class RastaHeadHallucination {
    private static final ResourceLocation TEXTURE = Psychedelicraft.id("textures/drug/cannabis/rasta_head_hallucination.png");
    private static final RastaHeadModel MODEL = new RastaHeadModel();

    private final EntityPlayerSP owner;
    private final EntityPig head;
    final RastaHeadChat chat;
    private final int maxAge;
    private final float scale;
    private final float distance;
    private final float planeRotationY;
    private final float planeRotationZ;
    private Vec3d previousPosition;
    private int age;

    RastaHeadHallucination(EntityPlayerSP owner, Vec3d position) {
        this.owner = owner;
        head = new EntityPig(owner.world);
        head.setPosition(position.x, position.y, position.z);
        previousPosition = position;
        chat = new RastaHeadChat(owner);
        Random random = owner.getRNG();
        maxAge = (random.nextInt(59) + 120) * 20;
        scale = 1 + random.nextFloat() / 2F;
        distance = 2 + random.nextFloat() * 5;
        planeRotationY = random.nextFloat() * (float) Math.PI / 2F;
        planeRotationZ = random.nextFloat() * (float) Math.PI / 2F;
    }

    boolean tick(boolean visible) {
        if (++age >= maxAge) return true;
        previousPosition = head.getPositionVector();
        head.prevRotationYawHead = head.rotationYawHead;
        head.prevRotationPitch = head.rotationPitch;
        head.getLookHelper().setLookPositionWithEntity(owner, 10F, 40F);
        head.getLookHelper().onUpdateLook();

        int seed = owner.ticksExisted + head.getEntityId() * 3;
        Vec3d offset = new Vec3d(MathHelper.sin(seed / 50F) * distance,
                MathHelper.sin(seed / 10F) + head.getEntityId() % 5 - 1,
                MathHelper.cos(seed / 50F) * distance).rotateYaw(planeRotationY);
        float sin = MathHelper.sin(planeRotationZ);
        float cos = MathHelper.cos(planeRotationZ);
        offset = new Vec3d(offset.x * cos + offset.y * sin, offset.y * cos - offset.x * sin, offset.z);
        Vec3d delta = owner.getPositionEyes(1F).add(offset).subtract(previousPosition);
        double length = delta.lengthVector();
        if (length > 0.0001) {
            Vec3d position = previousPosition.add(delta.scale(Math.log(length) / length));
            head.setPosition(position.x, position.y, position.z);
        }
        chat.tick(visible);
        return false;
    }

    void render(Minecraft client, float partialTicks, float strength) {
        float lifeAlpha = MathHelper.sin((float) Math.min(age, maxAge - 2) / (maxAge - 2) * (float) Math.PI) * 18;
        float alpha = MathHelper.clamp(lifeAlpha, 0F, 1F) * strength;
        if (alpha <= 0.001F) return;
        Vec3d position = previousPosition.add(head.getPositionVector().subtract(previousPosition).scale(partialTicks));
        RenderManager render = client.getRenderManager();
        float yaw = head.prevRotationYawHead + MathHelper.wrapDegrees(head.rotationYawHead - head.prevRotationYawHead) * partialTicks;
        float pitch = head.prevRotationPitch + (head.rotationPitch - head.prevRotationPitch) * partialTicks;

        int light = head.getBrightnessForRender();
        GlStateManager.pushMatrix();
        client.entityRenderer.enableLightmap();
        RenderHelper.enableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        try {
            GlStateManager.translate(position.x - render.viewerPosX, position.y - render.viewerPosY, position.z - render.viewerPosZ);
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.rotate(180F, 0, 0, -1);
            GlStateManager.rotate(pitch, -1, 0, 0);
            GlStateManager.rotate(180F - yaw, 0, -1, 0);
            GlStateManager.color(1F, 1F, 1F, alpha);
            client.getTextureManager().bindTexture(TEXTURE);
            MODEL.render();
        } finally {
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            RenderHelper.disableStandardItemLighting();
            client.entityRenderer.disableLightmap();
            GlStateManager.popMatrix();
        }
    }
}
