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
import java.util.Map;
import java.util.UUID;

public final class WorksiteReleases extends WorldSavedData {
    private static final String NAME = "psychedelicraft_worksite_releases";
    private final Map<Integer, Map<Long, Map<BlockPos, Map<UUID, Long>>>> pending = new HashMap<>();

    public WorksiteReleases(String name) {
        super(name);
    }

    static void release(WorldServer world, int dimension, BlockPos pos, UUID dealer) {
        release(world, dimension, pos, dealer, 0L);
    }

    static void onChunkLoad(WorldServer world, Chunk chunk) {
        records(world).apply(world.provider.getDimension(), chunk);
    }

    static void releaseReservation(WorldServer world, int dimension, BlockPos pos, UUID dealer, long expiresAt) {
        if (expiresAt != 0) release(world, dimension, pos, dealer, expiresAt);
    }

    private static void release(WorldServer world, int dimension, BlockPos pos, UUID dealer, long expiresAt) {
        WorldServer ownerWorld = DimensionManager.getWorld(dimension);
        if (ownerWorld != null && ownerWorld.isBlockLoaded(pos)) {
            DryingTableBlockEntity table = VillagerWork.table(ownerWorld, pos);
            if (table != null) release(table, dealer, expiresAt);
            return;
        }
        records(world).add(dimension, pos, dealer, expiresAt);
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
        add(dimension, pos, dealer, 0L);
    }

    private Map<UUID, Long> forTable(int dimension, BlockPos pos) {
        return pending.computeIfAbsent(dimension, key -> new HashMap<>())
                .computeIfAbsent(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), key -> new HashMap<>())
                .computeIfAbsent(pos.toImmutable(), key -> new HashMap<>());
    }

    void add(int dimension, BlockPos pos, UUID dealer, long expiresAt) {
        Map<UUID, Long> releases = forTable(dimension, pos);
        Long previous = releases.get(dealer);
        if (previous != null && (previous == 0L || previous == expiresAt)) return;
        releases.put(dealer, expiresAt);
        markDirty();
    }

    void apply(int dimension, Chunk chunk) {
        Map<Long, Map<BlockPos, Map<UUID, Long>>> chunks = pending.get(dimension);
        if (chunks == null) return;
        Map<BlockPos, Map<UUID, Long>> releases = chunks.remove(ChunkPos.asLong(chunk.x, chunk.z));
        if (releases == null) return;
        releases.forEach((pos, dealers) -> {
            TileEntity table = chunk.getTileEntityMap().get(pos);
            if (table instanceof DryingTableBlockEntity) {
                DryingTableBlockEntity dryingTable = (DryingTableBlockEntity) table;
                dealers.forEach((dealer, expiresAt) -> release(dryingTable, dealer, expiresAt));
            }
        });
        if (chunks.isEmpty()) pending.remove(dimension);
        markDirty();
    }

    private static void release(DryingTableBlockEntity table, UUID dealer, long expiresAt) {
        if (expiresAt == 0) table.release(dealer);
        else table.releaseReservation(dealer, expiresAt);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        pending.clear();
        NBTTagList list = tag.getTagList("Releases", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound entry = list.getCompoundTagAt(index);
            if (!entry.hasUniqueId("Dealer") || !entry.hasKey("Pos", 4) || !entry.hasKey("Dimension", 3)) continue;
            BlockPos pos = BlockPos.fromLong(entry.getLong("Pos"));
            forTable(entry.getInteger("Dimension"), pos).put(entry.getUniqueId("Dealer"), entry.getLong("ReservationExpires"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        pending.forEach((dimension, chunks) -> chunks.values().forEach(releases ->
                releases.forEach((pos, dealers) -> dealers.forEach((dealer, expiresAt) -> {
                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setInteger("Dimension", dimension);
                    entry.setLong("Pos", pos.toLong());
                    entry.setUniqueId("Dealer", dealer);
                    if (expiresAt != 0) entry.setLong("ReservationExpires", expiresAt);
                    list.appendTag(entry);
                }))));
        tag.setTag("Releases", list);
        return tag;
    }
}
