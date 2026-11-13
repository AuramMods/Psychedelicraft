package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIVillagerInteract;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class VillagerReputation {
    private static final String DATA = "psychedelicraftReputation";
    private static final String PLAYERS = "Players";
    private static final String DECAY = "LastDecay";
    private static final int DAY = 24000;
    private static final int MIN_GOSSIP = 2;
    private static final Map<EntityVillager, Long> LAST_TALK = new WeakHashMap<>();

    private VillagerReputation() {}

    static int reputation(EntityVillager villager, EntityPlayer player) {
        return score(players(villager).getCompoundTag(player.getUniqueID().toString()));
    }

    static void traded(EntityVillager villager) {
        EntityPlayer customer = villager.getCustomer();
        if (customer != null) record(villager, customer.getUniqueID(), Topic.TRADING, 2);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingDamageEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getSource().getTrueSource();
        if (!victim.world.isRemote && victim instanceof EntityVillager && attacker instanceof EntityPlayer && event.getAmount() > 0) {
            record((EntityVillager) victim, attacker.getUniqueID(), Topic.MINOR_NEGATIVE, 25);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void witnessedDeath(LivingDeathEvent event) {
        Entity attacker = event.getSource().getTrueSource();
        if (!(event.getEntityLiving() instanceof EntityVillager) || !(attacker instanceof EntityPlayer)) return;
        EntityVillager victim = (EntityVillager) event.getEntityLiving();
        if (victim.world.isRemote) return;
        for (EntityVillager witness : victim.world.getEntitiesWithinAABB(EntityVillager.class, victim.getEntityBoundingBox().grow(16))) {
            if (witness != victim && witness.isEntityAlive() && witness.getDistanceSq(victim) <= 256 && victim.canEntityBeSeen(witness)) {
                record(witness, attacker.getUniqueID(), Topic.MAJOR_NEGATIVE, 25);
            }
        }
    }

    static void copy(Entity source, Entity replacement) {
        NBTTagCompound data = source.getEntityData().getCompoundTag(DATA);
        if (data.hasKey(PLAYERS, 10)) {
            NBTTagCompound copy = data.copy();
            copy.setLong(DECAY, replacement.world.getTotalWorldTime());
            replacement.getEntityData().setTag(DATA, copy);
        }
    }

    static void cured(EntityVillager villager, @Nullable UUID healer) {
        if (healer != null && villager.world.getPlayerEntityByUUID(healer) instanceof EntityPlayerMP) {
            record(villager, healer, Topic.MAJOR_POSITIVE, 20);
            record(villager, healer, Topic.MINOR_POSITIVE, 25);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EntityVillager && !entity.world.isRemote && entity.getEntityData().hasKey(DATA, 10)) {
            players((EntityVillager) entity);
        }
    }

    public static void talk(EntityVillager villager, EntityAIVillagerInteract task) {
        Entity target;
        try {
            target = (Entity) ConversationTarget.FIELD.get(task);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not read the villager conversation target", error);
        }
        if (!(target instanceof EntityVillager) || !target.isEntityAlive() || !villager.canEntityBeSeen(target)) return;
        EntityVillager other = (EntityVillager) target;
        long now = villager.world.getTotalWorldTime();
        if (now - LAST_TALK.getOrDefault(villager, -1200L) < 1200 || now - LAST_TALK.getOrDefault(other, -1200L) < 1200) return;
        LAST_TALK.put(villager, now);
        LAST_TALK.put(other, now);
        NBTTagCompound source = players(other);
        List<Gossip> gossip = new ArrayList<>();
        int total = 0;
        for (String id : source.getKeySet()) {
            NBTTagCompound known = source.getCompoundTag(id);
            for (Topic topic : Topic.values()) {
                int value = known.getInteger(topic.name());
                if (value < MIN_GOSSIP) continue;
                total += Math.abs(value * topic.weight);
                gossip.add(new Gossip(id, topic, value, total));
            }
        }
        if (total == 0) return;
        for (int draw = 0; draw < 10; draw++) {
            int ticket = villager.getRNG().nextInt(total);
            for (Gossip entry : gossip) {
                if (ticket >= entry.limit) continue;
                int transferred = entry.value - entry.topic.transferDecay;
                if (transferred >= MIN_GOSSIP) {
                    NBTTagCompound known = player(villager, entry.player);
                    String key = entry.topic.name();
                    known.setInteger(key, Math.max(known.getInteger(key), transferred));
                }
                break;
            }
        }
    }

    private static NBTTagCompound players(EntityVillager villager) {
        NBTTagCompound data = villager.getEntityData().getCompoundTag(DATA);
        NBTTagCompound players = data.getCompoundTag(PLAYERS);
        long now = villager.world.getTotalWorldTime();
        if (data.hasKey(DECAY) && now - data.getLong(DECAY) >= DAY) {
            decay(players);
            data.setLong(DECAY, now);
        }
        return players;
    }

    private static NBTTagCompound player(EntityVillager villager, String id) {
        NBTTagCompound forge = villager.getEntityData();
        NBTTagCompound data = forge.getCompoundTag(DATA);
        NBTTagCompound players = players(villager);
        NBTTagCompound known = players.getCompoundTag(id);
        if (!data.hasKey(DECAY)) data.setLong(DECAY, villager.world.getTotalWorldTime());
        players.setTag(id, known);
        data.setTag(PLAYERS, players);
        forge.setTag(DATA, data);
        return known;
    }

    private static void record(EntityVillager villager, UUID id, Topic topic, int value) {
        NBTTagCompound known = player(villager, id.toString());
        known.setInteger(topic.name(), Math.min(topic.max, known.getInteger(topic.name()) + value));
    }

    static int score(NBTTagCompound known) {
        int result = 0;
        for (Topic topic : Topic.values()) result += known.getInteger(topic.name()) * topic.weight;
        return result;
    }

    static void decay(NBTTagCompound players) {
        for (String id : new ArrayList<>(players.getKeySet())) {
            NBTTagCompound known = players.getCompoundTag(id);
            for (Topic topic : Topic.values()) {
                String key = topic.name();
                int value = known.getInteger(key) - topic.dailyDecay;
                if (value < MIN_GOSSIP) known.removeTag(key);
                else known.setInteger(key, value);
            }
            if (known.hasNoTags()) players.removeTag(id);
        }
    }

    private enum Topic {
        MAJOR_NEGATIVE(-5, 100, 10, 10), MINOR_NEGATIVE(-1, 200, 20, 20),
        MINOR_POSITIVE(1, 200, 1, 5), MAJOR_POSITIVE(5, 100, 0, 100), TRADING(1, 25, 2, 20);

        final int weight, max, dailyDecay, transferDecay;

        Topic(int weight, int max, int dailyDecay, int transferDecay) {
            this.weight = weight;
            this.max = max;
            this.dailyDecay = dailyDecay;
            this.transferDecay = transferDecay;
        }
    }

    private static final class Gossip {
        final String player;
        final Topic topic;
        final int value, limit;

        Gossip(String player, Topic topic, int value, int limit) {
            this.player = player;
            this.topic = topic;
            this.value = value;
            this.limit = limit;
        }
    }

    private static final class ConversationTarget {
        static final Field FIELD = ReflectionHelper.findField(EntityAIWatchClosest.class, "closestEntity", "field_75334_a");
    }
}
