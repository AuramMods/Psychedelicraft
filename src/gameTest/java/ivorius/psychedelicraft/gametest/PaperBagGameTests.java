package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.item.*;
import ivorius.psychedelicraft.fluid.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class PaperBagGameTests {
    static void check(boolean v,String s){if(!v)throw new GameTestAssertException(s);}
    static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"PaperBagTest"));p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setPos(h.absolutePos(new BlockPos(3,3,3)).getCenter());return p;}
    static ItemStack bag(ItemStack prototype,int count){var bag=new ItemStack(PSItems.PAPER_BAG.get());PaperBagItem.setContents(bag,new PaperBagItem.Contents(prototype,count));return bag;}
    @GameTest(template="empty") public static void fullIntegerCountsPreserveTagsCapacityAndEmptyStackLimit(GameTestHelper h){
        var cookies=new ItemStack(Items.COOKIE,64);cookies.setHoverName(Component.literal("Named cookies"));var contents=new PaperBagItem.Contents(cookies,63970);check(contents.insert(cookies)&&cookies.getCount()==34,"only thirty fit at the integer cap");var bag=bag(contents.peek(1),contents.count());check(bag.getMaxStackSize()==1&&PaperBagItem.getContents(bag.copy()).count()==64000,"filled native bags are unstackable and save integer counts");var restored=PaperBagItem.getContents(bag);var output=restored.take(64);check(output.getCount()==64&&output.getHoverName().getString().equals("Named cookies")&&!restored.insert(new ItemStack(Items.COOKIE))&&!restored.insert(bag),"tags survive and mismatched or nested contents cannot mix");restored.take(64000);bag.setHoverName(Component.literal("Retained bag name"));PaperBagItem.setContents(bag,restored);check(bag.getMaxStackSize()==64&&bag.getTagElement("contents")==null&&bag.hasCustomHoverName(),"last removal clears contents without erasing other bag data");h.succeed();
    }
    @GameTest(template="empty") public static void nativeTaggedVanillaAndSnifferContentsMapLegacyReplacements(GameTestHelper h){
        for(var item:List.of(Items.COOKIE,Items.SWEET_BERRIES,Items.DRIED_KELP,Items.TORCHFLOWER_SEEDS,Items.PITCHER_POD,Items.COD,Items.COOKED_SALMON,Items.PUFFERFISH,Items.APPLE,Items.RED_DYE,Items.DANDELION,Items.CORNFLOWER,Items.OAK_BUTTON,Items.CHAIN))check(PaperBagItem.canStore(new ItemStack(item)),"native allowed item "+item);
        for(var item:List.of(Items.COBBLESTONE,Items.GOLDEN_APPLE,Items.SUNFLOWER,PSItems.PAPER_BAG.get()))check(!PaperBagItem.canStore(new ItemStack(item)),"unlisted or nested item is refused "+item);h.succeed();
    }
    @GameTest(template="empty") public static void bottleCapacityIsOneAndFluidMetadataCannotMix(GameTestHelper h){
        var contents=new PaperBagItem.Contents(ItemStack.EMPTY,0);var bottles=new ItemStack(PSItems.BOTTLE.get(),3);check(contents.insert(bottles)&&bottles.getCount()==2&&!contents.insert(bottles)&&contents.take(64).getCount()==1,"a bottle consumes the whole bag capacity");
        var syringe=new ItemStack(PSItems.SYRINGE.get());syringe=new FluidVesselHandler(syringe,PSItems.SYRINGE.get().getVessel()).replaceContents(new net.minecraftforge.fluids.FluidStack(PSFluids.COCAINE.get(),10));contents=new PaperBagItem.Contents(syringe,4);check(!contents.insert(new ItemStack(PSItems.SYRINGE.get())),"empty and filled syringes never mix");var saved=bag(syringe,4);var out=PaperBagItem.getContents(saved).take(1);check(FluidVesselHandler.forCrafting(out).getFluid().getAmount()==10,"native storage retains usable syringe contents");h.succeed();
    }
    @GameTest(template="empty") public static void nativeSecondaryClicksMoveFullStacksIntoAndOutOfCursorBags(GameTestHelper h){
        var p=player(h);var storage=new SimpleContainer(27);var menu=ChestMenu.threeRows(1,p.getInventory(),storage);var bag=new ItemStack(PSItems.PAPER_BAG.get());storage.setItem(0,new ItemStack(Items.COOKIE,64));menu.setCarried(bag);menu.clicked(0,1,ClickType.PICKUP,p);check(storage.getItem(0).isEmpty()&&PaperBagItem.getContents(menu.getCarried()).count()==64,"native menu hook stores the whole clicked stack");menu.clicked(1,1,ClickType.PICKUP,p);check(storage.getItem(1).getCount()==64&&PaperBagItem.getContents(menu.getCarried()).isEmpty(),"native menu hook returns a full stack into an empty slot");h.succeed();
    }
    @GameTest(template="empty") public static void nativeSecondaryClicksFillAndEmptyAStoredBag(GameTestHelper h){
        var p=player(h);var storage=new SimpleContainer(27);var menu=ChestMenu.threeRows(1,p.getInventory(),storage);storage.setItem(0,new ItemStack(PSItems.PAPER_BAG.get()));menu.setCarried(new ItemStack(Items.COOKIE,40));menu.clicked(0,1,ClickType.PICKUP,p);check(menu.getCarried().isEmpty()&&PaperBagItem.getContents(storage.getItem(0)).count()==40,"native stored-bag hook consumes matching cursor items");menu.clicked(0,1,ClickType.PICKUP,p);check(menu.getCarried().getCount()==40&&PaperBagItem.getContents(storage.getItem(0)).isEmpty(),"native stored-bag hook releases contents back to cursor");h.succeed();
    }
    @GameTest(template="empty") public static void nativeSlotsLimitInsertionAndRefusedSlotsRetainEverything(GameTestHelper h){
        var p=player(h);var bag=bag(new ItemStack(Items.COOKIE),30);var storage=new SimpleContainer(1);var slot=new Slot(storage,0,0,0){@Override public int getMaxStackSize(ItemStack stack){return 3;}};check(PSItems.PAPER_BAG.get().overrideStackedOnOther(bag,slot,ClickAction.SECONDARY,p)&&storage.getItem(0).getCount()==3&&PaperBagItem.getContents(bag).count()==27,"safe insertion respects an actual three-item slot limit");var refused=new Slot(new SimpleContainer(1),0,0,0){@Override public boolean mayPlace(ItemStack stack){return false;}};check(!PSItems.PAPER_BAG.get().overrideStackedOnOther(bag,refused,ClickAction.SECONDARY,p)&&PaperBagItem.getContents(bag).count()==27,"refused insertion retains all contents");h.succeed();
    }
    @GameTest(template="empty") public static void selectedNativeInventoryTickCollectsOnlyMatchingMainInventoryNeighbors(GameTestHelper h){
        var p=player(h);p.getInventory().selected=4;p.getInventory().setItem(4,bag(new ItemStack(Items.COOKIE),1));p.getInventory().setItem(3,new ItemStack(Items.COOKIE,20));p.getInventory().setItem(5,new ItemStack(Items.COOKIE,30));p.getInventory().setItem(2,new ItemStack(Items.COOKIE,40));p.getInventory().tick();check(PaperBagItem.getContents(p.getInventory().getItem(4)).count()==51&&p.getInventory().getItem(3).isEmpty()&&p.getInventory().getItem(5).isEmpty()&&p.getInventory().getItem(2).getCount()==40,"native tick consumes only adjacent matching stacks");h.succeed();
    }
    @GameTest(template="empty") public static void normalAndCrouchedUseReleaseAStackOrOneItem(GameTestHelper h){
        var p=player(h);var prototype=new ItemStack(Items.COOKIE);prototype.setHoverName(Component.literal("Released units "+p.getUUID()));p.setItemInHand(InteractionHand.MAIN_HAND,bag(prototype,100));var result=PSItems.PAPER_BAG.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(result.getResult().consumesAction()&&PaperBagItem.getContents(p.getMainHandItem()).count()==36,"normal native use releases sixty-four");p.setShiftKeyDown(true);PSItems.PAPER_BAG.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(PaperBagItem.getContents(p.getMainHandItem()).count()==35,"native crouched use releases one");int drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(5),e->ItemStack.isSameItemSameTags(e.getItem(),prototype)).stream().mapToInt(e->e.getItem().getCount()).sum();check(drops==65,"native dropped items conserve released units: "+drops);h.succeed();
    }
    @GameTest(template="empty") public static void cancelledNativeDropRetainsBagContents(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,bag(new ItemStack(Items.COOKIE),30));Object listener=new Object(){@SubscribeEvent public void cancelPaperDrop(EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity item&&item.getItem().is(Items.COOKIE))e.setCanceled(true);}};MinecraftForge.EVENT_BUS.register(listener);try{var result=PSItems.PAPER_BAG.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(!result.getResult().consumesAction()&&PaperBagItem.getContents(p.getMainHandItem()).count()==30,"failed native spawn keeps all contents");}finally{MinecraftForge.EVENT_BUS.unregister(listener);}h.succeed();
    }
    @GameTest(template="empty") public static void nativeBlockUseCollectsGroundItemsAndSplitsOneFilledBag(GameTestHelper h){
        var p=player(h);var pos=h.absolutePos(new BlockPos(3,2,3));h.setBlock(new BlockPos(3,2,3),Blocks.STONE);var point=pos.getCenter().add(0,.5,0);var drop=new ItemEntity(h.getLevel(),point.x,point.y,point.z,new ItemStack(Items.COOKIE,40));drop.setNoGravity(true);h.getLevel().addFreshEntity(drop);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.PAPER_BAG.get(),3));var context=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.UP,pos,false));var result=p.getMainHandItem().useOn(context);check(result.consumesAction()&&drop.isRemoved()&&p.getMainHandItem().getCount()==2,"authoritative native block use collects the item and consumes one empty bag");check(p.getInventory().items.stream().filter(s->s.is(PSItems.PAPER_BAG.get())).mapToInt(s->PaperBagItem.getContents(s).count()).sum()==40,"one actual filled bag receives exactly forty units");h.succeed();
    }
    @GameTest(template="dealer_work",timeoutTicks=30) public static void nativeDispenserRetainsTheBagAndReleasesOneUnit(GameTestHelper h){
        var pos=new BlockPos(3,2,3);h.setBlock(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));var dispenser=(DispenserBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));dispenser.setItem(0,bag(new ItemStack(Items.COOKIE),5));h.getLevel().scheduleTick(h.absolutePos(pos),Blocks.DISPENSER,1);h.runAfterDelay(4,()->{check(dispenser.getItem(0).is(PSItems.PAPER_BAG.get())&&PaperBagItem.getContents(dispenser.getItem(0)).count()==4,"native dispenser retains its bag and four remaining units");int count=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(pos)).inflate(4),e->e.getItem().is(Items.COOKIE)).stream().mapToInt(e->e.getItem().getCount()).sum();check(count==1,"one actual native item is dispensed");h.succeed();});
    }
    @GameTest(template="empty") public static void nativeCapturedDropsOwnReleasedContentsWithoutAWorldEntity(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,bag(new ItemStack(Items.COOKIE),30));var captured=new ArrayList<ItemEntity>();p.captureDrops(captured);try{var result=PSItems.PAPER_BAG.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);check(result.getResult().consumesAction()&&PaperBagItem.getContents(p.getMainHandItem()).isEmpty()&&captured.size()==1&&captured.get(0).getItem().getCount()==30,"native drop capture receives exactly the released contents");}finally{p.captureDrops(null);}h.succeed();
    }
    @GameTest(template="empty") public static void cancelledFullInventoryPickupLeavesSourceAndEmptyBagsIntact(GameTestHelper h){
        var p=player(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.PAPER_BAG.get(),3));var pos=h.absolutePos(new BlockPos(3,2,3));h.setBlock(new BlockPos(3,2,3),Blocks.STONE);var point=pos.getCenter().add(0,.5,0);var drop=new ItemEntity(h.getLevel(),point.x,point.y,point.z,new ItemStack(Items.COOKIE,40));h.getLevel().addFreshEntity(drop);
        Object listener=new Object(){@SubscribeEvent public void cancelFilledBagPickup(EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity item&&item.getItem().is(PSItems.PAPER_BAG.get()))e.setCanceled(true);}};MinecraftForge.EVENT_BUS.register(listener);try{var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.UP,pos,false)));check(!result.consumesAction()&&p.getMainHandItem().getCount()==3&&!drop.isRemoved()&&drop.getItem().getCount()==40,"cancelled output delivery leaves the entire collection transaction untouched");}finally{MinecraftForge.EVENT_BUS.unregister(listener);}h.succeed();
    }
    @GameTest(template="dealer_work",batch="paperbag_cancel",timeoutTicks=30) public static void cancelledDispenserSpawnRetainsItsExactBagCount(GameTestHelper h){
        var pos=new BlockPos(3,2,3);h.setBlock(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));var dispenser=(DispenserBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));var source=new ItemStack(Items.COOKIE);source.setHoverName(Component.literal("Cancelled dispenser"));dispenser.setItem(0,bag(source,5));Object listener=new Object(){@SubscribeEvent public void cancelBagDispenser(EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity item&&ItemStack.isSameItemSameTags(item.getItem(),source))e.setCanceled(true);}};MinecraftForge.EVENT_BUS.register(listener);h.getLevel().scheduleTick(h.absolutePos(pos),Blocks.DISPENSER,1);h.runAfterDelay(4,()->{try{check(dispenser.getItem(0).is(PSItems.PAPER_BAG.get())&&PaperBagItem.getContents(dispenser.getItem(0)).count()==5,"failed native dispenser spawn retains all five units and the bag");h.succeed();}finally{MinecraftForge.EVENT_BUS.unregister(listener);}});
    }

}
