package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.particle.ExhaledSmoke;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgSmoke implements IMessage {
    private int dimension;
    private Vec3d origin;
    private Vec3d velocity;
    private int color;
    private float scale;
    private int count;

    public MsgSmoke() {
    }

    MsgSmoke(int dimension, Vec3d origin, Vec3d velocity, int color, float scale, int count) {
        this.dimension = dimension;
        this.origin = origin;
        this.velocity = velocity;
        this.color = color;
        this.scale = scale;
        this.count = count;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        dimension = buffer.readInt();
        origin = readVector(buffer);
        velocity = readVector(buffer);
        color = buffer.readInt();
        scale = buffer.readFloat();
        count = buffer.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(dimension);
        writeVector(buffer, origin);
        writeVector(buffer, velocity);
        buffer.writeInt(color);
        buffer.writeFloat(scale);
        buffer.writeByte(count);
    }

    private static Vec3d readVector(ByteBuf buffer) {
        return new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeVector(ByteBuf buffer, Vec3d vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    public static final class Handler implements IMessageHandler<MsgSmoke, IMessage> {
        @Override
        public IMessage onMessage(MsgSmoke message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                    ExhaledSmoke.spawn(context.netHandler, message.dimension, message.origin, message.velocity,
                            message.color, message.scale, message.count));
            return null;
        }
    }
}
