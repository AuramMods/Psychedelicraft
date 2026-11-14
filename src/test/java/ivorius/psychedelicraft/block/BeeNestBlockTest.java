package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.block.entity.BeeNestBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
            for (EnumFacing facing : EnumFacing.HORIZONTALS) {
                assertEquals(level, nest.getMetaFromState(state.withProperty(BeeNestBlock.FACING, facing)));
            }
        }
        assertEquals(nest.getDefaultState(), nest.getStateFromMeta(15));
    }

    @Test
    public void campfireFacingAndLitStateRoundTrip() {
        CampfireBlock campfire = new CampfireBlock();
        for (int meta = 0; meta < 8; meta++) {
            IBlockState state = campfire.getStateFromMeta(meta);
            assertEquals(meta, campfire.getMetaFromState(state));
            assertEquals(meta < 4, state.getValue(CampfireBlock.LIT));
            assertEquals(meta < 4 ? 15 : 0, campfire.getLightValue(state, null, null));
        }
        IBlockState state = campfire.getDefaultState();
        assertSame(Items.COAL, campfire.getItemDropped(state, null, 0));
        assertEquals(1, campfire.damageDropped(state));
        assertEquals(2, campfire.quantityDropped(null));
        assertTrue(campfire.canSilkHarvest(null, null, state, null));
    }

    @Test
    public void missingOrInvalidEntranceDataDefaultsToNorth() {
        BeeNestBlockEntity tile = new BeeNestBlockEntity();
        NBTTagCompound tag = new NBTTagCompound();
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            tag.setString("Facing", facing.getName());
            tile.readFromNBT(tag);
            assertEquals(facing, tile.getFacing());
        }
        for (String invalid : new String[]{"up", "unknown"}) {
            tag.setString("Facing", invalid);
            tile.readFromNBT(tag);
            assertEquals(EnumFacing.NORTH, tile.getFacing());
        }
        tile.readFromNBT(new NBTTagCompound());
        assertEquals(EnumFacing.NORTH, tile.getFacing());
    }

    @Test
    public void storedOccupantsKeepNectarAndTimersWithinNestCapacity() {
        BeeNestBlockEntity tile = new BeeNestBlockEntity();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList records = new NBTTagList();
        for (int i = 0; i < 4; i++) {
            NBTTagCompound record = new NBTTagCompound();
            NBTTagCompound data = new NBTTagCompound();
            data.setBoolean("HasNectar", i == 1);
            record.setTag("EntityData", data);
            record.setInteger("TicksInHive", i * 700);
            records.appendTag(record);
        }
        tag.setTag("Bees", records);
        tile.readFromNBT(tag);

        assertEquals(3, tile.getOccupantCount());
        NBTTagList saved = tile.writeOccupants();
        for (int i = 0; i < 3; i++) {
            NBTTagCompound record = saved.getCompoundTagAt(i);
            assertEquals(i * 700, record.getInteger("TicksInHive"));
            assertEquals(i == 1, record.getCompoundTag("EntityData").getBoolean("HasNectar"));
        }
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
