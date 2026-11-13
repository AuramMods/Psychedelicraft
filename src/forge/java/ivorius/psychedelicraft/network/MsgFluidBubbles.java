package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.particle.SlurryBubble;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgFluidBubbles implements IMessage {
    private int dimension;
    private Vec3d origin;
    private int count;
    private float spread;
    private int color;

    public MsgFluidBubbles() {
    }

    MsgFluidBubbles(int dimension, Vec3d origin, int count, float spread, int color) {
        this.dimension = dimension;
        this.origin = origin;
        this.count = count;
        this.spread = spread;
        this.color = color;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        dimension = buffer.readInt();
        origin = new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        count = buffer.readUnsignedByte();
        spread = buffer.readFloat();
        color = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(dimension);
        buffer.writeDouble(origin.x);
        buffer.writeDouble(origin.y);
        buffer.writeDouble(origin.z);
        buffer.writeByte(count);
        buffer.writeFloat(spread);
        buffer.writeInt(color);
    }

    public static final class Handler implements IMessageHandler<MsgFluidBubbles, IMessage> {
        @Override
        public IMessage onMessage(MsgFluidBubbles message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                    SlurryBubble.receive(context.netHandler, message.dimension, message.origin,
                            message.count, message.spread, message.color));
            return null;
        }
    }
}
