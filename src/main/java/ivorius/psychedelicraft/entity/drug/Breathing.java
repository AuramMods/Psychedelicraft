/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on 15 Jan 2023. */
package ivorius.psychedelicraft.entity.drug;
import ivorius.psychedelicraft.PSSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Independent owner/server clocks; native entity-bound packets exclude the locally rendered owner. */
final class Breathing {
    private int delay;
    private boolean inhaling;
    boolean advance(float volume,float speedModifier){
        if(delay>0)delay--;
        if(delay!=0)return false;
        inhaling=!inhaling;
        if(volume<=0)return false;
        delay=Mth.floor(30/(1+speedModifier));return true;
    }
    float pitch(float speedModifier){return (1+speedModifier)*.05F+.9F+(inhaling?.15F:0);}
    void tick(DrugProperties drugs,Player player){
        if(player.level().isClientSide&&!player.isLocalPlayer())return;
        float volume=drugs.getModifier(Drug::breathVolume),speed=drugs.getModifier(Drug::breathSpeed);
        if(advance(volume,speed))player.level().playSound(player,player,PSSounds.BREATH.get(),SoundSource.PLAYERS,volume,pitch(speed));
    }
}
