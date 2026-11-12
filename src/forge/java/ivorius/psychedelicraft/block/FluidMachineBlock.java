/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.FluidMachineBlockEntity;
import ivorius.psychedelicraft.block.entity.FluidProcessingBlockEntity;
import ivorius.psychedelicraft.fluid.FluidContents;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

abstract class FluidMachineBlock extends BlockContainer {
    protected FluidMachineBlock(Material material) {
        super(material);
        setLightOpacity(0);
    }

    protected void openMachine(World world, BlockPos pos, EntityPlayer player, int gui) {
        player.openGui(Psychedelicraft.MOD_ID, gui, world, pos.getX(), pos.getY(), pos.getZ());
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
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (world.isRemote) return;
        FluidMachineBlockEntity machine = getMachine(world, pos);
        NBTTagCompound fluid = stack.getSubCompound("fluid");
        if (machine != null && fluid != null) {
            machine.getTank().replace(FluidContents.read(fluid, machine.getTank().getCapacity()));
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = new ItemStack(Item.getItemFromBlock(this));
        FluidMachineBlockEntity machine = getMachine(world, pos);
        if (machine != null) {
            FluidStack fluid = machine.getTank().getFluid();
            if (fluid != null) stack.setTagInfo("fluid", FluidContents.write(fluid));
            for (int slot = 0; slot < machine.getVessels().getSlots(); slot++) {
                ItemStack vessel = machine.getVessels().getStackInSlot(slot);
                if (!vessel.isEmpty()) drops.add(vessel.copy());
            }
            if (machine instanceof FluidProcessingBlockEntity) {
                ItemStack solid = ((FluidProcessingBlockEntity) machine).getSolidOutput();
                if (!solid.isEmpty()) drops.add(solid.copy());
            }
        }
        drops.add(stack);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
                             @Nullable TileEntity tile, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, tile, tool);
        world.setBlockToAir(pos);
    }

    @Nullable
    public static FluidMachineBlockEntity getMachine(IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof FluidMachineBlockEntity && !tile.isInvalid() ? (FluidMachineBlockEntity) tile : null;
    }
}
