/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.client.particle.SlurryBubble;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.recipe.MashingRecipe;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MashTubBlockEntity extends FluidProcessingBlockEntity {
    public static final int CAPACITY = 32000;

    private final List<ItemStack> supplied = new ArrayList<>();
    private ResourceLocation activeRecipe;
    private int pulsesLeft;
    private int totalPulses;
    private int lastLight;

    public MashTubBlockEntity() {
        super(CAPACITY);
    }

    public List<ItemStack> getSuppliedIngredients() {
        return Collections.unmodifiableList(supplied);
    }

    public float getStewProgress() {
        return totalPulses > 0 ? 1 - (float) pulsesLeft / totalPulses : 0;
    }

    public float getFluidSurface() {
        return getTank().getFluidAmount() == 0 ? 0 : 0.3F + 0.6F * getTank().getFluidAmount() / getTank().getCapacity();
    }

    @Override
    public float getProcessingProgress() {
        return activeRecipe != null ? getStewProgress() : super.getProcessingProgress();
    }

    public boolean addIngredient(ItemStack held) {
        FluidStack contents = getTank().getFluid();
        if (activeRecipe != null || held.isEmpty() || contents == null || FluidVesselHandler.forTransfer(held) != null) return false;
        List<MashingRecipe> recipes = new ArrayList<>();
        boolean valid = false;
        for (IRecipe entry : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (entry instanceof MashingRecipe) {
                MashingRecipe recipe = (MashingRecipe) entry;
                if (recipe.matchesPool(contents)) {
                    recipes.add(recipe);
                    valid |= recipe.acceptsIngredient(held);
                }
            }
        }
        if (!valid) return false;
        ItemStack ingredient = held.copy();
        ingredient.setCount(1);
        supplied.add(ingredient);
        boolean partial = false;
        for (MashingRecipe recipe : recipes) {
            if (!recipe.matchesPartial(supplied)) continue;
            partial = true;
            if (recipe.getIngredients().size() == supplied.size()) {
                activeRecipe = recipe.getRegistryName();
                pulsesLeft = totalPulses = 2 + world.rand.nextInt(4) + recipe.getStewTime();
                getTank().setCanFill(false);
                getTank().setCanDrain(false);
                break;
            }
        }
        if (!partial) {
            supplied.clear();
            getTank().replace(new FluidStack(PSFluids.SLURRY, contents.amount));
            bubbles(90, 0.5F);
            world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.BLOCKS, 0.8F, 0.7F);
        } else {
            bubbles(20, 0);
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.5F, 1.15F);
        }
        changed();
        return true;
    }

    @Override
    protected void tickProcessing() {
        updateLight();
        if (activeRecipe == null) {
            processFluid(false);
            return;
        }
        if (world.getTotalWorldTime() % 30 != 0) return;
        IRecipe entry = ForgeRegistries.RECIPES.getValue(activeRecipe);
        if (!(entry instanceof MashingRecipe)) {
            endStew();
            return;
        }
        MashingRecipe recipe = (MashingRecipe) entry;
        if (!recipe.matchesPool(getTank().getFluid())) {
            endStew();
            return;
        }
        bubbles(9, 0.55F);
        if (--pulsesLeft <= 0) {
            getTank().replace(recipe.finish(getTank().getFluidAmount()));
            supplied.clear();
            endStew();
        } else {
            changed();
        }
    }

    private void endStew() {
        activeRecipe = null;
        totalPulses = pulsesLeft = 0;
        getTank().setCanFill(true);
        getTank().setCanDrain(true);
        changed();
    }

    @Override
    protected void tickClient() {
        updateLight();
        FluidStack fluid = getTank().getFluid();
        if (fluid != null && !supplied.isEmpty() && world.getTotalWorldTime() % 3 == 0 && world.rand.nextFloat() < 0.33F) {
            SlurryBubble.spawn(world, bubbleOrigin(), 1 + (int) (supplied.size() * 1.5), 0,
                    fluid.getFluid().getColor(fluid));
        }
    }

    private void bubbles(int count, float spread) {
        FluidStack fluid = getTank().getFluid();
        if (fluid != null) Channel.spawnFluidBubbles(world, bubbleOrigin(), count, spread, fluid.getFluid().getColor(fluid));
    }

    private Vec3d bubbleOrigin() {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + getFluidSurface(), pos.getZ() + 0.5);
    }

    private void updateLight() {
        FluidStack fluid = getTank().getFluid();
        int light = fluid == null ? 0 : fluid.getFluid().getLuminosity(fluid);
        if (light == lastLight) return;
        lastLight = light;
        for (BlockPos part : BlockPos.getAllInBoxMutable(pos.add(-1, 0, -1), pos.add(1, 0, 1))) {
            world.checkLight(part);
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2, 2));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList items = new NBTTagList();
        for (ItemStack ingredient : supplied) items.appendTag(ingredient.writeToNBT(new NBTTagCompound()));
        compound.setTag("suppliedIngredients", items);
        if (activeRecipe != null) {
            compound.setString("stewRecipe", activeRecipe.toString());
            compound.setInteger("pulsesLeft", pulsesLeft);
            compound.setInteger("totalPulses", totalPulses);
        }
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        supplied.clear();
        NBTTagList items = compound.getTagList("suppliedIngredients", 10);
        for (int i = 0; i < Math.min(items.tagCount(), MashingRecipe.MAX_INGREDIENTS); i++) {
            ItemStack ingredient = new ItemStack(items.getCompoundTagAt(i));
            if (!ingredient.isEmpty()) {
                ingredient.setCount(1);
                supplied.add(ingredient);
            }
        }
        pulsesLeft = compound.getInteger("pulsesLeft");
        totalPulses = Math.max(pulsesLeft, compound.getInteger("totalPulses"));
        activeRecipe = pulsesLeft > 0 && compound.hasKey("stewRecipe", 8)
                ? new ResourceLocation(compound.getString("stewRecipe")) : null;
        getTank().setCanFill(activeRecipe == null);
        getTank().setCanDrain(activeRecipe == null);
    }
}
