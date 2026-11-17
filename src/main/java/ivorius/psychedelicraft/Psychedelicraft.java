package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entity.drug.DrugNetwork;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.world.gen.PSWorldGen;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.block.entity.PSBlockEntities;
import ivorius.psychedelicraft.recipe.PSRecipes;
import ivorius.psychedelicraft.screen.PSMenus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import ivorius.psychedelicraft.advancement.PSCriteria;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Psychedelicraft.MOD_ID)
public final class Psychedelicraft {
    public static final String MOD_ID = "psychedelicraft";
    public static final GameRules.Key<GameRules.BooleanValue> SLEEP_DEPRIVATION = GameRules.register(
            "doSleepDeprivation", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public Psychedelicraft() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ivorius.psychedelicraft.entity.PSEntities.TYPES.register(bus);
        PSItems.ITEMS.register(bus);
        ivorius.psychedelicraft.fluid.PSFluids.register(bus);
        PSSounds.SOUNDS.register(bus);
        ivorius.psychedelicraft.particle.PSParticles.TYPES.register(bus);
        PSItems.TABS.register(bus);
        PSBlocks.BLOCKS.register(bus);
        PSWorldGen.register(bus);
        PSBlockEntities.TYPES.register(bus);
        PSRecipes.register(bus);
        PSMenus.MENUS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PSConfig.COMMON);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
                ((net.minecraft.world.level.block.FlowerPotBlock)net.minecraft.world.level.block.Blocks.FLOWER_POT).addPlant(id("morning_glory"),PSBlocks.POTTED_MORNING_GLORY)));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(ivorius.psychedelicraft.block.JuniperContent::setup));
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(()->{ivorius.psychedelicraft.item.FilledBucketItem.registerDispenser();ivorius.psychedelicraft.fluid.PSFluids.registerInteractions();}));
        bus.addListener((RegisterCapabilitiesEvent event) -> event.register(DrugProperties.class));
        DrugNetwork.register();
        PSCriteria.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
