package ivorius.psychedelicraft.block.entity;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DryingTableBlockEntityTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void heatUsesBothLightAndBiomeTemperature() {
        assertEquals(0F, DryingTableBlockEntity.heatAt(0, 2F), 0F);
        assertEquals(0.7225F, DryingTableBlockEntity.heatAt(15, 0.8F), 1.0e-6F);
        assertEquals(0.0625F, DryingTableBlockEntity.heatAt(15, 0F), 1.0e-6F);
        assertEquals(0.1296F, DryingTableBlockEntity.heatAt(9, 1F), 1.0e-6F);
        assertEquals(1F, DryingTableBlockEntity.heatAt(15, 2F), 0F);
    }

    @Test
    public void vanillaHopperFillsDifferentInputsInsteadOfStacking() {
        DryingTableBlockEntity table = new DryingTableBlockEntity();
        table.setInventorySlotContents(1, new ItemStack(Items.APPLE));

        ItemStack remainder = TileEntityHopper.putStackInInventoryAllSlots(null, table,
                new ItemStack(Items.APPLE), EnumFacing.NORTH);

        assertTrue(remainder.isEmpty());
        assertEquals(1, table.getStackInSlot(1).getCount());
        assertEquals(1, table.getStackInSlot(2).getCount());
    }

    @Test
    public void itemHandlerSimulatesWithoutMutatingAndPreservesRemainders() {
        DryingTableBlockEntity table = new DryingTableBlockEntity();
        IItemHandler side = new SidedInvWrapper(table, EnumFacing.NORTH);
        ItemStack input = new ItemStack(Items.APPLE, 3);

        assertEquals(2, side.insertItem(0, input, true).getCount());
        assertTrue(table.isEmpty());
        assertEquals(2, side.insertItem(0, input, false).getCount());
        assertEquals(3, input.getCount());
        assertEquals(1, table.getStackInSlot(1).getCount());

        table.setInventorySlotContents(0, new ItemStack(Items.CARROT, 3));
        IItemHandler top = new SidedInvWrapper(table, EnumFacing.UP);
        assertEquals(3, top.insertItem(0, input, false).getCount());
        assertEquals(2, top.extractItem(0, 2, true).getCount());
        assertEquals(3, table.getStackInSlot(0).getCount());
        assertEquals(2, top.extractItem(0, 2, false).getCount());
        assertEquals(1, table.getStackInSlot(0).getCount());
    }
}
