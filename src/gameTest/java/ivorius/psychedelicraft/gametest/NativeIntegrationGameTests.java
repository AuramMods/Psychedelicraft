package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.*;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.stream.Collectors;

/** Native replacements are verified at their gameplay handoffs, not just by registry presence. */
@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class NativeIntegrationGameTests {
    static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    static ServerPlayer player(GameTestHelper h){var p=AdvancementGameTests.player(h);p.getInventory().clearContent();return p;}
    static BlockHitResult hit(BlockPos p){return new BlockHitResult(Vec3.atCenterOf(p).add(0,.5,0),Direction.UP,p,false);}
    static void use(GameTestHelper h,BlockPos p,ServerPlayer player){check(h.getLevel().getBlockState(p).use(h.getLevel(),player,InteractionHand.MAIN_HAND,hit(p)).consumesAction(),"native block use accepted");}
    static final List<Runnable> OUTDOOR_CLEANUP=new ArrayList<>();
    static BlockPos outdoor(GameTestHelper h,int x,int z){var p=h.absolutePos(new BlockPos(x,0,z));int y=96;for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)y=Math.max(y,h.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING,p.getX()+dx,p.getZ()+dz)+6);return new BlockPos(p.getX(),y,p.getZ());}
    static void cleanup(){OUTDOOR_CLEANUP.forEach(Runnable::run);OUTDOOR_CLEANUP.clear();}
    static Set<UUID> items(ServerLevel level,AABB box){return level.getEntitiesOfClass(ItemEntity.class,box).stream().map(Entity::getUUID).collect(Collectors.toSet());}
    static MashTubBlockEntity vat(GameTestHelper h){var pos=new BlockPos(2,1,2);h.setBlock(pos,PSBlocks.MASH_TUB.get());for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0)h.setBlock(pos.offset(x,0,z),MashTubBlock.wallState(x,z));return (MashTubBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
    @BeforeBatch(batch="native_flowers") public static void clearFlowerWeather(ServerLevel level){level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);level.setRainLevel(0);level.setThunderLevel(0);}
    @AfterBatch(batch="native_flowers") public static void cleanupFlowers(ServerLevel level){cleanup();}
    @AfterBatch(batch="native_trident") public static void restoreWeather(ServerLevel level){clearFlowerWeather(level);cleanup();}

    @GameTest(template="dealer_work",batch="native_flowers",timeoutTicks=850)
    public static void nativeBeePollinatesTheModMorningGlory(GameTestHelper h){
        var flower=outdoor(h,3,3);var cage=new ArrayList<BlockPos>();
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++)if(y==-1||y==2||Math.abs(x)==2||Math.abs(z)==2){var pos=flower.offset(x,y,z);h.getLevel().setBlock(pos,(y==-1?Blocks.GRASS_BLOCK:Blocks.GLASS).defaultBlockState(),3);cage.add(pos);}
        h.getLevel().setBlock(flower,PSBlocks.MORNING_GLORY.get().defaultBlockState(),3);
        Bee bee=EntityType.BEE.create(h.getLevel());bee.moveTo(flower.getX()+.5,flower.getY()+1.2,flower.getZ()+.5,0,0);bee.setPersistenceRequired();h.getLevel().addFreshEntity(bee);bee.setSavedFlowerPos(flower);OUTDOOR_CLEANUP.add(()->{bee.discard();h.getLevel().removeBlock(flower,false);cage.forEach(pos->h.getLevel().removeBlock(pos,false));});
        h.onEachTick(()->{if(h.getTick()%100==0)LogUtils.getLogger().info("NATIVE_BEE_STATE testTick={} entityTick={} pos={} flower={} nectar={} noAi={} rain={} night={} flowerTag={} nbt={}",h.getTick(),bee.tickCount,bee.position(),bee.getSavedFlowerPos(),bee.hasNectar(),bee.isNoAi(),h.getLevel().isRaining(),h.getLevel().isNight(),h.getLevel().getBlockState(flower).is(net.minecraft.tags.BlockTags.FLOWERS),bee.saveWithoutId(new CompoundTag()));});
        h.succeedWhen(()->{check(bee.hasNectar(),"native pollination goal collected nectar from the registered mod flower");check(flower.equals(bee.getSavedFlowerPos()),"native bee retained the actual morning-glory position");LogUtils.getLogger().info("NATIVE_INTEGRATION bee morning_glory nectar=true ticks={}",bee.tickCount);});
    }
    @GameTest(template="empty") public static void brownMooshroomUsesTheNativeMorningGloryStewEffect(GameTestHelper h){
        var p=player(h);var cow=h.spawn(EntityType.MOOSHROOM,new BlockPos(2,1,2));cow.setNoAi(true);cow.setVariant(MushroomCow.MushroomType.BROWN);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.MORNING_GLORY_SEEDS.get(),2));check(cow.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction()&&p.getMainHandItem().getCount()==1,"native brown mooshroom consumes one tagged mod flower");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL));check(cow.mobInteract(p,InteractionHand.MAIN_HAND).consumesAction()&&p.getMainHandItem().is(Items.SUSPICIOUS_STEW),"native milk result is suspicious stew");
        p.getFoodData().setFoodLevel(10);var bowl=p.getMainHandItem().finishUsingItem(h.getLevel(),p);check(bowl.is(Items.BOWL)&&p.getEffect(MobEffects.DIG_SLOWDOWN)!=null&&p.getEffect(MobEffects.DIG_SLOWDOWN).getDuration()==100,"native consumption supplies the source flower's five-second effect and bowl");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOWL));cow.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().is(Items.MUSHROOM_STEW),"native milking clears the stored flower effect");h.succeed();
    }
    @GameTest(template="dealer_work",timeoutTicks=300) public static void nativeHiveHoneycombFlowsThroughBlockUseIntoMead(GameTestHelper h){
        var level=h.getLevel();var p=player(h);var hive=h.absolutePos(new BlockPos(5,2,5));level.setBlock(hive.below(),Blocks.CAMPFIRE.defaultBlockState(),3);level.setBlock(hive,Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL,5),3);
        var box=new AABB(hive).inflate(2);var old=items(level,box);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));use(h,hive,p);
        var drops=level.getEntitiesOfClass(ItemEntity.class,box,e->!old.contains(e.getUUID())&&e.getItem().is(Items.HONEYCOMB));check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==3&&p.getMainHandItem().getDamageValue()==1&&level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL)==0,"native shearing produces three honeycomb, wears shears and empties the nest");
        for(var drop:drops){drop.setNoPickUpDelay();drop.playerTouch(p);}check(p.getInventory().countItem(Items.HONEYCOMB)==3,"native pickup receives all three harvested ingredients");
        var tub=vat(h);tub.getTank().fill(new FluidStack(Fluids.WATER,8000),FluidAction.EXECUTE);
        for(int i=0;i<3;i++){int slot=p.getInventory().findSlotMatchingItem(new ItemStack(Items.HONEYCOMB));var one=p.getInventory().removeItem(slot,1);p.setItemInHand(InteractionHand.MAIN_HAND,one);use(h,tub.getBlockPos(),p);check(p.getMainHandItem().isEmpty(),"block interaction consumes one harvested honeycomb");}
        check(tub.getSuppliedIngredients().size()==3,"all native harvested honeycomb reached the recipe");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.HONEYCOMB,5));for(int i=0;i<5;i++)use(h,tub.getBlockPos(),p);check(p.getMainHandItem().isEmpty()&&tub.isStewing(),"remaining fixture ingredients complete the native hand-fed batch");
        h.succeedWhen(()->check(!tub.isStewing()&&tub.getTank().getFluid().getFluid()==PSFluids.HONEY.get()&&tub.getTank().getFluidAmount()==8000,"real processing ticks finish the eight-litre mead base"));
    }
    @GameTest(template="dealer_work") public static void nativeHoneyBottlePoursIntoAnEmptyMashTubAndReturnsGlass(GameTestHelper h){
        var level=h.getLevel();var p=player(h);var hive=h.absolutePos(new BlockPos(5,2,5));level.setBlock(hive.below(),Blocks.CAMPFIRE.defaultBlockState(),3);level.setBlock(hive,Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL,5),3);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE));use(h,hive,p);check(p.getMainHandItem().is(Items.HONEY_BOTTLE)&&level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL)==0,"native bottle harvest is used as the actual vessel");
        var tub=vat(h);use(h,tub.getBlockPos(),p);check(p.getMainHandItem().is(Items.GLASS_BOTTLE)&&tub.getTank().getFluidAmount()==125&&tub.getTank().getFluid().getFluid()==PSFluids.HONEY.get(),"normal mod block use transfers 125 mB and returns the native glass bottle");h.succeed();
    }
    @GameTest(template="dealer_work",timeoutTicks=180) public static void nativeSnifferDigLootCanBeCollectedByThePaperBag(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(0,0,0,7,0,7))h.setBlock(pos,Blocks.MOSS_BLOCK);
        var sniffer=h.spawn(EntityType.SNIFFER,new BlockPos(3,1,3));sniffer.setNoAi(true);sniffer.setNoGravity(true);var box=sniffer.getBoundingBox().inflate(6);var old=items(h.getLevel(),box);sniffer.transitionTo(Sniffer.State.DIGGING);
        h.runAfterDelay(155,()->{var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!old.contains(e.getUUID())&&(e.getItem().is(Items.TORCHFLOWER_SEEDS)||e.getItem().is(Items.PITCHER_POD)));check(drops.size()==1&&drops.get(0).getItem().getCount()==1,"native digging state emitted one seed from the actual gameplay loot table");
            var drop=drops.get(0);Item kind=drop.getItem().getItem();var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.PAPER_BAG.get()));var below=drop.blockPosition().below();var point=new Vec3(drop.getX(),below.getY()+1,drop.getZ());
            LogUtils.getLogger().info("NATIVE_SEED_PICKUP pos={} ground={} click={} below={}",drop.position(),drop.onGround(),point,h.getLevel().getBlockState(below));var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(point,Direction.UP,below,false)));check(result.consumesAction()&&drop.isRemoved(),"normal paper-bag block use collected the native seed entity");
            var saved=ItemStack.of(p.getMainHandItem().save(new CompoundTag()));var contents=PaperBagItem.getContents(saved);check(contents.count()==1&&contents.peek(1).is(kind),"native seed identity and amount survive the real bag save format");LogUtils.getLogger().info("NATIVE_INTEGRATION sniffer seed={} bag_count={}",BuiltInRegistries.ITEM.getKey(kind),contents.count());h.succeed();});
    }
    @GameTest(template="dealer_work",timeoutTicks=160) public static void nativeBrushUseProducesWarmOceanArchaeologyLoot(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,1,3));h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);h.getLevel().setBlock(pos,Blocks.SUSPICIOUS_SAND.defaultBlockState(),3);var block=(BrushableBlockEntity)h.getLevel().getBlockEntity(pos);block.setLootTable(BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY,2);
        var p=player(h);p.moveTo(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5,0,90);p.xRotO=90;p.yRotO=0;var brush=new ItemStack(Items.BRUSH);p.setItemInHand(InteractionHand.MAIN_HAND,brush);check(brush.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit(pos))).consumesAction()&&p.isUsingItem(),"native brush starts from its actual block ray through useOn");var box=new AABB(pos).inflate(2);var old=items(h.getLevel(),box);
        h.onEachTick(()->{int remaining=brush.getUseDuration()-(int)h.getTick();if(h.getLevel().getBlockState(pos).is(Blocks.SUSPICIOUS_SAND))brush.getItem().onUseTick(h.getLevel(),p,brush,remaining);});
        h.succeedWhen(()->{check(h.getLevel().getBlockState(pos).is(Blocks.SAND),"ten normal native brush strokes complete excavation");var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->!old.contains(e.getUUID()));check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1&&brush.getDamageValue()==1,"native warm-ocean loot is emitted once and the brush wears once");LogUtils.getLogger().info("NATIVE_INTEGRATION archaeology native_loot={}",drops.get(0).getItem());});
    }
    @GameTest(template="dealer_work",batch="native_trident",timeoutTicks=100) public static void nativeThrownChannelingTridentAwardsBothVanillaAdvancements(GameTestHelper h){
        var level=h.getLevel();level.setDayTime(6000);level.setWeatherParameters(0,1000,true,true);level.setRainLevel(1);level.setThunderLevel(1);var p=player(h);var targetPos=outdoor(h,3,5);var start=new BlockPos(targetPos.getX(),targetPos.getY(),targetPos.getZ()-4);for(var pos:List.of(start.below(),targetPos.below()))level.setBlock(pos,Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);p.moveTo(start.getX()+.5,start.getY(),start.getZ()+.5,0,0);
        Villager target=EntityType.VILLAGER.create(level);target.moveTo(targetPos.getX()+.5,targetPos.getY(),targetPos.getZ()+.5,0,0);level.addFreshEntity(target);target.setNoAi(true);OUTDOOR_CLEANUP.add(()->{for(var entity:level.getEntitiesOfClass(Entity.class,new AABB(start,targetPos).inflate(5)))entity.discard();for(var pos:List.of(start.below(),targetPos.below()))level.removeBlock(pos,false);});target.setNoGravity(true);target.setInvulnerable(false);var stack=new ItemStack(Items.TRIDENT);stack.enchant(Enchantments.CHANNELING,1);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        for(String name:List.of("adventure/throw_trident","adventure/very_very_frightening")){var advancement=level.getServer().getAdvancements().getAdvancement(new ResourceLocation("minecraft",name));check(!p.getAdvancements().getOrStartProgress(advancement).isDone(),"native criterion starts unawarded "+name);}
        h.onEachTick(()->{if(h.getTick()%10==0)LogUtils.getLogger().info("NATIVE_TRIDENT_STATE tick={} health={} pos={} thunder={} sky={} bolts={} projectile={}",h.getTick(),target.getHealth(),target.position(),level.isThundering(),level.canSeeSky(target.blockPosition()),level.getEntitiesOfClass(LightningBolt.class,new AABB(targetPos).inflate(8)).stream().map(b->b.saveWithoutId(new CompoundTag())).toList(),level.getEntitiesOfClass(ThrownTrident.class,new AABB(start).inflate(12)).stream().map(t->t.saveWithoutId(new CompoundTag())).toList());});
        stack.getItem().releaseUsing(stack,level,p,stack.getUseDuration()-20);check(p.getMainHandItem().isEmpty()&&level.getEntitiesOfClass(ThrownTrident.class,new AABB(start).inflate(2)).stream().anyMatch(t->t.getOwner()==p),"normal native charged release consumes and spawns the owned projectile");
        h.succeedWhen(()->{for(String name:List.of("adventure/throw_trident","adventure/very_very_frightening")){var advancement=level.getServer().getAdvancements().getAdvancement(new ResourceLocation("minecraft",name));check(p.getAdvancements().getOrStartProgress(advancement).isDone(),"actual native projectile/lightning awarded "+name);}LogUtils.getLogger().info("NATIVE_INTEGRATION trident both_native_awards=true target_removed={}",target.isRemoved());});
    }
    @GameTest(template="empty") public static void nativeCopperAndChainsFeedTheActualMachineAndSignRecipes(GameTestHelper h){
        var menu=new TransientCraftingContainer(new AbstractContainerMenu(null,-1){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
        for(int slot:new int[]{1,3,5,6,7,8})menu.setItem(slot,new ItemStack(Items.COPPER_INGOT));menu.setItem(4,new ItemStack(Items.GLASS));var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,menu,h.getLevel()).orElseThrow();check(recipe.getId().equals(Psychedelicraft.id("flask"))&&recipe.assemble(menu,h.getLevel().registryAccess()).is(PSItems.FLASK.get()),"normal recipe lookup crafts a mod flask from native copper");
        for(String id:List.of("juniper_hanging_sign","distillery")){var entry=h.getLevel().getRecipeManager().byKey(Psychedelicraft.id(id)).orElseThrow();check(entry.getIngredients().stream().anyMatch(i->i.test(new ItemStack(id.equals("distillery")?Items.COPPER_INGOT:Items.CHAIN))),"native machine/sign ingredient "+id);}h.succeed();
    }
    @GameTest(template="empty") public static void campfireHeatKeepsTheAcceptedTagBehaviorForAllNativeStates(GameTestHelper h){
        for(Block block:List.of(Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE))for(boolean lit:new boolean[]{false,true})for(boolean water:new boolean[]{false,true}){var state=block.defaultBlockState().setValue(BlockStateProperties.LIT,lit).setValue(BlockStateProperties.WATERLOGGED,water);check(DistilleryBlockEntity.heatRate(state)==3,"accepted source counts all native campfire tag states as threefold distillation heat");}
        check(DistilleryBlockEntity.heatRate(Blocks.SOUL_FIRE.defaultBlockState())==3&&DistilleryBlockEntity.heatRate(Blocks.MAGMA_BLOCK.defaultBlockState())==2&&DistilleryBlockEntity.heatRate(Blocks.LAVA.defaultBlockState())==7,"native fire/magma/lava preserve accepted heat rates");h.succeed();
    }
}
