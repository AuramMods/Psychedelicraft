package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class FluidVesselHandler extends FluidHandlerItemStack {
    private static final String CONTENTS = "fluid";
    private final FluidVessel vessel;

    public FluidVesselHandler(ItemStack stack, FluidVessel vessel) {
        super(stack, vessel.capacity);
        this.vessel = vessel;
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        Item item = stack.getItem();
        if (item == Items.BOWL || item == Items.GLASS_BOTTLE || item == Items.POTIONITEM) {
            event.addCapability(Psychedelicraft.id("fluid_vessel"),
                    new FluidVesselHandler(stack, item == Items.BOWL ? FluidVessel.BOWL : FluidVessel.GLASS_BOTTLE));
        }
    }

    @Nullable
    public static FluidVesselHandler forCrafting(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        FluidVessel vessel;
        if (item instanceof PortableFluidItem) {
            vessel = ((PortableFluidItem) item).getVessel();
        } else if (item == Items.BUCKET || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET || item == Items.MILK_BUCKET) {
            vessel = FluidVessel.BUCKET;
        } else if (item == Items.BOWL) {
            vessel = FluidVessel.BOWL;
        } else if (item == Items.GLASS_BOTTLE || isWaterBottle(stack)) {
            vessel = FluidVessel.GLASS_BOTTLE;
        } else {
            return null;
        }
        ItemStack single = stack.copy();
        single.setCount(1);
        return new FluidVesselHandler(single, vessel);
    }

    @Nullable
    public static IFluidHandlerItem forTransfer(ItemStack stack) {
        if (stack.isEmpty()) return null;
        FluidVesselHandler vessel = forCrafting(stack);
        if (vessel != null) return vessel;
        ItemStack single = stack.copy();
        single.setCount(1);
        return FluidUtil.getFluidHandler(single);
    }

    @Nullable
    public static FluidStack contents(ItemStack stack, FluidVessel vessel) {
        NBTTagCompound root = stack.getTagCompound();
        if (root != null && root.hasKey(CONTENTS, 10)) {
            FluidStack contents = FluidContents.read(root.getCompoundTag(CONTENTS), vessel.capacity);
            if (contents != null) return contents;
        }
        Item item = stack.getItem();
        if (item == Items.WATER_BUCKET || isWaterBottle(stack)) return new FluidStack(FluidRegistry.WATER, vessel.capacity);
        if (item == Items.LAVA_BUCKET) return new FluidStack(FluidRegistry.LAVA, vessel.capacity);
        if (item == Items.MILK_BUCKET) return new FluidStack(PSFluids.MILK, vessel.capacity);
        return null;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.getItem() == Items.POTIONITEM && PotionUtils.getPotionFromItem(stack) == PotionTypes.WATER
                && PotionUtils.getEffectsFromStack(stack).isEmpty();
    }

    @Override
    @Nullable
    public FluidStack getFluid() {
        return contents(container, vessel);
    }

    /** A filling recipe authors the contents and can replace the existing fluid. */
    public ItemStack craftContents(Fluid fluid, int amount, @Nullable NBTTagCompound attributes) {
        FluidStack previous = getFluid();
        int existing = previous == null ? 0 : previous.amount;
        int level = amount <= 0 ? capacity : existing + Math.min(amount, capacity - existing);
        setFluid(new FluidStack(fluid, level, attributes));
        return getContainer();
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return vessel.accepts(fluid.getFluid());
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        return (container.getItem() != Items.POTIONITEM || isWaterBottle(container))
                && super.hasCapability(capability, side);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        return container.getItem() == Items.POTIONITEM && !isWaterBottle(container)
                ? null : super.getCapability(capability, side);
    }

    @Override
    public int fill(@Nullable FluidStack resource, boolean doFill) {
        return super.fill(FluidContents.normalize(resource), doFill);
    }

    @Override
    @Nullable
    public FluidStack drain(@Nullable FluidStack resource, boolean doDrain) {
        return super.drain(FluidContents.normalize(resource), doDrain);
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        return new IFluidTankProperties[]{new FluidTankProperties(getFluid(), capacity) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return FluidVesselHandler.this.canFillFluidType(fluid);
            }
        }};
    }

    @Override
    protected void setFluid(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        if (stack.amount == capacity && (stack.tag == null || stack.tag.hasNoTags())) {
            if (vessel == FluidVessel.BUCKET) {
                if (fluid == FluidRegistry.WATER) { replace(Items.WATER_BUCKET); return; }
                if (fluid == FluidRegistry.LAVA) { replace(Items.LAVA_BUCKET); return; }
                if (fluid == PSFluids.MILK) { replace(Items.MILK_BUCKET); return; }
            }
            if (vessel == FluidVessel.GLASS_BOTTLE && fluid == FluidRegistry.WATER) {
                replace(Items.POTIONITEM);
                PotionUtils.addPotionToItemStack(container, PotionTypes.WATER);
                return;
            }
        }
        replace(vessel.taggedItem(container));
        container.setTagInfo(CONTENTS, FluidContents.write(stack));
    }

    @Override
    protected void setContainerToEmpty() {
        Item empty = vessel.emptyItem();
        replace(empty == null ? container.getItem() : empty);
    }

    private void replace(Item item) {
        Item oldItem = container.getItem();
        NBTTagCompound tag = container.getTagCompound();
        if (oldItem != item) {
            container = new ItemStack(item);
            if (tag != null) tag = tag.copy();
        }
        if (tag != null) {
            tag.removeTag(CONTENTS);
            if (oldItem == Items.POTIONITEM) tag.removeTag("Potion");
            container.setTagCompound(tag.hasNoTags() ? null : tag);
        }
    }
}
