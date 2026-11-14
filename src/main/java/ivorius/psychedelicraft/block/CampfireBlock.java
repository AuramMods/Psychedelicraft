package ivorius.psychedelicraft.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CampfireBlock extends Block {
    public static final PropertyBool LIT = PropertyBool.create("lit");
    private static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0, 0, 0, 1, 7D / 16D, 1);
    private static final AxisAlignedBB SMOKE_COLUMN = new AxisAlignedBB(6D / 16D, 0, 6D / 16D, 10D / 16D, 1, 10D / 16D);

    CampfireBlock() {
        super(Material.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(LIT, true));
        setHardness(2F);
        setSoundType(SoundType.WOOD);
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LIT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3)).withProperty(LIT, (meta & 4) == 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 0 : 4);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z,
                                             int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withProperty(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.COAL;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 1;
    }

    @Override
    public int quantityDropped(Random random) {
        return 2;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LIT) ? 15 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        ItemStack held = player.getHeldItem(hand);
        boolean lit = state.getValue(LIT);
        if (!lit && held.getItem() == Items.FLINT_AND_STEEL) {
            if (!world.isRemote && world.setBlockState(pos, state.withProperty(LIT, true), 3)) {
                world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1F, 1F);
                if (!player.isCreative()) held.damageItem(1, player);
            }
            return true;
        }
        if (lit && side == EnumFacing.UP && held.getItem() instanceof ItemSpade) {
            if (!world.isRemote && world.setBlockState(pos, state.withProperty(LIT, false), 3)) {
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1F, 1F);
                if (!player.isCreative()) held.damageItem(1, player);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        if (!world.isRemote && world.getBlockState(pos).getValue(LIT)) entity.attackEntityFrom(DamageSource.IN_FIRE, 1F);
        super.onEntityWalk(world, pos, entity);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(3) == 0) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.55, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.06, 0);
        }
        if (random.nextInt(10) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT,
                    SoundCategory.BLOCKS, 0.5F, 1F, false);
        }
    }

    public static boolean hasSmokeAt(World world, BlockPos pos) {
        List<AxisAlignedBB> collisions = new ArrayList<>(1);
        for (int depth = 1; depth <= 5; depth++) {
            BlockPos below = pos.down(depth);
            IBlockState state = world.getBlockState(below);
            if (isLit(state)) return true;
            state.addCollisionBoxToList(world, below, SMOKE_COLUMN.offset(below), collisions, null, false);
            if (!collisions.isEmpty()) return isLit(world.getBlockState(below.down()));
        }
        return false;
    }

    private static boolean isLit(IBlockState state) {
        return state.getBlock() == PSBlocks.CAMPFIRE && state.getValue(LIT);
    }
}
