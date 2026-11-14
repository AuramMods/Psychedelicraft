package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.BrushableBlockEntity;
import ivorius.psychedelicraft.entity.DrownedEntity;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class WarmOceanRuinPiece extends StructureComponent {
    static final ResourceLocation ARCHAEOLOGY = Psychedelicraft.id("archaeology/ocean_ruin_warm");
    static final ResourceLocation SMALL_CHEST = Psychedelicraft.id("chests/underwater_ruin_small");
    static final ResourceLocation LARGE_CHEST = Psychedelicraft.id("chests/underwater_ruin_big");
    private static final int MAX_BRUSHABLE = 5;

    private BlockPos origin;
    private Rotation rotation;
    private boolean large;
    private boolean settled;
    private int variant;

    public WarmOceanRuinPiece() {
    }

    WarmOceanRuinPiece(BlockPos origin, Rotation rotation, boolean large, Random random) {
        super(0);
        this.origin = origin;
        this.rotation = rotation;
        this.large = large;
        this.variant = large ? 4 + random.nextInt(4) : 1 + random.nextInt(8);
        boundingBox = bounds(origin, rotation, large);
    }

    static StructureBoundingBox bounds(BlockPos origin, Rotation rotation, boolean large) {
        PlacementSettings placement = new PlacementSettings().setRotation(rotation);
        BlockPos farCorner = origin.add(Template.transformedBlockPos(placement,
                large ? new BlockPos(15, 0, 15) : new BlockPos(5, 0, 6)));
        return StructureBoundingBox.createProper(origin.getX(), 0, origin.getZ(),
                farCorner.getX(), 255, farCorner.getZ());
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
        tag.setLong("Origin", origin.toLong());
        tag.setString("Rotation", rotation.name());
        tag.setBoolean("Large", large);
        tag.setBoolean("Settled", settled);
        tag.setInteger("Variant", variant);
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager templates) {
        origin = BlockPos.fromLong(tag.getLong("Origin"));
        rotation = Rotation.valueOf(tag.getString("Rotation"));
        large = tag.getBoolean("Large");
        settled = tag.getBoolean("Settled");
        variant = tag.getInteger("Variant");
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox area) {
        if (!settled) {
            origin = origin.up(getFloorHeight((WorldServer) world));
            settled = true;
        }

        ResourceLocation id = Psychedelicraft.id("underwater_ruin/" + (large ? "big_warm_" : "warm_") + variant);
        Template template = world.getSaveHandler().getStructureTemplateManager().get(world.getMinecraftServer(), id);
        if (template == null) throw new IllegalStateException("Missing warm ocean ruin template " + id);
        PlacementSettings placement = new PlacementSettings().setRotation(rotation).setIgnoreEntities(true)
                .setIgnoreStructureBlock(true).setReplacedBlock(Blocks.AIR).setBoundingBox(area);
        float integrity = large ? .9F : .8F;
        Map<BlockPos, Long> buried = chooseBuriedSand((WorldServer) world, template, placement, integrity);
        OceanTemplateWater water = new OceanTemplateWater(world);
        template.addBlocksToWorld(world, origin, (level, pos, info) -> {
            Block block = info.blockState.getBlock();
            if (block == Blocks.AIR || block == Blocks.STRUCTURE_BLOCK || !survives(pos, integrity)) return null;
            water.remember(pos, info);
            return buried.containsKey(pos) ? new Template.BlockInfo(info.pos, PSBlocks.SUSPICIOUS_SAND.getDefaultState(), null) : info;
        }, placement, 2);
        water.restore();

        buried.forEach((pos, seed) -> {
            if (!area.isVecInside(pos)) return;
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof BrushableBlockEntity) ((BrushableBlockEntity) tile).setLootTable(ARCHAEOLOGY, seed);
        });
        template.getDataBlocks(origin, placement).forEach((pos, name) -> {
            if ("chest".equals(name)) placeChest(world, pos);
            else if ("drowned".equals(name)) spawnDrowned(world, pos);
        });
        return true;
    }

    private void spawnDrowned(World world, BlockPos pos) {
        DrownedEntity drowned = new DrownedEntity(world);
        drowned.enablePersistence();
        drowned.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drowned.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        world.spawnEntity(drowned);
        world.setBlockState(pos, (pos.getY() > world.getSeaLevel() ? Blocks.AIR : Blocks.WATER).getDefaultState(), 2);
    }

    private void placeChest(World world, BlockPos pos) {
        // The donor sets the chest wet even when the marker contains flowing water.
        boolean wet = Waterlogging.isWater(world, pos);
        world.setBlockState(pos, Blocks.CHEST.getDefaultState().withRotation(rotation), 2);
        if (wet) Waterlogging.fillSource(world, pos);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityChest) {
            ((TileEntityChest) tile).setLootTable(large ? LARGE_CHEST : SMALL_CHEST, positionSeed(pos));
            tile.markDirty();
        }
    }

    private Map<BlockPos, Long> chooseBuriedSand(WorldServer world, Template template, PlacementSettings placement, float integrity) {
        NBTTagCompound templateTag = template.writeToNBT(new NBTTagCompound());
        NBTTagList palette = templateTag.getTagList("palette", Constants.NBT.TAG_COMPOUND);
        NBTTagList blocks = templateTag.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
        IBlockState[] states = new IBlockState[palette.tagCount()];
        for (int i = 0; i < states.length; i++) {
            states[i] = NBTUtil.readBlockState(palette.getCompoundTagAt(i));
        }
        Map<BlockPos, IBlockState> placedBlocks = new LinkedHashMap<>();
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound block = blocks.getCompoundTagAt(i);
            IBlockState state = states[block.getInteger("state")];
            if (state.getBlock() == Blocks.AIR || state.getBlock() == Blocks.STRUCTURE_BLOCK) continue;
            NBTTagList coords = block.getTagList("pos", Constants.NBT.TAG_INT);
            BlockPos pos = new BlockPos(coords.getIntAt(0), coords.getIntAt(1), coords.getIntAt(2));
            BlockPos placed = origin.add(Template.transformedBlockPos(placement, pos));
            if (survives(placed, integrity)) placedBlocks.put(placed, state);
        }
        ChunkGeneratorOverworld generator = (ChunkGeneratorOverworld) world.getChunkProvider().chunkGenerator;
        Map<Long, ChunkPrimer> terrain = new HashMap<>();
        List<BlockPos> eligible = new ArrayList<>();
        placedBlocks.forEach((pos, state) -> {
            if (state.getBlock() == Blocks.SAND && hasSupport(pos, placedBlocks, generator, terrain)) eligible.add(pos);
        });
        Random random = new Random(world.getSeed() ^ positionSeed(origin));
        Collections.shuffle(eligible, random);
        Map<BlockPos, Long> buried = new HashMap<>();
        for (int i = 0; i < Math.min(MAX_BRUSHABLE, eligible.size()); i++) {
            buried.put(eligible.get(i), random.nextLong());
        }
        return buried;
    }

    private static boolean hasSupport(BlockPos sand, Map<BlockPos, IBlockState> placedBlocks,
                                      ChunkGeneratorOverworld generator, Map<Long, ChunkPrimer> terrain) {
        BlockPos below = sand.down();
        IBlockState state = placedBlocks.get(below);
        while (state != null && state.getBlock() instanceof BlockFalling) {
            below = below.down();
            state = placedBlocks.get(below);
        }
        if (state != null) return !BlockFalling.canFallThrough(state);
        int floor = floorAt(generator, terrain, below.getX(), below.getY() + 1, below.getZ());
        return below.getY() < floor;
    }

    private static boolean survives(BlockPos pos, float integrity) {
        return new Random(positionSeed(pos)).nextFloat() <= integrity;
    }

    private static long positionSeed(BlockPos pos) {
        return MathHelper.getCoordinateRandom(pos.getX(), pos.getY(), pos.getZ());
    }

    private int getFloorHeight(WorldServer world) {
        // Sample the base terrain without loading adjacent chunks or reading already placed ruin blocks.
        ChunkGeneratorOverworld generator = (ChunkGeneratorOverworld) world.getChunkProvider().chunkGenerator;
        Map<Long, ChunkPrimer> chunks = new HashMap<>();
        int seaLevel = world.getSeaLevel();
        int anchor = floorAt(generator, chunks, origin.getX(), seaLevel, origin.getZ());
        int lowest = anchor;
        int deepColumns = 0;
        for (int x = boundingBox.minX; x <= boundingBox.maxX; x++) {
            for (int z = boundingBox.minZ; z <= boundingBox.maxZ; z++) {
                int height = floorAt(generator, chunks, x, anchor, z);
                lowest = Math.min(lowest, height);
                if (height < anchor - 2) deepColumns++;
            }
        }
        return lowest < anchor - 2 && deepColumns > boundingBox.getXSize() - 3 ? lowest : anchor;
    }

    private static int floorAt(ChunkGeneratorOverworld generator, Map<Long, ChunkPrimer> chunks, int x, int ceiling, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        ChunkPrimer chunk = chunks.computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> {
            ChunkPrimer terrain = new ChunkPrimer();
            generator.setBlocksInChunk(chunkX, chunkZ, terrain);
            return terrain;
        });
        for (int y = ceiling - 1; y > 0; y--) {
            IBlockState state = chunk.getBlockState(x & 15, y, z & 15);
            if (state.getMaterial().blocksMovement()) return y + 1;
        }
        return 1;
    }
}
