/* Copyright (c) 2014, Lukas Tenbrink. Native behavior structure by Sollace. */
package ivorius.psychedelicraft.entity;

import com.google.common.collect.ImmutableMap;
import ivorius.psychedelicraft.block.CannabisPlantBlock;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.ForgeEventFactory;
import javax.annotation.Nullable;
import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=ivorius.psychedelicraft.Psychedelicraft.MOD_ID)
public final class DealerBehaviors {
    public static final net.minecraft.tags.TagKey<Item> DRUG_CROP_SEEDS=net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,ivorius.psychedelicraft.Psychedelicraft.id("drug_crop_seeds"));
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopDisabledWork(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event){
        if(event.getEntity() instanceof Villager villager&&villager.isNoAi()&&villager.level() instanceof ServerLevel level)
            for(var behavior:villager.getBrain().getRunningBehaviors())
                if(behavior instanceof WorkCycle||behavior instanceof Bonemeal)behavior.doStop(level,villager,level.getGameTime());
    }
    public static final class WorkCycle extends RunOne<Villager> {
        public WorkCycle(float speed){super(com.google.common.collect.ImmutableList.of(
                com.mojang.datafixers.util.Pair.of(new Work(),7),
                com.mojang.datafixers.util.Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE,.4F,4),2),
                com.mojang.datafixers.util.Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE,.4F,1,10),5),
                com.mojang.datafixers.util.Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE,speed,1,6,MemoryModuleType.JOB_SITE),5),
                com.mojang.datafixers.util.Pair.of(new Harvest(),2),com.mojang.datafixers.util.Pair.of(new Bonemeal(),4)));}
    }

    private static final String WORK_DISPLAY="psychedelicraftDealerWorkDisplay";
    private DealerBehaviors() {}
    private static void showWorkDisplay(Villager villager){
        restoreWorkDisplay(villager);
        var data=new net.minecraft.nbt.CompoundTag();
        data.put("Previous",villager.getMainHandItem().save(new net.minecraft.nbt.CompoundTag()));
        data.putFloat("DropChance",((ivorius.psychedelicraft.mixin.MobEquipmentAccessor)villager).psychedelicraft$getEquipmentDropChance(EquipmentSlot.MAINHAND));
        villager.getPersistentData().put(WORK_DISPLAY,data);
        villager.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BONE_MEAL));villager.setDropChance(EquipmentSlot.MAINHAND,0);
    }
    private static void restoreWorkDisplay(Villager villager){
        var persistent=villager.getPersistentData();if(!persistent.contains(WORK_DISPLAY,net.minecraft.nbt.Tag.TAG_COMPOUND))return;
        var data=persistent.getCompound(WORK_DISPLAY);
        if(ItemStack.matches(villager.getMainHandItem(),new ItemStack(Items.BONE_MEAL))){
            villager.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.of(data.getCompound("Previous")));
            villager.setDropChance(EquipmentSlot.MAINHAND,data.getFloat("DropChance"));
        }
        persistent.remove(WORK_DISPLAY);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void restoreLoadedDisplay(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
        if(!event.getLevel().isClientSide&&event.getEntity() instanceof Villager villager)restoreWorkDisplay(villager);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void restoreDyingDisplay(net.minecraftforge.event.entity.living.LivingDeathEvent event){
        if(!event.getEntity().level().isClientSide&&event.getEntity() instanceof Villager villager)restoreWorkDisplay(villager);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void restoreConvertingDisplay(net.minecraftforge.event.entity.living.LivingConversionEvent.Pre event){
        if(!event.getEntity().level().isClientSide&&event.getEntity() instanceof Villager villager)restoreWorkDisplay(villager);
    }

    private static boolean canWork(ServerLevel level,Villager villager){
        long day=Math.floorMod(level.getDayTime(),24000L);
        return villager.isAlive()&&!villager.isBaby()&&!villager.isTrading()&&!villager.isSleeping()
                &&villager.getVillagerData().getProfession()==PSTradeOffers.DRUG_DEALER.get()&&day>=2000&&day<9000;
    }
    @Nullable public static DryingTableBlockEntity worksite(ServerLevel level,Villager villager){
        var memory=villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if(memory.isEmpty()||memory.get().dimension()!=level.dimension()||!level.hasChunkAt(memory.get().pos()))return null;
        return level.getBlockEntity(memory.get().pos()) instanceof DryingTableBlockEntity table?table:null;
    }
    private static boolean nearWorksite(ServerLevel level,Villager villager){
        var table=worksite(level,villager);
        return table!=null&&villager.blockPosition().distManhattan(table.getBlockPos())<=9;
    }
    public static boolean mature(BlockState state){
        // The accepted crops use state-dependent ages (including coffea's age-three upper stalk).
        if(state.getBlock() instanceof CannabisPlantBlock crop)return state.getValue(crop.getAgeProperty())>=crop.getMaxAge(state);
        return state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state);
    }
    public static boolean isSeed(ItemStack stack){
        return stack.is(DRUG_CROP_SEEDS);
    }
    public static boolean loadBatch(ServerLevel level,SimpleContainer inventory,DryingTableBlockEntity table){
        if(!table.isEmpty())return false;
        for(var recipe:level.getRecipeManager().getAllRecipesFor(PSRecipes.DRYING_TYPE.get())){
            int[] sources=new int[DryingRecipe.INPUT_COUNT];int found=0;
            for(int slot=0;slot<inventory.getContainerSize()&&found<sources.length;slot++){
                var stack=inventory.getItem(slot);if(!recipe.getInput().test(stack))continue;
                for(int count=0;count<stack.getCount()&&found<sources.length;count++)sources[found++]=slot;
            }
            if(found==sources.length){
                for(int i=0;i<sources.length;i++)table.setItem(i+1,inventory.removeItem(sources[i],1));
                return true;
            }
        }
        return false;
    }
    public static void workTable(ServerLevel level,Villager villager,DryingTableBlockEntity table){
        var output=table.removeItemNoUpdate(DryingRecipe.OUTPUT);
        if(!output.isEmpty()){
            var excess=villager.getInventory().addItem(output);
            if(!excess.isEmpty()){
                var drop=villager.spawnAtLocation(excess.copy());
                boolean delivered=drop!=null&&(drop.isAddedToWorld()||villager.captureDrops()!=null&&villager.captureDrops().contains(drop));
                if(!delivered)table.setItem(DryingRecipe.OUTPUT,excess);
            }
        }
        if(table.isEmpty())loadBatch(level,villager.getInventory(),table);
    }
    public static class Work extends WorkAtPoi {
        @Override protected boolean checkExtraStartConditions(ServerLevel level,Villager villager){
            return canWork(level,villager)&&worksite(level,villager)!=null&&super.checkExtraStartConditions(level,villager);
        }
        @Override protected void useWorkstation(ServerLevel level,Villager villager){
            var table=worksite(level,villager);if(table!=null)workTable(level,villager,table);
        }
    }
    public static final class Harvest extends Behavior<Villager> {
        private final List<BlockPos> targets=new ArrayList<>();
        private BlockPos target;private long nextSession,nextAction;
        public Harvest(){super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET,MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET,MemoryStatus.VALUE_ABSENT,MemoryModuleType.SECONDARY_JOB_SITE,MemoryStatus.VALUE_PRESENT),60,60);}
        @Override protected boolean checkExtraStartConditions(ServerLevel level,Villager villager){
            if(!canWork(level,villager)||!nearWorksite(level,villager)||level.getGameTime()<nextSession
                    ||!ForgeEventFactory.getMobGriefingEvent(level,villager))return false;
            var origin=villager.blockPosition();boolean farmland=false;
            for(var pos:BlockPos.betweenClosed(origin.offset(-4,-2,-4),origin.offset(4,2,4)))
                if(level.hasChunkAt(pos)&&level.getBlockState(pos).getBlock() instanceof FarmBlock){farmland=true;break;}
            if(!farmland)return false;
            boolean seeds=villager.getInventory().hasAnyMatching(DealerBehaviors::isSeed);targets.clear();
            for(var pos:BlockPos.betweenClosed(origin.offset(-1,-1,-1),origin.offset(1,1,1))){
                if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);
                if(mature(state)||seeds&&state.isAir()&&level.getBlockState(pos.below()).getBlock() instanceof FarmBlock)targets.add(pos.immutable());
            }
            return !targets.isEmpty();
        }
        private void select(ServerLevel level,Villager villager,long time){
            target=targets.isEmpty()?null:targets.remove(level.random.nextInt(targets.size()));
            if(target!=null){var look=new BlockPosTracker(target);villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,look);
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(look,.5F,1));}
            nextAction=time;
        }
        @Override protected void start(ServerLevel level,Villager villager,long time){select(level,villager,time);}
        @Override protected boolean canStillUse(ServerLevel level,Villager villager,long time){
            return target!=null&&canWork(level,villager)&&nearWorksite(level,villager)&&ForgeEventFactory.getMobGriefingEvent(level,villager);
        }
        @Override protected void tick(ServerLevel level,Villager villager,long time){
            if(target==null||time<nextAction||!target.closerToCenterThan(villager.position(),1))return;
            if(level.hasChunkAt(target)){
                var state=level.getBlockState(target);
                if(mature(state)&&level.destroyBlock(target,true,villager))return;
                if(state.isAir()&&level.getBlockState(target.below()).getBlock() instanceof FarmBlock){
                    var inventory=villager.getInventory();
                    for(int slot=0;slot<inventory.getContainerSize();slot++){
                        var seed=inventory.getItem(slot);if(!isSeed(seed)||!(seed.getItem() instanceof BlockItem item))continue;
                        var planted=item.getBlock().defaultBlockState();
                        if(planted.canSurvive(level,target)&&level.setBlock(target,planted,Block.UPDATE_ALL)){
                            inventory.removeItem(slot,1);level.gameEvent(GameEvent.BLOCK_PLACE,target,GameEvent.Context.of(villager,planted));
                            level.playSound(null,target,SoundEvents.CROP_PLANTED,SoundSource.BLOCKS,1,1);
                        }break;
                    }
                }
            }
            select(level,villager,time+20);
        }
        @Override protected void stop(ServerLevel level,Villager villager,long time){
            target=null;targets.clear();nextSession=time+40;villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }
    public static final class Bonemeal extends Behavior<Villager> {
        private BlockPos crop;private int nextSearch;private long end,nextApplication;
        public Bonemeal(){super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET,MemoryStatus.VALUE_ABSENT,MemoryModuleType.WALK_TARGET,MemoryStatus.VALUE_ABSENT),60,60);}
        private static boolean valid(ServerLevel level,BlockPos pos){
            if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);
            return state.getBlock() instanceof CropBlock&& !mature(state)
                    &&((BonemealableBlock)state.getBlock()).isValidBonemealTarget(level,pos,state,false);
        }
        private static ItemStack meal(Villager villager){
            var inventory=villager.getInventory();for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(Items.BONE_MEAL))return inventory.getItem(i);return ItemStack.EMPTY;
        }
        private void select(ServerLevel level,Villager villager){
            crop=null;int count=0;var center=villager.blockPosition();
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
                if(valid(level,pos)&&level.random.nextInt(++count)==0)crop=pos.immutable();
            if(crop!=null){var look=new BlockPosTracker(crop);villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,look);
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(look,.5F,1));}
        }
        @Override protected boolean checkExtraStartConditions(ServerLevel level,Villager villager){
            if(villager.tickCount<nextSearch||!canWork(level,villager)||!nearWorksite(level,villager))return false;
            nextSearch=villager.tickCount+10;
            if(meal(villager).isEmpty()||!ForgeEventFactory.getMobGriefingEvent(level,villager))return false;
            select(level,villager);return crop!=null;
        }
        @Override protected void start(ServerLevel level,Villager villager,long time){
            showWorkDisplay(villager);
            end=time+60;nextApplication=time;
        }
        @Override protected boolean canStillUse(ServerLevel level,Villager villager,long time){
            return crop!=null&&time<=end&&canWork(level,villager)&&nearWorksite(level,villager)&&ForgeEventFactory.getMobGriefingEvent(level,villager);
        }
        @Override protected void tick(ServerLevel level,Villager villager,long time){
            if(crop==null)return;if(!valid(level,crop)){select(level,villager);if(crop==null)return;}
            if(time<nextApplication||!crop.closerToCenterThan(villager.position(),1))return;
            var stack=meal(villager);if(!stack.isEmpty()&&BoneMealItem.growCrop(stack,level,crop)){
                villager.getInventory().setChanged();level.levelEvent(1505,crop,0);nextApplication=time+40;select(level,villager);
            }
        }
        @Override protected void stop(ServerLevel level,Villager villager,long time){
            restoreWorkDisplay(villager);
            crop=null;nextSearch=villager.tickCount+160;
            villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }
}
