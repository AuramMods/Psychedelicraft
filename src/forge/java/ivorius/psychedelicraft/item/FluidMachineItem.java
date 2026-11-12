/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class FluidMachineItem extends ItemBlock {
    private final int capacity;

    public FluidMachineItem(Block block, int capacity) {
        super(block);
        this.capacity = capacity;
        setMaxStackSize(16);
    }

    public final int getCapacity() {
        return capacity;
    }

    @Nullable
    private FluidStack contents(ItemStack stack) {
        NBTTagCompound data = stack.getSubCompound("fluid");
        return data == null ? null : FluidContents.read(data, capacity);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable NBTTagCompound tag) {
        return new FluidHandlerItemStack(stack, capacity) {
            @Override
            public int fill(@Nullable FluidStack resource, boolean doFill) {
                return super.fill(FluidContents.normalize(resource), doFill);
            }

            @Override
            public FluidStack drain(@Nullable FluidStack resource, boolean doDrain) {
                return super.drain(FluidContents.normalize(resource), doDrain);
            }

            @Override
            public FluidStack getFluid() {
                return contents(container);
            }

            @Override
            protected void setFluid(FluidStack fluid) {
                container.setTagInfo("fluid", FluidContents.write(fluid));
            }

            @Override
            protected void setContainerToEmpty() {
                NBTTagCompound data = container.getTagCompound();
                if (data != null) {
                    data.removeTag("fluid");
                    if (data.hasNoTags()) container.setTagCompound(null);
                }
            }
        };
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return contents(stack) == null ? super.getItemStackLimit(stack) : 1;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        FluidStack fluid = contents(stack);
        return fluid == null ? super.getItemStackDisplayName(stack)
                : I18n.translateToLocalFormatted(block.getUnlocalizedName() + ".filled", PSFluids.displayName(fluid));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        FluidStack fluid = contents(stack);
        if (fluid != null || flag.isAdvanced()) {
            tooltip.add(I18n.translateToLocalFormatted("psychedelicraft.container.levels",
                    fluid == null ? 0 : fluid.amount, capacity));
        }
    }
}
