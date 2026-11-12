package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BrewProcessTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void fermentationRunsTwiceBeforeAcetifyingAndPreservesAttributes() {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setString("batch", "test");
        FluidStack mash = new FluidStack(PSFluids.WHEAT_HOP, 750, attributes);
        assertSame(BrewProcess.FERMENT, BrewProcess.find(mash, false, true));
        assertSame(BrewProcess.IDLE, BrewProcess.find(mash, true, true));
        assertEquals(30 * 60 * 20, BrewProcess.FERMENT.duration(mash));
        assertEquals(BrewFluid.texture("beer", "_still"), PSFluids.WHEAT_HOP.getStill(mash));

        FluidStack wash = BrewProcess.FERMENT.finish(mash);
        assertEquals(1, wash.tag.getInteger(BrewFluid.FERMENTATION));
        assertFalse(mash.tag.hasKey(BrewFluid.FERMENTATION));
        FluidStack beer = BrewProcess.FERMENT.finish(wash);
        assertEquals(2, beer.tag.getInteger(BrewFluid.FERMENTATION));
        assertEquals(BrewFluid.texture("rum_mature", "_still"), PSFluids.WHEAT_HOP.getStill(beer));
        assertSame(BrewProcess.ACETIFY, BrewProcess.find(beer, false, true));
        FluidStack vinegar = BrewProcess.ACETIFY.finish(beer);
        assertSame(BrewProcess.IDLE, BrewProcess.find(vinegar, false, true));
        assertTrue(BrewFluid.isVinegar(vinegar));
        assertEquals(750, vinegar.amount);
        assertEquals("test", vinegar.tag.getString("batch"));
    }

    @Test
    public void agingUsesWineThresholdsAndStopsAtSixteen() {
        FluidStack wine = new FluidStack(PSFluids.RED_GRAPES, 5000);
        wine = BrewProcess.FERMENT.finish(wine);
        int freshColor = PSFluids.RED_GRAPES.getColor(wine);
        assertEquals(30 * 60 * 20, BrewProcess.MATURE.duration(wine));
        for (int age = 1; age <= 16; age++) {
            assertSame(BrewProcess.MATURE, BrewProcess.find(wine, true, true));
            wine = BrewProcess.MATURE.finish(wine);
            assertEquals(age, wine.tag.getInteger(BrewFluid.MATURATION));
            assertEquals(age == 16 ? "fluid/vinegar" : "fluid/wine",
                    PSFluids.RED_GRAPES.getSymbol(wine).getResourcePath());
        }
        assertSame(BrewProcess.IDLE, BrewProcess.find(wine, true, true));
        assertTrue(freshColor != PSFluids.RED_GRAPES.getColor(wine));
        assertEquals(5000, wine.amount);
    }

    @Test
    public void slurryAtExactThresholdAndCoffeeCoolingDoNotLoseExtraFluid() {
        FluidStack slurry = new FluidStack(PSFluids.SLURRY, 4000);
        assertSame(BrewProcess.HARDEN, BrewProcess.find(slurry, false, true));
        assertSame(BrewProcess.IDLE, BrewProcess.find(slurry, true, false));
        assertNull(BrewProcess.HARDEN.finish(slurry));
        slurry.amount = 4500;
        assertEquals(500, BrewProcess.HARDEN.finish(slurry).amount);

        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("warmth", 2);
        FluidStack hot = new FluidStack(PSFluids.COFFEE, 250, attributes);
        assertSame(BrewProcess.COOL, BrewProcess.find(hot, false, true));
        assertSame(BrewProcess.IDLE, BrewProcess.find(hot, true, true));
        FluidStack warm = BrewProcess.COOL.finish(hot);
        assertEquals(250, warm.amount);
        assertEquals(1, warm.tag.getInteger("warmth"));
        assertSame(BrewProcess.IDLE, BrewProcess.find(warm, false, true));
    }

    @Test
    public void allBarrelOrientationsAndTapStatesRoundTrip() {
        BarrelBlock barrel = PSBlocks.OAK_BARREL;
        for (EnumFacing face : EnumFacing.values()) {
            if (face == EnumFacing.UP) continue;
            for (boolean tapped : new boolean[]{false, true}) {
                IBlockState state = barrel.getDefaultState().withProperty(BarrelBlock.FACING, face)
                        .withProperty(BarrelBlock.TAPPED, tapped);
                assertSame(state, barrel.getStateFromMeta(barrel.getMetaFromState(state)));
                assertSame(Rotation.CLOCKWISE_90.rotate(face),
                        barrel.withRotation(state, Rotation.CLOCKWISE_90).getValue(BarrelBlock.FACING));
            }
        }
    }
}
