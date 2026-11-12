/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.BarrelBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fluids.Fluid;

import javax.annotation.Nullable;

public final class BarrelBlockEntity extends FluidProcessingBlockEntity {
    public static final int CAPACITY = 16000;

    private int tapTicks;
    private float tapAngle;
    private float previousTapAngle;

    public BarrelBlockEntity() {
        super(CAPACITY);
    }

    public boolean dispense(EntityPlayer player, EnumHand hand) {
        if (!transferHeldFluid(player, hand, Fluid.BUCKET_VOLUME, true)) return false;
        tapTicks = 20;
        changed();
        return true;
    }

    public int getTapTicks() {
        return tapTicks;
    }

    public float getTapAngle(float partialTicks) {
        return previousTapAngle + (tapAngle - previousTapAngle) * partialTicks;
    }

    @Override
    protected void tickProcessing() {
        processFluid(true);
        if (tapTicks > 0 && tapTicks % 5 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.025F, 0.5F);
        }
        animateTap();
    }

    @Override
    protected void tickClient() {
        animateTap();
    }

    private void animateTap() {
        previousTapAngle = tapAngle;
        tapAngle = MathHelper.clamp(tapAngle + (tapTicks > 0 ? 18 : -18), 0, 90);
        if (tapTicks > 0) tapTicks--;
    }

    @Override
    @Nullable
    protected EnumFacing relativeSide(@Nullable EnumFacing side) {
        if (side == null || side.getAxis().isVertical() || world == null) return side;
        EnumFacing facing = world.getBlockState(pos).getValue(BarrelBlock.FACING);
        return facing.getAxis().isVertical() ? side
                : EnumFacing.getHorizontal((facing.getHorizontalIndex() - side.getHorizontalIndex()) & 3);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("timeLeftTapOpen", tapTicks);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tapTicks = compound.getInteger("timeLeftTapOpen");
    }
}
