package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.command.CommandLocate;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class WarmOceanRuin extends MapGenStructure {
    private static final String NAME = "Ocean_Ruin_Warm";
    private static final int SPACING = 20;
    private static final int OFFSET_RANGE = SPACING - 8;
    private static final int SALT = 14357621;
    private static final Map<World, WarmOceanRuin> GENERATORS = new IdentityHashMap<>();

    private WarmOceanRuin() {
        range = 3;
    }

    public static void register() {
        MapGenStructureIO.registerStructure(Start.class, Psychedelicraft.id("ocean_ruin_warm").toString());
        MapGenStructureIO.registerStructureComponent(WarmOceanRuinPiece.class, Psychedelicraft.id("ocean_ruin_piece").toString());
        LootTableList.register(WarmOceanRuinPiece.ARCHAEOLOGY);
        LootTableList.register(WarmOceanRuinPiece.SMALL_CHEST);
        LootTableList.register(WarmOceanRuinPiece.LARGE_CHEST);
    }

    @SubscribeEvent
    public static void populate(PopulateChunkEvent.Pre event) {
        World world = event.getWorld();
        if (!allowsRuins(world)) return;
        WarmOceanRuin generator = GENERATORS.computeIfAbsent(world, key -> new WarmOceanRuin());
        int x = event.getChunkX();
        int z = event.getChunkZ();
        generator.generate(world, x, z, null);
        generator.generateStructure(world, new Random(world.getSeed() ^ ChunkPos.asLong(x, z) ^ SALT), new ChunkPos(x, z));
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) GENERATORS.remove(event.getWorld());
    }

    @SubscribeEvent
    public static void locate(CommandEvent event) {
        if (!(event.getCommand() instanceof CommandLocate) || event.getParameters().length != 1) return;
        String name = event.getParameters()[0];
        if (!NAME.equalsIgnoreCase(name) && !"psychedelicraft:ocean_ruin_warm".equals(name)) return;
        ICommandSender sender = event.getSender();
        World world = sender.getEntityWorld();
        if (world.isRemote) return;
        event.setCanceled(true);
        BlockPos nearest = findWarmRuin(world, sender.getPosition(), false);
        if (nearest == null) {
            TextComponentTranslation failure = new TextComponentTranslation("commands.locate.failure", name);
            failure.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(failure);
        } else {
            sender.sendMessage(new TextComponentTranslation("commands.locate.success", name, nearest.getX(), nearest.getZ()));
        }
    }

    private static boolean allowsRuins(World world) {
        return world instanceof WorldServer && world.provider.getDimensionType() == DimensionType.OVERWORLD
                && world.getWorldInfo().isMapFeaturesEnabled()
                && ((WorldServer) world).getChunkProvider().chunkGenerator instanceof ChunkGeneratorOverworld;
    }

    @Override
    public String getStructureName() {
        return NAME;
    }

    static ChunkPos candidate(long worldSeed, int regionX, int regionZ) {
        Random random = new Random(worldSeed + regionX * 341873128712L + regionZ * 132897987541L + SALT);
        return new ChunkPos(regionX * SPACING + random.nextInt(OFFSET_RANGE), regionZ * SPACING + random.nextInt(OFFSET_RANGE));
    }

    static boolean isWarmOcean(Biome biome) {
        return biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) && !biome.isSnowyBiome();
    }

    private static BlockPos center(int chunkX, int chunkZ) {
        return new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8);
    }

    private static boolean isOceanAt(World world, BlockPos pos) {
        return isWarmOcean(world.getBiomeProvider().getBiome(pos));
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        ChunkPos pos = candidate(world.getSeed(), Math.floorDiv(chunkX, SPACING), Math.floorDiv(chunkZ, SPACING));
        return pos.x == chunkX && pos.z == chunkZ && isOceanAt(world, center(chunkX, chunkZ));
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(chunkX, chunkZ, rand);
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, BlockPos origin, boolean unexplored) {
        return findWarmRuin(world, origin, unexplored);
    }

    @Nullable
    private static BlockPos findWarmRuin(World world, BlockPos origin, boolean unexplored) {
        if (!allowsRuins(world)) return null;
        int regionX = Math.floorDiv(origin.getX() >> 4, SPACING);
        int regionZ = Math.floorDiv(origin.getZ() >> 4, SPACING);
        BlockPos nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (int ring = 0; ring <= 32; ring++) {
            for (int x = -ring; x <= ring; x++) {
                for (int z = -ring; z <= ring; z++) {
                    if (Math.abs(x) != ring && Math.abs(z) != ring) continue;
                    ChunkPos chunk = candidate(world.getSeed(), regionX + x, regionZ + z);
                    BlockPos pos = center(chunk.x, chunk.z);
                    double distance = pos.distanceSq(origin.getX(), 0, origin.getZ());
                    if (distance < best && isOceanAt(world, pos) && (!unexplored || !world.isChunkGeneratedAt(chunk.x, chunk.z))) {
                        nearest = pos.up(world.getSeaLevel());
                        best = distance;
                    }
                }
            }
            if (nearest != null && best <= Math.pow((double) ring * SPACING * 16, 2)) break;
        }
        return nearest;
    }

    public static final class Start extends StructureStart {
        private final Set<ChunkPos> populated = new HashSet<>();

        public Start() {
        }

        Start(int chunkX, int chunkZ, Random random) {
            super(chunkX, chunkZ);
            BlockPos origin = new BlockPos(chunkX << 4, 0, chunkZ << 4);
            boolean large = random.nextFloat() < .3F;
            Rotation rotation = randomRotation(random);
            components.add(new WarmOceanRuinPiece(origin, rotation, large, random));
            if (large && random.nextFloat() < .9F) addCluster(origin, rotation, random);
            updateBoundingBox();
        }

        @Override
        public boolean isValidForPostProcess(ChunkPos pos) {
            return !populated.contains(pos);
        }

        @Override
        public void notifyPostProcessAt(ChunkPos pos) {
            populated.add(pos);
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            NBTTagList chunks = new NBTTagList();
            for (ChunkPos pos : populated) {
                NBTTagCompound chunk = new NBTTagCompound();
                chunk.setInteger("X", pos.x);
                chunk.setInteger("Z", pos.z);
                chunks.appendTag(chunk);
            }
            tag.setTag("Populated", chunks);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            populated.clear();
            NBTTagList chunks = tag.getTagList("Populated", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < chunks.tagCount(); i++) {
                NBTTagCompound chunk = chunks.getCompoundTagAt(i);
                populated.add(new ChunkPos(chunk.getInteger("X"), chunk.getInteger("Z")));
            }
        }

        private void addCluster(BlockPos origin, Rotation rotation, Random random) {
            StructureBoundingBox main = WarmOceanRuinPiece.bounds(origin, rotation, true);
            BlockPos corner = new BlockPos(main.minX, 0, main.minZ);
            List<BlockPos> candidates = new ArrayList<>();
            candidates.add(corner.add(-16 + between(random, 1, 8), 0, 16 + between(random, 1, 7)));
            candidates.add(corner.add(-16 + between(random, 1, 8), 0, between(random, 1, 7)));
            candidates.add(corner.add(-16 + between(random, 1, 8), 0, -16 + between(random, 4, 8)));
            candidates.add(corner.add(between(random, 1, 7), 0, 16 + between(random, 1, 7)));
            candidates.add(corner.add(between(random, 1, 7), 0, -16 + between(random, 4, 6)));
            candidates.add(corner.add(16 + between(random, 1, 7), 0, 16 + between(random, 3, 8)));
            candidates.add(corner.add(16 + between(random, 1, 7), 0, between(random, 1, 7)));
            candidates.add(corner.add(16 + between(random, 1, 7), 0, -16 + between(random, 4, 8)));
            int count = between(random, 4, 8);
            for (int i = 0; i < count; i++) {
                BlockPos pos = candidates.remove(random.nextInt(candidates.size()));
                Rotation facing = randomRotation(random);
                StructureBoundingBox bounds = WarmOceanRuinPiece.bounds(pos, facing, false);
                if (StructureComponent.findIntersecting(components, bounds) == null) {
                    components.add(new WarmOceanRuinPiece(pos, facing, false, random));
                }
            }
        }

        private static Rotation randomRotation(Random random) {
            return Rotation.values()[random.nextInt(4)];
        }

        private static int between(Random random, int min, int max) {
            return min + random.nextInt(max - min + 1);
        }
    }
}
