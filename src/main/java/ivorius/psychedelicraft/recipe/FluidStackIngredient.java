/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.item.FluidMachineItem;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.PortableFluidItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public final class FluidStackIngredient extends Ingredient {
    private final Ingredient vessel;
    private final Fluid fluid;
    private final int amount;
    private final NBTTagCompound attributes;

    private FluidStackIngredient(@Nullable Ingredient vessel, Fluid fluid, int amount, NBTTagCompound attributes) {
        super(samples(vessel).map(stack -> sample(stack, fluid, amount, attributes))
                .filter(stack -> !stack.isEmpty())
                .filter(stack -> vessel == null || vessel.apply(stack))
                .toArray(ItemStack[]::new));
        this.vessel = vessel;
        this.fluid = fluid;
        this.amount = amount;
        this.attributes = attributes;
    }

    @Override
    public boolean apply(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || (vessel != null && !vessel.apply(stack))) return false;
        IFluidHandlerItem handler = FluidVesselHandler.forTransfer(stack);
        return handler != null && matchingContents(handler) != null;
    }

    @Nullable
    private FluidStack matchingContents(IFluidHandlerItem handler) {
        for (IFluidTankProperties tank : handler.getTankProperties()) {
            FluidStack contents = tank.getContents();
            if (contents == null || contents.getFluid() != fluid || contents.amount < amount
                    || (!attributes.hasNoTags() && !NBTUtil.areNBTEquals(attributes, contents.tag, true))) continue;
            if (amount <= 0 || canDrain(handler, new FluidStack(contents, amount))) return contents;
        }
        return null;
    }

    boolean consumesFluid() {
        return amount > 0;
    }

    ItemStack drainedVessel(ItemStack stack) {
        IFluidHandlerItem handler = Objects.requireNonNull(FluidVesselHandler.forTransfer(stack));
        FluidStack contents = Objects.requireNonNull(matchingContents(handler));
        handler.drain(new FluidStack(contents, amount), true);
        return handler.getContainer();
    }

    private static boolean canDrain(IFluidHandlerItem handler, FluidStack requested) {
        FluidStack drained = handler.drain(requested, false);
        return drained != null && drained.isFluidStackIdentical(requested);
    }

    private static Stream<ItemStack> samples(@Nullable Ingredient vessel) {
        if (vessel != null) return Stream.of(vessel.getMatchingStacks());
        return Stream.concat(
                PSItems.items().stream().filter(item -> item instanceof PortableFluidItem || item instanceof FluidMachineItem)
                        .map(ItemStack::new),
                Stream.of(Items.BUCKET, Items.BOWL, Items.GLASS_BOTTLE).map(ItemStack::new));
    }

    private static ItemStack sample(ItemStack stack, Fluid fluid, int amount, NBTTagCompound attributes) {
        FluidVesselHandler nativeVessel = FluidVesselHandler.forCrafting(stack);
        FluidStack contents = new FluidStack(fluid, amount, attributes.hasNoTags() ? null : attributes);
        if (nativeVessel != null) {
            int capacity = nativeVessel.getTankProperties()[0].getCapacity();
            if (amount > capacity) return ItemStack.EMPTY;
            contents.amount = amount > 0 ? amount : capacity;
            return nativeVessel.replaceContents(contents);
        }
        IFluidHandlerItem handler = FluidVesselHandler.forTransfer(stack);
        if (handler == null) return ItemStack.EMPTY;
        if (amount <= 0) contents.amount = Integer.MAX_VALUE;
        return handler.fill(contents, true) >= Math.max(1, amount) ? handler.getContainer() : ItemStack.EMPTY;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    static Fluid readFluid(JsonObject json) {
        String name = JsonUtils.getString(json, "fluid");
        Fluid fluid = PSFluids.resolve(name);
        if (fluid == null) throw new JsonSyntaxException("Unknown fluid " + name);
        return fluid;
    }

    static FluidStackIngredient parse(@Nullable Ingredient vessel, JsonElement fluidSpec) {
        JsonObject json;
        if (fluidSpec.isJsonObject()) {
            json = fluidSpec.getAsJsonObject();
        } else {
            json = new JsonObject();
            json.addProperty("fluid", JsonUtils.getString(fluidSpec, "fluid"));
        }
        int amount = JsonUtils.getInt(json, "level", -1);
        NBTTagCompound attributes = new NBTTagCompound();
        if (json.has("attributes")) {
            JsonElement tag = json.get("attributes");
            try {
                attributes = JsonToNBT.getTagFromJson(tag.isJsonPrimitive() ? tag.getAsString() : tag.toString());
            } catch (NBTException error) {
                throw new JsonSyntaxException("Invalid crafting fluid attributes", error);
            }
        }
        return new FluidStackIngredient(vessel, readFluid(json), amount, attributes);
    }

    public static final class Factory implements IIngredientFactory {
        @Override
        public Ingredient parse(JsonContext context, JsonObject json) {
            return FluidStackIngredient.parse(CraftingHelper.getIngredient(json.get("vessel"), context), json);
        }
    }
}
