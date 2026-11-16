package ivorius.psychedelicraft;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,Psychedelicraft.MOD_ID);
    public static final RegistryObject<SoundEvent> BREATH=SOUNDS.register("entity.player.breath",()->SoundEvent.createVariableRangeEvent(Psychedelicraft.id("entity.player.breath")));
    private PSSounds(){}
}
