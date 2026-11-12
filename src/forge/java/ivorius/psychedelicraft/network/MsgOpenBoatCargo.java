package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.entity.EntityJuniperChestBoat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgOpenBoatCargo implements IMessage {
    @Override
    public void fromBytes(ByteBuf buffer) {
    }

    @Override
    public void toBytes(ByteBuf buffer) {
    }

    public static final class Handler implements IMessageHandler<MsgOpenBoatCargo, IMessage> {
        @Override
        public IMessage onMessage(MsgOpenBoatCargo message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity vehicle = player.getRidingEntity();
                if (player.isEntityAlive() && vehicle instanceof EntityJuniperChestBoat) {
                    ((EntityJuniperChestBoat) vehicle).openCargo(player);
                }
            });
            return null;
        }
    }
}
