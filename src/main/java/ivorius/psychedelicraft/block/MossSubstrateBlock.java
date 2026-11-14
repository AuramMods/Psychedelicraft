package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public final class MossSubstrateBlock extends Block implements IGrowable {
    MossSubstrateBlock() {
        super(Material.GRASS, MapColor.GREEN);
        setHardness(0.1F);
        setSoundType(SoundType.PLANT);
        setHarvestLevel("hoe", 0);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        return world.isAirBlock(pos.up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        placePatch(world, random, pos.up(), 2 + random.nextInt(2), 2 + random.nextInt(2), .75F, .6F);
    }

    @Override
    public boolean canSustainPlant(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing direction, IPlantable plant) {
        return direction == EnumFacing.UP && plant.getPlantType(world, pos.up()) == EnumPlantType.Plains
                || super.canSustainPlant(state, world, pos, direction, plant);
    }

    public static boolean placePatch(World world, Random random, BlockPos origin, int xRadius, int zRadius,
                                     float edgeChance, float vegetationChance) {
        Set<BlockPos> moss = new LinkedHashSet<>();
        for (int x = -xRadius; x <= xRadius; x++) {
            for (int z = -zRadius; z <= zRadius; z++) {
                boolean xEdge = Math.abs(x) == xRadius;
                boolean zEdge = Math.abs(z) == zRadius;
                if (xEdge && zEdge || (xEdge || zEdge) && random.nextFloat() > edgeChance) continue;
                BlockPos air = origin.add(x, 0, z);
                if (!world.isBlockLoaded(air)) continue;
                for (int i = 0; i < 5 && world.isAirBlock(air); i++) air = air.down();
                for (int i = 0; i < 5 && !world.isAirBlock(air); i++) air = air.up();
                if (!world.isAirBlock(air)) continue;
                BlockPos ground = air.down();
                IBlockState state = world.getBlockState(ground);
                if (state.getBlockFaceShape(world, ground, EnumFacing.UP) != BlockFaceShape.SOLID) continue;
                if (state.getBlock() == PSBlocks.MOSS_BLOCK || replaceable(state.getBlock())
                        && world.setBlockState(ground, PSBlocks.MOSS_BLOCK.getDefaultState(), 2)) moss.add(ground);
            }
        }
        for (BlockPos pos : moss) {
            if (random.nextFloat() < vegetationChance) placeVegetation(world, pos.up(), random.nextInt(96));
        }
        return !moss.isEmpty();
    }

    private static boolean replaceable(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM
                || block == PSBlocks.ROOTED_DIRT || block == PSBlocks.MUD;
    }

    private static void placeVegetation(World world, BlockPos pos, int roll) {
        if (!world.isAirBlock(pos)) return;
        if (roll < 4) world.setBlockState(pos, PSBlocks.FLOWERING_AZALEA.getDefaultState(), 2);
        else if (roll < 11) world.setBlockState(pos, PSBlocks.AZALEA.getDefaultState(), 2);
        else if (roll < 36) world.setBlockState(pos, PSBlocks.MOSS_CARPET.getDefaultState(), 2);
        else if (roll < 86) world.setBlockState(pos, Blocks.TALLGRASS.getDefaultState()
                .withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), 2);
        else if (Blocks.DOUBLE_PLANT.canPlaceBlockAt(world, pos)) {
            Blocks.DOUBLE_PLANT.placeAt(world, pos, BlockDoublePlant.EnumPlantType.GRASS, 2);
        }
    }
}
