/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.fluid.physical.PhysicalFluidBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class EnvironmentEffects {
    private float experiencedHealth = 5F;
    private float heat;
    private boolean underwater;
    private int wetTicks;

    void update(EntityPlayer player) {
        World world = player.world;
        BlockPos pos = new BlockPos(player.posX, player.posY, player.posZ);
        BlockPos eyes = new BlockPos(player.getPositionEyes(1F));
        IBlockState eyeBlock = world.getBlockState(eyes);
        Block block = eyeBlock.getBlock();
        boolean inWater = eyeBlock.getMaterial() == Material.WATER
                || (block instanceof PhysicalFluidBlock && ((PhysicalFluidBlock) block).isWaterLike());

        Biome biome = world.getBiome(pos);
        boolean inRain = world.getRainStrength(1F) > 0 && biome.canRain() && biome.getTemperature(pos) >= 0.15F
                && world.getPrecipitationHeight(pos).getY() <= player.posY;
        float targetHeat = biome.getDefaultTemperature();
        if (world.provider.getDimensionType() != DimensionType.NETHER) {
            targetHeat *= sunIntensity(world) * world.getLightFor(EnumSkyBlock.SKY, eyes) / 15F;
        }
        update(player.getHealth(), targetHeat, inWater, inRain);
    }

    void update(float health, float targetHeat, boolean inWater, boolean inRain) {
        PSConfig.Visual options = PSConfig.visual;
        underwater = inWater;
        heat = smooth(heat, inWater ? 0F : targetHeat);
        if (options.hurtOverlayEnabled) experiencedHealth = smooth(experiencedHealth, health);
        if (options.waterOverlayEnabled) {
            wetTicks = MathHelper.clamp(wetTicks - 1 + (inWater ? 20 : 0) + (inRain ? 4 : 0), 0, 100);
        }
    }

    public float heatDistortion() {
        return underwater || !PSConfig.visual.doHeatDistortion ? 0F : MathHelper.clamp((heat - 1F) * 0.0015F, 0F, 0.01F);
    }

    public float waterDistortion(float peyote) {
        float water = underwater && PSConfig.visual.doWaterDistortion ? 0.025F : 0F;
        return Math.max(water, peyote * 0.01073F);
    }

    public float wetOverlay() {
        return underwater || !PSConfig.visual.waterOverlayEnabled ? 0F : Math.min(1F, wetTicks / 80F);
    }

    public float hurtOverlay(int hurtTime, int maxHurtTime, float heartbeat) {
        if (!PSConfig.visual.hurtOverlayEnabled) return 0F;
        float damage = hurtTime > 0 && maxHurtTime > 0 ? (float) hurtTime / maxHurtTime : 0F;
        float flash = Math.max(damage, heartbeat);
        return flash > 0F ? flash : Math.max(0F, (5F - experiencedHealth) / 6F);
    }

    private static float smooth(float value, float target) {
        float lerped = value + (target - value) * 0.01F;
        return lerped + MathHelper.clamp(target - lerped, -0.01F, 0.01F);
    }

    private static float sunIntensity(World world) {
        float angle = ((world.getCelestialAngle(1F) + 0.25F) % 1F) * 2F;
        if (angle > 1F) return 0F;
        float sun = MathHelper.cos((angle - 0.5F) * (float) Math.PI);
        if (world.isRaining()) sun *= 0.5F;
        if (world.isThundering()) sun *= 0.5F;
        return sun;
    }
}
