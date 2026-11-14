/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft;

import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.block.BeeNestBlock;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.command.VomitCommand;
import ivorius.psychedelicraft.command.DrugCommand;
import ivorius.psychedelicraft.command.HallucinateCommand;
import ivorius.psychedelicraft.entity.FarmerTrades;
import ivorius.psychedelicraft.entity.DrownedEntity;
import ivorius.psychedelicraft.entity.SnifferEntity;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import ivorius.psychedelicraft.item.FilledBucketItem;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PaperBagItem;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.recipe.FurnaceRecipe;
import ivorius.psychedelicraft.recipe.JuniperWoodRecipes;
import ivorius.psychedelicraft.screen.PSGuiHandler;
import ivorius.psychedelicraft.world.gen.BuriedTreasure;
import ivorius.psychedelicraft.world.gen.ChestLoot;
import ivorius.psychedelicraft.world.gen.CopperOreGenerator;
import ivorius.psychedelicraft.world.gen.MossCaveWorldGenerator;
import ivorius.psychedelicraft.world.gen.Shipwreck;
import ivorius.psychedelicraft.world.gen.SmallFlowerWorldGenerator;
import ivorius.psychedelicraft.world.gen.VillageGarden;
import ivorius.psychedelicraft.world.gen.WarmOceanRuin;
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
        PSCriteria.register();
        DrugCapability.register();
        Channel.register();
        ChestLoot.register();
        SnifferEntity.registerLoot();
        DrownedEntity.registerLoot();
        VillageGarden.register();
        WarmOceanRuin.register();
        Shipwreck.register();
        BuriedTreasure.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new PSGuiHandler());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FurnaceRecipe.load();
        GameRegistry.addSmelting(new ItemStack(PSItems.RAW_COPPER), new ItemStack(PSItems.COPPER_INGOT), 0.7F);
        GameRegistry.addSmelting(new ItemStack(PSItems.COPPER_ORE), new ItemStack(PSItems.COPPER_INGOT), 0.7F);
        GameRegistry.addSmelting(new ItemStack(PSItems.KELP), new ItemStack(PSItems.DRIED_KELP), 0.1F);
        OreDictionary.registerOre("oreCopper", PSItems.COPPER_ORE);
        OreDictionary.registerOre("ingotCopper", PSItems.COPPER_INGOT);
        OreDictionary.registerOre("beeCombHoney", PSItems.HONEYCOMB);
        OreDictionary.registerOre("cropTomato", PSItems.TOMATO);
        GameRegistry.registerWorldGenerator(new CopperOreGenerator(), 0);
        GameRegistry.registerWorldGenerator(new MossCaveWorldGenerator(), 1);
        SmallFlowerWorldGenerator.registerGrassFlowers();
        JuniperWoodRecipes.register();
        PSItems.JUNIPER_BOAT.registerDispenserBehavior();
        PSItems.JUNIPER_CHEST_BOAT.registerDispenserBehavior();
        FilledBucketItem.registerDispenserBehavior();
        Waterlogging.registerDispenserBehavior();
        PaperBagItem.registerDispenserBehavior();
        BeeNestBlock.registerDispenserBehaviors();
        FarmerTrades.register();
        DrownedEntity.registerSpawns();
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
