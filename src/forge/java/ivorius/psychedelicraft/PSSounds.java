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

    private PSSounds() {
    }

    private static SoundEvent sound(String name) {
        ResourceLocation id = Psychedelicraft.id(name);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(BREATH, HEARTBEAT);
    }
}
