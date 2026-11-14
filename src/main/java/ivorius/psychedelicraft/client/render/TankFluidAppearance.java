/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.function.Function;

public final class TankFluidAppearance {
    private static final ResourceLocation WATER = new ResourceLocation("textures/blocks/water_still.png");
    private static final ResourceLocation UNTINTED_WATER = Psychedelicraft.id("blocks/fluid/tank_water_still");

    public final TextureAtlasSprite sprite;
    public final int color;

    private TankFluidAppearance(TextureAtlasSprite sprite, int color) {
        this.sprite = sprite;
        this.color = color;
    }

    public static TankFluidAppearance of(FluidStack stack) {
        Minecraft client = Minecraft.getMinecraft();
        TextureMap atlas = client.getTextureMapBlocks();
        Fluid fluid = stack.getFluid();
        ResourceLocation still = fluid.getStill(stack);
        TextureAtlasSprite sprite = still == null ? atlas.getMissingSprite() : atlas.getAtlasSprite(still.toString());
        if (fluid == FluidRegistry.WATER && client.world != null && client.player != null) {
            int tint = client.getBlockColors().colorMultiplier(
                    Blocks.WATER.getDefaultState(), client.world, client.player.getPosition(), 0);
            return new TankFluidAppearance(sprite, 0xff000000 | tint);
        }
        if (fluid == PSFluids.KAVA || PSFluids.isFlowerExtract(fluid)) {
            TextureAtlasSprite fallback = atlas.getAtlasSprite(UNTINTED_WATER.toString());
            if (fallback != atlas.getMissingSprite()) sprite = fallback;
            return new TankFluidAppearance(sprite, 0xff000000 | fluid.getColor(stack));
        }
        // Authored Psychedelicraft sprites already carry the tint and opacity for this batch.
        int color = still != null && Psychedelicraft.MOD_ID.equals(still.getResourceDomain())
                ? 0xffffffff : fluid.getColor(stack);
        return new TankFluidAppearance(sprite, color);
    }

    public static void stitch(TextureMap atlas) {
        atlas.setTextureEntry(new WaterSprite(atlas.getMipmapLevels()));
    }

    private static final class WaterSprite extends TextureAtlasSprite {
        private final int mipmapLevels;

        private WaterSprite(int mipmapLevels) {
            super(UNTINTED_WATER.toString());
            this.mipmapLevels = mipmapLevels;
        }

        @Override
        public boolean hasCustomLoader(IResourceManager resources, ResourceLocation location) {
            return true;
        }

        @Override
        public boolean load(IResourceManager resources, ResourceLocation location,
                            Function<ResourceLocation, TextureAtlasSprite> dependencies) {
            // Forge 1.12 water is pre-colored blue. Make a neutral copy for the donor's tint fallback.
            try (IResource image = resources.getResource(WATER); IResource animation = resources.getResource(WATER)) {
                loadSprite(PngSizeInfo.makeFromResource(image), animation.getMetadata("animation") != null);
                loadSpriteFrames(animation, mipmapLevels + 1);
                for (int[][] frame : framesTextureData) {
                    if (frame == null) continue;
                    int[] pixels = frame[0];
                    for (int i = 0; i < pixels.length; i++) {
                        int pixel = pixels[i];
                        int red = pixel >> 16 & 255, green = pixel >> 8 & 255, blue = pixel & 255;
                        int gray = (Math.max(red, Math.max(green, blue)) + Math.min(red, Math.min(green, blue))) / 2;
                        pixels[i] = (pixel & 0xff000000) | (gray * 0x010101);
                    }
                }
                return false;
            } catch (IOException | RuntimeException error) {
                LogManager.getLogger(Psychedelicraft.MOD_ID).warn("Could not load neutral tank water sprite", error);
                return true;
            }
        }
    }
}
