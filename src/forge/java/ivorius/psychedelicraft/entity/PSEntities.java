package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSEntities {
    private PSEntities() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().registerAll(
                boat(EntityJuniperBoat.class, "juniper_boat", 0),
                boat(EntityJuniperChestBoat.class, "juniper_chest_boat", 1),
                EntityEntryBuilder.<RealityRiftEntity>create()
                        .entity(RealityRiftEntity.class)
                        .id(Psychedelicraft.id("reality_rift"), 2)
                        .name(Psychedelicraft.MOD_ID + ".reality_rift")
                        .tracker(80, 3, false)
                        .build());
    }

    private static EntityEntry boat(Class<? extends EntityJuniperBoat> type, String name, int networkId) {
        return EntityEntryBuilder.<EntityJuniperBoat>create()
                .entity(type)
                .id(Psychedelicraft.id(name), networkId)
                .name(Psychedelicraft.MOD_ID + "." + name)
                .tracker(80, 3, true)
                .build();
    }
}
