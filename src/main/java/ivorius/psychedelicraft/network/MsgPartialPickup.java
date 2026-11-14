package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.PartialPickup;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgPartialPickup implements IMessage {
    private int villagerId;
    private int itemId;
    private int dimension;
    private int count;

    public MsgPartialPickup() {
    }

    MsgPartialPickup(EntityVillager villager, EntityItem item, int count) {
        villagerId = villager.getEntityId();
        itemId = item.getEntityId();
        dimension = villager.dimension;
        this.count = count;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        villagerId = packet.readVarInt();
        itemId = packet.readVarInt();
        dimension = packet.readInt();
        count = packet.readVarInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        packet.writeVarInt(villagerId);
        packet.writeVarInt(itemId);
        packet.writeInt(dimension);
        packet.writeVarInt(count);
    }

    public static final class Handler implements IMessageHandler<MsgPartialPickup, IMessage> {
        @Override
        public IMessage onMessage(MsgPartialPickup message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                    PartialPickup.receive(context.netHandler, message.villagerId, message.itemId,
                            message.dimension, message.count));
            return null;
        }
    }
}
