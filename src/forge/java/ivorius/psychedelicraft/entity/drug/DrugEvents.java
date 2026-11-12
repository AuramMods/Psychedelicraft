package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class DrugEvents {
    private DrugEvents() {
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(Psychedelicraft.id("drug_properties"), new DrugCapability());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        DrugProperties properties = DrugCapability.get(event.player);
        if (properties != null) {
            properties.tick(event.player.ticksExisted, event.player);
            if (properties.takeDirty() && event.player instanceof EntityPlayerMP) {
                Channel.sync((EntityPlayerMP) event.player, properties);
            }
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            DrugProperties previous = DrugCapability.get(event.getOriginal());
            DrugProperties current = DrugCapability.get(event.getEntityPlayer());
            if (previous != null && current != null) {
                current.fromNbt(previous.toNbt());
            }
        }
    }

    @SubscribeEvent
    public static void wakeUp(PlayerWakeUpEvent event) {
        if (!event.getEntityPlayer().world.isRemote) {
            DrugProperties properties = DrugCapability.get(event.getEntityPlayer());
            if (properties != null) {
                properties.onAwoken();
            }
        }
    }

    @SubscribeEvent
    public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        syncOwner(event.player);
    }

    @SubscribeEvent
    public static void changeDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        syncOwner(event.player);
    }

    @SubscribeEvent
    public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        syncOwner(event.player);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntityPlayer() instanceof EntityPlayerMP && event.getTarget() instanceof EntityPlayerMP) {
            Channel.syncTo((EntityPlayerMP) event.getTarget(), (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    private static void syncOwner(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            Channel.syncTo(serverPlayer, serverPlayer);
        }
    }
}
