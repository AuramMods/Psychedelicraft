package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.block.MashTubBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ProcessingTankTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void simulationPartialTransferAndPersistenceConserveFluidAndAttributes() {
        AtomicInteger changes = new AtomicInteger();
        ProcessingTank tank = new ProcessingTank(32000, changes::incrementAndGet);
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("batch", 7);
        FluidStack input = new FluidStack(PSFluids.RED_GRAPES, 36000, attributes);

        assertEquals(32000, tank.fill(input, false));
        assertEquals(0, tank.getFluidAmount());
        assertEquals(0, changes.get());
        assertEquals(32000, tank.fill(input, true));
        assertEquals(1, changes.get());
        assertEquals(0, tank.fill(new FluidStack(FluidRegistry.WATER, 10), true));
        assertNull(tank.drain(new FluidStack(PSFluids.RED_GRAPES, 10), true));
        assertEquals(31000, tank.drain(31000, false).amount);
        assertEquals(32000, tank.getFluidAmount());
        assertEquals(31000, tank.drain(31000, true).amount);
        assertEquals(2, changes.get());

        NBTTagCompound saved = tank.writeToNBT(new NBTTagCompound());
        assertEquals("psychedelicraft:red_grapes", saved.getString("id"));
        ProcessingTank restored = new ProcessingTank(32000, changes::incrementAndGet);
        restored.readFromNBT(saved);
        assertSame(PSFluids.RED_GRAPES, restored.getFluid().getFluid());
        assertEquals(1000, restored.getFluidAmount());
        assertEquals(attributes, restored.getFluid().tag);
        assertEquals(2, changes.get());
    }

    @Test
    public void disabledTankRefusesSimulatedAndRealTransfers() {
        ProcessingTank tank = new ProcessingTank(32000, () -> {});
        FluidStack water = new FluidStack(FluidRegistry.WATER, 1000);
        tank.replace(water);
        tank.setCanFill(false);
        tank.setCanDrain(false);
        assertEquals(0, tank.fill(water, false));
        assertEquals(0, tank.fill(water, true));
        assertNull(tank.drain(100, false));
        assertNull(tank.drain(water, true));
        assertEquals(1000, tank.getFluidAmount());
    }

    @Test
    public void eachEdgeFindsItsCenterAfterMetadataRoundTrip() {
        BlockPos center = new BlockPos(4, 12, -7);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                IBlockState edge = PSBlocks.MASH_TUB_EDGE.edgeState(dx, dz);
                int metadata = PSBlocks.MASH_TUB_EDGE.getMetaFromState(edge);
                IBlockState restored = PSBlocks.MASH_TUB_EDGE.getStateFromMeta(metadata);
                assertSame(edge, restored);
                assertEquals(center, MashTubBlock.center(center.add(dx, 0, dz), restored));
            }
        }
    }
}
