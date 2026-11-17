package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.item.BagOfVomitItem;
import net.minecraft.client.Minecraft;

public final class BagDisguise {
    private static int chosen;
    private BagDisguise(){}
    public static int form(){var client=Minecraft.getInstance();if(!client.isSameThread())return 0;var player=client.player;
        if(player==null||!player.getCapability(DrugCapability.TYPE).map(properties->properties.isTripping()).orElse(false)){chosen=0;return 0;}
        if(chosen==0)chosen=1+player.getRandom().nextInt(BagOfVomitItem.FORMS.length);return chosen;
    }
}
