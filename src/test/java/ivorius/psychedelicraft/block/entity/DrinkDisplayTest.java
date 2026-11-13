package ivorius.psychedelicraft.block.entity;

import ivorius.psychedelicraft.block.BottleRackBlock;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraftforge.items.IItemHandler;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DrinkDisplayTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void eachColumnKeepsFiveIndependentVesselsAndRetrievesTheTopFirst() {
        PlacedDrinkBlockEntity display = new PlacedDrinkBlockEntity();
        int cell = PlacedDrinkBlockEntity.cell(0.5F, 0.5F);
        ItemStack bottle = new ItemStack(PSItems.BOTTLE, 7);
        for (int index = 0; index < 5; index++) {
            bottle.setStackDisplayName("Bottle " + index);
            assertTrue(display.place(cell, bottle, index * 45));
        }
        assertEquals(7, bottle.getCount());
        assertFalse(display.place(cell, bottle, 0));
        assertFalse(display.place(cell, new ItemStack(PSItems.SYRINGE), 0));
        ItemStack picked = display.peek(cell);
        assertEquals("Bottle 4", picked.getDisplayName());
        picked.setStackDisplayName("Changed");
        for (int index = 4; index >= 0; index--) {
            ItemStack returned = display.take(cell);
            assertEquals(1, returned.getCount());
            assertEquals("Bottle " + index, returned.getDisplayName());
        }
        assertTrue(display.take(cell).isEmpty());
    }

    @Test
    public void collectingLootKeepsTheDisplayAndReturnsIndependentStacks() {
        PlacedDrinkBlockEntity display = new PlacedDrinkBlockEntity();
        int cell = PlacedDrinkBlockEntity.cell(0.5F, 0.5F);
        assertTrue(display.place(cell, new ItemStack(PSItems.BOTTLE).setStackDisplayName("Lower"), 0));
        assertTrue(display.place(cell, new ItemStack(PSItems.WOODEN_MUG).setStackDisplayName("Upper"), 0));

        List<ItemStack> drops = new ArrayList<>();
        display.addDrops(drops);
        assertEquals(2, drops.size());
        assertEquals("Lower", drops.get(0).getDisplayName());
        assertEquals("Upper", drops.get(1).getDisplayName());
        drops.get(0).shrink(1);
        drops.get(1).setStackDisplayName("Changed");

        assertEquals("Upper", display.take(cell).getDisplayName());
        assertEquals("Lower", display.take(cell).getDisplayName());
    }

    @Test
    public void nearbyPickupUsesBoundedCoordinatesWithoutWrappingAcrossEdges() {
        PlacedDrinkBlockEntity display = new PlacedDrinkBlockEntity();
        int edge = PlacedDrinkBlockEntity.cell(0.5F, 1);
        int nextRow = PlacedDrinkBlockEntity.cell(0.57F, 0);
        display.place(edge, new ItemStack(PSItems.WOODEN_MUG), 0);
        assertEquals(edge, display.findColumn(PlacedDrinkBlockEntity.cell(0.5F, 0.84F)));
        assertEquals(-1, display.findColumn(nextRow));
        assertEquals(15 / 16F, PlacedDrinkBlockEntity.z(edge), 0);
    }

    @Test
    public void nearbyPickupSearchesEastFirstThenContinuesAroundEachRing() {
        PlacedDrinkBlockEntity display = new PlacedDrinkBlockEntity();
        int center = PlacedDrinkBlockEntity.cell(0.5F, 0.5F);
        int east = center + 16;
        int northEast = east - 1;
        int west = center - 16;
        int outerCorner = center + 2 * 16 + 2;
        int outOfRange = center + 3;
        for (int cell : new int[]{west, northEast, east, center, outerCorner, outOfRange}) {
            assertTrue(display.place(cell, new ItemStack(PSItems.WOODEN_MUG).setStackDisplayName(Integer.toString(cell)), 0));
        }
        for (int cell : new int[]{center, east, northEast, west, outerCorner}) {
            assertEquals(Integer.toString(cell), display.peek(center).getDisplayName());
            assertEquals(Integer.toString(cell), display.take(center).getDisplayName());
        }
        assertTrue(display.peek(center).isEmpty());
        assertEquals(Integer.toString(outOfRange), display.peek(outOfRange).getDisplayName());
    }

    @Test
    public void rackSlotsMatchAllFacingsAndKeepOneBottlePerSlot() {
        BottleRackBlock rack = PSBlocks.BOTTLE_RACK;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            IBlockState state = rack.getDefaultState().withProperty(BottleRackBlock.FACING, facing);
            assertSame(state, rack.getStateFromMeta(rack.getMetaFromState(state)));
            assertSame(facing.rotateY(), rack.withRotation(state, Rotation.CLOCKWISE_90).getValue(BottleRackBlock.FACING));
            assertEquals(4, BottleRackBlock.slotAt(facing, facing, 0.5F, 0.5F, 0.5F));
            assertEquals(-1, BottleRackBlock.slotAt(facing, facing.getOpposite(), 0.5F, 0.5F, 0.5F));
        }
        IItemHandler handler = new BottleRackBlockEntity().getBottles();
        ItemStack bottles = new ItemStack(PSItems.BOTTLE, 3);
        assertSame(Items.APPLE, handler.insertItem(0, new ItemStack(Items.APPLE), false).getItem());
        assertEquals(2, handler.insertItem(0, bottles, true).getCount());
        assertTrue(handler.getStackInSlot(0).isEmpty());
        assertEquals(2, handler.insertItem(0, bottles, false).getCount());
        assertEquals(1, handler.getStackInSlot(0).getCount());
        assertEquals(3, handler.insertItem(0, bottles, false).getCount());
        assertSame(PSItems.BOTTLE, handler.extractItem(0, 1, false).getItem());
    }
}
