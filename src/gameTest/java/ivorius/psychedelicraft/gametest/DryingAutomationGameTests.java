package ivorius.psychedelicraft.gametest;

import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Arrays;
import java.util.UUID;

@GameTestHolder("psychedelicrafttests")
@PrefixGameTestTemplate(false)
public final class DryingAutomationGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static DryingTableBlockEntity table(GameTestHelper helper){
        helper.setBlock(POS,PSBlocks.DRYING_TABLE.get());
        return (DryingTableBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    }
    @GameTest(template="empty") public static void bottomAccessTakesOnlyOutputAndInvalidatesOnRemoval(GameTestHelper helper){
        DryingTableBlockEntity table=table(helper);table.setItem(1,new ItemStack(Items.APPLE));
        var optional=table.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN);var bottom=optional.orElseThrow(AssertionError::new);
        helper.assertTrue(Arrays.equals(new int[]{0},table.getSlotsForFace(Direction.DOWN)),"native bottom slots expose only output");
        helper.assertFalse(table.canTakeItemThroughFace(1,table.getItem(1),Direction.DOWN),"native bottom extraction refuses inputs");
        helper.assertTrue(bottom.extractItem(0,64,false).isEmpty(),"Forge extraction refuses inputs");
        helper.assertTrue(bottom.insertItem(0,new ItemStack(Items.APPLE),false).getCount()==1,"Forge bottom insertion refuses items");
        table.setItem(0,new ItemStack(Items.CARROT,3));helper.assertTrue(bottom.extractItem(0,3,false).getCount()==3,"finished stack is extractable");
        helper.assertTrue(table.getItem(1).getCount()==1,"input remains untouched");
        helper.setBlock(POS,Blocks.AIR);helper.assertFalse(optional.isPresent(),"removed table invalidates cached capability");
        helper.succeed();
    }
    @GameTest(template="empty") public static void vanillaTransfersAndCapabilitySimulationConserveItems(GameTestHelper helper){
        DryingTableBlockEntity table=table(helper);
        // Vanilla hoppers split off one item before calling this helper; bulk transfers use IItemHandler.
        for(int i=0;i<3;i++){
            ItemStack remaining=HopperBlockEntity.addItem(null,table,new ItemStack(Items.APPLE),Direction.NORTH);
            helper.assertTrue(remaining.isEmpty(),"native single-item transfer succeeds");
        }
        for(int i=1;i<=3;i++)helper.assertTrue(table.getItem(i).getCount()==1,"native slots remain single-item");
        var side=table.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).orElseThrow(AssertionError::new);
        ItemStack supplied=new ItemStack(Items.CARROT,3);supplied.getOrCreateTag().putInt("sampleColor",0x22aa77);
        helper.assertTrue(side.insertItem(3,supplied,true).getCount()==2,"simulation returns the unaccepted remainder");
        helper.assertTrue(table.getItem(4).isEmpty(),"simulation leaves inventory untouched");
        helper.assertTrue(side.insertItem(3,supplied,false).getCount()==2,"execution returns the same remainder");
        helper.assertTrue(supplied.getCount()==3&&table.getItem(4).getTag().getInt("sampleColor")==0x22aa77,"input and custom data are conserved");
        table.setItem(0,new ItemStack(Items.CARROT,3));
        var top=table.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(AssertionError::new);
        helper.assertTrue(top.extractItem(0,2,true).getCount()==2&&table.getItem(0).getCount()==3,"output simulation preserves the stack");
        helper.assertTrue(top.insertItem(0,supplied,false).getCount()==3,"accepted top-output rule is preserved");
        helper.succeed();
    }
    @GameTest(template="empty") public static void nativeToolsAndFireUseTheTableMaterial(GameTestHelper helper){
        var wood=PSBlocks.DRYING_TABLE.get().defaultBlockState();var iron=PSBlocks.IRON_DRYING_TABLE.get().defaultBlockState();
        helper.assertTrue(new ItemStack(Items.IRON_AXE).getDestroySpeed(wood)>1,"native axe recognizes the wood table");
        helper.assertTrue(new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(iron)>1,"native pickaxe recognizes the iron table");
        helper.setBlock(POS,wood);
        helper.assertTrue(((FireBlock)Blocks.FIRE).canCatchFire(helper.getLevel(),helper.absolutePos(POS),Direction.UP),"native fire recognizes the flammable wood table");
        helper.assertTrue(wood.getFlammability(helper.getLevel(),helper.absolutePos(POS),Direction.UP)==20&&wood.getFireSpreadSpeed(helper.getLevel(),helper.absolutePos(POS),Direction.UP)==5,"accepted wood fire values preserved");
        helper.setBlock(POS,iron);
        helper.assertFalse(((FireBlock)Blocks.FIRE).canCatchFire(helper.getLevel(),helper.absolutePos(POS),Direction.UP),"iron table cannot catch fire");
        helper.succeed();
    }
    @GameTest(template="empty") public static void savedProgressAndInventorySurviveReload(GameTestHelper helper){
        DryingTableBlockEntity table=table(helper);table.setItem(1,new ItemStack(Items.APPLE));
        table.data.set(0,720);table.data.set(1,420);
        CompoundTag saved=table.saveWithoutMetadata();DryingTableBlockEntity restored=new DryingTableBlockEntity(helper.absolutePos(POS),table.getBlockState());restored.load(saved);
        helper.assertTrue(restored.getItem(1).getCount()==1&&!saved.contains("Dealer")&&!saved.contains("ReservationExpires"),"inventory persists; native POI ownership has no duplicate block-entity lock");
        helper.assertTrue(Math.abs(restored.getHeatRatio()-.72F)<.001&&Math.abs(restored.getDryingProgress()-.42F)<.001,"saved heat and progress persist");
        saved.putFloat("heatRatio",Float.NaN);saved.putFloat("dryingProgress",Float.POSITIVE_INFINITY);restored.load(saved);
        helper.assertTrue(restored.getHeatRatio()==0&&restored.getDryingProgress()==0,"nonfinite saved values cannot poison processing");
        helper.succeed();
    }
}
