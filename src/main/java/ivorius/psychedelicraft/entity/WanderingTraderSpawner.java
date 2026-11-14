package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class WanderingTraderSpawner {
    private static final String RULE = "doTraderSpawning";
    private static final String DATA = "psychedelicraft_trader_spawn";
    private static final int ATTEMPT_INTERVAL = 24000;
    private static final int VISIT_DURATION = 48000;
    private static final int MIN_CHANCE = 25;
    private static final int MAX_CHANCE = 75;
    private static final int VISIT_RADIUS = 48;

    private WanderingTraderSpawner() {
    }

    @SubscribeEvent
    public static void registerRule(WorldEvent.Load event) {
        GameRules rules = event.getWorld().getGameRules();
        if (!rules.areSameType(RULE, GameRules.ValueType.BOOLEAN_VALUE)) {
            boolean enabled = !rules.hasRule(RULE) || rules.getBoolean(RULE);
            rules.addGameRule(RULE, Boolean.toString(enabled), GameRules.ValueType.BOOLEAN_VALUE);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.world instanceof WorldServer)
                || event.world.provider.getDimension() != 0 || !event.world.getGameRules().getBoolean(RULE)) return;
        WorldServer world = (WorldServer) event.world;
        schedule(world).tick(world);
    }

    private static SpawnSchedule schedule(WorldServer world) {
        MapStorage storage = world.getPerWorldStorage();
        SpawnSchedule schedule = (SpawnSchedule) storage.getOrLoadData(SpawnSchedule.class, DATA);
        if (schedule == null) {
            schedule = new SpawnSchedule(DATA);
            storage.setData(DATA, schedule);
        }
        return schedule;
    }

    private static boolean visit(WorldServer world, Random random) {
        List<EntityPlayer> players = world.playerEntities.stream()
                .filter(player -> player.isEntityAlive() && !player.isSpectator())
                .collect(Collectors.toList());
        if (players.isEmpty()) return true;
        EntityPlayer player = players.get(random.nextInt(players.size()));
        if (random.nextInt(10) != 0) return false;

        BlockPos origin = player.getPosition();
        BlockPos center = origin;
        double nearest = VISIT_RADIUS * VISIT_RADIUS;
        for (Village village : world.getVillageCollection().getVillageList()) {
            double distance = village.getCenter().distanceSq(origin);
            if (distance <= nearest) {
                center = village.getCenter();
                nearest = distance;
            }
        }
        BlockPos ground = findPosition(world, random, center, VISIT_RADIUS, pos -> clearSpace(world, pos));
        if (ground == null || world.getBiome(ground) == Biomes.VOID) return false;

        WanderingTraderEntity trader = new WanderingTraderEntity(world);
        trader.setDespawnDelay(VISIT_DURATION);
        trader.setVisitTarget(center);
        if (!spawn(world, random, trader, ground)) return false;

        for (int escort = 0; escort < 2; ++escort) {
            TraderLlamaEntity llama = new TraderLlamaEntity(world);
            BlockPos position = findPosition(world, random, trader.getPosition(), 4, pos -> {
                llama.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                return llama.isNotColliding();
            });
            if (position != null && spawn(world, random, llama, position)) llama.setLeashHolder(trader, true);
        }
        return true;
    }

    @Nullable
    private static BlockPos findPosition(WorldServer world, Random random, BlockPos center, int radius,
                                         Predicate<BlockPos> fits) {
        for (int attempt = 0; attempt < 10; ++attempt) {
            BlockPos column = center.add(random.nextInt(2 * radius) - radius, 0, random.nextInt(2 * radius) - radius);
            if (!world.isBlockLoaded(column)) continue;
            BlockPos position = world.getHeight(column);
            if (WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, position)
                    && fits.test(position)) {
                return position;
            }
        }
        return null;
    }

    private static boolean clearSpace(WorldServer world, BlockPos pos) {
        return world.isAreaLoaded(pos, pos.add(1, 2, 1))
                && world.getCollisionBoxes(null, new AxisAlignedBB(pos, pos.add(2, 3, 2))).isEmpty();
    }

    private static boolean spawn(WorldServer world, Random random, EntityLiving entity, BlockPos pos) {
        entity.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360F, 0F);
        if (!entity.isNotColliding()
                || ForgeEventFactory.canEntitySpawn(entity, world, (float) entity.posX, (float) entity.posY,
                (float) entity.posZ, false) == Event.Result.DENY) return false;
        entity.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        if (entity instanceof TraderLlamaEntity) ((TraderLlamaEntity) entity).setGrowingAge(0);
        return world.spawnEntity(entity);
    }

    public static final class SpawnSchedule extends WorldSavedData {
        private final Random random = new Random();
        private int delay = ATTEMPT_INTERVAL;
        private int chance = MIN_CHANCE;

        public SpawnSchedule(String name) {
            super(name);
        }

        private void tick(WorldServer world) {
            markDirty();
            if (--delay > 0) return;
            delay = ATTEMPT_INTERVAL;
            if (!world.getGameRules().getBoolean("doMobSpawning") || !world.getMinecraftServer().getCanSpawnNPCs()) return;

            int threshold = chance;
            chance = Math.min(MAX_CHANCE, chance + MIN_CHANCE);
            if (random.nextInt(100) <= threshold && visit(world, random)) chance = MIN_CHANCE;
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            if (tag.hasKey("Delay", 99)) delay = MathHelper.clamp(tag.getInteger("Delay"), 1, ATTEMPT_INTERVAL);
            if (tag.hasKey("Chance", 99)) chance = MathHelper.clamp(tag.getInteger("Chance"), MIN_CHANCE, MAX_CHANCE);
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            tag.setInteger("Delay", delay);
            tag.setInteger("Chance", chance);
            return tag;
        }
    }
}
