package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.block.DistilleryBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.DistilleryBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DistillationTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void distillingFermentedBrewConservesVolumeAndStopsAtSixteen() {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setString("batch", "winter");
        FluidStack brew = new FluidStack(PSFluids.WHEAT_HOP, 1000, attributes);
        assertSame(BrewProcess.IDLE, BrewProcess.findDistillation(brew));
        brew = BrewProcess.FERMENT.finish(brew);
        assertEquals(60 * 60 * 20, BrewProcess.DISTILL.duration(brew));

        for (int stage = 1; stage <= 16; stage++) {
            assertSame(BrewProcess.DISTILL, BrewProcess.findDistillation(brew));
            FluidStack slurry = BrewProcess.distillationOutput(brew);
            FluidStack result = BrewProcess.DISTILL.finish(brew);
            assertSame(PSFluids.SLURRY, slurry.getFluid());
            assertEquals(1, slurry.amount);
            assertEquals(brew.amount, result.amount + slurry.amount);
            assertEquals(stage, result.tag.getInteger(BrewFluid.DISTILLATION));
            assertEquals("winter", result.tag.getString("batch"));
            brew = result;
        }
        assertSame(BrewProcess.IDLE, BrewProcess.findDistillation(brew));
        FluidStack aged = BrewProcess.MATURE.finish(BrewProcess.FERMENT.finish(new FluidStack(PSFluids.APPLE, 1000)));
        assertSame(BrewProcess.IDLE, BrewProcess.findDistillation(aged));
    }

    @Test
    public void eachFlowerExtractUsesTwoUnitsForOneAtTheNextConcentration() {
        for (Fluid fluid : new Fluid[]{
                PSFluids.MORNING_GLORY_EXTRACT, PSFluids.BELLADONNA_EXTRACT, PSFluids.JIMSONWEED_EXTRACT}) {
            FluidStack input = new FluidStack(fluid, 4);
            assertEquals(1200, BrewProcess.DISTILL.duration(input));
            FluidStack perfume = BrewProcess.distillationOutput(input);
            assertSame(fluid, perfume.getFluid());
            assertEquals(1, perfume.amount);
            assertEquals(1, perfume.tag.getInteger(BrewFluid.DISTILLATION));
            assertEquals(2400, BrewProcess.DISTILL.duration(perfume));
            assertEquals(2, BrewProcess.DISTILL.finish(input).amount);
            assertSame(BrewProcess.IDLE, BrewProcess.findDistillation(perfume));
            perfume.amount = 2;
            FluidStack concentrate = BrewProcess.distillationOutput(perfume);
            assertEquals(2, concentrate.tag.getInteger(BrewFluid.DISTILLATION));
            assertNull(BrewProcess.DISTILL.finish(perfume));
            assertTrue(concentrate.getFluid().getColor(concentrate) != input.getFluid().getColor(input));
        }
    }

    @Test
    public void flaskItemFluidSurvivesSimulationAndSaveThenClearsOnDrain() {
        ItemStack item = new ItemStack(PSItems.FLASK);
        IFluidHandlerItem handler = (IFluidHandlerItem) PSItems.FLASK.initCapabilities(item, null);
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("batch", 4);
        FluidStack brew = new FluidStack(PSFluids.APPLE, 9000, attributes);

        assertEquals(8000, handler.fill(brew, false));
        assertFalse(item.hasTagCompound());
        assertEquals(8000, handler.fill(brew, true));
        assertEquals(1, item.getMaxStackSize());
        NBTTagCompound serialized = item.writeToNBT(new NBTTagCompound());
        ItemStack saved = new ItemStack(PSItems.FLASK);
        saved.setTagCompound(serialized.getCompoundTag("tag"));
        IFluidHandlerItem restored = (IFluidHandlerItem) PSItems.FLASK.initCapabilities(saved, null);
        assertEquals(0, restored.fill(new FluidStack(FluidRegistry.WATER, 2), true));
        FluidStack drained = restored.drain(8000, true);
        assertEquals(8000, drained.amount);
        assertEquals(attributes, drained.tag);
        assertNull(saved.getSubCompound("fluid"));
        assertEquals(16, saved.getMaxStackSize());
    }

    @Test
    public void distilleryDirectionsRoundTrip() {
        DistilleryBlock block = PSBlocks.DISTILLERY;
        for (EnumFacing facing : EnumFacing.values()) {
            IBlockState state = block.getDefaultState().withProperty(DistilleryBlock.FACING, facing);
            assertSame(state, block.getStateFromMeta(block.getMetaFromState(state)));
        }
    }

    @Test
    public void aNewOutletKeepsTheExistingDistilleryTile() {
        IBlockState disconnected = PSBlocks.DISTILLERY.getDefaultState();
        IBlockState connected = disconnected.withProperty(DistilleryBlock.FACING, EnumFacing.EAST);
        DistilleryBlockEntity machine = new DistilleryBlockEntity();
        assertFalse(machine.shouldRefresh(null, BlockPos.ORIGIN, disconnected, connected));
        assertTrue(machine.shouldRefresh(null, BlockPos.ORIGIN, connected, PSBlocks.FLASK.getDefaultState()));
    }
}
