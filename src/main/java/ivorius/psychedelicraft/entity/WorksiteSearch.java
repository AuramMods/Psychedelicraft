package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class WorksiteSearch {
    private static final int SEARCH_RADIUS = 48;
    private static final int CANDIDATE_LIMIT = 5;
    private static final int MAX_RETRY_DELAY = 400;
    private static final float PATH_RANGE = 64F;

    private final Map<BlockPos, Retry> retries = new HashMap<>();
    private int dimension;

    @Nullable
    Result find(EntityVillager villager, long now, @Nullable BlockPos releasedSite) {
        WorldServer world = (WorldServer) villager.world;
        if (dimension != world.provider.getDimension()) {
            dimension = world.provider.getDimension();
            clearRetries();
        }
        retries.values().removeIf(retry -> now > retry.attemptedAt + MAX_RETRY_DELAY);
        BlockPos origin = villager.getPosition();
        List<BlockPos> sites = findCandidates(world, villager, origin, now, releasedSite);
        if (sites.isEmpty()) return null;
        sites.sort(Comparator.comparingDouble(origin::distanceSq));

        PathFinder finder = new PathFinder(((PathNavigateGround) villager.getNavigator()).getNodeProcessor());
        IBlockAccess blocks = new LoadedBlocks(world);
        Result nearest = null;
        float shortest = Float.POSITIVE_INFINITY;
        for (int i = 0; i < Math.min(sites.size(), CANDIDATE_LIMIT); i++) {
            BlockPos site = sites.get(i);
            if (isApproach(origin, site) && villager.getDistanceSqToCenter(site) < 4D) {
                return new Result(site, null);
            }
            Path path = finder.findPath(blocks, villager, site, PATH_RANGE);
            PathPoint end = path == null ? null : path.getFinalPathPoint();
            if (end == null || !isApproach(end.x, end.y, end.z, site)) {
                retryLater(site, villager);
            } else if (end.distanceFromOrigin < shortest) {
                nearest = new Result(site, path);
                shortest = end.distanceFromOrigin;
            }
        }
        return nearest;
    }

    private List<BlockPos> findCandidates(WorldServer world, EntityVillager villager, BlockPos origin, long now,
                                          @Nullable BlockPos releasedSite) {
        List<BlockPos> sites = new ArrayList<>();
        for (int x = (origin.getX() - SEARCH_RADIUS) >> 4; x <= (origin.getX() + SEARCH_RADIUS) >> 4; x++) {
            for (int z = (origin.getZ() - SEARCH_RADIUS) >> 4; z <= (origin.getZ() + SEARCH_RADIUS) >> 4; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk == null) continue;
                for (TileEntity tile : chunk.getTileEntityMap().values()) {
                    if (!(tile instanceof DryingTableBlockEntity) || tile.isInvalid()
                            || !((DryingTableBlockEntity) tile).availableTo(villager.getUniqueID())) continue;
                    BlockPos pos = tile.getPos();
                    if (pos.equals(releasedSite) || origin.distanceSq(pos) > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                    Retry retry = retries.get(pos);
                    if (retry == null || now >= retry.attemptedAt + retry.delay) sites.add(pos);
                }
            }
        }
        return sites;
    }

    void retryLater(BlockPos site, EntityVillager villager) {
        Retry retry = retries.computeIfAbsent(site, key -> new Retry());
        retry.attemptedAt = villager.world.getTotalWorldTime();
        retry.delay = Math.min(MAX_RETRY_DELAY, retry.delay + 40 + villager.world.rand.nextInt(40));
    }

    void clearRetries() {
        retries.clear();
    }

    static boolean isApproach(BlockPos pos, BlockPos site) {
        return isApproach(pos.getX(), pos.getY(), pos.getZ(), site);
    }

    private static boolean isApproach(int x, int y, int z, BlockPos site) {
        return Math.abs(x - site.getX()) + Math.abs(y - site.getY()) + Math.abs(z - site.getZ()) <= 1;
    }

    static final class Result {
        final BlockPos site;
        @Nullable
        final Path path;

        Result(BlockPos site, @Nullable Path path) {
            this.site = site;
            this.path = path;
        }
    }

    private static final class Retry {
        long attemptedAt;
        int delay;
    }

    // The legacy navigator's ChunkCache generates missing chunks. A worksite search only walks through loaded terrain.
    private static final class LoadedBlocks implements IBlockAccess {
        private final WorldServer world;

        LoadedBlocks(WorldServer world) {
            this.world = world;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? Blocks.STONE.getDefaultState() : chunk.getBlockState(pos);
        }

        @Nullable
        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            IBlockState state = getBlockState(pos);
            return state.getBlock().isAir(state, this, pos);
        }

        @Override
        public int getStrongPower(BlockPos pos, EnumFacing side) {
            return getBlockState(pos).getStrongPower(this, pos, side);
        }

        @Override
        public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean defaultValue) {
            return getBlockState(pos).isSideSolid(this, pos, side);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getCombinedLight(BlockPos pos, int light) {
            return world.isBlockLoaded(pos) ? world.getCombinedLight(pos, light) : light;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public Biome getBiome(BlockPos pos) {
            return world.isBlockLoaded(pos) ? world.getBiome(pos) : Biomes.PLAINS;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public WorldType getWorldType() {
            return world.getWorldType();
        }
    }
}
