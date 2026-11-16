package ivorius.psychedelicraft.gametest;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.block.*;
import ivorius.psychedelicraft.block.BarrelBlock;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.*;
import ivorius.psychedelicraft.recipe.*;
import ivorius.psychedelicraft.screen.FluidMachineMenu;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests")
@PrefixGameTestTemplate(false)
public final class FluidMachineGameTests {
    private static final BlockPos POS=new BlockPos(1,1,1);
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    private static FluidStack brew(int amount){var fluid=new FluidStack(PSFluids.WHEAT_HOP.get(),amount);fluid.getOrCreateTag().putInt("fermentation",2);fluid.getOrCreateTag().putString("batch","test");return fluid;}
    private static FlaskBlockEntity flask(GameTestHelper h,BlockPos pos){h.setBlock(pos,PSBlocks.FLASK.get());return(FlaskBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    private static BarrelBlockEntity barrel(GameTestHelper h){h.setBlock(POS,PSBlocks.OAK_BARREL.get());return(BarrelBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static DistilleryBlockEntity distillery(GameTestHelper h){h.setBlock(POS,PSBlocks.DISTILLERY.get().defaultBlockState().setValue(DistilleryBlock.FACING,Direction.EAST));return(DistilleryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));}
    private static ItemStack filled(Item item,FluidStack fluid){return FluidVesselHandler.forCrafting(new ItemStack(item)).replaceContents(fluid);}
    private static void lastTick(FluidProcessingBlockEntity machine,BrewProcess process,int duration){CompoundTag saved=machine.saveWithoutMetadata();saved.putInt("process",process.ordinal());saved.putInt("timeNeeded",duration);saved.putInt("timeProcessed",duration-1);machine.load(saved);machine.tick();}

    @GameTest(template="empty") public static void processingTankSimulationExactFillAndSnapshotsConserveData(GameTestHelper h){
        int[] changes={0};var tank=new ProcessingTank(1000,()->changes[0]++);FluidStack input=brew(1200);
        check(tank.fill(input,FluidAction.SIMULATE)==1000&&tank.isEmpty()&&changes[0]==0,"simulation changes no state or notification");
        check(!tank.fillExactly(input,FluidAction.EXECUTE)&&tank.isEmpty(),"exact fill refuses insufficient capacity without partial mutation");
        input.setAmount(700);check(tank.fillExactly(input,FluidAction.EXECUTE)&&tank.getFluidAmount()==700&&changes[0]==1,"exact fill commits once");
        var snapshot=tank.getFluid();snapshot.getTag().putString("batch","changed");snapshot.setAmount(1);
        check(tank.getFluidAmount()==700&&tank.getFluid().getTag().getString("batch").equals("test"),"tank snapshots cannot mutate stored data");
        var different=brew(100);different.getTag().putInt("fermentation",1);check(tank.fill(different,FluidAction.EXECUTE)==0&&tank.getFluidAmount()==700,"incompatible stage cannot mix");
        check(tank.drain(300,FluidAction.SIMULATE).getAmount()==300&&tank.getFluidAmount()==700,"drain simulation is inert");
        check(tank.drain(900,FluidAction.EXECUTE).getAmount()==700&&tank.isEmpty()&&input.getAmount()==700,"real drain bounds amount and leaves supplied stack intact");h.succeed();
    }
    @GameTest(template="empty") public static void acceptedBrewProcessesPreserveBoundariesAttributesAndQuantities(GameTestHelper h){
        FluidStack mash=brew(750);mash.getTag().remove("fermentation");
        check(BrewProcess.find(mash,false,true)==BrewProcess.FERMENT&&BrewProcess.find(mash,true,true)==BrewProcess.IDLE,"unfermented mash requires fermentation first");
        check(BrewProcess.FERMENT.duration(mash)==30*60*20,"accepted wheat-hop fermentation default");
        var wash=BrewProcess.FERMENT.finish(mash);var beer=BrewProcess.FERMENT.finish(wash);var vinegar=BrewProcess.ACETIFY.finish(beer);
        check(BrewProfile.stage(wash,"fermentation",2)==1&&BrewProfile.stage(beer,"fermentation",2)==2&&BrewProcess.find(beer,false,true)==BrewProcess.ACETIFY&&BrewProcess.find(vinegar,false,true)==BrewProcess.IDLE,"ferment twice then acetify and stop");
        check(vinegar.getAmount()==750&&vinegar.getTag().getString("batch").equals("test")&&!mash.getTag().contains("fermentation"),"processing copies metadata and conserves volume");
        for(int stage=0;stage<16;stage++)beer=BrewProcess.MATURE.finish(beer);
        check(BrewProfile.stage(beer,"maturation",16)==16&&BrewProcess.find(beer,true,true)==BrewProcess.IDLE,"aging stops at sixteen");
        check(BrewProcess.HARDEN.finish(new FluidStack(PSFluids.SLURRY.get(),4000)).isEmpty()&&BrewProcess.HARDEN.finish(new FluidStack(PSFluids.SLURRY.get(),4500)).getAmount()==500,"four buckets become one dirt without extra loss");
        FluidStack coffee=new FluidStack(PSFluids.COFFEE.get(),250);coffee.getOrCreateTag().putInt("warmth",2);
        check(BrewProcess.find(coffee,false,true)==BrewProcess.COOL&&PSFluids.warmth(BrewProcess.COOL.finish(coffee))==1&&BrewProcess.find(coffee,true,true)==BrewProcess.IDLE,"accepted hot-coffee cooling applies only to open processing");h.succeed();
    }
    @GameTest(template="empty") public static void twoVesselTransfersRetainPartialProgressAndSavedState(GameTestHelper h){
        var machine=flask(h,POS);var fluid=new FluidStack(PSFluids.WHEAT.get(),100);machine.getTank().replace(fluid);
        machine.setItem(0,filled(PSItems.BOTTLE.get(),new FluidStack(PSFluids.WHEAT.get(),75)));
        machine.setItem(1,filled(PSItems.BOTTLE.get(),new FluidStack(PSFluids.WHEAT.get(),1900)));
        machine.tick();check(Math.abs(machine.getVesselProgress(0)-2F/3)<.0001&&machine.getVesselProgress(1)==.5F,"50 mB transfer starts from actual remaining amount on each side");
        var saved=machine.saveWithoutMetadata();var restored=new FlaskBlockEntity(h.absolutePos(POS),machine.getBlockState());restored.load(saved);restored.setLevel(h.getLevel());restored.tick();
        check(restored.getVesselProgress(0)==1&&restored.getVesselProgress(1)==1&&restored.getTank().getFluidAmount()==75,"saved progress resumes both partial transfers exactly");
        check(FluidVesselHandler.forTransfer(restored.getItem(0)).getFluidInTank(0).isEmpty()&&FluidVesselHandler.forTransfer(restored.getItem(1)).getFluidInTank(0).getAmount()==2000,"input empty and output full");h.succeed();
    }
    @GameTest(template="empty") public static void capabilitiesRespectSidesAndCachedViewsStayInvalidAfterRemoval(GameTestHelper h){
        var machine=flask(h,POS);machine.getTank().replace(brew(700));
        check(!machine.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.NORTH).isPresent(),"north has no vessel automation");
        var input=machine.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST).orElseThrow(AssertionError::new);
        var output=machine.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.WEST).orElseThrow(AssertionError::new);
        ItemStack full=filled(PSItems.BOTTLE.get(),brew(100));check(input.insertItem(0,full,true).isEmpty()&&machine.getItem(0).isEmpty(),"vessel insertion simulation leaves inventory intact");
        check(input.insertItem(0,full,false).isEmpty()&&full.getCount()==1&&input.extractItem(0,1,false).isEmpty(),"input inserted once and cannot extract while still filled");
        check(output.insertItem(0,new ItemStack(PSItems.BOTTLE.get()),false).isEmpty()&&output.extractItem(0,1,false).isEmpty(),"output accepts empty vessel but cannot extract it before full");
        var fluidOptional=machine.getCapability(ForgeCapabilities.FLUID_HANDLER);var tank=fluidOptional.orElseThrow(AssertionError::new);var itemOptional=machine.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.EAST);
        h.setBlock(POS,Blocks.AIR);check(!fluidOptional.isPresent()&&!itemOptional.isPresent()&&tank.drain(100,FluidAction.EXECUTE).isEmpty()&&tank.fill(brew(100),FluidAction.EXECUTE)==0,"cached fluid capability cannot mutate removed machine");
        machine.reviveCaps();machine.clearRemoved();check(tank.drain(100,FluidAction.EXECUTE).isEmpty()&&input.extractItem(0,1,false).isEmpty(),"old epoch handles stay inert even if the old block entity is revived");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80) public static void nativeHoppersTransferOnlyCompletedVessels(GameTestHelper h){
        var machine=flask(h,POS);machine.getTank().replace(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,250));machine.setItem(1,new ItemStack(Items.GLASS_BOTTLE));
        h.setBlock(POS.above(),Blocks.HOPPER);var top=(HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.above()));top.setItem(0,PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER));
        h.setBlock(POS.below(),Blocks.HOPPER);var bottom=(HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.below()));
        h.runAtTickTime(40,()->{
            int bottles=0,water=0;for(int slot=0;slot<bottom.getContainerSize();slot++){var stack=bottom.getItem(slot);if(stack.is(Items.GLASS_BOTTLE))bottles+=stack.getCount();if(stack.is(Items.POTION)&&PotionUtils.getPotion(stack)==Potions.WATER)water+=stack.getCount();}
            check(top.isEmpty()&&machine.isEmpty()&&bottles==1&&water==1&&machine.getTank().getFluidAmount()==250,"real ticking hoppers inserted one input and took one empty input plus one full output, retaining exact tank volume");h.succeed();
        });
    }
    @GameTest(template="empty") public static void distillationHeatAndConnectionRulesUseNativeBlocks(GameTestHelper h){
        for(var block:List.of(Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE))for(boolean lit:new boolean[]{true,false})check(DistilleryBlockEntity.heatRate(block.defaultBlockState().setValue(CampfireBlock.LIT,lit))==3,"both native campfires heat regardless of flame state");
        check(DistilleryBlockEntity.heatRate(Blocks.FIRE.defaultBlockState())==3&&DistilleryBlockEntity.heatRate(Blocks.SOUL_FIRE.defaultBlockState())==3&&DistilleryBlockEntity.heatRate(Blocks.LAVA.defaultBlockState())==7&&DistilleryBlockEntity.heatRate(Blocks.MAGMA_BLOCK.defaultBlockState())==2&&DistilleryBlockEntity.heatRate(Blocks.SOUL_SAND.defaultBlockState())==1,"accepted fire/lava/magma/default rates");
        int[] rates={1,2,3,7},expected={102,51,34,15};for(int i=0;i<rates.length;i++)check(DistilleryBlockEntity.distillationTicks(101,rates[i])==expected[i],"duration rounds down and retains completion tick");
        check(DistilleryBlockEntity.distillationTicks(3,7)==2,"minimum remains two ticks");
        var source=distillery(h);var destination=flask(h,POS.east());check(DistilleryBlock.canConnectTo(h.getLevel(),h.absolutePos(POS),Direction.EAST),"flask is connected");
        h.setBlock(POS.east(),PSBlocks.DISTILLERY.get().defaultBlockState().setValue(DistilleryBlock.FACING,Direction.WEST));check(!DistilleryBlock.canConnectTo(h.getLevel(),h.absolutePos(POS),Direction.EAST),"head-on opposing distilleries refuse loop");h.succeed();
    }
    @GameTest(template="empty") public static void blockedAndCompetingDistilleriesNeverConsumeAnUnacceptedByproduct(GameTestHelper h){
        var source=distillery(h);var output=flask(h,POS.east());source.getTank().replace(brew(700));output.getTank().replace(new FluidStack(PSFluids.SLURRY.get(),8000));
        int duration=DistilleryBlockEntity.distillationTicks(BrewProcess.DISTILL.duration(source.getTank().getFluid()),1);
        lastTick(source,BrewProcess.DISTILL,duration);check(source.getOutlet()==DistilleryBlockEntity.Outlet.BLOCKED&&source.getTank().getFluidAmount()==700&&BrewProfile.stage(source.getTank().getFluid(),"distillation",16)==0,"full outlet pauses and preserves source");
        var saved=source.saveWithoutMetadata();check(saved.getInt("timeProcessed")==duration-1,"blocked time remains available");
        output.getTank().drain(1,FluidAction.EXECUTE);source.tick();check(source.getTank().getFluidAmount()==699&&output.getTank().getFluidAmount()==8000&&BrewProfile.stage(source.getTank().getFluid(),"distillation",16)==1,"one free mB accepts exactly one byproduct and commits source once");
        var otherPos=POS.east().north();h.setBlock(otherPos,PSBlocks.DISTILLERY.get().defaultBlockState().setValue(DistilleryBlock.FACING,Direction.SOUTH));var other=(DistilleryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(otherPos));other.getTank().replace(brew(400));
        lastTick(other,BrewProcess.DISTILL,duration);check(other.getOutlet()==DistilleryBlockEntity.Outlet.BLOCKED&&other.getTank().getFluidAmount()==400,"a competing source cannot reuse the previous receiver capacity");
        h.setBlock(POS.east(),Blocks.AIR);source.tick();check(source.getOutlet()==DistilleryBlockEntity.Outlet.MISSING&&source.getProcessingProgress()==0,"missing receiver clears processing without consuming source");h.succeed();
    }
    @GameTest(template="empty") public static void distillationBoundariesAndExtractOutputKeepEveryAuthoredAttribute(GameTestHelper h){
        var source=distillery(h);var output=flask(h,POS.east());source.getTank().replace(brew(700));
        int duration=DistilleryBlockEntity.distillationTicks(BrewProcess.DISTILL.duration(source.getTank().getFluid()),1);
        for(int stage=1;stage<=16;stage++){lastTick(source,BrewProcess.DISTILL,duration);check(BrewProfile.stage(source.getTank().getFluid(),"distillation",16)==stage,"one native completion per stage");}
        check(source.getTank().getFluidAmount()==684&&output.getTank().getFluidAmount()==16&&source.getTank().getFluid().getTag().getString("batch").equals("test")&&BrewProcess.findDistillation(source.getTank().getFluid())==BrewProcess.IDLE,"sixteen one-mB byproducts, stage cap and custom metadata preserved");
        var extract=new FluidStack(PSFluids.MORNING_GLORY_EXTRACT.get(),2);extract.getOrCreateTag().putString("batch","flowers");source.getTank().replace(extract);output.getTank().replace(FluidStack.EMPTY);
        duration=DistilleryBlockEntity.distillationTicks(BrewProcess.DISTILL.duration(extract),1);lastTick(source,BrewProcess.DISTILL,duration);
        check(source.getTank().isEmpty()&&output.getTank().getFluidAmount()==1&&BrewProfile.stage(output.getTank().getFluid(),"distillation",2)==1&&output.getTank().getFluid().getTag().getString("batch").equals("flowers"),"accepted two-mB extract conversion produces one concentrated mB without dropping attributes");h.succeed();
    }
    @GameTest(template="empty") public static void barrelProgressReloadAndPortableDropsPreserveData(GameTestHelper h){
        var machine=barrel(h);var fluid=brew(4500);machine.getTank().replace(fluid);machine.tick();machine.tick();var saved=machine.saveWithoutMetadata();
        check(saved.getInt("timeProcessed")==2&&machine.getRemainingTicks()==BrewProcess.MATURE.duration(fluid)-2,"every wall tick is persisted");
        var restored=new BarrelBlockEntity(h.absolutePos(POS),machine.getBlockState());restored.load(saved);restored.setLevel(h.getLevel());restored.tick();check(restored.getRemainingTicks()==BrewProcess.MATURE.duration(fluid)-3,"saved progress resumes exactly");
        lastTick(machine,BrewProcess.MATURE,BrewProcess.MATURE.duration(fluid));check(BrewProfile.stage(machine.getTank().getFluid(),"maturation",16)==1&&machine.getTank().getFluidAmount()==4500,"native barrel aging preserves amount");
        machine.getTank().replace(new FluidStack(PSFluids.SLURRY.get(),4500));lastTick(machine,BrewProcess.HARDEN,BrewProcess.HARDEN.duration(machine.getTank().getFluid()));
        check(machine.getTank().getFluidAmount()==500&&machine.getSolidOutput().is(Items.DIRT)&&machine.getSolidOutput().getCount()==1,"native barrel hardening consumes exactly four buckets for one dirt");
        ItemStack named=filled(PSItems.OAK_BARREL.get(),brew(1200));named.setHoverName(Component.literal("Named barrel"));named.getOrCreateTag().putString("custom","kept");machine.placedFrom(named);machine.setItem(0,filled(PSItems.BOTTLE.get(),brew(75)));
        var pos=h.absolutePos(POS);h.getLevel().destroyBlock(pos,true);var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1));
        int barrels=0,bottles=0,dirt=0;for(var drop:drops){var stack=drop.getItem();if(stack.is(PSItems.OAK_BARREL.get())){barrels+=stack.getCount();check(PSItems.OAK_BARREL.get().contents(stack).getAmount()==1200&&stack.getHoverName().getString().equals("Named barrel")&&stack.getTag().getString("custom").equals("kept"),"native block loot preserves volume, name and item metadata");}if(stack.is(PSItems.BOTTLE.get()))bottles+=stack.getCount();if(stack.is(Items.DIRT))dirt+=stack.getCount();drop.discard();}
        check(barrels==1&&bottles==1&&dirt==1,"block removal drops each machine, vessel and solid once");h.succeed();
    }
    @GameTest(template="empty") public static void machineItemsCraftPourHeatAndStackThroughNativeRecipes(GameTestHelper h){
        ItemStack flask=filled(PSItems.FLASK.get(),new FluidStack(PSFluids.COFFEE.get(),8000));flask.setHoverName(Component.literal("Saved flask"));flask.getOrCreateTag().putInt("custom",37);
        check(flask.getMaxStackSize()==1&&new ItemStack(PSItems.FLASK.get()).getMaxStackSize()==16,"filled machines do not stack; empties do");
        CraftingContainer grid=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}},3,3);
        grid.setItem(0,new ItemStack(Items.COPPER_INGOT));grid.setItem(1,new ItemStack(Items.COPPER_INGOT));grid.setItem(3,flask);
        var recipe=(DistilleryRecipe)h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("psychedelicraft:distillery")).orElseThrow();check(recipe.matches(grid,h.getLevel()),"native recipe matcher accepts named full flask");
        var result=recipe.assemble(grid,h.getLevel().registryAccess());check(result.is(PSItems.DISTILLERY.get())&&PSItems.DISTILLERY.get().contents(result).getAmount()==8000&&result.getHoverName().getString().equals("Saved flask")&&result.getTag().getInt("custom")==37,"distillery crafting preserves full fluid and item metadata");
        var heating=(HeatingRecipe)h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("psychedelicraft:hot_coffee")).orElseThrow();result=heating.assemble(new SimpleContainer(flask),h.getLevel().registryAccess());
        check(PSItems.FLASK.get().contents(result).getAmount()==8000&&PSFluids.warmth(PSItems.FLASK.get().contents(result))==1,"native furnace heating includes full fluid machines");
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());try{var serializer=new DistilleryRecipe.Serializer();serializer.toNetwork(buffer,recipe);var restored=serializer.fromNetwork(recipe.getId(),buffer);check(ItemStack.matches(recipe.assemble(grid,h.getLevel().registryAccess()),restored.assemble(grid,h.getLevel().registryAccess())),"distillery recipe packet retains metadata behavior");}finally{buffer.release();}
        grid.clearContent();grid.setItem(0,filled(PSItems.OAK_BARREL.get(),brew(16000)));grid.setItem(1,new ItemStack(PSItems.FLASK.get()));var pour=new PouringRecipe(new net.minecraft.resources.ResourceLocation("psychedelicraft:test_machine_pour"),net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        check(PSItems.FLASK.get().contents(pour.assemble(grid,h.getLevel().registryAccess())).getAmount()==8000&&PSItems.OAK_BARREL.get().contents(pour.getRemainingItems(grid).get(0)).getAmount()==8000&&PSItems.OAK_BARREL.get().contents(grid.getItem(0)).getAmount()==16000,"native machine pouring conserves full receiver, remainder and untouched recipe input");h.succeed();
    }
    @GameTest(template="empty") public static void nativeMachineMaterialsToolsAndItemBarsRemainAvailable(GameTestHelper h){
        check(new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(PSBlocks.FLASK.get().defaultBlockState())>1&&new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(PSBlocks.DISTILLERY.get().defaultBlockState())>1,"native pickaxe recognizes copper machines");
        check(new ItemStack(Items.IRON_AXE).getDestroySpeed(PSBlocks.OAK_BARREL.get().defaultBlockState())>1,"native axe recognizes wooden barrels");
        check(PSBlocks.BIRCH_BARREL.get().defaultMapColor()==Blocks.BIRCH_PLANKS.defaultMapColor()&&PSBlocks.SPRUCE_BARREL.get().defaultMapColor()==Blocks.SPRUCE_PLANKS.defaultMapColor(),"wood variants retain native map colors");
        ItemStack flask=filled(PSItems.FLASK.get(),brew(4000));check(flask.getItem().isBarVisible(flask)&&flask.getItem().getBarWidth(flask)==7,"machine item displays native half-capacity bar");h.succeed();
    }
    @GameTest(template="empty") public static void barrelOrientationsAndLargeMenuCountdownRoundTrip(GameTestHelper h){
        for(Direction side:Direction.values())if(side!=Direction.UP)for(boolean tapped:new boolean[]{true,false}){
            BlockState state=PSBlocks.OAK_BARREL.get().defaultBlockState().setValue(BarrelBlock.FACING,side).setValue(BarrelBlock.TAPPED,tapped);
            check(state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90).getValue(BarrelBlock.FACING)==net.minecraft.world.level.block.Rotation.CLOCKWISE_90.rotate(side),"native barrel rotation preserves tap/orientation");
        }
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"MachineMenuTest"));var data=new SimpleContainerData(9);int ticks=123456789;data.set(4,(short)(ticks&65535));data.set(5,(short)(ticks>>>16));
        var menu=new FluidMachineMenu(7,player.getInventory(),new SimpleContainer(2),data,h.absolutePos(POS));check(menu.getRemainingTicks()==ticks,"native signed-short menu transport reconstructs full countdown");h.succeed();
    }
}
