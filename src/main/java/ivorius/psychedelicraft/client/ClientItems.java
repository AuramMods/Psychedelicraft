package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientItems {
    private ClientItems() {}
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var item : java.util.List.of(PSItems.COCAINE_POWDER, PSItems.OBSIDIAN_DUST)) {
                ItemProperties.register(item.get(), Psychedelicraft.id("using"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0);
            }
        });
    }
}
