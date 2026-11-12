package ivorius.psychedelicraft.item;

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nullable;

import ivorius.psychedelicraft.block.JuniperHangingSigns;
import ivorius.psychedelicraft.block.JuniperSignBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.JuniperSignBlockEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

final class JuniperSignItem extends Item {
    private final boolean hanging;

    JuniperSignItem(boolean hanging) {
        this.hanging = hanging;
        setMaxStackSize(16);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos clicked, EnumHand hand,
                                     EnumFacing facing, float x, float y, float z) {
        if (!hanging && facing == EnumFacing.DOWN) {
            return EnumActionResult.FAIL;
        }
        boolean replaceable = world.getBlockState(clicked).getBlock().isReplaceable(world, clicked);
        BlockPos pos = replaceable ? clicked : clicked.offset(facing);
        ItemStack held = player.getHeldItem(hand);
        boolean attached = hanging && JuniperHangingSigns.usesAttachedChains(world, pos, player);
        IBlockState state = hanging ? hangingState(world, pos, player, facing, replaceable, attached)
                : regularState(player, facing);
        if (state == null) {
            return EnumActionResult.FAIL;
        }
        JuniperSignBlock block = (JuniperSignBlock) state.getBlock();
        if (!player.canPlayerEdit(pos, facing, held) || !world.isBlockModifiable(player, pos)
                || !world.mayPlace(block, pos, true, facing, player) || !block.canStay(world, pos, state)) {
            return EnumActionResult.FAIL;
        }
        AxisAlignedBB collision = state.getCollisionBoundingBox(world, pos);
        if (collision != null && !world.checkNoEntityCollision(collision.offset(pos))) {
            return EnumActionResult.FAIL;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!world.setBlockState(pos, state, 11)) {
            return EnumActionResult.FAIL;
        }
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            JuniperSignBlockEntity sign = JuniperSignBlockEntity.get(world, pos);
            if (sign != null) {
                if (block.form() == JuniperSignBlock.Form.HANGING_CEILING) {
                    sign.setAttachedChains(attached);
                }
                if (!ItemBlock.setTileEntityNBT(world, player, pos, held)) {
                    sign.queuePlacementEditor(serverPlayer);
                }
            }
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, held);
        }
        SoundType sound = block.getSoundType(state, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1F) * 0.5F, sound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    private static IBlockState regularState(EntityPlayer player, EnumFacing facing) {
        if (facing == EnumFacing.UP) {
            int rotation = MathHelper.floor((player.rotationYaw + 180F) * 16F / 360F + 0.5F) & 15;
            return PSBlocks.JUNIPER_SIGN.getDefaultState().withProperty(JuniperSignBlock.ROTATION, rotation);
        }
        return PSBlocks.JUNIPER_WALL_SIGN.getDefaultState().withProperty(JuniperSignBlock.FACING, facing);
    }

    @Nullable
    private static IBlockState hangingState(World world, BlockPos pos, EntityPlayer player,
                                            EnumFacing clickedFace, boolean replaceable, boolean attached) {
        EnumFacing[] directions = EnumFacing.values();
        Vec3d look = player.getLookVec();
        Arrays.sort(directions, Comparator.comparingDouble(direction ->
                !replaceable && direction == clickedFace.getOpposite() ? -2
                        : -look.dotProduct(new Vec3d(direction.getDirectionVec()))));
        IBlockState wall = null;
        for (EnumFacing direction : directions) {
            if (direction.getAxis().isHorizontal() && direction.getAxis() != clickedFace.getAxis()) {
                IBlockState state = PSBlocks.JUNIPER_WALL_HANGING_SIGN.getDefaultState()
                        .withProperty(JuniperSignBlock.FACING, direction.getOpposite());
                if (PSBlocks.JUNIPER_WALL_HANGING_SIGN.canStay(world, pos, state)) {
                    wall = state;
                    break;
                }
            }
        }
        IBlockState ceiling = PSBlocks.JUNIPER_HANGING_SIGN.getDefaultState()
                .withProperty(JuniperSignBlock.ROTATION, JuniperHangingSigns.placementRotation(player, attached));
        for (EnumFacing direction : directions) {
            if (direction == EnumFacing.UP) {
                if (PSBlocks.JUNIPER_HANGING_SIGN.canStay(world, pos, ceiling)) {
                    return ceiling;
                }
            } else if (direction.getAxis().isHorizontal() && wall != null) {
                return wall;
            }
        }
        return null;
    }
}
