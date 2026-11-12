package ivorius.psychedelicraft.network;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class Channel {
    private static SimpleNetworkWrapper network;

    private Channel() {
    }

    public static void register() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(Psychedelicraft.MOD_ID);
        network.registerMessage(MsgDrugProperties.Handler.class, MsgDrugProperties.class, 0, Side.CLIENT);
    }

    public static void sync(EntityPlayerMP player, DrugProperties properties) {
        MsgDrugProperties snapshot = new MsgDrugProperties(player, properties);
        network.sendTo(snapshot, player);
        network.sendToAllTracking(snapshot, player);
    }

    public static void syncTo(EntityPlayerMP subject, EntityPlayerMP recipient) {
        DrugProperties properties = DrugCapability.get(subject);
        if (properties != null) {
            network.sendTo(new MsgDrugProperties(subject, properties), recipient);
        }
    }
}
