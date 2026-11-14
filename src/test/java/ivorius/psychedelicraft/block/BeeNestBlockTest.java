package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BeeNestBlockTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void honeyLevelsRoundTripThroughMetadataAndComparator() {
        BeeNestBlock nest = new BeeNestBlock();
        for (int level = 0; level <= BeeNestBlock.FULL_HONEY; level++) {
            IBlockState state = nest.getDefaultState().withProperty(BeeNestBlock.HONEY_LEVEL, level);
            assertEquals(level, nest.getMetaFromState(state));
            assertEquals(state, nest.getStateFromMeta(level));
            assertEquals(level, nest.getComparatorInputOverride(state, null, null));
        }
        assertEquals(nest.getDefaultState(), nest.getStateFromMeta(15));
    }

    @Test
    public void silkTouchItemCarriesEveryHoneyLevel() {
        BeeNestBlock nest = PSBlocks.BEE_NEST;
        for (int level = 0; level <= BeeNestBlock.FULL_HONEY; level++) {
            ItemStack drop = nest.getSilkTouchDrop(nest.getStateFromMeta(level)).copy();
            assertSame(PSItems.BEE_NEST, drop.getItem());
            assertEquals(1, drop.getCount());
            assertEquals(Integer.toString(level), drop.getSubCompound("BlockStateTag").getString("honey_level"));
            assertEquals(level, BeeNestBlock.storedHoneyLevel(drop));
        }
    }

    @Test
    public void untaggedAndOutOfRangeNestItemsStartEmpty() {
        ItemStack nest = new ItemStack(PSItems.BEE_NEST);
        assertEquals(0, BeeNestBlock.storedHoneyLevel(nest));
        NBTTagCompound savedState = nest.getOrCreateSubCompound("BlockStateTag");
        savedState.setInteger("honey_level", 4);
        assertEquals(4, BeeNestBlock.storedHoneyLevel(nest));
        savedState.setString("honey_level", "6");
        assertEquals(0, BeeNestBlock.storedHoneyLevel(nest));
        savedState.setInteger("honey_level", -1);
        assertEquals(0, BeeNestBlock.storedHoneyLevel(nest));
    }
}
