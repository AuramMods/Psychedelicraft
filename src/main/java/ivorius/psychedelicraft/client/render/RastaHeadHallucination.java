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
import net.minecraft.util.math.BlockPos;
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
        head.ticksExisted = ++age;
        chat.tick(visible);
        previousPosition = head.getPositionVector();
        head.prevRotationYawHead = head.rotationYawHead;
        head.prevRotationPitch = head.rotationPitch;
        Vec3d eyes = owner.getPositionEyes(1F);
        lookAt(eyes);

        int seed = owner.ticksExisted + head.getEntityId() * 3;
        Vec3d offset = new Vec3d(MathHelper.sin(seed / 50F) * distance,
                MathHelper.sin(seed / 10F) + head.getEntityId() % 5 - 1,
                MathHelper.cos(seed / 50F) * distance).rotateYaw(planeRotationY);
        float sin = MathHelper.sin(planeRotationZ);
        float cos = MathHelper.cos(planeRotationZ);
        offset = new Vec3d(offset.x * cos + offset.y * sin, offset.y * cos - offset.x * sin, offset.z);
        Vec3d delta = eyes.add(offset).subtract(previousPosition);
        double length = delta.lengthVector();
        Vec3d velocity = length < 1.0E-4D ? Vec3d.ZERO : delta.normalize().scale(Math.log((float) length));
        head.motionX = velocity.x;
        head.motionY = velocity.y;
        head.motionZ = velocity.z;
        Vec3d position = previousPosition.add(velocity);
        head.setPosition(position.x, position.y, position.z);
        return age >= maxAge;
    }

    private void lookAt(Vec3d eyes) {
        double dx = eyes.x - head.posX;
        double dy = eyes.y - (head.posY + head.getEyeHeight());
        double dz = eyes.z - head.posZ;
        if (Math.abs(dx) > 1.0E-5F || Math.abs(dz) > 1.0E-5F) {
            float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180F / (float) Math.PI)) - 90F;
            float yawDelta = MathHelper.wrapDegrees(targetYaw - head.rotationYawHead);
            head.rotationYawHead += MathHelper.clamp(yawDelta, -10F, 10F);
        }
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = (float) (-MathHelper.atan2(dy, horizontal) * (180F / (float) Math.PI));
        head.rotationPitch = MathHelper.clamp(targetPitch, -40F, 40F);
    }

    void render(Minecraft client, float partialTicks, float strength) {
        float lifeAlpha = MathHelper.sin((float) Math.min(age, maxAge - 2) / (maxAge - 2) * (float) Math.PI) * 18;
        float alpha = MathHelper.clamp(lifeAlpha, 0F, 1F) * strength;
        if (alpha <= 0.001F) return;
        Vec3d position = previousPosition.add(head.getPositionVector().subtract(previousPosition).scale(partialTicks));
        RenderManager render = client.getRenderManager();
        float yaw = head.prevRotationYawHead + (head.rotationYawHead - head.prevRotationYawHead) * partialTicks;
        // The donor interpolates toward the entity's already interpolated pitch.
        float pitch = head.prevRotationPitch + (head.rotationPitch - head.prevRotationPitch) * partialTicks * partialTicks;

        BlockPos lightPosition = new BlockPos(position.addVector(0, head.getEyeHeight(), 0));
        int light = head.world.isBlockLoaded(lightPosition) ? head.world.getCombinedLight(lightPosition, 0) : 0;
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
