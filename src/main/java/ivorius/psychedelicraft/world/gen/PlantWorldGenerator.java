/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 16 Jan 2023.
 */

package ivorius.psychedelicraft.world.gen;

import java.util.Random;
import java.util.function.Predicate;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.TallCropBlock;
import ivorius.psychedelicraft.block.VineStemBlock;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class PlantWorldGenerator {
    private static final int TALL_RARITY = 160;
    private static final int LOW_RARITY = 20;
    private static final int LOW_ATTEMPTS = 5;
    private static final int LOW_XZ_SPREAD = 7;
    private static final int LOW_Y_SPREAD = 3;
    private static final long JUNIPER_SALT = 0x9e3779b97f4a7c15L * "juniper".hashCode();
    private static final long BEE_NEST_SALT = 0x9e3779b97f4a7c15L * "bee_nest".hashCode();
    private static final PSConfig.Generation OPTIONS = PSConfig.balancing.worldGeneration;
    private static final Patch[] PATCHES = {
            tall("cannabis", PSBlocks.NATURAL_CANNABIS, OPTIONS.cannabis, false),
            tall("hop", PSBlocks.NATURAL_HOP, OPTIONS.hop, false),
            tall("tobacco", PSBlocks.NATURAL_TOBACCO, OPTIONS.tobacco, false),
            tall("coffea", PSBlocks.NATURAL_COFFEA, OPTIONS.coffea, false),
            tall("coca", PSBlocks.NATURAL_COCA, OPTIONS.coca, true),
            low("morning_glory", PSBlocks.MORNING_GLORY, VineStemBlock.MAX_AGE, OPTIONS.morningGlories, PlantBiomes::morningGlory),
            low("belladonna", PSBlocks.BELLADONNA, PSBlocks.BELLADONNA.maxAge(), OPTIONS.belladonna, PlantBiomes::belladonna),
            low("jimsonweed", PSBlocks.JIMSONWEED, PSBlocks.JIMSONWEED.maxAge(), OPTIONS.jimsonweed, PlantBiomes::jungle),
            low("tomato", PSBlocks.TOMATOES, PSBlocks.TOMATOES.maxAge(), OPTIONS.tomato, PlantBiomes::tomato),
            low("peyote", PSBlocks.PEYOTE, PSBlocks.PEYOTE.maxAge(), OPTIONS.peyote, PlantBiomes::peyote),
            low("agave", PSBlocks.AGAVE_PLANT, PSBlocks.AGAVE_PLANT.maxAge(), OPTIONS.peyote, PlantBiomes::agave)
    };

    private PlantWorldGenerator() {
    }

    @SubscribeEvent
    public static void decorate(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimensionType() != DimensionType.OVERWORLD) {
            return;
        }
        ChunkPos chunk = event.getChunkPos();
        // Forge decorates inside the populated chunk and its eastern and southern neighbours.
        if (!world.isAreaLoaded(chunk.getBlock(0, 0, 0), chunk.getBlock(31, 0, 31))) {
            return;
        }
        long seed = world.getSeed() ^ (341873128712L * chunk.x + 132897987541L * chunk.z);
        Random random = new Random();
        if (OPTIONS.beeNests.enabled) {
            random.setSeed(seed ^ BEE_NEST_SALT);
            BeeNestWorldGenerator.decorate(world, chunk, random, OPTIONS.beeNests);
        }
        if (OPTIONS.juniper.enabled) {
            random.setSeed(seed ^ JUNIPER_SALT);
            int attempts = random.nextFloat() < 0.05F ? 3 : 1;
            for (int attempt = 0; attempt < attempts; attempt++) {
                BlockPos column = chunk.getBlock(8 + random.nextInt(16), 0, 8 + random.nextInt(16));
                if (PlantBiomes.allows(OPTIONS.juniper, PlantBiomes::juniper, world.getBiome(column))) {
                    BlockPos surface = juniperSurface(world, column);
                    if (surface != null) JuniperTree.generate(world, random, surface);
                }
            }
        }
        for (Patch patch : PATCHES) {
            random.setSeed(seed ^ patch.salt);
            if (!patch.options.enabled || random.nextInt(patch.rarity) != 0) {
                continue;
            }
            BlockPos column = chunk.getBlock(8 + random.nextInt(16), 0, 8 + random.nextInt(16));
            BlockPos surface = motionBlockingSurface(world, column);
            if (PlantBiomes.allows(patch.options, patch.defaults, world.getBiome(surface))) {
                patch.placement.place(world, surface, random);
            }
        }
    }

    static BlockPos motionBlockingSurface(World world, BlockPos column) {
        Chunk chunk = world.getChunkFromBlockCoords(column);
        BlockPos.MutableBlockPos probe = topOfColumn(world, chunk, column);
        while (probe.getY() >= 0) {
            IBlockState state = chunk.getBlockState(probe);
            if (state.getMaterial().blocksMovement() || state.getMaterial().isLiquid()
                    || state.getBlock() instanceof IFluidBlock || Waterlogging.isWet(world, probe, state)) {
                return probe.up();
            }
            probe.move(EnumFacing.DOWN);
        }
        return new BlockPos(column.getX(), 0, column.getZ());
    }

    @Nullable
    static BlockPos juniperSurface(World world, BlockPos column) {
        Chunk chunk = world.getChunkFromBlockCoords(column);
        BlockPos.MutableBlockPos probe = topOfColumn(world, chunk, column);
        while (probe.getY() >= 0) {
            IBlockState state = chunk.getBlockState(probe);
            if (state.getMaterial() != Material.AIR) {
                // Zero surface depth requires the first non-air block to also be the ocean floor.
                return state.getMaterial().blocksMovement() ? probe.up() : null;
            }
            probe.move(EnumFacing.DOWN);
        }
        return null;
    }

    private static BlockPos.MutableBlockPos topOfColumn(World world, Chunk chunk, BlockPos column) {
        int top = Math.min(world.getActualHeight() - 1, chunk.getTopFilledSegment() + 15);
        return new BlockPos.MutableBlockPos(column.getX(), top, column.getZ());
    }

    static void placeLowPatch(World world, BlockPos surface, Random random, BlockBush plant, int maxAge) {
        for (int attempt = 0; attempt < LOW_ATTEMPTS; attempt++) {
            BlockPos pos = surface.add(offset(random, LOW_XZ_SPREAD), offset(random, LOW_Y_SPREAD),
                    offset(random, LOW_XZ_SPREAD));
            if (!insideWorld(world, pos) || world.getBlockState(pos).getBlock() != Blocks.AIR) {
                continue;
            }
            // The simple-block feature samples a state after the air filter and before testing survival.
            IBlockState state = plant.getStateFromMeta(random.nextInt(maxAge + 1));
            if (plant.canBlockStay(world, pos, state)) {
                world.setBlockState(pos, state, 2);
            }
        }
    }

    private static int offset(Random random, int radius) {
        return random.nextInt(radius + 1) - random.nextInt(radius + 1);
    }

    static void placeTallPatch(World world, BlockPos surface, Random random, TallCropBlock crop, boolean waterRequired) {
        BlockPos origin = findTerrainLevel(world, surface);
        if (!insideWorld(world, origin) || !TallCropBlock.isWildSoil(world.getBlockState(origin.down()))
                || (waterRequired && !waterNearby(world, origin.down()))) {
            return;
        }
        int radius = 1 + random.nextInt(3);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                BlockPos base = findTerrainLevel(world, origin.add(dx, 0, dz));
                if (insideWorld(world, base) && TallCropBlock.isWildSoil(world.getBlockState(base.down()))
                        && random.nextInt(3) == 0) {
                    placeStalk(world, base, crop, random);
                }
            }
        }
    }

    private static void placeStalk(World world, BlockPos base, TallCropBlock crop, Random random) {
        int height = Math.min(2 + random.nextInt(random.nextInt(3) + 1), crop.maxHeight());
        BlockPos cursor = base;
        for (int level = 0; level < height; level++) {
            cursor = base.up(level);
            IBlockState section = crop.stateForHeight(level);
            int maxAge = crop.maxAge(section);
            int age = level == height - 1 ? random.nextInt(maxAge + 1) : maxAge;
            IBlockState state = crop.stateForHeight(level, age);
            // The donor checks the terrain at the base, then uses the sampled state's support rule for each section.
            if (!insideWorld(world, cursor) || !crop.canBlockStay(world, cursor, state)) {
                break;
            }
            world.setBlockState(cursor, state, 2);
        }
        // Match the donor's cursor-relative substrate update, using coarse dirt on 1.12.
        BlockPos roots = cursor.down();
        IBlockState under = world.getBlockState(roots);
        if (TallCropBlock.isWildSoil(under)
                || (under.getBlock() == Blocks.STONE && under.getValue(BlockStone.VARIANT).isNatural())) {
            world.setBlockState(roots, Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT), 2);
        }
    }

    private static boolean waterNearby(World world, BlockPos soil) {
        for (BlockPos pos : BlockPos.getAllInBox(soil.add(-1, -1, -1), soil.add(1, 1, 1))) {
            if (Waterlogging.isWater(world, pos)) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos findTerrainLevel(World world, BlockPos start) {
        BlockPos pos = start;
        if (replaceableTerrain(world, pos)) {
            while (pos.getY() > 0 && replaceableTerrain(world, pos.down())) {
                pos = pos.down();
            }
        } else {
            while (pos.getY() < world.getActualHeight() && !replaceableTerrain(world, pos)) {
                pos = pos.up();
            }
        }
        return pos;
    }

    private static boolean insideWorld(World world, BlockPos pos) {
        return pos.getY() > 0 && pos.getY() < world.getActualHeight();
    }

    private static boolean replaceableTerrain(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block.isAir(state, world, pos) || block == Blocks.WATER || block == Blocks.FLOWING_WATER
                || block.isLeaves(state, world, pos) || block instanceof BlockDoublePlant
                || block == Blocks.TALLGRASS || block == Blocks.DEADBUSH || block == Blocks.VINE;
    }

    private static Patch tall(String name, TallCropBlock crop, PSConfig.Feature options, boolean waterRequired) {
        return new Patch(name, TALL_RARITY, options, PlantBiomes::tallCrops,
                (world, origin, random) -> placeTallPatch(world, origin, random, crop, waterRequired));
    }

    private static Patch low(String name, BlockBush plant, int maxAge, PSConfig.Feature options, Predicate<Biome> defaults) {
        return new Patch(name, LOW_RARITY, options, defaults,
                (world, origin, random) -> placeLowPatch(world, origin, random, plant, maxAge));
    }

    private interface Placement {
        void place(World world, BlockPos origin, Random random);
    }

    private static final class Patch {
        private final long salt;
        private final int rarity;
        private final PSConfig.Feature options;
        private final Predicate<Biome> defaults;
        private final Placement placement;

        private Patch(String name, int rarity, PSConfig.Feature options, Predicate<Biome> defaults, Placement placement) {
            salt = 0x9e3779b97f4a7c15L * name.hashCode();
            this.rarity = rarity;
            this.options = options;
            this.defaults = defaults;
            this.placement = placement;
        }
    }
}
