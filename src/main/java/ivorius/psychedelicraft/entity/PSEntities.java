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
                        .build(),
                EntityEntryBuilder.<MolotovCocktailEntity>create()
                        .entity(MolotovCocktailEntity.class)
                        .id(Psychedelicraft.id("molotov_cocktail"), 3)
                        .name(Psychedelicraft.MOD_ID + ".molotov_cocktail")
                        .tracker(64, 10, true)
                        .build(),
                EntityEntryBuilder.<BeeEntity>create()
                        .entity(BeeEntity.class)
                        .id(Psychedelicraft.id("bee"), 4)
                        .name(Psychedelicraft.MOD_ID + ".bee")
                        .tracker(128, 3, true)
                        .build(),
                EntityEntryBuilder.<SnifferEntity>create()
                        .entity(SnifferEntity.class)
                        .id(Psychedelicraft.id("sniffer"), 5)
                        .name(Psychedelicraft.MOD_ID + ".sniffer")
                        .tracker(160, 3, true)
                        .egg(0x871f1a, 0x246c42)
                        .build(),
                EntityEntryBuilder.<DrownedEntity>create()
                        .entity(DrownedEntity.class)
                        .id(Psychedelicraft.id("drowned"), 6)
                        .name(Psychedelicraft.MOD_ID + ".drowned")
                        .tracker(128, 3, true)
                        .egg(0x8ff1d7, 0x799c65)
                        .build(),
                EntityEntryBuilder.<ThrownTridentEntity>create()
                        .entity(ThrownTridentEntity.class)
                        .id(Psychedelicraft.id("thrown_trident"), 7)
                        .name(Psychedelicraft.MOD_ID + ".thrown_trident")
                        .tracker(64, 1, true)
                        .build(),
                EntityEntryBuilder.<WanderingTraderEntity>create()
                        .entity(WanderingTraderEntity.class)
                        .id(Psychedelicraft.id("wandering_trader"), 8)
                        .name(Psychedelicraft.MOD_ID + ".wandering_trader")
                        .tracker(100, 3, true)
                        .egg(0x456296, 0xeaa430)
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
