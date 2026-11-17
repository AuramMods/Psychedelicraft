/* Copyright (c) 2014, Lukas Tenbrink. Background music selection by Sollace. */
package ivorius.psychedelicraft.client.sound;
import ivorius.psychedelicraft.client.ClientDrugState;
import ivorius.psychedelicraft.*;
import ivorius.psychedelicraft.config.PSClientConfig;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import javax.annotation.Nullable;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT)
public final class ClientDrugMusic {
    private static final double PLAY_THRESHOLD=.01;
    private static DrugTrack playing;
    private static int retryTicks;
    private ClientDrugMusic(){}
    @SubscribeEvent(priority=EventPriority.LOWEST)public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        var client=Minecraft.getInstance();var sounds=client.getSoundManager();var player=client.player;
        if(client.level==null||player==null||!player.isAlive()||!PSClientConfig.DRUG_MUSIC.get()){stop(sounds);return;}
        if(client.isPaused())return;
        if(client.options.getSoundSourceVolume(SoundSource.MASTER)<=0||ClientSoundEffects.scaleVolume(client.options.getSoundSourceVolume(SoundSource.AMBIENT))<=0){stop(sounds);return;}
        var drugs=ClientDrugState.get(player);var type=strongestTheme(drugs,t->PSClientConfig.THEMES.get(t).get());
        if(playing!=null&&(playing.player!=player||playing.world!=client.level||playing.drugs!=drugs||playing.type!=type||playing.isStopped()))stop(sounds);
        if(type==null)return;
        if(retryTicks>0){retryTicks--;return;}
        if(playing!=null&&sounds.isActive(playing))return;
        stop(sounds);playing=new DrugTrack(themeSound(sounds,type,player),player,client.level,drugs,type);sounds.play(playing);retryTicks=20;
    }
    public static boolean owns(SoundInstance sound){return sound instanceof DrugTrack;}
    @SubscribeEvent public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event){stop(Minecraft.getInstance().getSoundManager());}
    private static void stop(SoundManager sounds){if(playing!=null){playing.finish();sounds.stop(playing);playing=null;}retryTicks=0;}
    @Nullable static DrugType strongestTheme(DrugProperties drugs,Predicate<DrugType> enabled){
        DrugType selected=null;double strongest=PLAY_THRESHOLD;
        for(var type:DrugType.values()){double value=drugs.getActiveValue(type);if(value>strongest&&enabled.test(type)){strongest=value;selected=type;}}return selected;
    }
    static float themeVolume(double value){return value<=PLAY_THRESHOLD?0:Mth.clamp((float)value/.4F,0,1);}
    private static SoundEvent themeSound(SoundManager sounds,DrugType type,LocalPlayer player){
        var id=Psychedelicraft.id("drug."+new ResourceLocation(type.id()).getPath());var theme=sounds.getSoundEvent(id);
        return theme!=null&&theme.getSound(player.getRandom())!=SoundManager.EMPTY_SOUND?SoundEvent.createVariableRangeEvent(id):PSSounds.DRUG_GENERIC.get();
    }
    private static final class DrugTrack extends AbstractTickableSoundInstance {
        private final LocalPlayer player;private final ClientLevel world;private final DrugProperties drugs;private final DrugType type;
        private DrugTrack(SoundEvent sound,LocalPlayer player,ClientLevel world,DrugProperties drugs,DrugType type){
            super(sound,SoundSource.AMBIENT,SoundInstance.createUnseededRandom());this.player=player;this.world=world;this.drugs=drugs;this.type=type;
            looping=true;delay=0;relative=true;attenuation=SoundInstance.Attenuation.NONE;tick();
        }
        @Override public void tick(){volume=themeVolume(drugs.getActiveValue(type));if(volume<=0||!player.isAlive()||player.isRemoved()||Minecraft.getInstance().level!=world)finish();}
        private void finish(){stop();}
    }
}
