package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import ivorius.psychedelicraft.block.entity.JuniperSignText;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgEditJuniperSign implements IMessage {
    private BlockPos pos;
    private int dimension;
    private long token;
    private String[] lines;

    public MsgEditJuniperSign() {
    }

    MsgEditJuniperSign(BlockPos pos, int dimension, long token, String[] lines) {
        this.pos = pos;
        this.dimension = dimension;
        this.token = token;
        this.lines = lines;
    }

    static String[] readLines(PacketBuffer packet) {
        String[] lines = new String[JuniperSignText.LINE_COUNT];
        for (int index = 0; index < lines.length; index++) {
            lines[index] = packet.readString(JuniperSignText.MAX_LINE_LENGTH);
        }
        return lines;
    }

    static void writeLines(PacketBuffer packet, String[] lines) {
        for (String line : lines) {
            packet.writeString(line.substring(0, Math.min(line.length(), JuniperSignText.MAX_LINE_LENGTH)));
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        pos = packet.readBlockPos();
        dimension = packet.readInt();
        token = packet.readLong();
        lines = readLines(packet);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        packet.writeBlockPos(pos);
        packet.writeInt(dimension);
        packet.writeLong(token);
        writeLines(packet, lines);
    }

    public static final class Handler implements IMessageHandler<MsgEditJuniperSign, IMessage> {
        @Override
        public IMessage onMessage(MsgEditJuniperSign message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                WorldServer world = player.getServerWorld();
                if (world.provider.getDimension() == message.dimension && world.isBlockLoaded(message.pos)) {
                    JuniperSignBlockEntity sign = JuniperSignBlockEntity.get(world, message.pos);
                    if (sign != null) {
                        sign.finishEdit(player, message.token, message.lines);
                    }
                }
            });
            return null;
        }
    }
}
