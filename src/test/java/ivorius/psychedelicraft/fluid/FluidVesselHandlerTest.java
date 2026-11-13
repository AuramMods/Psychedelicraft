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

    @Test
    public void authoredRecipeClampsVolumeAndReplacesFluidWithoutMutatingInput() {
        ItemStack bottle = new ItemStack(PSItems.BOTTLE);
        bottle.setStackDisplayName("Sample");
        FluidVesselHandler original = new FluidVesselHandler(bottle, FluidVessel.BOTTLE);
        assertEquals(300, original.fill(new FluidStack(FluidRegistry.WATER, 300), true));
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger("batch", 17);

        FluidVesselHandler crafted = FluidVesselHandler.forCrafting(bottle);
        ItemStack output = crafted.craftContents(PSFluids.CANNABIS_TEA, 500, attributes);
        attributes.setInteger("batch", 18);
        assertSame(PSFluids.CANNABIS_TEA, crafted.getFluid().getFluid());
        assertEquals(800, crafted.getFluid().amount);
        assertEquals(17, crafted.getFluid().tag.getInteger("batch"));
        assertEquals("Sample", output.getDisplayName());
        assertSame(FluidRegistry.WATER, original.getFluid().getFluid());
        assertEquals(300, original.getFluid().amount);

        crafted.craftContents(PSFluids.AGAVE, Integer.MAX_VALUE, null);
        assertEquals(FluidVessel.BOTTLE.capacity, crafted.getFluid().amount);
        assertNull(crafted.getFluid().tag);
    }

    @Test
    public void plainFullHoneyUsesTheNativeEquivalentAndPartialHoneyPreservesVolume() {
        FluidVesselHandler bottle = FluidVesselHandler.forCrafting(new ItemStack(Items.GLASS_BOTTLE));
        int capacity = FluidVessel.GLASS_BOTTLE.capacity;
        assertEquals(capacity, bottle.fill(new FluidStack(PSFluids.HONEY, capacity), true));
        assertSame(PSItems.HONEY_BOTTLE, bottle.getContainer().getItem());
        FluidVesselHandler saved = FluidVesselHandler.forCrafting(bottle.getContainer());

        assertEquals(25, saved.drain(25, false).amount);
        assertEquals(capacity, saved.getFluid().amount);
        assertEquals(25, saved.drain(25, true).amount);
        assertSame(PSItems.FILLED_GLASS_BOTTLE, saved.getContainer().getItem());
        assertEquals(capacity - 25, saved.drain(capacity, true).amount);
        assertSame(Items.GLASS_BOTTLE, saved.getContainer().getItem());

        NBTTagCompound brewed = new NBTTagCompound();
        brewed.setInteger(BrewFluid.FERMENTATION, 2);
        assertEquals(capacity, saved.fill(new FluidStack(PSFluids.HONEY, capacity, brewed), true));
        assertSame(PSItems.FILLED_GLASS_BOTTLE, saved.getContainer().getItem());
        assertEquals(brewed, saved.getFluid().tag);
    }

    @Test
    public void drugExtractTransfersRespectTheWoodenMugRestriction() {
        FluidVesselHandler mug = FluidVesselHandler.forCrafting(new ItemStack(PSItems.WOODEN_MUG));
        FluidVesselHandler cup = FluidVesselHandler.forCrafting(new ItemStack(PSItems.STONE_CUP));
        for (FluidStack extract : new FluidStack[]{new FluidStack(PSFluids.BELLADONNA_EXTRACT, 15),
                new FluidStack(PSFluids.JIMSONWEED_EXTRACT, 15)}) {
            cup.fill(extract, true);
            assertNull(FluidUtil.tryFluidTransfer(mug, cup, 15, true));
            assertEquals(15, cup.drain(15, true).amount);
            assertNull(mug.getFluid());
        }
    }
}
