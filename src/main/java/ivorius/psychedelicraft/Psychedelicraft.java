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
        PSItems.ITEMS.register(bus);
        PSItems.TABS.register(bus);
        PSBlocks.BLOCKS.register(bus);
        PSWorldGen.register(bus);
        PSBlockEntities.TYPES.register(bus);
        PSRecipes.register(bus);
        PSMenus.MENUS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PSConfig.COMMON);
        bus.addListener((RegisterCapabilitiesEvent event) -> event.register(DrugProperties.class));
        DrugNetwork.register();
        PSCriteria.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
