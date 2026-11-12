/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.recipe.JuniperWoodRecipes;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

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
        OreDictionary.registerOre("plankWood", PSItems.JUNIPER_PLANKS);
        OreDictionary.registerOre("stairWood", PSItems.JUNIPER_STAIRS);
        OreDictionary.registerOre("slabWood", PSItems.JUNIPER_SLAB);
        OreDictionary.registerOre("fenceWood", PSItems.JUNIPER_FENCE);
        OreDictionary.registerOre("fenceGateWood", PSItems.JUNIPER_FENCE_GATE);
        OreDictionary.registerOre("doorWood", PSItems.JUNIPER_DOOR);
        JuniperWoodRecipes.preferJuniperRecipes();
        PSItems.JUNIPER_BOAT.registerDispenserBehavior();
        PSItems.JUNIPER_CHEST_BOAT.registerDispenserBehavior();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
