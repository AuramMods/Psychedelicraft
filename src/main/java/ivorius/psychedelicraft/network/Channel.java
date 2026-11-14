package ivorius.psychedelicraft.network;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.JuniperSignBlock.Form;
import ivorius.psychedelicraft.block.entity.JuniperSignText;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

public final class Channel {
    private static SimpleNetworkWrapper network;

    private Channel() {
    }

    public static void register() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(Psychedelicraft.MOD_ID);
        network.registerMessage(MsgDrugProperties.Handler.class, MsgDrugProperties.class, 0, Side.CLIENT);
        network.registerMessage(MsgOpenJuniperSign.Handler.class, MsgOpenJuniperSign.class, 1, Side.CLIENT);
        network.registerMessage(MsgEditJuniperSign.Handler.class, MsgEditJuniperSign.class, 2, Side.SERVER);
        network.registerMessage(MsgOpenBoatCargo.Handler.class, MsgOpenBoatCargo.class, 3, Side.SERVER);
        network.registerMessage(MsgSmoke.Handler.class, MsgSmoke.class, 4, Side.CLIENT);
        network.registerMessage(MsgHallucinate.Handler.class, MsgHallucinate.class, 5, Side.CLIENT);
        network.registerMessage(MsgFluidBubbles.Handler.class, MsgFluidBubbles.class, 6, Side.CLIENT);
        network.registerMessage(MsgVillagerOfferPreview.Handler.class, MsgVillagerOfferPreview.class, 7, Side.CLIENT);
        network.registerMessage(MsgEntitySound.Handler.class, MsgEntitySound.class, 8, Side.CLIENT);
        network.registerMessage(MsgPartialPickup.Handler.class, MsgPartialPickup.class, 9, Side.CLIENT);
    }

    public static void spawnSmoke(World world, Vec3d origin, Vec3d velocity, int color, float scale, int count) {
        int dimension = world.provider.getDimension();
        network.sendToAllAround(new MsgSmoke(dimension, origin, velocity, color, scale, count),
                new NetworkRegistry.TargetPoint(dimension, origin.x, origin.y, origin.z, 32));
    }

    public static void spawnFluidBubbles(World world, Vec3d origin, int count, float spread, int color) {
        int dimension = world.provider.getDimension();
        network.sendToAllAround(new MsgFluidBubbles(dimension, origin, count, spread, color),
                new NetworkRegistry.TargetPoint(dimension, origin.x, origin.y, origin.z, 32));
    }

    public static void playEntitySound(Entity source, SoundEvent sound, float volume, float pitch) {
        playEntitySound(source, sound, volume, pitch, null);
    }

    public static void playEntitySound(Entity source, SoundEvent sound, float volume, float pitch,
                                       @Nullable EntityPlayer excluded) {
        PlaySoundAtEntityEvent event = ForgeEventFactory.onPlaySoundAtEntity(source, sound, source.getSoundCategory(), volume, pitch);
        if (event.isCanceled() || event.getSound() == null || event.getSound().getRegistryName() == null) return;
        int dimension = source.world.provider.getDimension();
        float range = 16F * Math.max(1F, event.getVolume());
        MsgEntitySound message = new MsgEntitySound(source.getEntityId(), dimension, event.getSound().getRegistryName(),
                event.getCategory(), event.getVolume(), event.getPitch());
        for (EntityPlayerMP recipient : source.world.getPlayers(EntityPlayerMP.class,
                player -> player != excluded && player.getDistanceSq(source) < range * range)) {
            network.sendTo(message, recipient);
        }
    }

    public static void sync(EntityPlayerMP player, DrugProperties properties) {
        MsgDrugProperties snapshot = new MsgDrugProperties(player, properties);
        network.sendTo(snapshot, player);
        network.sendToAllTracking(snapshot, player);
    }

    public static void syncTo(EntityPlayerMP subject, EntityPlayerMP recipient) {
        DrugProperties properties = DrugCapability.get(subject);
        if (properties != null) {
            network.sendTo(new MsgDrugProperties(subject, properties), recipient);
        }
    }

    public static void showVillagerOffer(EntityVillager villager, ItemStack item) {
        network.sendToAllTracking(new MsgVillagerOfferPreview(villager, item), villager);
    }

    public static void showPartialPickup(EntityVillager villager, EntityItem item, int count) {
        network.sendToAllTracking(new MsgPartialPickup(villager, item, count), item);
    }

    public static void showVillagerOffer(EntityVillager villager, ItemStack item, EntityPlayerMP recipient) {
        network.sendTo(new MsgVillagerOfferPreview(villager, item), recipient);
    }

    public static void openSignEditor(EntityPlayerMP player, BlockPos pos, long token, boolean front,
                                      Form form, JuniperSignText text) {
        network.sendTo(new MsgOpenJuniperSign(pos, player.dimension, token, front, form, text), player);
    }

    public static void submitSignEdit(BlockPos pos, int dimension, long token, String[] lines) {
        network.sendToServer(new MsgEditJuniperSign(pos, dimension, token, lines));
    }

    public static void openMountedBoatCargo() {
        network.sendToServer(new MsgOpenBoatCargo());
    }

    public static void hallucinate(EntityPlayerMP player, HallucinationType type, @Nullable BlockPos position) {
        network.sendTo(new MsgHallucinate(player.getEntityId(), player.dimension, type, position), player);
    }
}
