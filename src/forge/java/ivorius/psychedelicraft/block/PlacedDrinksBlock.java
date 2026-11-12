/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class PlacedDrinksBlock extends Block {
    private static final AxisAlignedBB SELECTION = new AxisAlignedBB(0, 0, 0, 1, 1 / 16D, 1);

    public PlacedDrinksBlock() {
        super(Material.CIRCUITS);
        setHardness(0);
        setSoundType(SoundType.GLASS);
    }

    public static boolean canPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == PSItems.WOODEN_MUG || item == PSItems.STONE_CUP || item == PSItems.GLASS_CHALICE
                || item == PSItems.SHOT_GLASS || item == PSItems.BOTTLE;
    }

    public static EnumActionResult placeOnBlock(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                                 EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!canPlace(held)) return EnumActionResult.PASS;
        IBlockState target = world.getBlockState(pos);
        if (target.getBlock() == PSBlocks.PLACED_DRINK) {
            return PSBlocks.PLACED_DRINK.onBlockActivated(world, pos, target, player, hand, side, hitX, hitY, hitZ)
                    ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }
        boolean replaceable = target.getBlock().isReplaceable(world, pos);
        if (side != EnumFacing.UP && !replaceable) return EnumActionResult.PASS;
        BlockPos placement = replaceable ? pos : pos.offset(side);
        PlacedDrinksBlock block = PSBlocks.PLACED_DRINK;
        if (!player.canPlayerEdit(placement, side, held) || !world.isBlockModifiable(player, pos)
                || !world.isBlockModifiable(player, placement)) return EnumActionResult.FAIL;
        if (!world.mayPlace(block, placement, false, side, player)) return EnumActionResult.PASS;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        if (!world.setBlockState(placement, block.getDefaultState(), 3)) return EnumActionResult.FAIL;
        float localX = hitX + pos.getX() - placement.getX();
        float localZ = hitZ + pos.getZ() - placement.getZ();
        if (!block.place(world, placement, player, held, PlacedDrinkBlockEntity.cell(localX, localZ), player.rotationYaw)) {
            world.setBlockToAir(placement);
            return EnumActionResult.FAIL;
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        if (side.getAxis() != EnumFacing.Axis.Y || !world.isBlockModifiable(player, pos)) return false;
        PlacedDrinkBlockEntity display = display(world, pos);
        if (display == null) return false;
        ItemStack held = player.getHeldItem(hand);
        int cell = PlacedDrinkBlockEntity.cell(hitX, hitZ);
        if (held.isEmpty()) {
            if (world.isRemote) return !display.peek(cell).isEmpty();
            ItemStack retrieved = display.take(cell);
            if (retrieved.isEmpty()) return false;
            if (!player.inventory.addItemStackToInventory(retrieved)) player.dropItem(retrieved, false);
            playSound(world, pos);
            return true;
        }
        if (!canPlace(held) || !player.canPlayerEdit(pos, side, held)) return false;
        return world.isRemote ? display.canPlace(cell) : place(world, pos, player, held, cell, player.rotationYawHead);
    }

    private boolean place(World world, BlockPos pos, EntityPlayer player, ItemStack held, int cell, float yaw) {
        PlacedDrinkBlockEntity display = display(world, pos);
        if (display == null || !display.place(cell, held, yaw)) return false;
        if (!player.capabilities.isCreativeMode) held.shrink(1);
        playSound(world, pos);
        return true;
    }

    private static void playSound(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 0.65F, 1.2F);
    }

    private static PlacedDrinkBlockEntity display(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof PlacedDrinkBlockEntity ? (PlacedDrinkBlockEntity) tile : null;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        PlacedDrinkBlockEntity display = display(world, pos);
        if (!world.isRemote && display != null) display.dropContents();
        super.breakBlock(world, pos, state);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && entity instanceof EntityLivingBase
                && (entity.posY > pos.getY() || entity.posY >= pos.getY() && !entity.isSneaking())
                && Math.max(Math.abs(entity.posX - entity.prevPosX), Math.abs(entity.posZ - entity.prevPosZ)) >= 0.003) {
            world.setBlockToAir(pos);
            playSound(world, pos);
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        PlacedDrinkBlockEntity display = display(world, pos);
        if (display == null || target.sideHit.getAxis() != EnumFacing.Axis.Y) return ItemStack.EMPTY;
        return display.peek(PlacedDrinkBlockEntity.cell((float) (target.hitVec.x - pos.getX()),
                (float) (target.hitVec.z - pos.getZ())));
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SELECTION;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
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
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new PlacedDrinkBlockEntity();
    }
}
