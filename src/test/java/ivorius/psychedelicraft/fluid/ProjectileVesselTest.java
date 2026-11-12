package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.block.PlacedDrinksBlock;
import ivorius.psychedelicraft.block.entity.BottleRackBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectileVesselTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void projectileStrengthUsesBothBrewStageAndVolume() {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger(BrewFluid.DISTILLATION, 1);
        assertEquals(0F, BrewFluid.projectileStrength(new FluidStack(FluidRegistry.WATER, 2000)), 0F);
        assertEquals(0.1875F, BrewFluid.projectileStrength(new FluidStack(PSFluids.AGAVE, 500, attributes)), 0F);
        assertEquals(0.75F, BrewFluid.projectileStrength(new FluidStack(PSFluids.AGAVE, 1000, attributes)), 0F);
        assertEquals(2F, BrewFluid.projectileStrength(new FluidStack(PSFluids.AGAVE, 2000, attributes)), 0F);
    }

    @Test
    public void simulatedAndRealRackTransfersRetainTaggedContents() {
        ItemStack single = new ItemStack(PSItems.MOLOTOV_COCKTAIL);
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger(BrewFluid.DISTILLATION, 3);
        new FluidVesselHandler(single, FluidVessel.BOTTLE).craftContents(PSFluids.WHEAT, 625, attributes);
        assertTrue(PlacedDrinksBlock.canPlace(single));

        BottleRackBlockEntity rack = new BottleRackBlockEntity();
        assertTrue(rack.getBottles().insertItem(4, single, true).isEmpty());
        assertTrue(rack.getBottles().getStackInSlot(4).isEmpty());
        assertTrue(rack.getBottles().insertItem(4, single, false).isEmpty());
        ItemStack restored = rack.getBottles().extractItem(4, 1, false);
        assertTrue(ItemStack.areItemStacksEqual(single, restored));
        assertEquals(625, PSItems.MOLOTOV_COCKTAIL.contents(restored).amount);
        assertEquals(3, PSItems.MOLOTOV_COCKTAIL.contents(restored).tag.getInteger(BrewFluid.DISTILLATION));
        assertFalse(restored.getTagCompound().hasKey("flying"));
    }
}
