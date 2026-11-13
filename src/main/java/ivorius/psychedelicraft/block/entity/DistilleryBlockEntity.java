/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.DistilleryBlock;
import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.BrewProcess;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.ProcessingTank;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.Locale;

public final class DistilleryBlockEntity extends FluidProcessingBlockEntity {
    private Outlet outlet = Outlet.MISSING;

    public DistilleryBlockEntity() {
        super(FlaskBlockEntity.CAPACITY);
    }

    public Outlet getOutlet() {
        return outlet;
    }

    @Override
    protected void tickProcessing() {
        FluidStack contents = getTank().getFluid();
        BrewProcess process = BrewProcess.findDistillation(contents);
        ProcessingTank output = findOutput();
        Outlet status = Outlet.MISSING;
        if (output != null) {
            FluidStack byproduct = process == BrewProcess.DISTILL ? BrewProcess.distillationOutput(contents) : null;
            status = byproduct == null || output.fill(byproduct, false) == byproduct.amount
                    ? Outlet.READY : Outlet.BLOCKED;
        }
        if (status != outlet) {
            outlet = status;
            changed();
        }
        if (contents != null) {
            Fluid fluid = contents.getFluid();
            // The donor vents during its processing check, before testing the batch and outlet.
            if (fluid instanceof BrewFluid || PSFluids.isFlowerExtract(fluid)
                    || fluid == PSFluids.COFFEE || fluid == PSFluids.SLURRY) {
                if (world.rand.nextInt(120) == 0) steam();
                if (world.rand.nextInt(50) == 0) bubbleSound();
            }
        }
        processFluid(process, heatRate(), outlet == Outlet.READY);
    }

    @Override
    protected boolean completeProcess(BrewProcess process, FluidStack contents) {
        ProcessingTank output = findOutput();
        if (output == null) return false;
        FluidStack byproduct = BrewProcess.distillationOutput(contents);
        if (byproduct != null) {
            if (output.fill(byproduct, false) != byproduct.amount) return false;
            output.fill(byproduct, true);
        }
        steam();
        if (world.rand.nextInt(10) == 0) bubbleSound();
        return super.completeProcess(process, contents);
    }

    private void steam() {
        double x = pos.getX() + (world.rand.nextDouble() + world.rand.nextDouble()) * 0.5;
        double z = pos.getZ() + (world.rand.nextDouble() + world.rand.nextDouble()) * 0.5;
        ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD, x, pos.getY() + 0.6, z, 2, 0D, 0D, 0D, 0D);
    }

    private void bubbleSound() {
        // The 1.20.1 mixer clamps the donor's 0.02 pitch to 0.5.
        world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.25F, 0.5F);
    }

    private int heatRate() {
        net.minecraft.block.state.IBlockState below = world.getBlockState(pos.down());
        Block block = below.getBlock();
        if (below.getMaterial() == Material.LAVA) return 7;
        if (block == Blocks.FIRE) return 3;
        if (block == Blocks.MAGMA) return 2;
        return 1;
    }

    @Nullable
    private ProcessingTank findOutput() {
        EnumFacing direction = world.getBlockState(pos).getValue(DistilleryBlock.FACING);
        if (!direction.getAxis().isHorizontal() || !DistilleryBlock.canConnectTo(world, pos, direction)) return null;
        BlockPos destination = pos.offset(direction);
        TileEntity tile = world.getBlockState(destination).getBlock() == ivorius.psychedelicraft.block.PSBlocks.MASH_TUB_EDGE
                ? MashTubBlock.getVat(world, destination) : world.getTileEntity(destination);
        return tile instanceof FluidMachineBlockEntity && !tile.isInvalid()
                ? ((FluidMachineBlockEntity) tile).getTank() : null;
    }

    @Override
    @Nullable
    protected EnumFacing relativeSide(@Nullable EnumFacing side) {
        if (side == null || side.getAxis().isVertical() || world == null) return side;
        EnumFacing facing = world.getBlockState(pos).getValue(DistilleryBlock.FACING);
        return facing.getAxis().isVertical() ? side
                : EnumFacing.getHorizontal((facing.getHorizontalIndex() - side.getHorizontalIndex()) & 3);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("outlet", outlet.ordinal());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int index = compound.getInteger("outlet");
        outlet = index >= 0 && index < Outlet.values().length ? Outlet.values()[index] : Outlet.MISSING;
    }

    public enum Outlet {
        MISSING,
        READY,
        BLOCKED;

        public String key() {
            return "psychedelicraft.distillery.outlet." + name().toLowerCase(Locale.ROOT);
        }
    }
}
