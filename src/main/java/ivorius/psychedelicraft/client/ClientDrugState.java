package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.world.entity.player.Player;
import javax.annotation.Nullable;

/** Read-only client view during death, removal and capability attachment transitions. */
public final class ClientDrugState {
    private static final DrugProperties EMPTY=new DrugProperties();
    private ClientDrugState(){}
    public static boolean alive(@Nullable Player player){return player!=null&&player.isAlive()&&!player.isRemoved();}
    public static DrugProperties get(@Nullable Player player){
        return alive(player)?player.getCapability(DrugCapability.TYPE).orElse(EMPTY):EMPTY;
    }
}
