package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.CampfireBlockEntity;
import ivorius.psychedelicraft.client.particle.CampfireSmoke;
import ivorius.psychedelicraft.fluid.physical.Waterloggable;
import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLlamaSpit;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
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
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class CampfireBlock extends Block implements Waterloggable {
    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0, 0, 0, 1, 7D / 16D, 1);
    private static final AxisAlignedBB SMOKE_COLUMN = new AxisAlignedBB(6D / 16D, 0, 6D / 16D, 10D / 16D, 1, 10D / 16D);
    private static final int DOWSE_EVENT = 1;
    private final boolean soul;

    CampfireBlock(boolean soul) {
        super(Material.WOOD);
        this.soul = soul;
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
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing())
                .withProperty(LIT, !Waterlogging.isWaterSource(world.getBlockState(pos)));
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
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new CampfireBlockEntity();
    }

    @Override
    public boolean hasOwnWaterTile() {
        return true;
    }

    @Override
    public boolean canPistonMove() {
        return false;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        Waterlogging.scheduleIfWet(world, pos);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof CampfireBlockEntity) ((CampfireBlockEntity) tile).dropFood();
        super.breakBlock(world, pos, state);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return soul ? Item.getItemFromBlock(PSBlocks.SOUL_SOIL) : Items.COAL;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return soul ? 0 : 1;
    }

    @Override
    public int quantityDropped(Random random) {
        return soul ? 1 : 2;
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
        return state.getValue(LIT) ? (soul ? 10 : 15) : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float x, float y, float z) {
        ItemStack held = player.getHeldItem(hand);
        boolean lit = state.getValue(LIT);
        boolean fireCharge = held.getItem() == Items.FIRE_CHARGE;
        if (!lit && (fireCharge || held.getItem() == Items.FLINT_AND_STEEL)) {
            if (Waterlogging.isWet(world, pos, state)) return true;
            if (!world.isRemote && world.setBlockState(pos, state.withProperty(LIT, true), 3)) {
                world.playSound(null, pos, fireCharge ? SoundEvents.ITEM_FIRECHARGE_USE : SoundEvents.ITEM_FLINTANDSTEEL_USE,
                        SoundCategory.BLOCKS, 1F, 1F);
                if (!player.isCreative()) {
                    if (fireCharge) held.shrink(1);
                    else held.damageItem(1, player);
                }
            }
            return true;
        }
        if (lit && side == EnumFacing.UP && held.getItem() instanceof ItemSpade) {
            if (!world.isRemote && dowse(world, pos, state) && !player.isCreative()) held.damageItem(1, player);
            return true;
        }
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof CampfireBlockEntity && ((CampfireBlockEntity) tile).placeFood(held, player.isCreative());
    }

    public static boolean dowse(World world, BlockPos pos, IBlockState state) {
        if (!world.setBlockState(pos, state.withProperty(LIT, false), 3)) return false;
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1F, 1F);
        world.addBlockEvent(pos, state.getBlock(), DOWSE_EVENT, isSignalFire(world, pos) ? 1 : 0);
        return true;
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        if (id != DOWSE_EVENT) return super.eventReceived(state, world, pos, id, param);
        if (world.isRemote) CampfireSmoke.spawn(world, pos, param != 0, 20, true);
        return true;
    }

    public static boolean isSignalFire(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() == Blocks.HAY_BLOCK;
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && state.getValue(LIT) && entity instanceof EntityLivingBase
                && !EnchantmentHelper.hasFrostWalkerEnchantment((EntityLivingBase) entity)) {
            entity.attackEntityFrom(DamageSource.IN_FIRE, soul ? 2F : 1F);
        }
    }

    public static boolean afterProjectileImpact(Entity projectile, RayTraceResult hit, boolean canceled) {
        if (canceled || projectile.world.isRemote || hit.typeOfHit != RayTraceResult.Type.BLOCK || !projectile.isBurning()) {
            return canceled;
        }
        World world = projectile.world;
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CampfireBlock && !state.getValue(LIT) && !Waterlogging.isWet(world, pos, state)) {
            Entity owner = projectileOwner(projectile);
            boolean permitted = owner instanceof EntityPlayer ? world.isBlockModifiable((EntityPlayer) owner, pos)
                    : owner == null || ForgeEventFactory.getMobGriefingEvent(world, owner);
            if (permitted) world.setBlockState(pos, state.withProperty(LIT, true), 3);
        }
        return false;
    }

    private static Entity projectileOwner(Entity projectile) {
        if (projectile instanceof EntityArrow) return ((EntityArrow) projectile).shootingEntity;
        if (projectile instanceof EntityFireball) return ((EntityFireball) projectile).shootingEntity;
        if (projectile instanceof EntityThrowable) return ((EntityThrowable) projectile).getThrower();
        if (projectile instanceof EntityLlamaSpit) return ((EntityLlamaSpit) projectile).owner;
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(10) == 0) {
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_AMBIENT,
                    SoundCategory.BLOCKS, 0.5F + random.nextFloat(), 0.6F + 0.7F * random.nextFloat(), false);
        }
        if (!soul && random.nextInt(5) == 0) {
            world.spawnParticle(EnumParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    random.nextFloat() / 2F, 0.00005D, random.nextFloat() / 2F);
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
        return state.getBlock() instanceof CampfireBlock && state.getValue(LIT);
    }
}
