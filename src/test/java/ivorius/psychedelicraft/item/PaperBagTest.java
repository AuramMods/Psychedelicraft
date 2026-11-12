package ivorius.psychedelicraft.item;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaperBagTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void taggedContentsKeepAnIntegerCountAndStopAtCapacity() {
        ItemStack cookies = new ItemStack(Items.COOKIE, 64);
        cookies.setStackDisplayName("Tagged cookies");
        PaperBagItem.Contents contents = new PaperBagItem.Contents(cookies.copy(), 63_970);
        assertTrue(contents.insert(cookies));
        assertEquals(34, cookies.getCount());
        ItemStack bag = new ItemStack(PSItems.PAPER_BAG);
        PaperBagItem.setContents(bag, contents);

        PaperBagItem.Contents restored = PaperBagItem.getContents(bag.copy());
        assertEquals(64_000, restored.getCount());
        assertEquals(1, bag.getMaxStackSize());
        ItemStack output = restored.take(64);
        assertEquals(64, output.getCount());
        assertEquals("Tagged cookies", output.getDisplayName());
        assertFalse(restored.insert(new ItemStack(Items.COOKIE)));
        assertFalse(restored.insert(bag));
        assertEquals(63_936, restored.getCount());
    }

    @Test
    public void aBottleUsesTheWholeBagAndTheLastRemovalClearsItsTag() {
        PaperBagItem.Contents contents = new PaperBagItem.Contents(ItemStack.EMPTY, 0);
        ItemStack bottles = new ItemStack(PSItems.BOTTLE, 3);
        assertTrue(contents.insert(bottles));
        assertEquals(2, bottles.getCount());
        assertFalse(contents.insert(bottles));
        assertEquals(1, contents.take(64).getCount());
        ItemStack bag = new ItemStack(PSItems.PAPER_BAG);
        bag.setStackDisplayName("Keep this name");
        PaperBagItem.setContents(bag, contents);
        assertTrue(PaperBagItem.getContents(bag).isEmpty());
        assertEquals("Keep this name", bag.getDisplayName());
        assertEquals(64, bag.getMaxStackSize());
    }
}
