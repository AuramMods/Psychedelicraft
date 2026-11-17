package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.entity.*;
import ivorius.psychedelicraft.item.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class VillagerGameTests {
    static final BlockPos P=new BlockPos(2,2,2);
    static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    static Villager villager(GameTestHelper h,VillagerProfession profession){var v=EntityType.VILLAGER.create(h.getLevel());v.setVillagerData(v.getVillagerData().setProfession(profession));v.setNoAi(true);v.setPos(h.absolutePos(P).getCenter());h.getLevel().addFreshEntity(v);return v;}
    static net.minecraftforge.common.util.FakePlayer player(GameTestHelper h){var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"VillageTest"));p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.setPos(h.absolutePos(P).getCenter().add(0,0,1));return p;}
    static MerchantMenu menu(GameTestHelper h,Villager v,net.minecraft.server.level.ServerPlayer p,MerchantOffer offer){var offers=new MerchantOffers();offers.add(offer);v.setOffers(offers);v.setTradingPlayer(p);var menu=new MerchantMenu(1,p.getInventory(),v);p.containerMenu=menu;menu.setSelectionHint(0);menu.getSlot(0).set(offer.getBaseCostA().copy());menu.getSlot(1).set(offer.getCostB().copy());return menu;}
    static MerchantOffer offer(Villager v,int level,Item result){return Arrays.stream(VillagerTrades.TRADES.get(v.getVillagerData().getProfession()).get(level)).map(listing->listing.getOffer(v,v.getRandom())).filter(Objects::nonNull).filter(o->o.getResult().is(result)).findFirst().orElseThrow();}
    @GameTest(template="empty") public static void nativePoiRegistryRecognizesEveryDryingStateAndAcquireTag(GameTestHelper h){
        for(var table:List.of(PSBlocks.DRYING_TABLE.get(),PSBlocks.IRON_DRYING_TABLE.get()))for(var state:table.getStateDefinition().getPossibleStates()){
            var poi=PoiTypes.forState(state).orElseThrow();check(poi.is(PSTradeOffers.DEALER_POI)&&poi.is(PoiTypeTags.ACQUIRABLE_JOB_SITE),"native state lookup and unemployed acquire tag recognize each drying-table orientation");check(poi.value().maxTickets()==1&&PSTradeOffers.DRUG_DEALER.get().heldJobSite().test(poi),"one native occupation ticket and correct held-job predicate");
        }h.succeed();
    }
    @GameTest(template="empty") public static void nativeJobAssignmentUsesRegisteredDealerProfession(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(P);h.setBlock(P,PSBlocks.IRON_DRYING_TABLE.get());var v=villager(h,VillagerProfession.NONE);v.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE,GlobalPos.of(level.dimension(),pos));
        check(AssignProfessionFromJobSite.create().tryStart(level,v,level.getGameTime()),"native assignment behavior starts at the actual workstation");check(v.getVillagerData().getProfession()==PSTradeOffers.DRUG_DEALER.get()&&v.getBrain().getMemory(MemoryModuleType.JOB_SITE).orElseThrow().pos().equals(pos),"native job assignment installs dealer and job memory");check(v.getOffers().size()==2,"native novice dealer draws two offers");v.discard();h.succeed();
    }
    @GameTest(template="empty") public static void muffinFeedsOnlyAdultUnemployedAndNitwitAndSpendsOne(GameTestHelper h){
        var p=player(h);for(var profession:List.of(VillagerProfession.NONE,VillagerProfession.NITWIT)){
            var v=villager(h,profession);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.HASH_MUFFIN.get(),2));check(p.interactOn(v,InteractionHand.MAIN_HAND).consumesAction(),"normal native interaction consumes the feed action");check(v.getVillagerData().getProfession()==PSTradeOffers.DRUG_ADDICT.get()&&v.getVillagerXp()==0&&v.getOffers().size()==2&&p.getMainHandItem().getCount()==1,"one survival muffin converts to a fresh native addict with two offers");
            p.interactOn(v,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==1&&v.getTradingPlayer()==null,"already-employed addict refuses another muffin without opening trade or consuming food");v.discard();
        }
        var employed=villager(h,VillagerProfession.FARMER);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.HASH_MUFFIN.get(),2));p.interactOn(employed,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==2&&employed.getVillagerData().getProfession()==VillagerProfession.FARMER,"employed farmer is neither converted nor fed");employed.discard();
        var baby=villager(h,VillagerProfession.NONE);baby.setAge(-24000);p.interactOn(baby,InteractionHand.MAIN_HAND);check(baby.getVillagerData().getProfession()==VillagerProfession.NONE&&p.getMainHandItem().getCount()==2,"baby is excluded");baby.discard();
        p.setGameMode(GameType.CREATIVE);var creative=villager(h,VillagerProfession.NONE);p.interactOn(creative,InteractionHand.MAIN_HAND);check(creative.getVillagerData().getProfession()==PSTradeOffers.DRUG_ADDICT.get()&&p.getMainHandItem().getCount()==2,"native creative abilities preserve the muffin stack");creative.discard();h.succeed();
    }
    @GameTest(template="empty") public static void joblessAddictKeepsProfessionButUntradedDealerUsesNativeReset(GameTestHelper h){
        for(var profession:List.of(PSTradeOffers.DRUG_ADDICT.get(),PSTradeOffers.DRUG_DEALER.get(),VillagerProfession.FARMER)){
            var v=villager(h,profession);v.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);ResetProfession.create().tryStart(h.getLevel(),v,h.getLevel().getGameTime());check(v.getVillagerData().getProfession()==(profession==PSTradeOffers.DRUG_ADDICT.get()?profession:VillagerProfession.NONE),"only the jobless addict is exempt from native untraded profession loss");v.discard();
        }h.succeed();
    }
    @GameTest(template="empty") public static void nativeFarmerPoolProvidesGrapesAndPreservesVanillaTrades(GameTestHelper h){
        var v=villager(h,VillagerProfession.FARMER);var offers=Arrays.stream(VillagerTrades.TRADES.get(VillagerProfession.FARMER).get(1)).map(factory->factory.getOffer(v,v.getRandom())).filter(Objects::nonNull).toList();
        var grapes=offers.stream().filter(o->o.getResult().is(PSItems.WINE_GRAPES.get())).findFirst().orElseThrow();check(grapes.getBaseCostA().is(Items.EMERALD)&&grapes.getBaseCostA().getCount()==2&&grapes.getResult().getCount()==3&&grapes.getMaxUses()==8&&grapes.getPriceMultiplier()==.5F,"original grape acquisition offer keeps native price/count/uses/multiplier");check(offers.stream().anyMatch(o->o.getBaseCostA().is(Items.WHEAT))&&offers.size()==15,"native novice pool retains five vanilla entries and adds ten original entries");v.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nativeTwoInputTradeConsumesExactMaterialsAndSurvivesSave(GameTestHelper h){
        var v=villager(h,PSTradeOffers.DRUG_DEALER.get());v.setVillagerData(v.getVillagerData().setLevel(3));var p=player(h);var offer=offer(v,3,PSItems.LSA_SQUARE.get());var menu=menu(h,v,p,offer);check(menu.getSlot(2).getItem().getCount()==3,"native merchant container matches both costs");menu.quickMoveStack(p,2);check(p.getInventory().countItem(PSItems.LSA_SQUARE.get())==3&&menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty()&&offer.getUses()==1&&v.getVillagerXp()==3,"native merchant pays three emeralds plus two papers once, awards xp and three squares");
        menu.quickMoveStack(p,2);check(p.getInventory().countItem(PSItems.LSA_SQUARE.get())==3&&offer.getUses()==1,"retry cannot duplicate the result");v.setTradingPlayer(null);var tag=new CompoundTag();v.save(tag);var restored=(Villager)EntityType.loadEntityRecursive(tag,h.getLevel(),java.util.function.Function.identity());var saved=restored.getOffers().get(0);check(restored.getVillagerData().getProfession()==PSTradeOffers.DRUG_DEALER.get()&&restored.getVillagerData().getLevel()==3&&restored.getVillagerXp()==3&&saved.getUses()==1&&saved.getCostB().getCount()==2&&saved.getPriceMultiplier()==.5F,"native entity save retains profession, level, xp and both trade inputs/stock");v.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nativeToolTradeSplitsUnstackableResultAndRefusesInsufficientSpace(GameTestHelper h){
        var v=villager(h,PSTradeOffers.DRUG_DEALER.get());var p=player(h);var offer=offer(v,2,PSItems.SMOKING_PIPE.get());for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));for(int i=0;i<4;i++)p.getInventory().setItem(i,ItemStack.EMPTY);var menu=menu(h,v,p,offer);
        check(menu.quickMoveStack(p,2).isEmpty()&&offer.getUses()==0&&menu.getSlot(0).getItem().getCount()==1&&menu.getSlot(2).getItem().getCount()==5,"four free slots refuse a five-tool trade without spending price or losing output");p.getInventory().setItem(4,ItemStack.EMPTY);menu.quickMoveStack(p,2);check(p.getInventory().countItem(PSItems.SMOKING_PIPE.get())==5&&p.getInventory().items.stream().filter(s->s.is(PSItems.SMOKING_PIPE.get())).allMatch(s->s.getCount()==1)&&offer.getUses()==1&&menu.getSlot(0).getItem().isEmpty(),"native quick move delivers five independent pipes for one price after space becomes available");v.setTradingPlayer(null);v.discard();h.succeed();
    }
    @GameTest(template="empty") public static void addictNativeTradeInflictsAcceptedDoseDamage(GameTestHelper h){
        var v=villager(h,PSTradeOffers.DRUG_ADDICT.get());var p=player(h);var offer=Arrays.stream(VillagerTrades.TRADES.get(PSTradeOffers.DRUG_ADDICT.get()).get(1)).map(factory->factory.getOffer(v,v.getRandom())).filter(o->o.getBaseCostA().is(PSItems.LSD_PILL.get())).findFirst().orElseThrow();var menu=menu(h,v,p,offer);menu.quickMoveStack(p,2);check(offer.getUses()==1&&v.getHealth()==17&&p.getInventory().countItem(Items.EMERALD)==2,"native trade completion damages addict by uses times payout plus one, and pays two emeralds");v.setTradingPlayer(null);v.discard();h.succeed();
    }
    @GameTest(template="empty",setupTicks=310) public static void addictRestocksWithoutPoiUsingNativeDailyLimitAndPersistsCooldown(GameTestHelper h){
        var level=h.getLevel();level.setDayTime(3000);var v=villager(h,PSTradeOffers.DRUG_ADDICT.get());var offer=v.getOffers().get(0);offer.increaseUses();var task=new AddictBehaviors.Restock();long now=level.getGameTime();boolean ran=false;for(int i=0;i<100&&!ran;i++)ran=task.tryStart(level,v,now);check(ran&&offer.getUses()==0&&v.getHealth()>=14&&v.getHealth()<20,"jobless restock uses native stock reset and overdose damage");float health=v.getHealth();offer.increaseUses();check(!new AddictBehaviors.Restock().tryStart(level,v,now)&&offer.getUses()==1&&v.getHealth()==health,"persisted 300-tick check cooldown prevents an immediate extra restock");var tag=new CompoundTag();v.save(tag);var restored=(Villager)EntityType.loadEntityRecursive(tag,level,java.util.function.Function.identity());check(restored.getPersistentData().getLong("psychedelicraftNextRestockCheck")==now+300&&!restored.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE),"native save retains restock check without inventing a job site");
        v.getPersistentData().putLong("psychedelicraftNextRestockCheck",0);v.restock();offer.increaseUses();check(!v.shouldRestock(),"native twice-daily restock limit applies to addicts");v.discard();h.succeed();
    }
    @GameTest(template="empty") public static void nativeAddictPaymentCollectsFiveFreshSeparateCigarsAndReturnsCancelledBatches(GameTestHelper h){
        for(boolean enough:List.of(false,true)){
            var v=villager(h,PSTradeOffers.DRUG_ADDICT.get());var p=player(h);
            var offer=Arrays.stream(VillagerTrades.TRADES.get(PSTradeOffers.DRUG_ADDICT.get()).get(1)).map(factory->factory.getOffer(v,v.getRandom())).filter(o->o.getBaseCostA().is(PSItems.CIGAR.get())).findFirst().orElseThrow();
            check(offer.getBaseCostA().getCount()==5&&offer.getCostA().getCount()==5,"native dynamic price preserves the five-cigar quantity rather than clamping it to one");
            var offers=new MerchantOffers();offers.add(offer);v.setOffers(offers);v.setTradingPlayer(p);var menu=new MerchantMenu(1,p.getInventory(),v);p.containerMenu=menu;
            int fresh=enough?5:3;for(int i=0;i<fresh;i++)p.getInventory().setItem(i,new ItemStack(PSItems.CIGAR.get()));var worn=new ItemStack(PSItems.CIGAR.get());worn.setDamageValue(1);p.getInventory().setItem(8,worn);
            menu.setSelectionHint(0);menu.tryMoveItems(0);check(menu.getSlot(0).getItem().getCount()==fresh&&p.getInventory().getItem(8).isDamaged(),"native offer selection combines matching fresh items from separate slots and excludes the worn cigar");
            if(enough){check(menu.getSlot(2).getItem().is(Items.EMERALD),"all five cigars make the buying offer available");menu.clicked(2,1,net.minecraft.world.inventory.ClickType.PICKUP,p);check(menu.getCarried().is(Items.EMERALD)&&menu.getCarried().getCount()==1&&offer.getUses()==1&&menu.getSlot(0).getItem().isEmpty()&&v.getHealth()==18,"native right-click spends five fresh cigars once and applies the correct addict overdose");}
            else{check(menu.getSlot(2).getItem().isEmpty()&&offer.getUses()==0,"three fresh plus a worn cigar cannot pay the five-fresh price");menu.removed(p);check(p.getInventory().countItem(PSItems.CIGAR.get())==4&&p.getInventory().items.stream().filter(stack->stack.is(PSItems.CIGAR.get())).allMatch(stack->stack.getCount()==1),"native close returns the incomplete payment as separate stacks without loss or duplication");}
            v.setTradingPlayer(null);v.discard();
        }h.succeed();
    }
    @GameTest(template="empty") public static void harmoniumTradeCarriesNativeMixedColorAndOriginalStock(GameTestHelper h){
        var v=villager(h,PSTradeOffers.DRUG_DEALER.get());var offer=offer(v,3,PSItems.HARMONIUM.get());check(HarmoniumItem.hasColor(offer.getResult())&&offer.getBaseCostA().getCount()==3&&offer.getMaxUses()==7&&offer.getXp()==2&&offer.getPriceMultiplier()==.2F,"native dye mix is stored on the harmonium trade result");v.discard();h.succeed();
    }
}
