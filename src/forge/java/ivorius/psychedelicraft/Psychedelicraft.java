/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Psychedelicraft.MOD_ID, name = "Psychedelicraft", useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]")
public class Psychedelicraft {
    public static final String MOD_ID = "psychedelicraft";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        DrugCapability.register();
        Channel.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
