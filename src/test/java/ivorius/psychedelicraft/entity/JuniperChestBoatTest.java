package ivorius.psychedelicraft.entity;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JuniperChestBoatTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void restoresCargoInItsOriginalSlots() {
        EntityJuniperChestBoat boat = new EntityJuniperChestBoat(null);
        ItemStack named = new ItemStack(Items.APPLE, 37);
        named.setStackDisplayName("Juniper");
        boat.setInventorySlotContents(26, named);
        NBTTagCompound tag = new NBTTagCompound();
        boat.writeEntityToNBT(tag);

        EntityJuniperChestBoat restored = new EntityJuniperChestBoat(null);
        restored.setInventorySlotContents(0, new ItemStack(Items.STICK));
        restored.readEntityFromNBT(tag);
        assertEquals(27, restored.getSizeInventory());
        assertTrue(restored.getStackInSlot(0).isEmpty());
        assertTrue(ItemStack.areItemStacksEqual(named, restored.getStackInSlot(26)));
    }

    @Test
    public void keepsAnAuthoredLootTableDeferredThroughSave() {
        NBTTagCompound authored = new NBTTagCompound();
        authored.setString("LootTable", "minecraft:chests/simple_dungeon");
        authored.setLong("LootTableSeed", 1234L);
        EntityJuniperChestBoat boat = new EntityJuniperChestBoat(null);
        boat.readEntityFromNBT(authored);

        NBTTagCompound saved = new NBTTagCompound();
        boat.writeEntityToNBT(saved);
        assertEquals(new ResourceLocation("minecraft", "chests/simple_dungeon"), boat.getLootTable());
        assertEquals(authored.getString("LootTable"), saved.getString("LootTable"));
        assertEquals(1234L, saved.getLong("LootTableSeed"));
        assertFalse(saved.hasKey("Items"));
    }

    @Test
    public void automationSimulatesAndTransfersOnlyAvailableSpace() {
        EntityJuniperChestBoat boat = new EntityJuniperChestBoat(null);
        boat.setInventorySlotContents(0, new ItemStack(Items.APPLE, 60));
        IItemHandler handler = new InvWrapper(boat);
        ItemStack apples = new ItemStack(Items.APPLE, 7);
        assertEquals(3, handler.insertItem(0, apples, true).getCount());
        assertEquals(60, boat.getStackInSlot(0).getCount());
        assertEquals(3, handler.insertItem(0, apples, false).getCount());
        assertEquals(7, apples.getCount());
        assertEquals(5, handler.extractItem(0, 5, true).getCount());
        assertEquals(64, boat.getStackInSlot(0).getCount());
        assertEquals(5, handler.extractItem(0, 5, false).getCount());
        assertEquals(59, boat.getStackInSlot(0).getCount());
    }
}
