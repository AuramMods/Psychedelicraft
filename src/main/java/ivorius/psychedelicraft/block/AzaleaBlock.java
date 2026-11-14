package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLog;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.TerrainGen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AzaleaBlock extends BlockBush implements IGrowable {
    private static final AxisAlignedBB CANOPY = new AxisAlignedBB(0, .5, 0, 1, 1, 1);
    private static final AxisAlignedBB STEM = new AxisAlignedBB(.375, 0, .375, .625, .5, .625);

    AzaleaBlock() {
        setSoundType(SoundType.PLANT);
        setTickRandomly(false);
    }

    @Override
    protected boolean canSustainBush(IBlockState soil) {
        Block block = soil.getBlock();
        return block == PSBlocks.MOSS_BLOCK || block == PSBlocks.ROOTED_DIRT || block == PSBlocks.MUD
                || block == Blocks.CLAY || block == Blocks.MYCELIUM || super.canSustainBush(soil);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && canBlockStay(world, pos, getDefaultState());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return canSustainBush(world.getBlockState(pos.down()));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB box,
                                      List<AxisAlignedBB> collisions, @Nullable Entity entity, boolean actual) {
        addCollisionBoxToList(pos, box, collisions, CANOPY);
        addCollisionBoxToList(pos, box, collisions, STEM);
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean client) {
        return !world.getBlockState(pos.up()).getMaterial().isLiquid();
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return random.nextFloat() < .45F;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        int height = 4 + random.nextInt(3);
        if (pos.getY() + height + 2 >= world.getHeight() || !world.isAreaLoaded(pos, 5)
                || !TerrainGen.saplingGrowTree(world, random, pos)) return;
        EnumFacing bend = EnumFacing.Plane.HORIZONTAL.random(random);
        int bendLength = 1 + random.nextInt(2);
        List<BlockPos> trunk = new ArrayList<>();
        for (int y = 0; y < height; y++) trunk.add(pos.up(y));
        BlockPos top = pos.up(height - 1);
        for (int i = 0; i < bendLength; i++) {
            top = top.offset(bend);
            trunk.add(top);
        }
        for (int i = 1; i < trunk.size(); i++) {
            if (!canReplace(world, trunk.get(i))) return;
        }

        IBlockState log = Blocks.LOG.getDefaultState();
        for (int i = 0; i < trunk.size(); i++) {
            world.setBlockState(trunk.get(i), i < height ? log
                    : log.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.fromFacingAxis(bend.getAxis())), 3);
        }
        world.setBlockState(pos.down(), PSBlocks.ROOTED_DIRT.getDefaultState(), 3);
        BlockPos crown = top.up();
        for (int i = 0; i < 50; i++) {
            BlockPos leaf = crown.add(random.nextInt(4) - random.nextInt(4),
                    random.nextInt(2) - random.nextInt(2), random.nextInt(4) - random.nextInt(4));
            if (world.isAirBlock(leaf)) {
                Block leaves = random.nextInt(4) == 0 ? PSBlocks.FLOWERING_AZALEA_LEAVES : PSBlocks.AZALEA_LEAVES;
                world.setBlockState(leaf, leaves.getDefaultState(), 2);
            }
        }
    }

    private static boolean canReplace(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isAir(state, world, pos) || state.getBlock().isLeaves(state, world, pos);
    }
}
