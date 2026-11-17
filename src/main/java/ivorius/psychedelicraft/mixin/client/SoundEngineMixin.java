package ivorius.psychedelicraft.mixin.client;
import ivorius.psychedelicraft.client.sound.ClientSoundEffects;
import ivorius.psychedelicraft.client.sound.ClientDrugMusic;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Map;
import java.util.List;
import com.google.common.collect.Multimap;
@Mixin(SoundEngine.class)
abstract class SoundEngineMixin {
    @Shadow @Final private Map<SoundInstance,ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow @Final private List<TickableSoundInstance> tickingSounds;
    @Shadow @Final private Map<SoundInstance,Integer> soundDeleteTime;
    @Shadow @Final private Multimap<SoundSource,SoundInstance> instanceBySource;
    @Shadow private float calculateVolume(SoundInstance sound){throw new AssertionError();}
    @Unique private float psychedelicraft$lastMultiplier=1;
    @Inject(method="getVolume",at=@At("RETURN"),cancellable=true)
    private void psychedelicraft$perceivedVolume(SoundSource category,CallbackInfoReturnable<Float> cir){cir.setReturnValue(ClientSoundEffects.scaleVolume(cir.getReturnValueF()));}
    @Inject(method="tickNonPaused",at=@At("RETURN"))
    private void psychedelicraft$refreshStationarySounds(CallbackInfo ci){
        // Native category mute removes the channel without clearing these indexes; isActive then stays true.
        tickingSounds.removeIf(sound->{
            if(!ClientDrugMusic.owns(sound)||!sound.isStopped()||instanceToChannel.containsKey(sound))return false;
            soundDeleteTime.remove(sound);instanceBySource.remove(sound.getSource(),sound);return true;
        });
        float multiplier=ClientSoundEffects.multiplier();if(multiplier==psychedelicraft$lastMultiplier)return;
        psychedelicraft$lastMultiplier=multiplier;
        instanceToChannel.forEach((sound,handle)->{if(!(sound instanceof TickableSoundInstance)){float volume=calculateVolume(sound);handle.execute(channel->channel.setVolume(volume));}});
    }
}
