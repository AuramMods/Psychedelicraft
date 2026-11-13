/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.util.Random;

final class ScreenOverlays {
    private static final ResourceLocation WARMTH = Psychedelicraft.id("textures/drug/coffee/overlay.png");
    private static final ResourceLocation HURT = Psychedelicraft.id("textures/environment/hurt_overlay.png");
    private static final ResourceLocation EYELIDS = Psychedelicraft.id("textures/environment/eyelid_overlay.png");
    private static final ResourceLocation GLARE = Psychedelicraft.id("textures/environment/lense_flare/sun_blindness.png");
    private static final float[] FLARE_SIZES = {
            .15F, .24F, .12F, .036F, .06F, .048F, .006F, .012F, .5F, .09F, .036F, .09F, .06F, .05F, .6F
    };
    private static final float[] FLARE_OFFSETS = {
            -1.3F, -2F, .2F, .4F, .25F, -.25F, -.7F, -1F, 1F, 1.4F, -1.31F, -1.2F, -1.5F, -1.55F, -3F
    };
    private static final ResourceLocation[] FLARES = new ResourceLocation[FLARE_SIZES.length];

    static {
        for (int index = 0; index < FLARES.length; index++) {
            FLARES[index] = Psychedelicraft.id("textures/environment/lense_flare/flare" + index + ".png");
        }
    }

    private int blinkTicks;
    private float previousEyelids;
    private float eyelids;
    private float sunAlpha;

    void tick(Minecraft client, EntityPlayerSP player, DrugProperties drugs) {
        float drowsiness = (float) (drugs.getActiveValue(DrugType.SLEEP_DEPRIVATION)
                + drugs.getActiveValue(DrugType.KAVA) * 0.4);
        float tiredness = Math.max(0F, drowsiness - 0.6F);
        previousEyelids = eyelids;
        float target = blinkTicks > 0 ? tiredness * 0.9F + MathHelper.sin(player.ticksExisted / 10F) : 0F;
        eyelids = approach(eyelids, target, 0.03F);
        if (tiredness > 0.3F && eyelids > 0.6F) {
            client.world.playSound(player.posX, player.posY, player.posZ, PSSounds.HEARTBEAT,
                    SoundCategory.AMBIENT, tiredness, 0.3F, false);
        }
        Random random = player.getRNG();
        if (tiredness < 0.2F) {
            blinkTicks = Math.max(blinkTicks - 1, 0);
        } else if (--blinkTicks <= 0 && (blinkTicks < -300 || random.nextFloat() < drowsiness)) {
            blinkTicks = (int) (300 + (random.nextDouble() - random.nextDouble()) * 200);
            player.sendStatusMessage(new TextComponentTranslation("psychedelicraft.sleep.tired"), true);
        }

        Entity camera = client.getRenderViewEntity();
        float sun = 0F;
        if (PSConfig.visual.sunFlareIntensity > 0 && camera != null && client.world.provider.isSurfaceWorld()
                && camera.rayTrace(100, 1F).typeOfHit == RayTraceResult.Type.MISS) {
            Vec3d direction = sunInView(camera, client.world.getCelestialAngleRadians(1F), 1F);
            ScaledResolution size = new ScaledResolution(client);
            if (direction.z > 0) {
                sun = (float) Math.max(0, 1 - Math.hypot(direction.x * size.getScaledWidth(),
                        direction.y * size.getScaledHeight()) / 170);
                sun *= 1F - client.world.getRainStrength(1F);
            }
        }
        sunAlpha = approach(sunAlpha + (sun - sunAlpha) * 0.1F, sun, 0.01F);
    }

    void render(Minecraft client, EntityPlayerSP player, DrugProperties drugs, float partialTicks, float hurt) {
        float alcohol = (float) drugs.getActiveValue(DrugType.ALCOHOL);
        float warmth = (float) drugs.getActiveValue(DrugType.WARMTH) * 0.5F;
        float power = (float) drugs.getActiveValue(DrugType.POWER);
        float blink = MathHelper.clamp(previousEyelids + (eyelids - previousEyelids) * partialTicks, 0F, 1F);
        boolean flare = PSConfig.visual.sunFlareIntensity > 0 && sunAlpha > 0;
        if (!flare && alcohol <= 0 && warmth <= 0 && power <= 0 && hurt <= 0 && blink <= 0) return;

        ScaledResolution resolution = new ScaledResolution(client);
        int width = resolution.getScaledWidth();
        int height = resolution.getScaledHeight();
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int program = OpenGlHelper.shadersSupported ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        boolean lightmap = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, width, height, 0, -1, 1);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        try (TranslucentRenderState ignored = new TranslucentRenderState()) {
            GlStateManager.loadIdentity();
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            if (OpenGlHelper.shadersSupported) OpenGlHelper.glUseProgram(0);

            if (flare) drawFlare(client, width, height, partialTicks);
            if (warmth > 0) drawWarmth(client, width, height, player.ticksExisted, warmth);
            if (alcohol > 0) drawAlcohol(client, width, height, player.ticksExisted + partialTicks, alcohol);
            PowerOverlay.render(client, width, height, player.ticksExisted, partialTicks, power);
            if (hurt > 0) drawVignette(client, width, height, HURT, hurt, (int) ((1F - hurt) * 40F));
            if (blink > 0) {
                drawVignette(client, width, height, EYELIDS, blink * 0.8F, (int) (blink * 5.8F));
            }
        } finally {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(matrixMode);
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (OpenGlHelper.shadersSupported) OpenGlHelper.glUseProgram(program);
            GlStateManager.bindTexture(boundTexture);
            if (!texture) GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            if (lightmap) GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(activeTexture);
        }
    }

    private static float approach(float value, float target, float step) {
        return value + MathHelper.clamp(target - value, -step, step);
    }

    private static Vec3d sunInView(Entity camera, float angle, float partialTicks) {
        float yaw = camera.prevRotationYaw + (camera.rotationYaw - camera.prevRotationYaw) * partialTicks;
        float pitch = camera.prevRotationPitch + (camera.rotationPitch - camera.prevRotationPitch) * partialTicks;
        Vec3d sun = new Vec3d(-MathHelper.sin(angle), MathHelper.cos(angle), 0);
        return sun.rotateYaw((float) Math.toRadians(yaw)).rotatePitch((float) Math.toRadians(pitch));
    }

    private void drawFlare(Minecraft client, int width, int height, float partialTicks) {
        Entity camera = client.getRenderViewEntity();
        if (camera == null) return;
        Vec3d sun = sunInView(camera, client.world.getCelestialAngleRadians(partialTicks), partialTicks);
        if (sun.z <= 0) return;
        float intensity = PSConfig.visual.sunFlareIntensity;
        float size = Math.max(width, height);
        float offsetX = (float) sun.x * width;
        float offsetY = (float) -sun.y * height;
        Vec3d fog = client.world.getFogColor(partialTicks);
        float red = MathHelper.clamp((float) fog.x - 0.1F, 0F, 1F);
        float green = MathHelper.clamp((float) fog.y - 0.1F, 0F, 1F);
        float blue = MathHelper.clamp((float) fog.z - 0.1F, 0F, 1F);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        for (int index = 0; index < FLARES.length; index++) {
            client.getTextureManager().bindTexture(FLARES[index]);
            float alpha = (index == 8 ? 1F : 0.5F) * sunAlpha * intensity;
            drawFlareSprite(width * 0.5F + offsetX * FLARE_OFFSETS[index],
                    height * 0.5F + offsetY * FLARE_OFFSETS[index], FLARE_SIZES[index] * size, red, green, blue, alpha);
        }
        float glare = (float) (sun.z * sun.z - 0.1F) * intensity * 250F;
        if (glare > 0) {
            client.getTextureManager().bindTexture(GLARE);
            drawFlareSprite(width * 0.5F + offsetX, height * 0.5F + offsetY, glare * size,
                    red, green, blue, Math.min(1F, glare / 150F) * sunAlpha);
        }
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    private static void drawFlareSprite(float x, float y, float size, float red, float green, float blue, float alpha) {
        float half = size * 0.5F;
        drawQuad(x - half, y - half, x + half, y + half, 0, 0, 1, 1, red, green, blue, alpha);
    }

    private static void drawAlcohol(Minecraft client, int width, int height, float ticks, float alcohol) {
        TextureAtlasSprite sprite = client.getBlockRendererDispatcher().getBlockModelShapes()
                .getTexture(Blocks.PORTAL.getDefaultState());
        client.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        float alpha = Math.min(0.8F, alcohol + MathHelper.sin(ticks / 80F) * alcohol * 0.5F) * 0.25F;
        drawQuad(0, 0, width, height, sprite.getMinU(), sprite.getMinV(), sprite.getMaxU(), sprite.getMaxV(), 1, 1, 1, alpha);
    }

    private static void drawVignette(Minecraft client, int width, int height, ResourceLocation texture,
                                     float alpha, int margin) {
        client.getTextureManager().bindTexture(texture);
        drawQuad(-margin, -margin, width + margin, height + margin, 0, 0, 1, 1, 1, 1, 1, alpha);
    }

    private static void drawWarmth(Minecraft client, int width, int height, int ticks, float alpha) {
        client.getTextureManager().bindTexture(WARMTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        int segmentWidth = width / 9;
        for (int strip = 0; strip < 3; strip++) {
            int center = (int) (segmentWidth * (strip * 3 + 1.5F));
            for (int row = 1; row < 30; row++) {
                float v0 = (row - 1) / 30F, v1 = row / 30F;
                float fade = 1F - v1;
                float opacity = Math.max(0, alpha - fade * 0.4F);
                float green = 0.5F + fade * 0.3F, blue = 0.35F + fade * 0.1F;
                float x0 = center + MathHelper.sin(v0 * 5F + ticks / 10F) * segmentWidth * 0.25F;
                float x1 = center + MathHelper.sin(v1 * 5F + ticks / 10F) * segmentWidth * 0.25F;
                float y0 = height / 7 + v0 * height / 7 * 5;
                float y1 = height / 7 + v1 * height / 7 * 5;
                vertex(buffer, x1 - segmentWidth, y1, 0, v1, 1, green, blue, opacity);
                vertex(buffer, x1 + segmentWidth, y1, 1, v1, 1, green, blue, opacity);
                vertex(buffer, x0 + segmentWidth, y0, 1, v0, 1, green, blue, opacity);
                vertex(buffer, x0 - segmentWidth, y0, 0, v0, 1, green, blue, opacity);
            }
        }
        DrugRenderer.drawScreenQuads(tessellator);
    }

    private static void drawQuad(float left, float top, float right, float bottom,
                                 float u0, float v0, float u1, float v1, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertex(buffer, left, bottom, u0, v1, red, green, blue, alpha);
        vertex(buffer, right, bottom, u1, v1, red, green, blue, alpha);
        vertex(buffer, right, top, u1, v0, red, green, blue, alpha);
        vertex(buffer, left, top, u0, v0, red, green, blue, alpha);
        DrugRenderer.drawScreenQuads(tessellator);
    }

    private static void vertex(BufferBuilder buffer, float x, float y, float u, float v,
                               float red, float green, float blue, float alpha) {
        buffer.pos(x, y, 0).tex(u, v).color(red, green, blue, alpha).endVertex();
    }
}
