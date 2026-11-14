package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.BrushableBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

public final class SuspiciousSandBlock extends BlockFalling {
    public static final PropertyInteger DUSTED = PropertyInteger.create("dusted", 0, 3);

    SuspiciousSandBlock() {
        super(Material.SAND);
        setDefaultState(blockState.getBaseState().withProperty(DUSTED, 0));
        setHardness(0.25F);
        setResistance(0.25F);
        setSoundType(SoundType.SAND);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DUSTED);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DUSTED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(DUSTED, MathHelper.clamp(meta, 0, 3));
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new BrushableBlockEntity();
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote) return;
        if (canFallThrough(world.getBlockState(pos.down()))) {
            if (fallInstantly || !world.isAreaLoaded(pos.add(-32, -32, -32), pos.add(32, 32, 32))) {
                onBroken(world, pos);
                world.setBlockToAir(pos);
            } else {
                super.updateTick(world, pos, state, random);
            }
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof BrushableBlockEntity) ((BrushableBlockEntity) tile).checkReset();
    }

    @Override
    protected void onStartFalling(EntityFallingBlock entity) {
        entity.shouldDropItem = false;
    }

    @Override
    public void onEndFalling(World world, BlockPos pos, IBlockState fallen, IBlockState replaced) {
        world.playEvent(2001, pos, Block.getStateId(fallen));
        world.setBlockToAir(pos);
    }

    @Override
    public void onBroken(World world, BlockPos pos) {
        world.playEvent(2001, pos, Block.getStateId(getDefaultState()));
    }

    @Override
    public int getDustColor(IBlockState state) {
        return Blocks.SAND.getMaterial(state).getMaterialMapColor().colorValue;
    }
}
