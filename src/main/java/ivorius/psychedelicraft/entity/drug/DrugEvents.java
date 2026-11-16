package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import ivorius.psychedelicraft.PSDamageTypes;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class DrugEvents {
    private static final UUID ATTRIBUTE_MODIFIER = UUID.fromString("624dc5e6-4166-4931-8ed3-f6ab68ae76ef");
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
        if (event.phase == TickEvent.Phase.START) {
            StomachFoodStats.bind(event.player);
            return;
        }
        if (event.phase != TickEvent.Phase.END) return;
        DrugProperties properties = DrugCapability.get(event.player);
        properties.tick(event.player.tickCount, event.player);
        if (!event.player.level().isClientSide) {
            float speed = properties.getMultiplier(Drug::speedModifier);
            updateModifier(event.player.getAttribute(Attributes.MOVEMENT_SPEED), speed - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
            updateModifier(event.player.getAttribute(Attributes.ATTACK_SPEED), speed - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
            updateModifier(event.player.getAttribute(Attributes.ATTACK_DAMAGE), properties.getMultiplier(Drug::attackDamageModifier) - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
            updateModifier(event.player.getAttribute(ForgeMod.BLOCK_REACH.get()), properties.getModifier(Drug::extraBlockReach), AttributeModifier.Operation.ADDITION);
            MushroomGrowth.tick(event.player, properties);
        }
        if (event.player instanceof ServerPlayer player && (properties.takeDirty() || player.tickCount % 20 == 0)) {
            DrugNetwork.sync(player);
        }
    }

    private static void updateModifier(AttributeInstance attribute, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) return;
        AttributeModifier previous = attribute.getModifier(ATTRIBUTE_MODIFIER);
        if (previous != null && Math.abs(previous.getAmount() - amount) < 0.0001) return;
        if (previous != null) attribute.removeModifier(previous);
        if (Math.abs(amount) >= 0.0001) attribute.addTransientModifier(new AttributeModifier(ATTRIBUTE_MODIFIER,
                "Psychedelicraft effects", amount, operation));
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(event.getNewSpeed() * DrugCapability.get(event.getEntity()).getMultiplier(Drug::digSpeedModifier));
    }

    @SubscribeEvent
    public static void incomingDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)
                || event.getSource().is(PSDamageTypes.ALCOHOL_POISONING)) return;
        event.setAmount(event.getAmount() * DrugCapability.get(player).getMultiplier(Drug::incomingDamageModifier));
    }

    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getNewTarget() instanceof Player player)) return;
        float chance = DrugCapability.get(player).getModifier(Drug::mobAvoidanceChance);
        boolean sameTarget = event.getEntity() instanceof Mob mob && mob.getTarget() == player;
        if (chance >= 1 || (!sameTarget && chance > 0 && event.getEntity().getRandom().nextFloat() < chance)) event.setNewTarget(null);
    }

    @SubscribeEvent
    public static void mobTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && mob.getTarget() instanceof Player player
                && DrugCapability.get(player).getModifier(Drug::mobAvoidanceChance) >= 1) mob.setTarget(null);
    }

    @SubscribeEvent
    public static void trySleep(PlayerSleepInBedEvent event) {
        if (event.getEntity().level().isClientSide || event.getResultStatus() != null) return;
        String warning = DrugCapability.get(event.getEntity()).sleepWarning();
        if (warning != null) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            event.getEntity().displayClientMessage(Component.translatable(warning), true);
        }
    }

    @SubscribeEvent
    public static void wake(PlayerWakeUpEvent event) {
        if (!event.getEntity().level().isClientSide) DrugCapability.get(event.getEntity()).queueWakeUp();
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
