/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.command.VomitCommand;
import ivorius.psychedelicraft.command.DrugCommand;
import ivorius.psychedelicraft.command.HallucinateCommand;
import ivorius.psychedelicraft.entity.FarmerTrades;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.FilledBucketItem;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PaperBagItem;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.recipe.JuniperWoodRecipes;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import ivorius.psychedelicraft.world.gen.ChestLoot;
import ivorius.psychedelicraft.world.gen.CopperOreGenerator;
import ivorius.psychedelicraft.world.gen.VillageGarden;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

@Mod(modid = Psychedelicraft.MOD_ID, name = "Psychedelicraft", useMetadata = true,
        acceptedMinecraftVersions = "[1.12.2]",
        guiFactory = "ivorius.psychedelicraft.client.PSConfigGuiFactory")
public class Psychedelicraft {
    public static final String MOD_ID = "psychedelicraft";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PSFluids.register();
        DrugCapability.register();
        Channel.register();
        ChestLoot.register();
        VillageGarden.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new PSGuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.addSmelting(new ItemStack(PSItems.COFFEA_CHERRIES), new ItemStack(PSItems.COFFEE_BEANS), 0.2F);
        GameRegistry.addSmelting(new ItemStack(PSItems.RAW_COPPER), new ItemStack(PSItems.COPPER_INGOT), 0.7F);
        GameRegistry.addSmelting(new ItemStack(PSItems.COPPER_ORE), new ItemStack(PSItems.COPPER_INGOT), 0.7F);
        OreDictionary.registerOre("oreCopper", PSItems.COPPER_ORE);
        OreDictionary.registerOre("ingotCopper", PSItems.COPPER_INGOT);
        GameRegistry.registerWorldGenerator(new CopperOreGenerator(), 0);
        OreDictionary.registerOre("plankWood", PSItems.JUNIPER_PLANKS);
        OreDictionary.registerOre("stairWood", PSItems.JUNIPER_STAIRS);
        OreDictionary.registerOre("slabWood", PSItems.JUNIPER_SLAB);
        OreDictionary.registerOre("fenceWood", PSItems.JUNIPER_FENCE);
        OreDictionary.registerOre("fenceGateWood", PSItems.JUNIPER_FENCE_GATE);
        OreDictionary.registerOre("doorWood", PSItems.JUNIPER_DOOR);
        JuniperWoodRecipes.preferJuniperRecipes();
        PSItems.JUNIPER_BOAT.registerDispenserBehavior();
        PSItems.JUNIPER_CHEST_BOAT.registerDispenserBehavior();
        FilledBucketItem.registerDispenserBehavior();
        PaperBagItem.registerDispenserBehavior();
        FarmerTrades.register();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new VomitCommand());
        event.registerServerCommand(new DrugCommand());
        event.registerServerCommand(new HallucinateCommand());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
