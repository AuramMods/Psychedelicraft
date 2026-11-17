package ivorius.psychedelicraft.gametest;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.*;
import ivorius.psychedelicraft.recipe.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class LatticeGameTests {
    private static final BlockPos POS = new BlockPos(2,2,2);
    private static void check(boolean value,String text){if(!value)throw new GameTestAssertException(text);}
    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"VineTest"));player.setPos(Vec3.atCenterOf(h.absolutePos(POS)).add(2,0,2));player.getInventory().clearContent();player.setShiftKeyDown(false);return player;}
    private static String residentChunkStatus(GameTestHelper h,BlockPos pos){var chunk=h.getLevel().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?"not resident":chunk.getFullStatus().toString();}
    private static BlockHitResult hit(BlockPos pos){return new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);}
    private static CraftingContainer grid(){return new TransientCraftingContainer(new AbstractContainerMenu(null,-1){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);}
    private static List<ItemEntity> drops(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(POS)).inflate(3));}
    private static ItemStack extract(int amount,int distillation){var fluid=new FluidStack(PSFluids.MORNING_GLORY_EXTRACT.get(),amount);fluid.getOrCreateTag().putInt("distillation",distillation);fluid.getOrCreateTag().putString("batch","vine-test");var stack=FluidVesselHandler.forCrafting(new ItemStack(PSItems.BOTTLE.get())).replaceContents(fluid);stack.setHoverName(Component.literal("Named concentrate"));stack.getOrCreateTag().putInt("custom",19);return stack;}
    @GameTest(template="empty") public static void nativeConnectionsRotateMirrorAndRetainAxis(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);check(!PSBlocks.LATTICE.get().defaultBlockState().getValue(LatticeBlock.NORTH)&&!PSBlocks.LATTICE.get().defaultBlockState().getValue(LatticeBlock.SOUTH),"default lattice has only east/west links");h.setBlock(POS,PSBlocks.LATTICE.get());h.setBlock(POS.north(),Blocks.STONE);h.setBlock(POS.south(),Blocks.STONE);
        var state=level.getBlockState(pos);check(state.getValue(LatticeBlock.NORTH)&&state.getValue(LatticeBlock.SOUTH)&&!state.getValue(LatticeBlock.EAST),"two opposite solid faces select north/south");
        h.setBlock(POS.north(),Blocks.AIR);h.setBlock(POS.south(),Blocks.AIR);state=level.getBlockState(pos);check(state.getValue(LatticeBlock.NORTH)&&state.getValue(LatticeBlock.SOUTH),"removing supports retains previous axis");
        var rotated=state.rotate(Rotation.CLOCKWISE_90);check(rotated.getValue(LatticeBlock.EAST)&&rotated.getValue(LatticeBlock.WEST)&&!rotated.getValue(LatticeBlock.NORTH),"native rotation remaps links");
        var arm=state.setValue(LatticeBlock.SOUTH,false);check(arm.mirror(Mirror.LEFT_RIGHT).getValue(LatticeBlock.SOUTH),"native mirror remaps asymmetric links");h.succeed();
    }
    @GameTest(template="empty") public static void nativeWaterBucketsAndGrapePlantingRetainConnectionsAndWater(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var block=PSBlocks.LATTICE.get();h.setBlock(POS,block);var state=level.getBlockState(pos);
        check(block.placeLiquid(level,pos,state,Fluids.WATER.defaultFluidState()),"native bucket waterlogging accepts source water");var wet=level.getBlockState(pos);check(wet.getValue(LatticeBlock.WATERLOGGED)&&wet.getFluidState().isSource(),"native fluid state is a water source");
        var player=player(h);var grapes=new ItemStack(PSItems.WINE_GRAPES.get(),5);player.setItemInHand(InteractionHand.MAIN_HAND,grapes);check(grapes.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(pos))).consumesAction(),"normal grape use plants lattice");
        var planted=level.getBlockState(pos);check(planted.is(PSBlocks.WINE_GRAPE_LATTICE.get())&&planted.getValue(LatticeBlock.WATERLOGGED)&&!planted.getValue(LatticeCropBlock.PERSISTENT)&&grapes.getCount()==5,"planting preserves source water and planting grapes");
        check(planted.getValue(LatticeBlock.EAST)==wet.getValue(LatticeBlock.EAST)&&planted.getValue(LatticeBlock.NORTH)==wet.getValue(LatticeBlock.NORTH),"conversion retains connection mask");
        check(PSBlocks.WINE_GRAPE_LATTICE.get().pickupBlock(level,pos,planted).is(Items.WATER_BUCKET)&&!level.getBlockState(pos).getValue(LatticeBlock.WATERLOGGED),"native bucket pickup removes only water");h.succeed();
    }
    @GameTest(template="empty") public static void stemGrowsOncePerTickAndDiesWhenSoilIsRemoved(GameTestHelper h){
        h.setBlock(POS.below(),Blocks.DIRT);h.setBlock(POS,PSBlocks.MORNING_GLORY.get());h.setBlock(POS.north(),PSBlocks.LATTICE.get().defaultBlockState().setValue(LatticeBlock.WATERLOGGED,true));h.setBlock(POS.east(),PSBlocks.LATTICE.get());
        var level=h.getLevel();var pos=h.absolutePos(POS);var stem=PSBlocks.MORNING_GLORY.get();stem.randomTick(level.getBlockState(pos),level,pos,level.random);
        check(level.getBlockState(pos).getValue(VineStemBlock.AGE)==1&&h.getBlockState(POS.north()).is(PSBlocks.MORNING_GLORY_LATTICE.get())&&h.getBlockState(POS.east()).is(PSBlocks.LATTICE.get()),"one stem tick ages and colonizes first square target only");
        check(h.getBlockState(POS.north()).getValue(LatticeBlock.WATERLOGGED)&&h.getBlockState(POS).getValue(LatticeBlock.NORTH),"colonization preserves water and updates stem connection geometry");
        h.setBlock(POS.below(),Blocks.STONE);check(h.getBlockState(POS).isAir()&&drops(h).stream().anyMatch(item->item.getItem().is(PSItems.MORNING_GLORY.get())),"native survival removes unsupported stem and drops flower");drops(h).forEach(ItemEntity::discard);h.succeed();
    }
    @GameTest(template="empty") public static void morningGloryRootsAndDecaysWhileGrapesRemainIndependent(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var crop=PSBlocks.MORNING_GLORY_LATTICE.get();h.setBlock(POS,crop.defaultBlockState().setValue(LatticeCropBlock.AGE,3));h.setBlock(POS.north().below(),Blocks.DIRT);h.setBlock(POS.north(),PSBlocks.MORNING_GLORY.get());
        check(crop.isRooted(level,level.getBlockState(pos),pos),"stem on a free horizontal face roots morning glory");h.setBlock(POS.north(),Blocks.AIR);
        for(int age=2;age>=0;age--){crop.randomTick(level.getBlockState(pos),level,pos,level.random);check(level.getBlockState(pos).getValue(LatticeCropBlock.AGE)==age,"unrooted vines decay one stage per random tick");}
        check(!crop.isRooted(level,level.getBlockState(pos),pos),"closed root search terminates without stem");check(PSBlocks.WINE_GRAPE_LATTICE.get().isRooted(level,PSBlocks.WINE_GRAPE_LATTICE.get().defaultBlockState(),pos),"grapes never require a stem");h.succeed();
    }
    @GameTest(template="empty") public static void stemSearchHonorsTwentyLevelsAndNeverReadsUnloadedNeighbors(GameTestHelper h){
        var crop=PSBlocks.MORNING_GLORY_LATTICE.get();var origin=new BlockPos(0,80,0);Map<BlockPos,BlockState> blocks=new HashMap<>();boolean[] unloaded={false};
        for(int x=0;x<=20;x++)blocks.put(origin.east(x),crop.defaultBlockState().setValue(LatticeBlock.NORTH,false).setValue(LatticeBlock.SOUTH,false).setValue(LatticeBlock.EAST,true).setValue(LatticeBlock.WEST,true));
        var view=(net.minecraft.world.level.LevelReader)java.lang.reflect.Proxy.newProxyInstance(LatticeGameTests.class.getClassLoader(),new Class<?>[]{net.minecraft.world.level.LevelReader.class},(proxy,method,args)->{
            if(method.getName().equals("hasChunkAt"))return !unloaded[0]||((BlockPos)args[0]).getX()<=0;
            if(method.getName().equals("getBlockState")){var pos=(BlockPos)args[0];check(!unloaded[0]||pos.getX()<=0,"root search never forces an unloaded block-state read");return blocks.getOrDefault(pos,Blocks.AIR.defaultBlockState());}
            throw new AssertionError("Unexpected root-search read "+method.getName());
        });
        blocks.put(origin.east(20).north(),PSBlocks.MORNING_GLORY.get().defaultBlockState());check(!crop.isRooted(view,crop.defaultBlockState(),origin),"stem beyond twenty BFS levels cannot root the vine");
        blocks.put(origin.east(19).north(),PSBlocks.MORNING_GLORY.get().defaultBlockState());check(crop.isRooted(view,crop.defaultBlockState(),origin),"stem at the final permitted level roots the vine; default links="+crop.defaultBlockState());
        unloaded[0]=true;check(!crop.isRooted(view,crop.defaultBlockState(),origin),"unloaded neighbor chain is skipped without loading it");h.succeed();
    }
    @GameTest(template="empty") public static void bonemealSpreadsAndDecorativeStagesStayFrozen(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var crop=PSBlocks.WINE_GRAPE_LATTICE.get();h.setBlock(POS,crop);h.setBlock(POS.east().above(),PSBlocks.LATTICE.get().defaultBlockState().setValue(LatticeBlock.WATERLOGGED,true));
        crop.performBonemeal(level,level.random,pos,level.getBlockState(pos));check(level.getBlockState(pos).getValue(LatticeCropBlock.AGE)==3,"bonemeal matures natural vine");crop.performBonemeal(level,level.random,pos,level.getBlockState(pos));check(h.getBlockState(POS.east().above()).is(crop)&&h.getBlockState(POS.east().above()).getValue(LatticeBlock.WATERLOGGED),"mature bonemeal colonizes linked upper lattice preserving water");
        for(int age=0;age<4;age++){var decorative=crop.defaultBlockState().setValue(LatticeCropBlock.AGE,age).setValue(LatticeCropBlock.PERSISTENT,true);level.setBlock(pos,decorative,2);crop.randomTick(decorative,level,pos,level.random);crop.performBonemeal(level,level.random,pos,decorative);check(!crop.isRandomlyTicking(decorative)&&!crop.isValidBonemealTarget(level,pos,decorative,false)&&level.getBlockState(pos)==decorative,"persistent decorative stage ignores random ticks and bonemeal");}
        h.succeed();
    }
    @GameTest(template="empty") public static void normalGrowthHonorsForgeCropEvents(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var crop=PSBlocks.WINE_GRAPE_LATTICE.get();h.setBlock(POS,crop);h.setBlock(POS.above(),Blocks.SEA_LANTERN);h.runAtTickTime(5,()->{int[] posts={0};Object force=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void allowCropGrowth(BlockEvent.CropGrowEvent.Pre event){if(event.getPos().equals(pos))event.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);}@net.minecraftforge.eventbus.api.SubscribeEvent public void post(BlockEvent.CropGrowEvent.Post event){if(event.getPos().equals(pos))posts[0]++;}};
        MinecraftForge.EVENT_BUS.register(force);try{crop.randomTick(level.getBlockState(pos),level,pos,level.random);}finally{MinecraftForge.EVENT_BUS.unregister(force);}check(level.getBlockState(pos).getValue(LatticeCropBlock.AGE)==1&&posts[0]==1,"normal tick age="+level.getBlockState(pos).getValue(LatticeCropBlock.AGE)+" post="+posts[0]+" light="+level.getRawBrightness(pos,0));
        Object deny=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void denyCropGrowth(BlockEvent.CropGrowEvent.Pre event){if(event.getPos().equals(pos))event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);}};MinecraftForge.EVENT_BUS.register(deny);try{for(int i=0;i<50;i++)crop.randomTick(level.getBlockState(pos),level,pos,level.random);}finally{MinecraftForge.EVENT_BUS.unregister(deny);}check(level.getBlockState(pos).getValue(LatticeCropBlock.AGE)==1,"Forge denial prevents even a naturally successful growth roll");h.succeed();});
    }
    @GameTest(template="empty") public static void shearsHarvestExactCropYieldAndResetStageWithoutDuplication(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var player=player(h);
        for(var crop:List.of(PSBlocks.WINE_GRAPE_LATTICE.get(),PSBlocks.MORNING_GLORY_LATTICE.get())){var state=crop.defaultBlockState().setValue(LatticeCropBlock.AGE,3).setValue(LatticeBlock.WATERLOGGED,true);h.setBlock(POS,state);var tool=new ItemStack(Items.SHEARS);player.setItemInHand(InteractionHand.MAIN_HAND,tool);check(crop.use(state,level,pos,player,InteractionHand.MAIN_HAND,hit(pos)).consumesAction(),"mature natural crop accepts shears");var items=drops(h);com.mojang.logging.LogUtils.getLogger().info("LATTICE_HARVEST_STATE crop={} time={} pos={} status={} items={} toolDamage={} creative={}",crop,level.getGameTime(),pos,residentChunkStatus(h,pos),items.stream().limit(10).map(e->e.getItem()+"/age="+e.getAge()+"/uuid="+e.getUUID()+"/pos="+e.position()).toList(),tool.getDamageValue(),player.isCreative());check(items.size()==1&&items.get(0).getItem().getCount()>=3&&items.get(0).getItem().getCount()<=4&&tool.getDamageValue()==1,"one harvest yields three/four produce and one tool wear");check(items.get(0).getItem().is(crop==PSBlocks.WINE_GRAPE_LATTICE.get()?PSItems.WINE_GRAPES.get():PSItems.MORNING_GLORY.get()),"harvest yields crop-specific produce");var next=level.getBlockState(pos);check(next.getValue(LatticeCropBlock.AGE)==(crop==PSBlocks.WINE_GRAPE_LATTICE.get()?1:2)&&next.getValue(LatticeBlock.WATERLOGGED),"shearing preserves water and resets correct crop stage");check(!crop.use(next,level,pos,player,InteractionHand.MAIN_HAND,hit(pos)).consumesAction()&&tool.getDamageValue()==1,"immediate retry cannot harvest or wear tools twice");items.forEach(ItemEntity::discard);state=state.setValue(LatticeCropBlock.PERSISTENT,true);h.setBlock(POS,state);check(!crop.use(state,level,pos,player,InteractionHand.MAIN_HAND,hit(pos)).consumesAction()&&drops(h).isEmpty(),"decorative mature stage cannot be farmed");}h.succeed();
    }
    @GameTest(template="empty") public static void nativeSilkLootAndPickRetainAllFourDecorativeStages(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var player=player(h);var silk=new ItemStack(Items.DIAMOND_AXE);silk.enchant(Enchantments.SILK_TOUCH,1);
        for(var crop:List.of(PSBlocks.WINE_GRAPE_LATTICE.get(),PSBlocks.MORNING_GLORY_LATTICE.get()))for(int age=0;age<4;age++){var state=crop.defaultBlockState().setValue(LatticeCropBlock.AGE,age);h.setBlock(POS,state);var loot=Block.getDrops(state,level,pos,null,player,silk);check(loot.size()==1&&loot.get(0).is(crop.asItem())&&LatticeCropItem.age(loot.get(0))==age,"native silk loot copies growth into BlockStateTag");var picked=crop.getCloneItemStack(level,pos,state);check(LatticeCropItem.age(picked)==age,"native pick carries same stage");var placement=crop.getStateForPlacement(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,picked,hit(pos)));check(placement.getValue(LatticeCropBlock.AGE)==age&&placement.getValue(LatticeCropBlock.PERSISTENT),"placing picked/silk crop creates frozen decorative stage");var normal=Block.getDrops(state,level,pos,null,player,ItemStack.EMPTY);check(normal.size()==1&&normal.get(0).getCount()==1&&(normal.get(0).is(PSItems.LATTICE.get())||normal.get(0).is(crop==PSBlocks.WINE_GRAPE_LATTICE.get()?PSItems.WINE_GRAPES.get():PSItems.MORNING_GLORY.get())),"ordinary loot returns exactly lattice or produce");}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60) public static void nativePistonMovesAgeAndPersistenceWithoutLegacyTileEntities(GameTestHelper h){
        var pos=h.absolutePos(POS);var crop=PSBlocks.MORNING_GLORY_LATTICE.get();h.setBlock(POS,crop.defaultBlockState().setValue(LatticeCropBlock.AGE,2).setValue(LatticeCropBlock.PERSISTENT,true).setValue(LatticeBlock.WATERLOGGED,true));h.setBlock(POS.west(),Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST));h.setBlock(POS.west().below(),Blocks.REDSTONE_BLOCK);
        h.runAtTickTime(12,()->{var moved=h.getLevel().getBlockState(pos.east());check(moved.is(crop)&&moved.getValue(LatticeCropBlock.AGE)==2&&moved.getValue(LatticeCropBlock.PERSISTENT),"actual native piston retains decorative growth state");check(!moved.getValue(LatticeBlock.WATERLOGGED),"native piston releases water instead of moving a source inside the crop");check(h.getLevel().getBlockEntity(pos.east())==null,"finished movement needs no crop block entity");h.succeed();});
    }
    @GameTest(template="empty") public static void nativePotAndSuspiciousStewRecognizeMorningGlory(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);var player=player(h);h.setBlock(POS,Blocks.FLOWER_POT);var seeds=new ItemStack(PSItems.MORNING_GLORY_SEEDS.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,seeds);check(Blocks.FLOWER_POT.use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos)).consumesAction()&&level.getBlockState(pos).is(PSBlocks.POTTED_MORNING_GLORY.get())&&seeds.getCount()==1,"native empty pot consumes one seed to create potted stem");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);PSBlocks.POTTED_MORNING_GLORY.get().use(level.getBlockState(pos),level,pos,player,InteractionHand.MAIN_HAND,hit(pos));check(level.getBlockState(pos).is(Blocks.FLOWER_POT)&&player.getMainHandItem().is(PSItems.MORNING_GLORY_SEEDS.get()),"native pot removal recovers its plant item");
        var grid=grid();grid.setItem(0,new ItemStack(Items.BROWN_MUSHROOM));grid.setItem(1,new ItemStack(Items.RED_MUSHROOM));grid.setItem(2,new ItemStack(Items.BOWL));grid.setItem(3,new ItemStack(PSItems.MORNING_GLORY_SEEDS.get()));var recipe=new SuspiciousStewRecipe(Psychedelicraft.id("test_stew"),CraftingBookCategory.MISC);check(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(Items.SUSPICIOUS_STEW),"native suspicious stew uses the actual FlowerBlock");check(PSBlocks.MORNING_GLORY.get().getSuspiciousEffect()==net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN&&PSBlocks.MORNING_GLORY.get().getEffectDuration()==100,"native flower retains source five-second stew effect");h.succeed();
    }
    @GameTest(template="empty") public static void extractAndLsdPaperRecipesConserveFluidDataAndNativeRemainders(GameTestHelper h){
        var level=h.getLevel();var fill=(FillReceptacleRecipe)level.getRecipeManager().byKey(Psychedelicraft.id("fluids/holder_with_morning_glory_extract")).orElseThrow();var grid=grid();grid.setItem(0,new ItemStack(PSItems.BOTTLE.get()));grid.setItem(1,new ItemStack(Items.WATER_BUCKET));for(int i=2;i<5;i++)grid.setItem(i,new ItemStack(i==4?PSItems.MORNING_GLORY_SEEDS.get():PSItems.MORNING_GLORY.get()));check(fill.matches(grid,level),"source flower/seed tag fills extract");var raw=fill.assemble(grid,level.registryAccess());check(PSItems.BOTTLE.get().contents(raw).getAmount()==500&&fill.getRemainingItems(grid).get(1).is(Items.BUCKET),"extract recipe makes 500 mB and returns water bucket");
        var paper=(FluidAwareShapelessRecipe)level.getRecipeManager().byKey(Psychedelicraft.id("lsa_square")).orElseThrow();grid=grid();var original=extract(17,2);var snapshot=original.copy();grid.setItem(2,new ItemStack(Items.PAPER));grid.setItem(7,original);
        for(int retry=0;retry<8;retry++){check(paper.matches(grid,level)&&paper.assemble(grid,level.registryAccess()).is(PSItems.LSA_SQUARE.get()),"concentrated extract matches LSD paper");var remainder=paper.getRemainingItems(grid).get(7);var contents=PSItems.BOTTLE.get().contents(remainder);check(contents.getAmount()==16&&contents.getTag().getInt("distillation")==2&&contents.getTag().getString("batch").equals("vine-test")&&remainder.getTag().getInt("custom")==19&&remainder.getHoverName().getString().equals("Named concentrate"),"one craft drains exactly 1 mB and keeps vessel/fluid metadata");check(ItemStack.matches(original,snapshot),"recipe preview and remainder retries leave input unchanged");}
        grid.setItem(7,extract(1,2));var empty=paper.getRemainingItems(grid).get(7);check(empty.is(PSItems.BOTTLE.get())&&PSItems.BOTTLE.get().contents(empty).isEmpty()&&empty.getTag().getInt("custom")==19,"final millibucket returns same named empty bottle");grid.setItem(7,extract(17,1));check(!paper.matches(grid,level),"insufficient distillation refuses paper");grid.setItem(7,raw);check(!paper.matches(grid,level),"raw extract refuses paper");h.succeed();
    }
    @GameTest(template="empty") public static void nativeMachineWithdrawalAcceptsDrinkableAndInjectableMorningGlory(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(POS);h.setBlock(POS,PSBlocks.FLASK.get());var tank=((ivorius.psychedelicraft.block.entity.FlaskBlockEntity)level.getBlockEntity(pos)).getTank();var fluid=new FluidStack(PSFluids.MORNING_GLORY_EXTRACT.get(),125);fluid.getOrCreateTag().putInt("distillation",2);tank.replace(fluid);var player=player(h);var bottle=new ItemStack(PSItems.BOTTLE.get());bottle.setHoverName(Component.literal("Concentrate bottle"));bottle.getOrCreateTag().putInt("custom",81);player.setItemInHand(InteractionHand.MAIN_HAND,bottle);
        check(PSItems.BOTTLE.get().onItemUseFirst(bottle,new UseOnContext(player,InteractionHand.MAIN_HAND,hit(pos))).consumesAction(),"normal machine item-first interaction accepts dual-use flower extract");var filled=player.getMainHandItem();check(tank.getFluidAmount()==0&&PSItems.BOTTLE.get().contents(filled).getAmount()==125&&PSItems.BOTTLE.get().contents(filled).getTag().getInt("distillation")==2&&filled.getTag().getInt("custom")==81&&filled.getHoverName().getString().equals("Concentrate bottle"),"withdrawal conserves concentration, quantity and vessel metadata");
        var own=FluidVesselHandler.forCrafting(new ItemStack(PSItems.BOTTLE.get()));check(own.fill(new FluidStack(PSFluids.COCAINE.get(),10),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)==0,"exclusive injectable drugs remain excluded from drinking vessels");var syringe=FluidVesselHandler.forCrafting(new ItemStack(PSItems.SYRINGE.get()));check(syringe.fill(fluid,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)==10,"morning glory remains injectable as well");h.succeed();
    }
    @GameTest(template="empty") public static void overlappingFluidIngredientsUseOneConsistentAssignmentAndPacketRoundTrip(GameTestHelper h){
        var json=JsonParser.parseString("{\"ingredients\":[{\"item\":\"psychedelicraft:bottle\"},{\"item\":\"psychedelicraft:bottle\",\"fluid\":{\"fluid\":\"psychedelicraft:morning_glory_extract\",\"level\":3,\"attributes\":{\"distillation\":2}}}],\"result\":{\"item\":\"minecraft:paper\"}}").getAsJsonObject();var serializer=new FluidAwareShapelessRecipe.Serializer();var recipe=serializer.fromJson(Psychedelicraft.id("overlap"),json);var grid=grid();grid.setItem(0,extract(10,2));grid.setItem(4,new ItemStack(PSItems.BOTTLE.get()));check(recipe.matches(grid,h.getLevel()),"matcher backtracks ordinary vessel away from restricted filled vessel");var remaining=recipe.getRemainingItems(grid);check(PSItems.BOTTLE.get().contents(remaining.get(0)).getAmount()==7&&remaining.get(4).isEmpty(),"same assignment drains exactly the restricted vessel");
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{serializer.toNetwork(buffer,recipe);var restored=serializer.fromNetwork(recipe.getId(),buffer);check(restored.matches(grid,h.getLevel())&&ItemStack.matches(remaining.get(0),restored.getRemainingItems(grid).get(0)),"native packet round trip retains fluid restrictions and exact remainder");}finally{buffer.release();}grid.setItem(8,new ItemStack(Items.DIRT));check(!recipe.matches(grid,h.getLevel()),"unrelated extra inputs cannot be silently consumed");h.succeed();
    }
}
