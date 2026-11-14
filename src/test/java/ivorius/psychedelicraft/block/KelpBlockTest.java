package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.fluid.physical.Waterlogging;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.CampfireRecipe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class KelpBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void tipsAndStemKeepTheirAgeWithoutAWaterTile() {
        for (int age = 0; age <= KelpBlock.MAX_AGE; age++) {
            IBlockState tip = KelpBlock.tip(age);
            assertEquals(age, KelpBlock.age(tip));
            assertSame(tip, tip.getBlock().getStateFromMeta(tip.getBlock().getMetaFromState(tip)));
            assertFalse(tip.getBlock().hasTileEntity(tip));
            assertTrue(Waterlogging.isWet(null, null, tip));
        }
        IBlockState stem = KelpBlock.stem();
        assertEquals(-1, KelpBlock.age(stem));
        assertSame(stem, stem.getBlock().getStateFromMeta(0));
        assertFalse(Waterlogging.needsWaterTile(stem));
    }

    @Test
    public void oneKelpCooksIntoOneFastFoodWithoutMutatingTheInput() {
        ItemStack input = new ItemStack(PSItems.KELP, 3);
        ItemStack output = CampfireRecipe.cook(input);
        assertSame(PSItems.DRIED_KELP, output.getItem());
        assertEquals(1, output.getCount());
        assertEquals(16, output.getMaxItemUseDuration());
        assertEquals(3, input.getCount());
    }
}
