/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.block.BlockLog;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public final class JuniperLogBlock extends BlockLog {
    private static final EnumAxis[] AXES = EnumAxis.values();
    @Nullable
    private final Supplier<JuniperLogBlock> stripped;

    JuniperLogBlock(@Nullable Supplier<JuniperLogBlock> stripped) {
        this.stripped = stripped;
        setResistance(10F / 3F);
        setDefaultState(blockState.getBaseState().withProperty(LOG_AXIS, EnumAxis.Y));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LOG_AXIS);
    }

    @Override
    public IBlockState getStateFromMeta(int metadata) {
        return getDefaultState().withProperty(LOG_AXIS, AXES[metadata & 3]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LOG_AXIS).ordinal();
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LOG_AXIS) == EnumAxis.Y ? MapColor.CYAN : MapColor.SILVER;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing face, float x, float y, float z) {
        ItemStack tool = player.getHeldItem(hand);
        if (stripped == null || !(tool.getItem() instanceof ItemAxe || tool.getItem().getToolClasses(tool).contains("axe"))) {
            return false;
        }
        if (!world.isRemote && world.setBlockState(pos,
                stripped.get().getDefaultState().withProperty(LOG_AXIS, state.getValue(LOG_AXIS)), 3)) {
            tool.damageItem(1, player);
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 1, 1);
        }
        return true;
    }
}
