package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WorksiteReleases extends WorldSavedData {
    private static final String NAME = "psychedelicraft_worksite_releases";
    private final Map<Integer, Map<Long, Map<BlockPos, Set<UUID>>>> pending = new HashMap<>();

    public WorksiteReleases(String name) {
        super(name);
    }

    static void release(WorldServer world, int dimension, BlockPos pos, UUID dealer) {
        WorldServer ownerWorld = DimensionManager.getWorld(dimension);
        if (ownerWorld != null && ownerWorld.isBlockLoaded(pos)) {
            DryingTableBlockEntity table = VillagerWork.table(ownerWorld, pos);
            if (table != null) table.release(dealer);
            return;
        }
        records(world).add(dimension, pos, dealer);
    }

    static void onChunkLoad(WorldServer world, Chunk chunk) {
        records(world).apply(world.provider.getDimension(), chunk);
    }

    private static WorksiteReleases records(WorldServer world) {
        // The map storage is shared across the save, including unloaded dimensions.
        MapStorage storage = world.getMapStorage();
        WorksiteReleases records = (WorksiteReleases) storage.getOrLoadData(WorksiteReleases.class, NAME);
        if (records == null) {
            records = new WorksiteReleases(NAME);
            storage.setData(NAME, records);
        }
        return records;
    }

    void add(int dimension, BlockPos pos, UUID dealer) {
        if (forChunk(dimension, pos).computeIfAbsent(pos.toImmutable(), key -> new HashSet<>()).add(dealer)) markDirty();
    }

    private Map<BlockPos, Set<UUID>> forChunk(int dimension, BlockPos pos) {
        return pending.computeIfAbsent(dimension, key -> new HashMap<>())
                .computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), key -> new HashMap<>());
    }

    void apply(int dimension, Chunk chunk) {
        Map<Long, Map<BlockPos, Set<UUID>>> chunks = pending.get(dimension);
        if (chunks == null) return;
        Map<BlockPos, Set<UUID>> releases = chunks.remove(ChunkPos.asLong(chunk.x, chunk.z));
        if (releases == null) return;
        releases.forEach((pos, dealers) -> {
            TileEntity table = chunk.getTileEntityMap().get(pos);
            if (table instanceof DryingTableBlockEntity) {
                dealers.forEach(((DryingTableBlockEntity) table)::release);
            }
        });
        if (chunks.isEmpty()) pending.remove(dimension);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        pending.clear();
        NBTTagList list = tag.getTagList("Releases", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            if (!entry.hasUniqueId("Dealer") || !entry.hasKey("Pos", 4) || !entry.hasKey("Dimension", 3)) continue;
            BlockPos pos = BlockPos.fromLong(entry.getLong("Pos"));
            forChunk(entry.getInteger("Dimension"), pos)
                    .computeIfAbsent(pos, key -> new HashSet<>()).add(entry.getUniqueId("Dealer"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, Map<Long, Map<BlockPos, Set<UUID>>>> dimension : pending.entrySet()) {
            for (Map<BlockPos, Set<UUID>> releases : dimension.getValue().values()) {
                releases.forEach((pos, dealers) -> dealers.forEach(dealer -> {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setInteger("Dimension", dimension.getKey());
                    entry.setLong("Pos", pos.toLong());
                    entry.setUniqueId("Dealer", dealer);
                    list.appendTag(entry);
                }));
            }
        }
        tag.setTag("Releases", list);
        return tag;
    }
}
