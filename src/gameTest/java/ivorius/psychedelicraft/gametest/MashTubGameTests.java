package ivorius.psychedelicraft.gametest;
import com.mojang.authlib.GameProfile;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.block.*;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.*;
import ivorius.psychedelicraft.particle.*;
import ivorius.psychedelicraft.recipe.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class MashTubGameTests {
    private static final BlockPos POS=new BlockPos(2,1,2);
    private static void check(boolean value,String text){if(!value)throw new GameTestAssertException(text);}
    private static MashTubBlockEntity vat(GameTestHelper h){
        h.setBlock(POS,PSBlocks.MASH_TUB.get());for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dx!=0||dz!=0)h.setBlock(POS.offset(dx,0,dz),MashTubBlock.wallState(dx,dz));
        return (MashTubBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"MashTest"));player.setPos(h.absolutePos(POS).getX(),h.absolutePos(POS).getY()+4,h.absolutePos(POS).getZ());return player;}
    private static MashingRecipe wheat(GameTestHelper h){return(MashingRecipe)h.getLevel().getRecipeManager().byKey(new ResourceLocation("psychedelicraft:fluids/mash_table_with_wheat")).orElseThrow();}
    private static void begin(MashTubBlockEntity vat){vat.getTank().replace(new FluidStack(Fluids.WATER,31000));for(int i=0;i<7;i++)check(vat.addIngredient(new ItemStack(Items.WHEAT)),"native wheat ingredient accepted");check(vat.isStewing(),"complete wheat batch begins");}
    private static void pulseAt(GameTestHelper h,MashTubBlockEntity vat,int remaining,Runnable after){
        CompoundTag saved=vat.saveWithoutMetadata();saved.putInt("pulsesLeft",remaining);saved.putInt("totalPulses",5);
        int delay=30-(int)(h.getLevel().getGameTime()%30);h.runAfterDelay(delay,()->{vat.load(saved);vat.tick();after.run();});
    }
    @GameTest(template="empty") public static void nativeItemPlacementReservesNineCellsAndWorksBelowZero(GameTestHelper h){
        var player=player(h);var item=new ItemStack(PSItems.MASH_TUB.get(),2);var fluid=FluidVesselHandler.forCrafting(item.copyWithCount(1)).replaceContents(new FluidStack(PSFluids.WHEAT.get(),12345));fluid.setHoverName(Component.literal("Named basin"));fluid.getOrCreateTag().putInt("custom",29);player.setItemInHand(InteractionHand.MAIN_HAND,fluid);
        BlockPos target=h.absolutePos(POS);check(target.getY()<0,"fixture exercises native negative build height");
        var context=new BlockPlaceContext(player,InteractionHand.MAIN_HAND,fluid,new BlockHitResult(target.getCenter(),Direction.UP,target,false));
        check(MashTubItem.canPlaceBasin(context,target)&&PSItems.MASH_TUB.get().place(context).consumesAction(),"real BlockItem entry point places basin");
        var machine=MashTubBlock.getVat(h.getLevel(),target);check(machine!=null&&machine.getTank().getFluidAmount()==12345&&machine.portableStack().getHoverName().getString().equals("Named basin")&&machine.portableStack().getTag().getInt("custom")==29,"placed fluid/name/root item data survive");
        for(BlockPos part:BlockPos.betweenClosed(target.offset(-1,0,-1),target.offset(1,0,1)))check(MashTubBlock.getVat(h.getLevel(),part)==machine,"every proxy resolves exact owner");
        check(!MashTubItem.canPlaceBasin(context,target)&&fluid.isEmpty(),"reserved cells refuse overlap and placement consumes once");h.succeed();
    }
    @GameTest(template="empty") public static void nativeUseRollsBackEveryCellWhenForgeMultiplaceIsCancelled(GameTestHelper h){
        var player=player(h);var held=FluidVesselHandler.forCrafting(new ItemStack(PSItems.MASH_TUB.get())).replaceContents(new FluidStack(Fluids.WATER,17000));player.setItemInHand(InteractionHand.MAIN_HAND,held);
        BlockPos target=h.absolutePos(POS);int[] seen={0};Object listener=new Object(){
            @net.minecraftforge.eventbus.api.SubscribeEvent public void placed(net.minecraftforge.event.level.BlockEvent.EntityMultiPlaceEvent event){if(event.getEntity()==player){seen[0]=event.getReplacedBlockSnapshots().size();event.setCanceled(true);}}
        };
        MinecraftForge.EVENT_BUS.register(listener);
        try{var result=held.useOn(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(target.getCenter(),Direction.UP,target,false)));check(result==InteractionResult.FAIL&&seen[0]==9,"real Forge item-use path publishes nine-cell cancellable placement");}finally{MinecraftForge.EVENT_BUS.unregister(listener);}
        check(held.getCount()==1&&PSItems.MASH_TUB.get().contents(held).getAmount()==17000,"cancelled placement retains exact portable item and fluid");
        for(BlockPos part:BlockPos.betweenClosed(target.offset(-1,0,-1),target.offset(1,0,1)))check(h.getLevel().getBlockState(part).isAir()&&h.getLevel().getBlockEntity(part)==null,"Forge rollback restores all cells and removes transient entities");
        check(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(target).inflate(3),entity->entity.getItem().is(PSItems.MASH_TUB.get())).isEmpty(),"cancelled placement creates no duplicate portable drops");h.succeed();
    }
    @GameTest(template="empty") public static void wallsClipCollisionWhileKeepingBasinHollow(GameTestHelper h){
        var vat=vat(h);var world=h.getLevel();BlockPos center=vat.getBlockPos();
        check(!Shapes.joinIsNotEmpty(MashTubBlock.collision(vat.getBlockState()),Shapes.box(.1,.2,.1,.9,.9,.9),BooleanOp.AND),"interior is hollow above floor");
        for(BlockPos part:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))){var shape=MashTubBlock.collision(world.getBlockState(part));check(!shape.isEmpty()&&!Shapes.joinIsNotEmpty(shape,Shapes.block(),BooleanOp.ONLY_FIRST),"all nine collision cells stay within native cell bounds");}
        check(MashTubBlock.collision(MashTubBlock.wallState(1,0)).max(Direction.Axis.X)<=.5,"east outer wall stops at actual two-block basin edge");h.succeed();
    }
    @GameTest(template="empty") public static void ingredientMatchingUsesAssignmentAndNativeRecipePackets(GameTestHelper h){
        String json="{\"base_fluid\":{\"fluid\":\"minecraft:water\",\"level\":500,\"attributes\":{\"custom\":7}},\"ingredients\":[{\"tag\":\"minecraft:planks\"},{\"item\":\"minecraft:oak_planks\"}],\"result\":{\"fluid\":\"psychedelicraft:wheat\",\"attributes\":{\"fermentation\":1}},\"stew_time\":9}";
        var serializer=new MashingRecipe.Serializer();var recipe=serializer.fromJson(new ResourceLocation("psychedelicraft:test_assignment"),JsonParser.parseString(json).getAsJsonObject());
        check(recipe.matchesIngredients(List.of(new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.BIRCH_PLANKS)))&&!recipe.matchesIngredients(List.of(new ItemStack(Items.BIRCH_PLANKS),new ItemStack(Items.BIRCH_PLANKS))),"overlapping tags backtrack to correct distinct ingredient slots");
        var pool=new FluidStack(Fluids.WATER,500);pool.getOrCreateTag().putInt("custom",7);check(recipe.matchesPool(pool)&&!recipe.matchesPool(new FluidStack(Fluids.WATER,500)),"minimum and partial NBT pool matching");
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{serializer.toNetwork(buffer,recipe);var restored=serializer.fromNetwork(recipe.getId(),buffer);check(restored.matchesPool(pool)&&restored.matchesIngredients(List.of(new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.BIRCH_PLANKS)))&&restored.getStewTime()==9&&restored.finish(31877).getAmount()==31877&&BrewProfile.stage(restored.finish(500),"fermentation",2)==1,"native recipe packet retains all matching and result rules");}finally{buffer.release();}h.succeed();
    }
    @GameTest(template="empty") public static void stewLocksEveryTransferAndPersistsNamedIngredients(GameTestHelper h){
        var vat=vat(h);vat.getTank().replace(new FluidStack(Fluids.WATER,31000));var wheat=new ItemStack(Items.WHEAT);wheat.setHoverName(Component.literal("Saved wheat"));wheat.getOrCreateTag().putInt("custom",91);check(vat.addIngredient(wheat),"named wheat accepted");for(int i=1;i<7;i++)vat.addIngredient(new ItemStack(Items.WHEAT));
        var proxy=h.getLevel().getBlockEntity(vat.getBlockPos().east());var tank=proxy.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
        for(var action:FluidAction.values()){check(tank.drain(1000,action).isEmpty()&&tank.drain(new FluidStack(Fluids.WATER,1000),action).isEmpty()&&tank.fill(new FluidStack(Fluids.WATER,1000),action)==0,"active stew refuses simulation and execution through proxy");}
        var saved=vat.saveWithoutMetadata();var restored=new MashTubBlockEntity(vat.getBlockPos(),vat.getBlockState());restored.load(saved);restored.setLevel(h.getLevel());
        check(restored.isStewing()&&restored.getTank().getFluidAmount()==31000&&restored.getRemainingTicks()==vat.getRemainingTicks()&&restored.getSuppliedIngredients().get(0).getHoverName().getString().equals("Saved wheat")&&restored.getSuppliedIngredients().get(0).getTag().getInt("custom")==91,"persisted stew retains exact pulses, volume and ingredient NBT");
        restored.getSuppliedIngredients().get(0).getTag().putInt("custom",0);check(restored.getSuppliedIngredients().get(0).getTag().getInt("custom")==91,"ingredient rendering snapshots are independent");h.succeed();
    }
    @GameTest(template="empty") public static void nativeStewCompletionUsesEntireVolumeThenFermentsAndAcetifies(GameTestHelper h){
        var vat=vat(h);begin(vat);pulseAt(h,vat,1,()->{check(!vat.isStewing()&&vat.getSuppliedIngredients().isEmpty()&&vat.getTank().getFluid().getFluid()==PSFluids.WHEAT.get()&&vat.getTank().getFluidAmount()==31000,"last stew pulse converts full native pool and consumes only ingredients");
        for(var stage:List.of(BrewProcess.FERMENT,BrewProcess.FERMENT,BrewProcess.ACETIFY)){
            var saved=vat.saveWithoutMetadata();int duration=stage.duration(vat.getTank().getFluid());saved.putInt("process",stage.ordinal());saved.putInt("timeNeeded",duration);saved.putInt("timeProcessed",duration-1);vat.load(saved);vat.tick();
        }
        check(BrewProfile.stage(vat.getTank().getFluid(),"fermentation",2)==2&&BrewProfile.isVinegar(vat.getTank().getFluid())&&vat.getTank().getFluidAmount()==31000,"open mash ferments twice then becomes vinegar without changing volume");h.succeed();});
    }
    @GameTest(template="empty") public static void changedRecipeUnlocksBatchWithoutFabricatingOrDeletingContents(GameTestHelper h){
        var vat=vat(h);begin(vat);var saved=vat.saveWithoutMetadata();saved.putString("stewRecipe","psychedelicraft:removed_recipe");vat.load(saved);pulseAt(h,vat,1,()->{
        check(!vat.isStewing()&&vat.getSuppliedIngredients().size()==7&&vat.getTank().getFluid().getFluid()==Fluids.WATER&&vat.getTank().getFluidAmount()==31000&&vat.getTank().drain(1,FluidAction.SIMULATE).getAmount()==1,"removed recipe unlocks intact batch, so reload can recover inputs");h.succeed();});
    }
    @GameTest(template="empty") public static void invalidMixtureBecomesSlurryAndNativeHoneycombMakesMead(GameTestHelper h){
        var vat=vat(h);vat.getTank().replace(new FluidStack(Fluids.WATER,4000));check(!vat.addIngredient(new ItemStack(Items.STONE)),"unrelated item leaves basin intact");vat.addIngredient(new ItemStack(Items.WHEAT));vat.addIngredient(new ItemStack(Items.POTATO));
        check(vat.getTank().getFluid().getFluid()==PSFluids.SLURRY.get()&&vat.getTank().getFluidAmount()==4000&&vat.getSuppliedIngredients().isEmpty(),"incompatible individually valid ingredients become source-volume slurry");
        vat.getTank().replace(new FluidStack(Fluids.WATER,8000));for(int i=0;i<8;i++)check(vat.addIngredient(new ItemStack(Items.HONEYCOMB)),"native honeycomb feeds source honey recipe");pulseAt(h,vat,1,()->{
        check(vat.getTank().getFluid().getFluid()==PSFluids.HONEY.get()&&vat.getTank().getFluidAmount()==8000,"native bee product becomes complete mead base");h.succeed();});
    }
    @GameTest(template="empty") public static void breakingAnyWallDropsOnePortableBasinAndEachContentOnce(GameTestHelper h){
        var vat=vat(h);var center=vat.getBlockPos();vat.getTank().replace(new FluidStack(Fluids.WATER,17500));var ingredient=new ItemStack(Items.WHEAT);ingredient.setHoverName(Component.literal("Drop wheat"));vat.addIngredient(ingredient);vat.setItem(1,new ItemStack(Items.GLASS_BOTTLE));
        var fluidOptional=h.getLevel().getBlockEntity(center.east()).getCapability(ForgeCapabilities.FLUID_HANDLER);var handle=fluidOptional.orElseThrow(AssertionError::new);
        h.getLevel().destroyBlock(center.north(),true);int basins=0,wheat=0,bottles=0;
        for(var drop:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(center).inflate(3))){var stack=drop.getItem();if(stack.is(PSItems.MASH_TUB.get())){basins+=stack.getCount();check(PSItems.MASH_TUB.get().contents(stack).getAmount()==17500,"portable basin retains full tank exactly once");}if(stack.is(Items.WHEAT)){wheat+=stack.getCount();check(stack.getHoverName().getString().equals("Drop wheat"),"drop retains ingredient data");}if(stack.is(Items.GLASS_BOTTLE))bottles+=stack.getCount();drop.discard();}
        check(basins==1&&wheat==1&&bottles==1&&!fluidOptional.isPresent()&&handle.drain(1,FluidAction.EXECUTE).isEmpty(),"one break drops each content once and invalidates cached proxy");
        for(BlockPos part:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))check(h.getLevel().getBlockState(part).isAir(),"all nine cells removed");h.succeed();
    }
    @GameTest(template="empty") public static void edgeUnloadAndOwnerReplacementNeverReviveCachedCapabilities(GameTestHelper h){
        var vat=vat(h);vat.getTank().replace(new FluidStack(Fluids.WATER,2000));var edge=(MashTubEdgeBlockEntity)h.getLevel().getBlockEntity(vat.getBlockPos().east());var old=edge.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);
        edge.invalidateCaps();edge.reviveCaps();check(old.drain(1,FluidAction.EXECUTE).isEmpty()&&edge.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new).drain(1,FluidAction.SIMULATE).getAmount()==1,"edge unload/revive creates a new handle epoch");
        var fresh=edge.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(AssertionError::new);vat.setRemoved();vat.clearRemoved();vat.reviveCaps();check(fresh.fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE)==0,"owner replacement invalidates resolved proxy references permanently");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=80) public static void nativeHoppersReachTankSlotsThroughEdgeContainer(GameTestHelper h){
        var vat=vat(h);vat.getTank().replace(new FluidStack(Fluids.WATER,125));vat.setItem(1,new ItemStack(Items.GLASS_BOTTLE));h.setBlock(POS.east().below(),Blocks.HOPPER);var hopper=(HopperBlockEntity)h.getLevel().getBlockEntity(vat.getBlockPos().east().below());
        h.runAtTickTime(40,()->{check(hopper.getItem(0).is(Items.POTION)&&vat.getItem(1).isEmpty()&&vat.getTank().isEmpty(),"native hopper extracts finished water bottle through a wall proxy");h.succeed();});
    }
    @GameTest(template="empty") public static void distilleryConnectsToWallAndRespectsStewLock(GameTestHelper h){
        var vat=vat(h);begin(vat);BlockPos source=POS.east(2);h.setBlock(source,PSBlocks.DISTILLERY.get().defaultBlockState().setValue(DistilleryBlock.FACING,Direction.WEST));var still=(DistilleryBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(source));
        var fluid=new FluidStack(PSFluids.WHEAT.get(),1000);fluid.getOrCreateTag().putInt("fermentation",2);still.getTank().replace(fluid);still.tick();check(DistilleryBlock.canConnectTo(h.getLevel(),still.getBlockPos(),Direction.WEST)&&still.getOutlet()==DistilleryBlockEntity.Outlet.BLOCKED,"native distillery sees wall owner but active stew refuses byproduct");
        pulseAt(h,vat,1,()->{vat.getTank().replace(FluidStack.EMPTY);still.tick();check(still.getOutlet()==DistilleryBlockEntity.Outlet.READY,"unlocked vat becomes actual distillery receiver");h.succeed();});
    }
    @GameTest(template="empty") public static void containedNativeFluidContactUsesForgeBodyAndEyeStateAndClearsOnExit(GameTestHelper h){
        var vat=vat(h);var center=vat.getBlockPos();var chicken=EntityType.CHICKEN.create(h.getLevel());chicken.setPos(center.getX()+.5,center.getY()+.1,center.getZ()+.5);
        for(var fluid:List.of(Fluids.WATER,Fluids.LAVA,PSFluids.WHEAT.get())){vat.getTank().replace(new FluidStack(fluid,32000));chicken.baseTick();var type=fluid==Fluids.LAVA?ForgeMod.LAVA_TYPE.get():ForgeMod.WATER_TYPE.get();check(chicken.getFluidTypeHeight(type)>0&&chicken.isEyeInFluidType(type),"contained native fluid updates Forge body and eye state");}
        chicken.setPos(center.getX()+4,center.getY()+2,center.getZ());chicken.baseTick();check(chicken.getFluidTypeHeight(ForgeMod.WATER_TYPE.get())==0&&chicken.getFluidTypeHeight(ForgeMod.LAVA_TYPE.get())==0&&!chicken.isEyeInFluidType(ForgeMod.LAVA_TYPE.get()),"native update clears pseudo-fluid state after exiting");
        check(h.getLevel().getFluidState(center).isEmpty(),"contact hooks never fabricate a flowing world block");chicken.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nativeBucketsAndBubblePacketsRespectExactCapacity(GameTestHelper h){
        var vat=vat(h);var block=PSBlocks.MASH_TUB_EDGE.get();var edge=vat.getBlockPos().east();var state=h.getLevel().getBlockState(edge);vat.getTank().replace(new FluidStack(Fluids.WATER,31500));
        check(!block.canPlaceLiquid(h.getLevel(),edge,state,Fluids.WATER)&&!block.placeLiquid(h.getLevel(),edge,state,Fluids.WATER.defaultFluidState())&&vat.getTank().getFluidAmount()==31500,"bucket requires all 1000 mB capacity and cannot lose partial contents");
        vat.getTank().replace(new FluidStack(Fluids.LAVA,31000));check(block.placeLiquid(h.getLevel(),edge,state,Fluids.LAVA.defaultFluidState())&&vat.getTank().getFluidAmount()==32000,"native liquid container fills through wall exactly");vat.tick();check(vat.getBlockState().getValue(MashTubBlock.LIGHT)==15&&h.getLevel().getBlockState(edge).getValue(MashTubBlock.LIGHT)==15,"lava luminance syncs across proxies using native light property");
        var bubble=new BubbleParticleOptions(0xff369acf,1);var buffer=new FriendlyByteBuf(Unpooled.buffer());try{bubble.writeToNetwork(buffer);var restored=BubbleParticleOptions.DESERIALIZER.fromNetwork(PSParticles.BUBBLE.get(),buffer);check(restored.getType()==PSParticles.BUBBLE.get()&&restored.getColor().equals(bubble.getColor()),"native custom bubble packet retains actual type and color");}finally{buffer.release();}h.succeed();
    }
}
