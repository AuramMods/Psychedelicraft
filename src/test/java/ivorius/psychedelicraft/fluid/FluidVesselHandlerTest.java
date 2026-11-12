package ivorius.psychedelicraft.fluid;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class FluidVesselHandlerTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
        PSFluids.register();
    }

    @Test
    public void authorTaggedCoffeeSurvivesSimulationPartialTransferAndSave() {
        ItemStack mug = new ItemStack(PSItems.WOODEN_MUG);
        mug.setStackDisplayName("Village mug");
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("id", "psychedelicraft:coffee");
        saved.setInteger("level", 500);
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("warmth", 2);
        attributes.setString("custom", "preserve me");
        saved.setTag("attributes", attributes);
        mug.setTagInfo("fluid", saved);
        FluidVesselHandler source = new FluidVesselHandler(mug, FluidVessel.MUG);
        FluidVesselHandler target = new FluidVesselHandler(new ItemStack(PSItems.STONE_CUP), FluidVessel.CUP);

        assertEquals(250, FluidUtil.tryFluidTransfer(target, source, 300, false).amount);
        assertEquals(500, source.getFluid().amount);
        assertNull(target.getFluid());
        assertEquals(250, FluidUtil.tryFluidTransfer(target, source, 300, true).amount);
        assertEquals(250, source.getFluid().amount);
        assertEquals("Village mug", source.getContainer().getDisplayName());
        assertEquals(attributes, source.getFluid().tag);

        ItemStack restored = new ItemStack(target.getContainer().writeToNBT(new NBTTagCompound()));
        FluidStack fluid = PSItems.STONE_CUP.contents(restored);
        assertSame(PSFluids.COFFEE, fluid.getFluid());
        assertEquals(250, fluid.amount);
        assertEquals(attributes, fluid.tag);
    }

    @Test
    public void refusesDifferentAttributesAndReturnsTheVanillaEmptyVessel() {
        FluidVesselHandler cup = new FluidVesselHandler(new ItemStack(PSItems.STONE_CUP), FluidVessel.CUP);
        FluidStack cold = new FluidStack(PSFluids.COFFEE, 125);
        NBTTagCompound warmth = new NBTTagCompound();
        warmth.setInteger("warmth", 2);
        FluidStack hot = new FluidStack(PSFluids.COFFEE, 125, warmth);
        assertEquals(125, cup.fill(cold, true));
        assertEquals(0, cup.fill(hot, true));
        assertNull(cup.drain(hot, true));
        assertEquals(125, cup.getFluid().amount);
        assertEquals(25, cup.fill(new FluidStack(PSFluids.COFFEE, 25, new NBTTagCompound()), true));
        assertEquals(150, cup.getFluid().amount);

        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        bottle.setStackDisplayName("Sample");
        FluidVesselHandler handler = new FluidVesselHandler(bottle, FluidVessel.GLASS_BOTTLE);
        assertEquals(125, handler.fill(new FluidStack(FluidRegistry.WATER, 125), true));
        assertSame(Items.POTIONITEM, handler.getContainer().getItem());
        assertEquals(100, handler.drain(100, true).amount);
        assertSame(PSItems.FILLED_GLASS_BOTTLE, handler.getContainer().getItem());
        assertEquals(25, handler.drain(125, true).amount);
        assertSame(Items.GLASS_BOTTLE, handler.getContainer().getItem());
        assertEquals("Sample", handler.getContainer().getDisplayName());
    }
}
