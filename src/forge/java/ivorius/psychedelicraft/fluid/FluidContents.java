package ivorius.psychedelicraft.fluid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

public final class FluidContents {
    private FluidContents() {
    }

    @Nullable
    public static FluidStack read(NBTTagCompound data, int capacity) {
        Fluid fluid = PSFluids.resolve(data.getString("id"));
        int amount = Math.min(data.getInteger("level"), capacity);
        if (fluid == null || amount <= 0) return null;
        NBTTagCompound attributes = data.getCompoundTag("attributes");
        return new FluidStack(fluid, amount, attributes.hasNoTags() ? null : attributes);
    }

    public static NBTTagCompound write(@Nullable FluidStack fluid) {
        NBTTagCompound data = new NBTTagCompound();
        if (fluid != null && fluid.amount > 0) {
            data.setString("id", PSFluids.id(fluid.getFluid()));
            data.setInteger("level", fluid.amount);
            if (fluid.tag != null && !fluid.tag.hasNoTags()) {
                data.setTag("attributes", fluid.tag.copy());
            }
        }
        return data;
    }

    @Nullable
    static FluidStack normalize(@Nullable FluidStack stack) {
        return stack != null && stack.tag != null && stack.tag.hasNoTags()
                ? new FluidStack(stack.getFluid(), stack.amount) : stack;
    }
}
