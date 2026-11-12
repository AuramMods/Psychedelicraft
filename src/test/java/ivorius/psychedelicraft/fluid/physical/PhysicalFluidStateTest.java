package ivorius.psychedelicraft.fluid.physical;

import ivorius.psychedelicraft.fluid.FluidContents;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PhysicalFluidStateTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void allFlowLevelsRoundTripIndependentlyOfTheAttributeTileBit() {
        PhysicalFluidBlock block = new PhysicalFluidBlock(PSFluids.COFFEE);
        for (int metadata = 0; metadata < 16; metadata++) {
            IBlockState state = block.getStateFromMeta(metadata);
            assertEquals(metadata, block.getMetaFromState(state));
            assertEquals(metadata % 8, state.getValue(BlockFluidBase.LEVEL).intValue());
            assertEquals(metadata >= 8, block.hasTileEntity(state));
            assertEquals(metadata % 8 == 0, block.canCollideCheck(state, true));
        }
    }

    @Test
    public void sourceAndBucketShareAStableFluidAndAttributeEncoding() {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("warmth", 2);
        NBTTagCompound batch = new NBTTagCompound();
        batch.setString("origin", "spring");
        attributes.setTag("batch", batch);
        NBTTagCompound data = FluidContents.write(new FluidStack(PSFluids.COFFEE, Fluid.BUCKET_VOLUME, attributes));
        FluidStack restored = FluidContents.read(data, Fluid.BUCKET_VOLUME);
        assertSame(PSFluids.COFFEE, restored.getFluid());
        assertEquals(Fluid.BUCKET_VOLUME, restored.amount);
        assertEquals(attributes, restored.tag);
        assertTrue(restored.getFluid().doesVaporize(restored));
    }
}
