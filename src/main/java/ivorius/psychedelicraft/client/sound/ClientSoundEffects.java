package ivorius.psychedelicraft.client.sound;
import ivorius.psychedelicraft.client.ClientDrugState;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
public final class ClientSoundEffects {
    private ClientSoundEffects(){}
    public static float multiplier(){var player=Minecraft.getInstance().player;return player==null?1:ClientDrugState.get(player).getMultiplier(Drug::soundVolumeModifier);}
    public static float scaleVolume(float categoryVolume){return Mth.clamp(categoryVolume*multiplier(),0,1);}
}
