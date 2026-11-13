package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.village.VillageSiege;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.village.VillageSiegeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class VillageSiegeReward {
    private static final String DATA = "psychedelicraft_village_sieges";
    private static final String SIEGE = "PsychedelicraftSiege";
    private static final int ACTIVE = 1;
    private static final int COMPLETE = 2;
    private static final long MAX_DURATION = 48000;

    private VillageSiegeReward() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void start(VillageSiegeEvent event) {
        if (event.getWorld().isRemote) return;
        Records records = records(event.getWorld());
        records.endWave(false);
        records.sieges.add(new Siege(UUID.randomUUID(), event.getVillage().getCenter(),
                event.getWorld().getTotalWorldTime() + MAX_DURATION));
        records.markDirty();
    }

    public static boolean spawnAttacker(World world, Entity entity) {
        if (!world.spawnEntity(entity)) return false;
        Records records = records(world);
        Siege siege = records.openWave();
        if (siege != null) {
            entity.getEntityData().setUniqueId(SIEGE, siege.id);
            siege.attackers.add(entity.getUniqueID());
            records.markDirty();
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void attackerDied(LivingDeathEvent event) {
        EntityLivingBase attacker = event.getEntityLiving();
        NBTTagCompound tag = attacker.getEntityData();
        if (!(attacker instanceof EntityZombie) || attacker.world.isRemote || !tag.hasUniqueId(SIEGE)) return;
        UUID id = tag.getUniqueId(SIEGE);
        Records records = records(attacker.world);
        for (Siege siege : records.sieges) {
            if (!siege.id.equals(id) || !siege.attackers.remove(attacker.getUniqueID())) continue;
            Entity killer = event.getSource().getTrueSource();
            if (killer instanceof EntityPlayer && !((EntityPlayer) killer).isSpectator()) {
                siege.defenders.add(killer.getUniqueID());
            }
            records.markDirty();
            return;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (event.phase != TickEvent.Phase.END || world.isRemote) return;
        Records records = records(world);
        if (records.openWave() != null) {
            int state = siegeState(world);
            if (state != ACTIVE) records.endWave(state == COMPLETE);
        }
        if (records.sieges.removeIf(siege -> finished(world, siege))) records.markDirty();
    }

    private static boolean finished(World world, Siege siege) {
        if (world.getTotalWorldTime() >= siege.expires) return true;
        if (!siege.closed || !siege.attackers.isEmpty()) return false;
        Village village = world.getVillageCollection().getNearestVillage(siege.center, 0);
        if (village != null && hasResidents(world, village)) {
            for (UUID defender : siege.defenders) {
                EntityPlayer player = world.getPlayerEntityByUUID(defender);
                if (player != null && player.isEntityAlive() && !player.isSpectator()) {
                    VillagerPrices.rewardVillageDefender(player);
                }
            }
        }
        return true;
    }

    private static boolean hasResidents(World world, Village village) {
        BlockPos center = village.getCenter();
        int radius = village.getVillageRadius();
        AxisAlignedBB bounds = new AxisAlignedBB(center.add(-radius, -4, -radius), center.add(radius, 4, radius));
        return !world.getEntitiesWithinAABB(EntityVillager.class, bounds, EntityLivingBase::isEntityAlive).isEmpty();
    }

    private static int siegeState(World world) {
        try {
            return SiegeAccess.STATE.getInt(SiegeAccess.SIEGE.get(world));
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not read the native village siege state", error);
        }
    }

    private static final class SiegeAccess {
        static final Field SIEGE = ReflectionHelper.findField(WorldServer.class, "villageSiege", "field_175740_d");
        static final Field STATE = ReflectionHelper.findField(VillageSiege.class, "siegeState", "field_75536_c");
    }

    private static Records records(World world) {
        MapStorage storage = world.getPerWorldStorage();
        Records records = (Records) storage.getOrLoadData(Records.class, DATA);
        if (records == null) {
            records = new Records(DATA);
            storage.setData(DATA, records);
        }
        return records;
    }

    public static final class Records extends WorldSavedData {
        private final List<Siege> sieges = new ArrayList<>();

        public Records(String name) {
            super(name);
        }

        private Siege openWave() {
            if (sieges.isEmpty()) return null;
            Siege siege = sieges.get(sieges.size() - 1);
            return siege.closed ? null : siege;
        }

        private void endWave(boolean completed) {
            Siege siege = openWave();
            if (siege != null) {
                if (completed) siege.closed = true;
                else sieges.remove(sieges.size() - 1);
                markDirty();
            }
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            sieges.clear();
            NBTTagList list = tag.getTagList("Sieges", 10);
            for (int index = 0; index < list.tagCount(); index++) {
                NBTTagCompound entry = list.getCompoundTagAt(index);
                if (!entry.hasUniqueId("Id") || !entry.getBoolean("Closed")) continue;
                Siege siege = new Siege(entry.getUniqueId("Id"), BlockPos.fromLong(entry.getLong("Center")), entry.getLong("Expires"));
                siege.closed = true;
                readIds(entry.getTagList("Attackers", 10), siege.attackers);
                readIds(entry.getTagList("Defenders", 10), siege.defenders);
                sieges.add(siege);
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            for (Siege siege : sieges) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setUniqueId("Id", siege.id);
                entry.setLong("Center", siege.center.toLong());
                entry.setLong("Expires", siege.expires);
                entry.setBoolean("Closed", siege.closed);
                entry.setTag("Attackers", writeIds(siege.attackers));
                entry.setTag("Defenders", writeIds(siege.defenders));
                list.appendTag(entry);
            }
            tag.setTag("Sieges", list);
            return tag;
        }

        private static void readIds(NBTTagList list, Set<UUID> ids) {
            for (int index = 0; index < list.tagCount(); index++) {
                NBTTagCompound tag = list.getCompoundTagAt(index);
                if (tag.hasUniqueId("Id")) ids.add(tag.getUniqueId("Id"));
            }
        }

        private static NBTTagList writeIds(Set<UUID> ids) {
            NBTTagList list = new NBTTagList();
            for (UUID id : ids) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setUniqueId("Id", id);
                list.appendTag(tag);
            }
            return list;
        }
    }

    private static final class Siege {
        final UUID id;
        final BlockPos center;
        final long expires;
        final Set<UUID> attackers = new HashSet<>();
        final Set<UUID> defenders = new HashSet<>();
        boolean closed;

        Siege(UUID id, BlockPos center, long expires) {
            this.id = id;
            this.center = center;
            this.expires = expires;
        }
    }
}
