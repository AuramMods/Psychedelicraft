package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.render.ContextualHallucinations;
import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

public final class MsgHallucinate implements IMessage {
    private int entityId;
    private int dimension;
    private HallucinationType type;
    private BlockPos position;

    public MsgHallucinate() {
    }

    MsgHallucinate(int entityId, int dimension, HallucinationType type, @Nullable BlockPos position) {
        this.entityId = entityId;
        this.dimension = dimension;
        this.type = type;
        this.position = position;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        entityId = packet.readVarInt();
        dimension = packet.readInt();
        type = HallucinationType.byName(packet.readString(64));
        position = packet.readBoolean() ? packet.readBlockPos() : null;
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        packet.writeVarInt(entityId);
        packet.writeInt(dimension);
        packet.writeString(type.id());
        packet.writeBoolean(position != null);
        if (position != null) packet.writeBlockPos(position);
    }

    public static final class Handler implements IMessageHandler<MsgHallucinate, IMessage> {
        @Override
        public IMessage onMessage(MsgHallucinate message, MessageContext context) {
            if (message.type != null) {
                FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                        ContextualHallucinations.receive(context.netHandler, message.entityId, message.dimension,
                                message.type, message.position));
            }
            return null;
        }
    }
}
