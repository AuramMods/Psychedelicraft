package ivorius.psychedelicraft;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PSSounds {
    public static final SoundEvent BREATH = sound("entity.player.breath");
    public static final SoundEvent HEARTBEAT = sound("entity.player.heartbeat");
    public static final SoundEvent RIFT_JAR_TOGGLE = sound("block.rift_jar.toggle");
    public static final SoundEvent RIFT_JAR_OPEN = sound("block.rift_jar.open");
    public static final SoundEvent RIFT_JAR_CLOSE = sound("block.rift_jar.close");
    public static final SoundEvent BOTTLE_RACK_USE = sound("block.bottle_rack.use");
    public static final SoundEvent PAPER_BAG_INSERT = sound("item.paper_bag.insert");
    public static final SoundEvent PAPER_BAG_REMOVE = sound("item.paper_bag.remove");
    public static final SoundEvent DRUG_GENERIC = sound("drug.generic");

    private PSSounds() {
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = Psychedelicraft.id(name);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(BREATH, HEARTBEAT, RIFT_JAR_TOGGLE, RIFT_JAR_OPEN, RIFT_JAR_CLOSE,
                BOTTLE_RACK_USE, PAPER_BAG_INSERT, PAPER_BAG_REMOVE, DRUG_GENERIC);
    }
}
