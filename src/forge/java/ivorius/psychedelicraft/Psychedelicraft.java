/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = Psychedelicraft.MOD_ID, name = "Psychedelicraft", useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]")
public class Psychedelicraft {
    public static final String MOD_ID = "psychedelicraft";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        DrugCapability.register();
        Channel.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new PSGuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.addSmelting(new ItemStack(PSItems.COFFEA_CHERRIES), new ItemStack(PSItems.COFFEE_BEANS), 0.2F);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
