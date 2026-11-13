package ivorius.psychedelicraft.network;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.ClientDrugSync;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgDrugProperties implements IMessage {
    private int entityId;
    private int dimension;
    private UUID playerId;
    private NBTTagCompound data;

    public MsgDrugProperties() {
    }

    MsgDrugProperties(EntityPlayerMP player, DrugProperties properties) {
        entityId = player.getEntityId();
        dimension = player.dimension;
        playerId = player.getUniqueID();
        data = properties.toNbt();
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        dimension = buffer.readInt();
        playerId = new UUID(buffer.readLong(), buffer.readLong());
        data = ByteBufUtils.readTag(buffer);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeInt(dimension);
        buffer.writeLong(playerId.getMostSignificantBits());
        buffer.writeLong(playerId.getLeastSignificantBits());
        ByteBufUtils.writeTag(buffer, data);
    }

    public static final class Handler implements IMessageHandler<MsgDrugProperties, IMessage> {
        @Override
        public IMessage onMessage(MsgDrugProperties message, MessageContext context) {
            if (message.data != null) {
                FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                        ClientDrugSync.apply(context.netHandler, message.entityId, message.dimension,
                                message.playerId, message.data));
            }
            return null;
        }
    }
}
