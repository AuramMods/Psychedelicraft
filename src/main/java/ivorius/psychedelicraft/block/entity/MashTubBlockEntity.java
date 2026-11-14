/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.fluid.PSFluids;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.client.particle.SlurryBubble;
import ivorius.psychedelicraft.network.Channel;
import ivorius.psychedelicraft.recipe.MashingRecipe;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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

    public boolean interactWithVessel(EntityPlayer player, EnumHand hand) {
        IFluidHandlerItem vessel = FluidVesselHandler.forTransfer(player.getHeldItem(hand));
        if (vessel == null) return false;
        boolean withdraw = vessel.drain(Integer.MAX_VALUE, false) == null;
        FluidStack moved = transferHeldFluid(player, hand, vessel, Integer.MAX_VALUE, withdraw);
        if (moved == null) return false;
        if (withdraw) PSCriteria.tookMash(player, moved);
        SoundEvent sound = withdraw ? moved.getFluid().getFillSound(moved) : SoundEvents.ITEM_BUCKET_FILL;
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1, 1);
        return true;
    }

    public boolean dropSolidOutput(EntityPlayer player) {
        ItemStack output = takeSolidOutput();
        if (output.isEmpty()) return false;
        PSCriteria.tookMash(player, output);
        Block.spawnAsEntity(world, pos, output);
        return true;
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
        int matches = 0;
        MashingRecipe complete = null;
        for (MashingRecipe recipe : recipes) {
            if (!recipe.matchesPartial(supplied)) continue;
            matches++;
            if (recipe.getIngredients().size() == supplied.size()) complete = recipe;
        }
        if (matches == 0) {
            supplied.clear();
            getTank().replace(new FluidStack(PSFluids.SLURRY, contents.amount, contents.tag));
            bubbles(90, 0.5F, SoundEvents.BLOCK_GRAVEL_BREAK);
        } else if (matches == 1 && complete != null) {
            activeRecipe = complete.getRegistryName();
            pulsesLeft = totalPulses = 2 + world.rand.nextInt(4) + complete.getStewTime();
            getTank().setCanFill(false);
            getTank().setCanDrain(false);
        }
        bubbles(20, 0, SoundEvents.BLOCK_LAVA_POP);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1, 1);
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
        if (!recipe.matchesPool(getTank().getFluid()) || !recipe.matchesIngredients(supplied)) {
            endStew();
            return;
        }
        bubbles(9, 0.5F, SoundEvents.BLOCK_LAVA_AMBIENT);
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
            SlurryBubble.spawnMash(world, bubbleOrigin(), 1 + (int) (supplied.size() * 1.5),
                    fluid.getFluid().getColor(fluid));
            world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, bubbleVolume(), bubblePitch(), true);
        }
    }

    private void bubbles(int count, float spread, SoundEvent sound) {
        FluidStack fluid = getTank().getFluid();
        if (fluid == null) return;
        Channel.spawnFluidBubbles(world, bubbleOrigin(), count, spread, fluid.getFluid().getColor(fluid));
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, bubbleVolume(), bubblePitch());
    }

    private Vec3d bubbleOrigin() {
        Random random = world.rand;
        return new Vec3d(pos.getX() + 0.5 + (random.nextDouble() - random.nextDouble()) * 0.25,
                pos.getY() + 1 + (random.nextDouble() - random.nextDouble()) * 0.25,
                pos.getZ() + 0.5 + (random.nextDouble() - random.nextDouble()) * 0.25);
    }

    private float bubbleVolume() {
        return 0.5F + world.rand.nextFloat();
    }

    private float bubblePitch() {
        return 0.3F + world.rand.nextFloat();
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
