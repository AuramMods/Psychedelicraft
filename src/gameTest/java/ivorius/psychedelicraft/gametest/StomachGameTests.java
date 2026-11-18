package ivorius.psychedelicraft.gametest;
import ivorius.psychedelicraft.entity.drug.*;

import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.PSDamageTypes;
import ivorius.psychedelicraft.item.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class StomachGameTests {
    static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"StomachTest"));p.setPos(h.absolutePos(new net.minecraft.core.BlockPos(3,3,3)).getCenter());p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();StomachFoodStats.bind(p);return p;}
    static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    static void close(float actual,float expected,String message){check(Math.abs(actual-expected)<.0001F,message+": "+actual+" vs "+expected);}
    static void effect(net.minecraft.world.entity.player.Player p,DrugType type,double intensity){var root=DrugCapability.get(p).toNbt();var drug=new CompoundTag();drug.putDouble("effect",intensity);drug.putDouble("effectActive",intensity);drug.putBoolean("locked",true);root.getCompound("Drugs").put(type.id(),drug);DrugCapability.get(p).fromNbt(root);}
    @GameTest(template="empty") public static void appetiteUsesRealCapWhilePerceivedHungerAndOvereatingPersist(GameTestHelper h){
        var p=player(h);var food=p.getFoodData();var stomach=DrugCapability.get(p).getStomach();food.setFoodLevel(12);food.setSaturation(5);stomach.updateHunger(12,5,-.5F);food.eat(8,.8F);
        check(food.needsFood()&&food.getFoodLevel()==16&&StomachFoodStats.packetFoodLevel(food)==20,"appetite permits eating above the real food cap while projecting once");for(int i=0;i<10;i++)food.eat(8,.8F);close(stomach.toNbt().getFloat("overeating"),10,"only food above the real cap counts");
        var restored=new Stomach();restored.fromNbt(stomach.toNbt());check(restored.perceivedFood(20)==16&&restored.needsFood(false),"save retains anchor and appetite permission");close(restored.toNbt().getFloat("overeating"),10,"save retains accumulated overeating");h.succeed();
    }
    @GameTest(template="empty") public static void suppressionKeepsRawHealthBaselineAndReleasesAtZero(GameTestHelper h){
        var p=player(h);var food=p.getFoodData();var stomach=DrugCapability.get(p).getStomach();food.setFoodLevel(16);food.setSaturation(5);stomach.updateHunger(16,5,.5F);food.eat(-8,.25F);
        check(!food.needsFood()&&food.getFoodLevel()==12&&StomachFoodStats.packetFoodLevel(food)==8,"suppression reports projected hunger and raw packet baseline independently");close(food.getSaturationLevel(),3,"saturation is projected once");close(StomachFoodStats.packetSaturation(food),1,"raw saturation stays distinct");food.setFoodLevel(2);check(StomachFoodStats.packetFoodLevel(food)==8,"server direct setter cannot overwrite a hunger lock");
        stomach.updateHunger(8,1,0);check(food.needsFood()&&food.getFoodLevel()==8,"zero effect releases eating permission and projection");h.succeed();
    }
    @GameTest(template="empty") public static void invalidStomachDataIsBoundedAndEmptyLoadResetsEverything(GameTestHelper h){
        var data=new CompoundTag();data.putFloat("overeating",Float.NaN);data.putInt("vomitCount",999);data.putInt("vomitingTicks",-5);var hunger=new CompoundTag();hunger.putFloat("food",Float.POSITIVE_INFINITY);data.put("hunger",hunger);var stomach=new Stomach();stomach.fromNbt(data);var saved=stomach.toNbt();check(!stomach.isLocked()&&saved.getFloat("overeating")==0&&saved.getInt("vomitCount")==17&&saved.getInt("vomitingTicks")==0,"invalid and out-of-range values cannot strand hunger or vomiting");stomach.fromNbt(new CompoundTag());check(stomach.toNbt().getInt("vomitCount")==0&&!stomach.isLocked(),"empty state clears previous persistence");h.succeed();
    }
    @GameTest(template="empty") public static void retainedFoodManagerAndInternalRegenerationUseExhaustionScaleOnce(GameTestHelper h){
        var p=player(h);var original=new FoodData();original.setFoodLevel(20);original.setSaturation(6);original.setExhaustion(0);p.foodData=original;StomachFoodStats.bind(p);effect(p,DrugType.CAFFEINE,1);float scale=DrugCapability.get(p).getMultiplier(Drug::saturationExhaustionModifier);p.getFoodData().addExhaustion(2);close(original.getExhaustionLevel(),2*scale,"ordinary exhaustion reaches the retained food manager once");original.setExhaustion(0);p.setHealth(10);for(int i=0;i<10;i++)p.getFoodData().tick(p);close(original.getExhaustionLevel(),6*scale,"native regeneration's internal exhaustion uses the same scale once");check(p.getHealth()>10,"native regeneration still heals");original.setSaturation(0);original.setExhaustion(0);p.getFoodData().addExhaustion(2);close(original.getExhaustionLevel(),2,"zero raw saturation restores ordinary hunger exhaustion");h.succeed();
    }
    @GameTest(template="empty") public static void rawSaturationBenefitsAndFlightNeverSpendPerceivedSaturation(GameTestHelper h){
        var p=player(h);var food=p.getFoodData();food.setFoodLevel(20);food.setSaturation(5);var stomach=DrugCapability.get(p).getStomach();stomach.updateHunger(20,5,1);food.setSaturation(0);close(food.getSaturationLevel(),5,"suppression can still perceive a full saturation anchor");effect(p,DrugType.PEYOTE,1);effect(p,DrugType.CAFFEINE,.5);DrugCapability.get(p).tick(1,p);check(!p.getAbilities().mayfly,"flight requires actual remaining saturation");StomachFoodStats.restoreSaturation(p,1);close(StomachFoodStats.packetSaturation(food),1,"serving restores one real saturation point");DrugCapability.get(p).tick(2,p);check(p.getAbilities().mayfly,"actual saturation enables native flight");p.getAbilities().flying=true;DrugCapability.get(p).tick(2,p);check(StomachFoodStats.packetSaturation(food)<1,"only actual saturation is charged");effect(p,DrugType.PEYOTE,0);DrugCapability.get(p).tick(4,p);h.succeed();
    }
    @GameTest(template="empty") public static void nativeFoodOverloadsRecordOneServingAndRetainNativeSaveFields(GameTestHelper h){
        var p=player(h);var food=p.getFoodData();var stomach=DrugCapability.get(p).getStomach();food.setFoodLevel(20);stomach.updateHunger(20,5,-.2F);food.eat(Items.COOKED_BEEF,new ItemStack(Items.COOKED_BEEF),p);close(stomach.toNbt().getFloat("overeating"),1,"entity-aware native food records exactly one eight-point serving");food.eat(Items.COOKED_BEEF,new ItemStack(Items.COOKED_BEEF));close(stomach.toNbt().getFloat("overeating"),2,"legacy native overload also records exactly one serving");food.setExhaustion(2.5F);var saved=new CompoundTag();food.addAdditionalSaveData(saved);var restored=new FoodData();restored.readAdditionalSaveData(saved);check(restored.getFoodLevel()==20,"vanilla save stores real food");close(restored.getExhaustionLevel(),2.5F,"native exhaustion survives wrapper save");h.succeed();
    }
    @GameTest(template="empty") public static void copiedFoodManagerRebindsToTheNewPlayerAfterNativeClone(GameTestHelper h){
        var old=player(h);effect(old,DrugType.CAFFEINE,1);var replacement=player(h);replacement.foodData=old.getFoodData();old.remove(Entity.RemovalReason.CHANGED_DIMENSION);StomachFoodStats.bind(replacement);replacement.getFoodData().setExhaustion(0);replacement.getFoodData().addExhaustion(2);close(replacement.getFoodData().getExhaustionLevel(),2,"rebinding uses the new player's modifiers without reading removed capabilities");h.succeed();
    }
    @GameTest(template="empty") public static void emptyOffhandBagCatchesVomitWithoutBodyCountOrAnimation(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(PSItems.PAPER_BAG.get()));var stomach=DrugCapability.get(p).getStomach();stomach.vomit(p);check(p.getOffhandItem().is(PSItems.BAG_O_VOMIT.get())&&p.getOffhandItem().getCount()==1,"one empty offhand bag becomes one filled bag");check(stomach.toNbt().getInt("vomitCount")==0&&stomach.toNbt().getInt("vomitingTicks")==0,"bagged vomiting adds no body vomit count or expulsion timer");check(p.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION),"native accepted nausea remains part of vomiting");h.succeed();
    }
    @GameTest(template="empty") public static void stackedAndCreativeBagsRetainTheirAcceptedConsumptionRules(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(PSItems.PAPER_BAG.get(),2));DrugCapability.get(p).getStomach().vomit(p);check(p.getOffhandItem().getCount()==1&&p.getInventory().countItem(PSItems.BAG_O_VOMIT.get())==1,"stacked bags consume one and inventory receives one filled bag");p.getInventory().clearContent();p.setGameMode(GameType.CREATIVE);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(PSItems.PAPER_BAG.get(),2));DrugCapability.get(p).getStomach().vomit(p);check(p.getOffhandItem().getCount()==2&&p.getInventory().countItem(PSItems.BAG_O_VOMIT.get())==1,"creative keeps the empty bag stack while creating one filled bag");h.succeed();
    }
    @GameTest(template="empty") public static void filledBagsCannotEraseTheirContentsWhenVomiting(GameTestHelper h){
        var p=player(h);var bag=new ItemStack(PSItems.PAPER_BAG.get());PaperBagItem.setContents(bag,new PaperBagItem.Contents(new ItemStack(Items.COOKIE),50));var before=bag.copy();p.setItemInHand(InteractionHand.OFF_HAND,bag);var stomach=DrugCapability.get(p).getStomach();stomach.vomit(p);check(ItemStack.matches(before,p.getOffhandItem())&&stomach.toNbt().getInt("vomitCount")==1&&stomach.toNbt().getInt("vomitingTicks")>=10,"filled bag stays intact and normal vomiting begins");h.succeed();
    }
    @GameTest(template="empty") public static void lastVomitTickTerminatesAndExpelledItemsCannotBePickedUp(GameTestHelper h){
        var p=player(h);var stomach=DrugCapability.get(p).getStomach();var data=new CompoundTag();data.putInt("vomitingTicks",1);stomach.fromNbt(data);stomach.tick(p,0);check(stomach.toNbt().getInt("vomitingTicks")==0,"a one-tick remainder always terminates");data.putInt("vomitingTicks",100);stomach.fromNbt(data);for(int i=0;i<100;i++){p.tickCount++;stomach.tick(p,0);}check(stomach.toNbt().getInt("vomitingTicks")==0,"every vomiting episode ends within one hundred ticks");var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(8),e->e.getItem().is(PSItems.VOMIT.get()));check(!drops.isEmpty()&&drops.stream().allMatch(ItemEntity::hasPickUpDelay),"actual expelled native items have infinite pickup delay");h.succeed();
    }
    @GameTest(template="empty",batch="stomach_lethal") public static void seventeenthUnbaggedVomitUsesNativeOvereatingDamage(GameTestHelper h){
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"MortalStomach"));p.connection=player(h).connection;p.setGameMode(GameType.SURVIVAL);for(int i=0;i<61;i++)p.tick();var data=new CompoundTag();data.putInt("vomitCount",16);DrugCapability.get(p).getStomach().fromNbt(data);DrugCapability.get(p).getStomach().vomit(p);check(!p.isAlive()&&p.getLastDamageSource()!=null&&p.getLastDamageSource().is(PSDamageTypes.OVER_EATING),"seventeenth body vomit uses the original native fatal damage type");h.succeed();
    }
    @GameTest(template="empty") public static void bagOfVomitIsAlwaysEdibleAndCreativeRetainsTheStack(GameTestHelper h){
        var p=player(h);var bag=new ItemStack(PSItems.BAG_O_VOMIT.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,bag);check(bag.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"filled bag starts eating at full hunger");p.stopUsingItem();var result=bag.finishUsingItem(h.getLevel(),p);check(result.getCount()==1,"survival eats exactly one actual filled bag");p.setGameMode(GameType.CREATIVE);result=bag.finishUsingItem(h.getLevel(),p);check(result==bag&&result.getCount()==1,"creative completion retains the exact held stack");h.succeed();
    }
    @GameTest(template="empty") public static void cancelledFilledVomitBagDeliveryPreservesEmptyBags(GameTestHelper h){
        var p=player(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(PSItems.PAPER_BAG.get(),2));Object listener=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void cancelCaughtVomit(net.minecraftforge.event.entity.EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity item&&item.getItem().is(PSItems.BAG_O_VOMIT.get()))e.setCanceled(true);}};net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);try{DrugCapability.get(p).getStomach().vomit(p);check(p.getOffhandItem().getCount()==2&&p.getInventory().countItem(PSItems.BAG_O_VOMIT.get())==0,"failed catch delivery consumes no empty bag or native inventory items");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);}h.succeed();
    }

    private static FoodData maskedFood(net.minecraft.world.entity.player.Player player){
        var food=player.getFoodData();food.setFoodLevel(12);food.setSaturation(3);
        DrugCapability.get(player).getStomach().updateHunger(20,5,1);
        check(food.getFoodLevel()==20&&!food.needsFood(),"living hunger suppression masks the retained manager");
        close(food.getSaturationLevel(),5,"living saturation view uses the active anchor");return food;
    }
    private static void neutralFood(FoodData food){
        check(food.getFoodLevel()==12&&food.needsFood(),"unavailable/dead owner reads native food and appetite");
        close(food.getSaturationLevel(),3,"unavailable/dead owner reads native saturation");
    }
    @GameTest(template="empty") public static void removedFoodViewUsesNativeStateAndNeverRevivesCapability(GameTestHelper h){
        var p=player(h);var food=maskedFood(p);var previous=p.getCapability(DrugCapability.TYPE);
        p.remove(Entity.RemovalReason.CHANGED_DIMENSION);neutralFood(food);
        check(!previous.isPresent()&&!p.getCapability(DrugCapability.TYPE).isPresent(),"food reads leave both invalidated handles retired");h.succeed();
    }
    @GameTest(template="empty") public static void deadFoodViewIsNeutralBeforeAndAfterNativeRemoval(GameTestHelper h){
        var p=player(h);var food=maskedFood(p);p.setHealth(0);neutralFood(food);
        var previous=p.getCapability(DrugCapability.TYPE);p.remove(Entity.RemovalReason.KILLED);neutralFood(food);
        check(!previous.isPresent()&&!p.getCapability(DrugCapability.TYPE).isPresent(),"death HUD reads never revive the owner capability");h.succeed();
    }
    @GameTest(template="empty") public static void reboundFoodUsesReplacementOwnerAndLeavesStaleViewNeutral(GameTestHelper h){
        var original=player(h);var stale=maskedFood(original);var previous=original.getCapability(DrugCapability.TYPE);original.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        var replacement=player(h);replacement.foodData=stale;StomachFoodStats.bind(replacement);neutralFood(replacement.getFoodData());
        DrugCapability.get(replacement).getStomach().updateHunger(20,5,1);
        check(replacement.getFoodData().getFoodLevel()==20,"replacement uses its own live perception");neutralFood(stale);
        effect(replacement,DrugType.CAFFEINE,.5);replacement.getFoodData().setExhaustion(0);replacement.getFoodData().addExhaustion(1);
        close(replacement.getFoodData().getExhaustionLevel(),.8F,"retained native manager scales exhaustion through the replacement owner");
        check(!previous.isPresent()&&!original.getCapability(DrugCapability.TYPE).isPresent(),"rebinding leaves original handles invalid");replacement.remove(Entity.RemovalReason.DISCARDED);h.succeed();
    }
}
