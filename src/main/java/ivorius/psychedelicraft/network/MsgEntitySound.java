package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.EntitySound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgEntitySound implements IMessage {
    private int entityId;
    private int dimension;
    private ResourceLocation sound;
    private SoundCategory category;
    private float volume;
    private float pitch;

    public MsgEntitySound() {
    }

    MsgEntitySound(int entityId, int dimension, ResourceLocation sound, SoundCategory category, float volume, float pitch) {
        this.entityId = entityId;
        this.dimension = dimension;
        this.sound = sound;
        this.category = category;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        entityId = packet.readVarInt();
        dimension = packet.readInt();
        sound = packet.readResourceLocation();
        category = packet.readEnumValue(SoundCategory.class);
        volume = packet.readFloat();
        pitch = packet.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        packet.writeVarInt(entityId);
        packet.writeInt(dimension);
        packet.writeResourceLocation(sound);
        packet.writeEnumValue(category);
        packet.writeFloat(volume);
        packet.writeFloat(pitch);
    }

    public static final class Handler implements IMessageHandler<MsgEntitySound, IMessage> {
        @Override
        public IMessage onMessage(MsgEntitySound message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                    EntitySound.receive(context.netHandler, message.entityId, message.dimension,
                            message.sound, message.category, message.volume, message.pitch));
            return null;
        }
    }
}
