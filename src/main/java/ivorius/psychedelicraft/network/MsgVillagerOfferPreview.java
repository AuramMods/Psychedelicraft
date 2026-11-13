package ivorius.psychedelicraft.network;

import io.netty.buffer.ByteBuf;
import ivorius.psychedelicraft.client.render.VillagerClothes;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public final class MsgVillagerOfferPreview implements IMessage {
    private int villagerId;
    private int dimension;
    private ItemStack item;

    public MsgVillagerOfferPreview() {
    }

    MsgVillagerOfferPreview(EntityVillager villager, ItemStack item) {
        villagerId = villager.getEntityId();
        dimension = villager.dimension;
        this.item = item;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        villagerId = packet.readVarInt();
        dimension = packet.readInt();
        try {
            item = packet.readItemStack();
        } catch (IOException error) {
            throw new IllegalArgumentException("Could not read villager trade preview", error);
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        PacketBuffer packet = new PacketBuffer(buffer);
        packet.writeVarInt(villagerId);
        packet.writeInt(dimension);
        packet.writeItemStack(item);
    }

    public static final class Handler implements IMessageHandler<MsgVillagerOfferPreview, IMessage> {
        @Override
        public IMessage onMessage(MsgVillagerOfferPreview message, MessageContext context) {
            FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() ->
                    VillagerClothes.receivePreview(context.netHandler, message.dimension, message.villagerId, message.item));
            return null;
        }
    }
}
