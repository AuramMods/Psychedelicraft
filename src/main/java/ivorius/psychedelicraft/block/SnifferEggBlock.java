package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.entity.SnifferEntity;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Random;

public final class SnifferEggBlock extends Block {
    public static final PropertyInteger HATCH = PropertyInteger.create("hatch", 0, 2);
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(1D / 16, 0, 2D / 16, 15D / 16, 1, 14D / 16);

    SnifferEggBlock() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(HATCH, 0));
        setHardness(.5F);
        setResistance(.5F);
        setSoundType(SoundType.STONE);
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HATCH);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(HATCH, MathHelper.clamp(meta, 0, 2));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HATCH);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (world instanceof WorldServer) scheduleCrack((WorldServer) world, pos);
    }

    static int stageInterval(boolean moss, Random random) {
        return (moss ? 4000 : 8000) + random.nextInt(300);
    }

    private void scheduleCrack(WorldServer world, BlockPos pos) {
        boolean moss = world.getBlockState(pos.down()).getBlock() == PSBlocks.MOSS_BLOCK;
        if (moss) world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, pos.getX() + .5, pos.getY() + .5,
                pos.getZ() + .5, 12, .45, .45, .45, 0);
        world.scheduleUpdate(pos, this, stageInterval(moss, world.rand));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!(world instanceof WorldServer)) return;
        int stage = state.getValue(HATCH);
        float pitch = .9F + random.nextFloat() * .2F;
        if (stage < 2) {
            if (world.setBlockState(pos, state.withProperty(HATCH, stage + 1), 2)) {
                world.playSound(null, pos, PSSounds.SNIFFER_EGG_CRACK, SoundCategory.BLOCKS, .7F, pitch);
                scheduleCrack((WorldServer) world, pos);
            }
            return;
        }

        SnifferEntity baby = new SnifferEntity(world);
        baby.setGrowingAge(-SnifferEntity.BABY_TICKS);
        baby.setLocationAndAngles(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                MathHelper.wrapDegrees(random.nextFloat() * 360F), 0F);
        if (world.destroyBlock(pos, false)) {
            world.playSound(null, pos, PSSounds.SNIFFER_EGG_HATCH, SoundCategory.BLOCKS, .7F, pitch);
            world.spawnEntity(baby);
        }
    }
}
