package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.block.TallCropBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.DryingRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

final class VillagerWork extends EntityAIBase {
    private static final String WORKSITE = "Worksite";
    private static final String WORKSITE_DIMENSION = "WorksiteDimension";
    private static final int SEARCH_RADIUS = 32;
    private static final double WORK_DISTANCE_SQ = 1.73D * 1.73D;
    private static final int CROP_SESSION_TICKS = 60;
    private static final int CROP_TARGET_DELAY = 20;
    private static final int CROP_SESSION_DELAY = 40;
    private static final int FARMLAND_WALK_DELAY = 100;
    private static final int DIRECT_RETURN_DISTANCE = 100;
    private static final int UNREACHABLE_WORKSITE_TICKS = 1200;
    private static final double WORKSITE_WAYPOINT_RANGE = 7D;

    private final EntityVillager villager;
    private final VillagerTradePreview preview;
    private final List<BlockPos> cropTargets = new ArrayList<>();
    private long nextTask;
    private long lastWorkCheck;
    private long nextCropAction;
    private long nextCropSession;
    private long nextFarmlandWalk;
    private long unreachableSince = -1;
    private int nearestReturnDistance;
    private long retryReleasedSite;
    private BlockPos returnSite;
    private BlockPos releasedSite;
    private BlockPos target;
    private Vec3d wander;
    private Job job;
    private int remaining;

    VillagerWork(EntityVillager villager, VillagerTradePreview preview) {
        this.villager = villager;
        this.preview = preview;
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!canWork(villager)) return false;
        VillagerProfession profession = villager.getProfessionForge();
        if (profession != VillagerCareers.DEALER && profession != VillagerCareers.UNEMPLOYED
                && profession != VillagerCareers.ADDICT) return false;
        long now = villager.world.getTotalWorldTime();
        if (now < nextTask) return false;
        nextTask = now + 40;

        if (profession == VillagerCareers.ADDICT) {
            EntityPlayer player = villager.world.getClosestPlayerToEntity(villager, 9);
            if (player == null) return false;
            wander = RandomPositionGenerator.findRandomTarget(villager, 8, 6);
            nextTask = now + 180;
            job = Job.WANDER;
            return wander != null;
        }

        BlockPos site = worksite(villager);
        if (site != null) {
            if (worksiteDimension(villager) != villager.world.provider.getDimension() || !villager.world.isBlockLoaded(site)) {
                unreachableSince = -1;
                return false;
            }
            DryingTableBlockEntity table = table(villager.world, site);
            if (table == null || !table.availableTo(villager.getUniqueID())) {
                loseWorksite();
                site = null;
            }
        }
        if (site == null) {
            target = findWorksite();
            job = Job.CLAIM;
            return target != null;
        }
        if (now >= nextCropSession && withinWorkArea(site) && villager.getRNG().nextInt(3) == 0
                && ForgeEventFactory.getMobGriefingEvent(villager.world, villager)) {
            findCrops();
            if (!cropTargets.isEmpty()) {
                target = chooseCrop();
                job = Job.CROP;
                return true;
            }
        }
        if (now >= nextFarmlandWalk && villager.getDistanceSqToCenter(site) < 36) {
            BlockPos farmland = findFarmland();
            if (farmland != null) {
                nextFarmlandWalk = now + FARMLAND_WALK_DELAY;
                target = farmland.up();
                job = Job.VISIT_FARMLAND;
                return true;
            }
        }
        target = site;
        job = Job.DRY;
        return true;
    }

    private boolean withinWorkArea(BlockPos site) {
        return manhattan(villager.getPosition(), site) <= 9;
    }

    private static int manhattan(BlockPos first, BlockPos second) {
        return Math.abs(first.getX() - second.getX()) + Math.abs(first.getY() - second.getY())
                + Math.abs(first.getZ() - second.getZ());
    }

    static boolean canWork(EntityVillager villager) {
        long time = Math.floorMod(villager.world.getWorldTime(), 24000L);
        return !villager.isChild() && !villager.isTrading() && time >= 2000 && time < 9000;
    }

    @Override
    public void startExecuting() {
        remaining = job == Job.CROP ? CROP_SESSION_TICKS : 160;
        nextCropAction = villager.world.getTotalWorldTime();
        if (job == Job.WANDER) {
            villager.getNavigator().tryMoveToXYZ(wander.x, wander.y, wander.z, 0.4D);
        } else if (job == Job.DRY) {
            if (villager.getDistanceSqToCenter(target) > WORK_DISTANCE_SQ) returnToWorksite();
        } else {
            moveTowardTarget();
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        return remaining > 0 && villager.isEntityAlive() && canWork(villager)
                && (job != Job.WANDER || !villager.getNavigator().noPath());
    }

    @Override
    public void updateTask() {
        remaining--;
        if (job == Job.WANDER) return;
        if (job == Job.CROP) {
            workCrops();
            return;
        }
        if (!villager.world.isBlockLoaded(target)) {
            if (job == Job.DRY) unreachableSince = -1;
            remaining = 0;
            return;
        }
        if (job == Job.VISIT_FARMLAND && villager.world.getBlockState(target.down()).getBlock() != Blocks.FARMLAND) {
            remaining = 0;
            return;
        }
        if (preview.shown().isEmpty()) {
            villager.getLookHelper().setLookPosition(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 20, 20);
        }
        double range = job == Job.CLAIM ? 6.25D : job == Job.VISIT_FARMLAND ? 1D : WORK_DISTANCE_SQ;
        if (villager.getDistanceSqToCenter(target) > range) {
            if (remaining % 20 == 0) {
                if (job == Job.DRY) {
                    if (villager.getNavigator().noPath()) returnToWorksite();
                } else if (!moveTowardTarget()) remaining = 0;
            }
            return;
        }
        if (job == Job.CLAIM) claim();
        else if (job == Job.DRY) {
            unreachableSince = -1;
            workTable();
        }
        remaining = 0;
    }

    @Override
    public void resetTask() {
        if (job == Job.CROP) {
            nextCropSession = villager.world.getTotalWorldTime() + CROP_SESSION_DELAY;
            cropTargets.clear();
        }
        villager.getNavigator().clearPath();
    }

    private boolean moveTowardTarget() {
        return villager.getNavigator().tryMoveToXYZ(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 0.5D);
    }

    private void returnToWorksite() {
        if (!target.equals(returnSite)) {
            returnSite = target;
            unreachableSince = -1;
            nearestReturnDistance = Integer.MAX_VALUE;
        }
        BlockPos origin = villager.getPosition();
        int distance = manhattan(origin, target);
        Path path = distance <= DIRECT_RETURN_DISTANCE ? villager.getNavigator().getPathToPos(target) : null;
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        if (end != null && target.distanceSqToCenter(end.x + 0.5D, end.y, end.z + 0.5D) <= WORK_DISTANCE_SQ
                && villager.getNavigator().setPath(path, 0.5D)) {
            unreachableSince = -1;
            return;
        }

        long now = villager.world.getTotalWorldTime();
        if (unreachableSince < 0 || distance < nearestReturnDistance) {
            unreachableSince = now;
            nearestReturnDistance = distance;
        }
        if (now - unreachableSince > UNREACHABLE_WORKSITE_TICKS) {
            releasedSite = target;
            retryReleasedSite = now + UNREACHABLE_WORKSITE_TICKS;
            VillagerCareers.releaseWorksite(villager);
            loseWorksite();
            remaining = 0;
            return;
        }
        if (end != null && manhattan(new BlockPos(end.x, end.y, end.z), target) < distance
                && villager.getNavigator().setPath(path, 0.5D)) return;

        // Legacy pathfinding only expands nodes close to its destination, so distant sites need a local target.
        double offsetX = target.getX() - origin.getX();
        double offsetZ = target.getZ() - origin.getZ();
        double horizontalDistance = Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        if (horizontalDistance > WORKSITE_WAYPOINT_RANGE) {
            double scale = WORKSITE_WAYPOINT_RANGE / horizontalDistance;
            BlockPos waypoint = origin.add(offsetX * scale, 0D, offsetZ * scale);
            if (walkTowardWaypoint(waypoint, distance)) return;
        }

        Vec3d waypoint = RandomPositionGenerator.findRandomTargetBlockTowards(villager, 15, 7,
                new Vec3d(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D));
        if (waypoint != null) walkTowardWaypoint(new BlockPos(waypoint), distance);
    }

    private boolean walkTowardWaypoint(BlockPos waypoint, int distanceToSite) {
        if (!villager.world.isBlockLoaded(waypoint)) return false;
        Path path = villager.getNavigator().getPathToPos(waypoint);
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        return end != null && manhattan(new BlockPos(end.x, end.y, end.z), target) < distanceToSite
                && villager.getNavigator().setPath(path, 0.5D);
    }

    private void loseWorksite() {
        forgetWorksite(villager);
        unreachableSince = -1;
        returnSite = null;
        if (VillagerCareers.savedData(villager).getInteger("Experience") == 0) {
            VillagerCareers.changeCareer(villager, VillagerCareers.UNEMPLOYED);
        }
    }

    private void claim() {
        VillagerProfession profession = villager.getProfessionForge();
        if (profession != VillagerCareers.UNEMPLOYED && profession != VillagerCareers.DEALER) return;
        DryingTableBlockEntity table = table(villager.world, target);
        if (table != null && table.claim(villager.getUniqueID())) {
            if (profession == VillagerCareers.UNEMPLOYED) {
                VillagerCareers.changeCareer(villager, VillagerCareers.DEALER);
                villager.world.setEntityState(villager, (byte) 14);
            }
            NBTTagCompound data = VillagerCareers.data(villager);
            data.setLong(WORKSITE, target.toLong());
            data.setInteger(WORKSITE_DIMENSION, villager.world.provider.getDimension());
        }
    }

    private void workTable() {
        DryingTableBlockEntity table = table(villager.world, target);
        if (table == null || !table.claim(villager.getUniqueID())) return;
        long now = villager.world.getTotalWorldTime();
        if (now - lastWorkCheck < 300 || villager.world.rand.nextInt(2) != 0) return;
        lastWorkCheck = now;
        // The 1.12 drink sample is also one of the donor's wandering-trader samples.
        villager.playSound(SoundEvents.ENTITY_GENERIC_DRINK, 0.65F,
                1F + (villager.getRNG().nextFloat() - villager.getRNG().nextFloat()) * 0.2F);
        ItemStack output = table.removeStackFromSlot(DryingRecipe.OUTPUT);
        if (!output.isEmpty()) {
            ItemStack excess = villager.getVillagerInventory().addItem(output);
            if (!excess.isEmpty()) villager.entityDropItem(excess, 0F);
        }
        if (table.isEmpty()) loadBatch(villager.getVillagerInventory(), table);
        VillagerCareers.restock(villager);
    }

    static boolean loadBatch(IInventory inventory, DryingTableBlockEntity table) {
        if (!table.isEmpty()) return false;
        for (DryingRecipe recipe : DryingRecipe.recipes()) {
            int[] sources = new int[DryingRecipe.INPUT_COUNT];
            int found = 0;
            for (int slot = 0; slot < inventory.getSizeInventory() && found < sources.length; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!recipe.getInput().apply(stack)) continue;
                for (int count = 0; count < stack.getCount() && found < sources.length; count++) sources[found++] = slot;
            }
            if (found == sources.length) {
                for (int i = 0; i < sources.length; i++) table.setInventorySlotContents(i + 1, inventory.decrStackSize(sources[i], 1));
                return true;
            }
        }
        return false;
    }

    @Nullable
    private BlockPos findWorksite() {
        WorldServer world = (WorldServer) villager.world;
        BlockPos center = villager.getPosition();
        BlockPos best = null;
        double distance = SEARCH_RADIUS * SEARCH_RADIUS;
        long now = world.getTotalWorldTime();
        for (int x = (center.getX() - SEARCH_RADIUS) >> 4; x <= (center.getX() + SEARCH_RADIUS) >> 4; x++) {
            for (int z = (center.getZ() - SEARCH_RADIUS) >> 4; z <= (center.getZ() + SEARCH_RADIUS) >> 4; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk == null) continue;
                for (TileEntity tile : chunk.getTileEntityMap().values()) {
                    if (!(tile instanceof DryingTableBlockEntity) || tile.isInvalid()
                            || !((DryingTableBlockEntity) tile).availableTo(villager.getUniqueID())) continue;
                    if (now < retryReleasedSite && tile.getPos().equals(releasedSite)) continue;
                    double candidate = villager.getDistanceSqToCenter(tile.getPos());
                    if (candidate < distance) {
                        distance = candidate;
                        best = tile.getPos();
                    }
                }
            }
        }
        return best;
    }

    private void findCrops() {
        cropTargets.clear();
        World world = villager.world;
        BlockPos origin = villager.getPosition();
        boolean hasSeed = seedSlot(villager.getVillagerInventory()) >= 0;
        boolean farmlandNearby = false;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-1, -1, -1), origin.add(1, 1, 1))) {
            if (!world.isBlockLoaded(pos)) continue;
            IBlockState state = world.getBlockState(pos);
            if (world.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND) {
                farmlandNearby = true;
                if (hasSeed && world.isAirBlock(pos)) cropTargets.add(pos.toImmutable());
            }
            if (mature(state)) cropTargets.add(pos.toImmutable());
        }
        if (!farmlandNearby && !cropTargets.isEmpty() && !hasFarmland(origin)) cropTargets.clear();
    }

    private boolean hasFarmland(BlockPos origin) {
        World world = villager.world;
        for (BlockPos pos : farmlandSearch(origin)) {
            if (world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() == Blocks.FARMLAND) return true;
        }
        return false;
    }

    @Nullable
    private BlockPos findFarmland() {
        World world = villager.world;
        BlockPos chosen = null;
        int candidates = 0;
        for (BlockPos pos : farmlandSearch(villager.getPosition())) {
            if (world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() == Blocks.FARMLAND
                    && world.rand.nextInt(++candidates) == 0) {
                chosen = pos.toImmutable();
            }
        }
        return chosen;
    }

    private static Iterable<BlockPos.MutableBlockPos> farmlandSearch(BlockPos origin) {
        return BlockPos.getAllInBoxMutable(origin.add(-4, -2, -4), origin.add(4, 2, 4));
    }

    private BlockPos chooseCrop() {
        return cropTargets.remove(villager.world.rand.nextInt(cropTargets.size()));
    }

    private void workCrops() {
        World world = villager.world;
        if (villager.getProfessionForge() != VillagerCareers.DEALER || !ForgeEventFactory.getMobGriefingEvent(world, villager)) {
            remaining = 0;
            return;
        }
        if (!world.isBlockLoaded(target)) {
            advanceCrop();
            return;
        }
        if (preview.shown().isEmpty()) {
            villager.getLookHelper().setLookPosition(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 20, 20);
        }
        if (villager.getDistanceSqToCenter(target) >= 1D) {
            if (remaining % 20 == 0 && !moveTowardTarget()) advanceCrop();
            return;
        }
        if (world.getTotalWorldTime() < nextCropAction) return;

        IBlockState state = world.getBlockState(target);
        IInventory inventory = villager.getVillagerInventory();
        if (mature(state) && world.destroyBlock(target, true)) return;
        if (world.isAirBlock(target) && world.getBlockState(target.down()).getBlock() == Blocks.FARMLAND) {
            int seed = seedSlot(inventory);
            if (seed >= 0) {
                Block crop = ((ItemBlock) inventory.getStackInSlot(seed).getItem()).getBlock();
                if (crop.canPlaceBlockAt(world, target) && world.setBlockState(target, crop.getDefaultState(), 3)) {
                    inventory.decrStackSize(seed, 1);
                    world.playSound(null, target.getX(), target.getY(), target.getZ(), PSSounds.CROP_PLANT,
                            SoundCategory.BLOCKS, 1F, 1F);
                }
            }
        }
        advanceCrop();
    }

    private void advanceCrop() {
        if (cropTargets.isEmpty()) {
            remaining = 0;
            return;
        }
        target = chooseCrop();
        nextCropAction = villager.world.getTotalWorldTime() + CROP_TARGET_DELAY;
        moveTowardTarget();
    }

    static boolean mature(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockCrops) return ((BlockCrops) block).isMaxAge(state);
        return block instanceof TallCropBlock && ((TallCropBlock) block).isMature(state);
    }

    static boolean isSeed(Item item) {
        return item == PSItems.CANNABIS_SEEDS || item == PSItems.HOP_SEEDS || item == PSItems.TOBACCO_SEEDS
                || item == PSItems.COCA_SEEDS || item == PSItems.COFFEA_CHERRIES || item == PSItems.MORNING_GLORY_SEEDS;
    }

    private static int seedSlot(IInventory inventory) {
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            if (isSeed(inventory.getStackInSlot(slot).getItem())) return slot;
        }
        return -1;
    }

    @Nullable
    static BlockPos worksite(EntityVillager villager) {
        NBTTagCompound data = VillagerCareers.savedData(villager);
        return data.hasKey(WORKSITE, 4) ? BlockPos.fromLong(data.getLong(WORKSITE)) : null;
    }

    static int worksiteDimension(EntityVillager villager) {
        NBTTagCompound data = VillagerCareers.savedData(villager);
        return data.hasKey(WORKSITE_DIMENSION, 3) ? data.getInteger(WORKSITE_DIMENSION)
                : villager.world.provider.getDimension();
    }

    static void rememberWorksiteDimension(EntityVillager villager) {
        NBTTagCompound data = VillagerCareers.savedData(villager);
        if (data.hasKey(WORKSITE, 4) && !data.hasKey(WORKSITE_DIMENSION, 3)) {
            data.setInteger(WORKSITE_DIMENSION, villager.world.provider.getDimension());
        }
    }

    static void forgetWorksite(EntityVillager villager) {
        NBTTagCompound data = VillagerCareers.savedData(villager);
        data.removeTag(WORKSITE);
        data.removeTag(WORKSITE_DIMENSION);
    }

    @Nullable
    static DryingTableBlockEntity table(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof DryingTableBlockEntity && !tile.isInvalid() ? (DryingTableBlockEntity) tile : null;
    }

    private enum Job { CLAIM, DRY, CROP, VISIT_FARMLAND, WANDER }
}
