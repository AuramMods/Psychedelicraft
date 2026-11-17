package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.ClientEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

public final class DrugNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(Psychedelicraft.id("main"),
            () -> "2", "2"::equals, "2"::equals);

    private DrugNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Snapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::encode).decoder(Snapshot::decode)
                .consumerMainThread((message, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ClientEffects.receive(message)))
                .add();
        CHANNEL.messageBuilder(ForcedHallucination.class,1,NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ForcedHallucination::encode).decoder(ForcedHallucination::decode)
                .consumerMainThread((message,context)->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        ()->()->ivorius.psychedelicraft.client.render.ContextualHallucinations.receive(context.get().getNetworkManager(),message))).add();
    }

    public static void hallucinate(ServerPlayer player,HallucinationType type,net.minecraft.core.BlockPos position){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new ForcedHallucination(player.getId(),player.getUUID(),player.level().dimension().location(),type,position));
    }

    public record ForcedHallucination(int entityId,UUID player,ResourceLocation dimension,HallucinationType type,net.minecraft.core.BlockPos position){
        public void encode(FriendlyByteBuf buffer){buffer.writeVarInt(entityId);buffer.writeUUID(player);buffer.writeResourceLocation(dimension);buffer.writeUtf(type.id().toString(),64);buffer.writeBoolean(position!=null);if(position!=null)buffer.writeBlockPos(position);}
        public static ForcedHallucination decode(FriendlyByteBuf buffer){
            int id=buffer.readVarInt();UUID player=buffer.readUUID();ResourceLocation dimension=buffer.readResourceLocation();HallucinationType type=HallucinationType.byName(buffer.readUtf(64));
            return new ForcedHallucination(id,player,dimension,type,buffer.readBoolean()?buffer.readBlockPos():null);
        }
    }

    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), Snapshot.of(player));
    }

    public static void syncTo(ServerPlayer subject, ServerPlayer recipient) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), Snapshot.of(subject));
    }

    public record Snapshot(UUID player, ResourceLocation dimension, CompoundTag data) {
        private static Snapshot of(ServerPlayer player) {
            return new Snapshot(player.getUUID(), player.level().dimension().location(), DrugCapability.get(player).toNbt());
        }

        private void encode(FriendlyByteBuf buffer) {
            buffer.writeUUID(player);
            buffer.writeResourceLocation(dimension);
            buffer.writeNbt(data);
        }

        private static Snapshot decode(FriendlyByteBuf buffer) {
            UUID player = buffer.readUUID();
            ResourceLocation dimension = buffer.readResourceLocation();
            CompoundTag tag = buffer.readNbt();
            return new Snapshot(player, dimension, tag == null ? new CompoundTag() : tag);
        }
    }
}
