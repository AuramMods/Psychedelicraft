package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class DrugEvents {
    private DrugEvents() {}

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            DrugCapability provider = new DrugCapability();
            event.addCapability(Psychedelicraft.id("drug_properties"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DrugProperties properties = DrugCapability.get(event.player);
        properties.tick(event.player.tickCount, event.player);
        if (event.player instanceof ServerPlayer player && (properties.takeDirty() || player.tickCount % 20 == 0)) {
            DrugNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event.getEntity()); }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event.getEntity()); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event.getEntity()); }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer subject && event.getEntity() instanceof ServerPlayer recipient) {
            DrugNetwork.syncTo(subject, recipient);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) return;
        event.getOriginal().reviveCaps();
        try {
            DrugCapability.get(event.getEntity()).fromNbt(DrugCapability.get(event.getOriginal()).toNbt());
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) DrugNetwork.sync(serverPlayer);
    }
}
