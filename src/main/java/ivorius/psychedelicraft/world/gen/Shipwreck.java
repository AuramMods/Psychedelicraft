package ivorius.psychedelicraft.world.gen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.command.CommandLocate;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Biomes;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class Shipwreck extends MapGenStructure {
    private static final String NAME = "Shipwreck";
    private static final int SPACING = 24;
    private static final int SEPARATION = 4;
    private static final int SALT = 165745295;
    private static final Map<World, Shipwreck> GENERATORS = new IdentityHashMap<>();
    private static final String[] BEACH = {
            "with_mast", "sideways_full", "sideways_fronthalf", "sideways_backhalf",
            "rightsideup_full", "rightsideup_fronthalf", "rightsideup_backhalf",
            "with_mast_degraded", "rightsideup_full_degraded", "rightsideup_fronthalf_degraded", "rightsideup_backhalf_degraded"
    };
    private static final String[] OCEAN = {
            "with_mast", "upsidedown_full", "upsidedown_fronthalf", "upsidedown_backhalf",
            "sideways_full", "sideways_fronthalf", "sideways_backhalf", "rightsideup_full", "rightsideup_fronthalf", "rightsideup_backhalf",
            "with_mast_degraded", "upsidedown_full_degraded", "upsidedown_fronthalf_degraded", "upsidedown_backhalf_degraded",
            "sideways_full_degraded", "sideways_fronthalf_degraded", "sideways_backhalf_degraded",
            "rightsideup_full_degraded", "rightsideup_fronthalf_degraded", "rightsideup_backhalf_degraded"
    };

    private Shipwreck() {
        range = 3;
    }

    public static void register() {
        MapGenStructureIO.registerStructure(Start.class, Psychedelicraft.id("shipwreck").toString());
        MapGenStructureIO.registerStructureComponent(ShipwreckPiece.class, Psychedelicraft.id("shipwreck_piece").toString());
        ShipwreckPiece.LOOT.values().forEach(LootTableList::register);
        ShipwreckStew.register();
    }

    @SubscribeEvent
    public static void populate(PopulateChunkEvent.Pre event) {
        World world = event.getWorld();
        if (!WarmOceanRuin.allowsOceanStructures(world)) return;
        Shipwreck generator = GENERATORS.computeIfAbsent(world, key -> new Shipwreck());
        int x = event.getChunkX();
        int z = event.getChunkZ();
        ChunkPos pos = new ChunkPos(x, z);
        generator.generate(world, x, z, null);
        generator.generateStructure(world, new Random(world.getSeed() ^ ChunkPos.asLong(x, z) ^ SALT), pos);
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) GENERATORS.remove(event.getWorld());
    }

    @SubscribeEvent
    public static void locate(CommandEvent event) {
        if (!(event.getCommand() instanceof CommandLocate) || event.getParameters().length != 1) return;
        String name = event.getParameters()[0];
        if (!NAME.equalsIgnoreCase(name) && !"psychedelicraft:shipwreck".equals(name)) return;
        ICommandSender sender = event.getSender();
        World world = sender.getEntityWorld();
        if (world.isRemote) return;
        // A pack can supply its own native locate target.
        if (world.findNearestStructure(name, sender.getPosition(), false) != null) return;
        event.setCanceled(true);
        BlockPos nearest = new Shipwreck().getNearestStructurePos(world, sender.getPosition(), false);
        if (nearest == null) {
            TextComponentTranslation failure = new TextComponentTranslation("commands.locate.failure", name);
            failure.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(failure);
        } else {
            sender.sendMessage(new TextComponentTranslation("commands.locate.success", name, nearest.getX(), nearest.getZ()));
        }
    }

    @Override
    public String getStructureName() {
        return NAME;
    }

    static ChunkPos candidate(long seed, int regionX, int regionZ) {
        Random random = new Random(seed + regionX * 341873128712L + regionZ * 132897987541L + SALT);
        return new ChunkPos(regionX * SPACING + random.nextInt(SPACING - SEPARATION),
                regionZ * SPACING + random.nextInt(SPACING - SEPARATION));
    }

    private static BlockPos center(int x, int z) {
        return new BlockPos((x << 4) + 8, 0, (z << 4) + 8);
    }

    static boolean isBeach(Biome biome) {
        return biome == Biomes.BEACH || biome == Biomes.COLD_BEACH || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)
                && biome != Biomes.STONE_BEACH;
    }

    private static boolean matches(Biome biome) {
        return biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN || biome == Biomes.FROZEN_OCEAN
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || isBeach(biome);
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int x, int z) {
        ChunkPos candidate = candidate(world.getSeed(), Math.floorDiv(x, SPACING), Math.floorDiv(z, SPACING));
        return x == candidate.x && z == candidate.z && matches(world.getBiomeProvider().getBiome(center(x, z)));
    }

    @Override
    protected StructureStart getStructureStart(int x, int z) {
        Rotation rotation = Rotation.values()[rand.nextInt(Rotation.values().length)];
        boolean beach = isBeach(world.getBiomeProvider().getBiome(center(x, z)));
        String[] templates = beach ? BEACH : OCEAN;
        String template = templates[rand.nextInt(templates.length)];
        return new Start(world, x, z, rotation, beach, template, rand.nextInt(8), rand.nextInt(3));
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, BlockPos origin, boolean unexplored) {
        if (!WarmOceanRuin.allowsOceanStructures(world)) return null;
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
                    if (distance < best && matches(world.getBiomeProvider().getBiome(pos))
                            && (!unexplored || !world.isChunkGeneratedAt(chunk.x, chunk.z))) {
                        nearest = pos.up(world.getSeaLevel());
                        best = distance;
                    }
                }
            }
            double radius = (double) ring * SPACING * 16;
            if (nearest != null && best <= radius * radius) break;
        }
        return nearest;
    }

    public static final class Start extends PopulatedStructureStart {
        public Start() {
        }

        private Start(World world, int x, int z, Rotation rotation, boolean beach, String template, int palette, int burial) {
            super(x, z);
            components.add(new ShipwreckPiece(world, new BlockPos(x << 4, 0, z << 4), rotation, beach, template, palette, burial));
            updateBoundingBox();
        }
    }
}
