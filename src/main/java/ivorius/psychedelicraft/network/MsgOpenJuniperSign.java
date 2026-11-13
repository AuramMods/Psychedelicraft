package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.block.JuniperSignBlock.Form;
import ivorius.psychedelicraft.block.entity.JuniperSignText;
import ivorius.psychedelicraft.client.screen.JuniperSignScreen;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class MsgOpenJuniperSign implements IMessage {
    private BlockPos pos;
    private int dimension;
    private long token;
    private boolean front;
    private Form form;
    private EnumDyeColor color;
    private boolean glowing;
    private String[] lines;

    public MsgOpenJuniperSign() {
    }

    MsgOpenJuniperSign(BlockPos pos, int dimension, long token, boolean front, Form form, JuniperSignText text) {
        this.pos = pos;
        this.dimension = dimension;
        this.token = token;
        this.front = front;
        this.form = form;
        color = text.color();
        glowing = text.isGlowing();
        lines = new String[JuniperSignText.LINE_COUNT];
        for (int index = 0; index < lines.length; index++) {
            lines[index] = text.line(index).getUnformattedText();
        }
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        pos = packet.readBlockPos();
        dimension = packet.readInt();
        token = packet.readLong();
        front = packet.readBoolean();
        form = packet.readEnumValue(Form.class);
        color = EnumDyeColor.byMetadata(packet.readUnsignedByte());
        glowing = packet.readBoolean();
        lines = MsgEditJuniperSign.readLines(packet);
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        packet.writeBlockPos(pos);
        packet.writeInt(dimension);
        packet.writeLong(token);
        packet.writeBoolean(front);
        packet.writeEnumValue(form);
        packet.writeByte(color.getMetadata());
        packet.writeBoolean(glowing);
        MsgEditJuniperSign.writeLines(packet, lines);
    }

    public static final class Handler implements IMessageHandler<MsgOpenJuniperSign, IMessage> {
        @Override
        public IMessage onMessage(MsgOpenJuniperSign message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                    JuniperSignScreen.open(context.netHandler, message.pos, message.dimension, message.token,
                            message.front, message.form, message.color, message.glowing, message.lines));
            return null;
        }
    }
}
