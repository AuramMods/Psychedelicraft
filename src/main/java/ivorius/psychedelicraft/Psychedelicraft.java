package ivorius.psychedelicraft;

import ivorius.psychedelicraft.entity.drug.DrugNetwork;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Psychedelicraft.MOD_ID)
public final class Psychedelicraft {
    public static final String MOD_ID = "psychedelicraft";

    public Psychedelicraft() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        PSItems.ITEMS.register(bus);
        bus.addListener((RegisterCapabilitiesEvent event) -> event.register(DrugProperties.class));
        DrugNetwork.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
