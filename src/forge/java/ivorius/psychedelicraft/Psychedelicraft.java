/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;

@Mod(modid = Psychedelicraft.MOD_ID, name = "Psychedelicraft", useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]")
public class Psychedelicraft {
    public static final String MOD_ID = "psychedelicraft";

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
