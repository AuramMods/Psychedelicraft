package ivorius.psychedelicraft.network;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.JuniperSignText;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
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
        network.registerMessage(MsgOpenJuniperSign.Handler.class, MsgOpenJuniperSign.class, 1, Side.CLIENT);
        network.registerMessage(MsgEditJuniperSign.Handler.class, MsgEditJuniperSign.class, 2, Side.SERVER);
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

    public static void openSignEditor(EntityPlayerMP player, BlockPos pos, long token, boolean front,
                                      boolean standing, JuniperSignText text) {
        network.sendTo(new MsgOpenJuniperSign(pos, player.dimension, token, front, standing, text), player);
    }

    public static void submitSignEdit(BlockPos pos, int dimension, long token, String[] lines) {
        network.sendToServer(new MsgEditJuniperSign(pos, dimension, token, lines));
    }
}
