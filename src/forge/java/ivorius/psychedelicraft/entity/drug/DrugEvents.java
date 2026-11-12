package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.entity.drug.type.SleepDeprivationDrug;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameRules;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class DrugEvents {
    private static final UUID SPEED_MODIFIER = UUID.fromString("624dc5e6-4166-4931-8ed3-f6ab68ae76ef");
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
            if (!event.player.world.isRemote) {
                float speed = properties.getMultiplier(Drug::speedModifier);
                updateSpeed(event.player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED), speed);
                updateSpeed(event.player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED), speed);
            }
            if (properties.takeDirty() && event.player instanceof EntityPlayerMP) {
                Channel.sync((EntityPlayerMP) event.player, properties);
            }
        }
    }

    private static void updateSpeed(IAttributeInstance attribute, float speed) {
        AttributeModifier previous = attribute.getModifier(SPEED_MODIFIER);
        double amount = speed - 1.0;
        if (previous != null && Math.abs(previous.getAmount() - amount) < 0.0001) return;
        if (previous != null) attribute.removeModifier(previous);
        if (Math.abs(amount) >= 0.0001) {
            attribute.applyModifier(new AttributeModifier(SPEED_MODIFIER, "Psychedelicraft effects", amount, 2).setSaved(false));
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        DrugProperties properties = DrugCapability.get(event.getEntityPlayer());
        if (properties != null) event.setNewSpeed(event.getNewSpeed() * properties.getMultiplier(Drug::digSpeedModifier));
    }

    @SubscribeEvent
    public static void trySleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || event.getResultStatus() != null) return;
        DrugProperties properties = DrugCapability.get(player);
        String warning = properties == null ? null : properties.sleepWarning();
        if (warning != null) {
            event.setResult(EntityPlayer.SleepResult.OTHER_PROBLEM);
            player.sendStatusMessage(new TextComponentTranslation(warning), true);
        }
    }

    @SubscribeEvent
    public static void registerGameRules(WorldEvent.Load event) {
        GameRules rules = event.getWorld().getGameRules();
        if (!rules.hasRule(SleepDeprivationDrug.GAME_RULE)) {
            rules.addGameRule(SleepDeprivationDrug.GAME_RULE, "false", GameRules.ValueType.BOOLEAN_VALUE);
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
                properties.queueWakeUp();
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
