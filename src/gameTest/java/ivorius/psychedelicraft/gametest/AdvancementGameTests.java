package ivorius.psychedelicraft.gametest;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.MashTubBlockEntity;
import ivorius.psychedelicraft.entity.drug.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class AdvancementGameTests {
    static final List<String> IDS = List.of("all_drugs","cannabis_buds","drink_basi","drink_beer","drink_brandy","drink_mature_beer",
            "drink_rum","drink_wine","dry_brown_mushroom","dry_cannabis_buds","dry_red_mushroom","feed_a_villager","grapes","hangover",
            "hash_a_muffin","hop_cones","make_distillery","make_drying_table","make_mash_tub","roll_joint","root","wash_beer","wash_grapes","wash_sugar_cane");
    static void check(boolean value, String message) { if (!value) throw new GameTestAssertException(message); }
    static ServerPlayer player(GameTestHelper h) {
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(UUID.randomUUID(), "AdvTest"));
        p.connection = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "AdvConn")).connection;
        p.setGameMode(GameType.SURVIVAL); p.setPos(h.absolutePos(new BlockPos(1,2,1)).getCenter());
        p.getAdvancements(); p.initInventoryMenu(); return p;
    }
    static Advancement advancement(GameTestHelper h, String name) {
        var result = h.getLevel().getServer().getAdvancements().getAdvancement(Psychedelicraft.id(name));
        check(result != null, "loaded advancement " + name); return result;
    }
    static boolean done(GameTestHelper h, ServerPlayer p, String name) { return p.getAdvancements().getOrStartProgress(advancement(h,name)).isDone(); }
    static FluidStack brew(PSFluids.Family family, int fermentation, int maturation, int distillation) {
        var fluid = new FluidStack(family.get(), 1000); var tag = fluid.getOrCreateTag();
        tag.putInt("fermentation", fermentation); tag.putInt("maturation", maturation); tag.putInt("distillation", distillation); return fluid;
    }
    static void drink(GameTestHelper h, ServerPlayer p, FluidStack fluid) {
        var stack = FluidVesselHandler.forCrafting(new ItemStack(PSItems.WOODEN_MUG.get())).replaceContents(fluid);
        stack.finishUsingItem(h.getLevel(), p);
    }
    @GameTest(template="empty") public static void fullModTreeAndNativeTridentMappingsLoad(GameTestHelper h) {
        for (String name : IDS) {
            var advancement = advancement(h,name); check(advancement.getDisplay() != null && !advancement.getDisplay().getIcon().isEmpty(), "display/icon " + name);
            check(name.equals("root") ? advancement.getParent() == null : advancement.getParent() != null, "parent " + name);
        }
        for (String name : List.of("adventure/throw_trident","adventure/very_very_frightening")) check(h.getLevel().getServer().getAdvancements().getAdvancement(new ResourceLocation("minecraft",name)) != null,"native trident replacement " + name);
        check(advancement(h,"feed_a_villager").getCriteria().get("done_the_thing").getTrigger().serializeToJson(SerializationContext.INSTANCE).get("event").getAsString().equals("feed_villager"), "custom event survives serialization");
        h.succeed();
    }
    @GameTest(template="empty") public static void nativeInventoryChangesAwardOnlyTheirRequiredItems(GameTestHelper h) {
        Map<String,Item> items = new LinkedHashMap<>();
        items.put("root",Items.CRAFTING_TABLE); items.put("make_drying_table",PSItems.DRYING_TABLE.get()); items.put("make_mash_tub",PSItems.MASH_TUB.get());
        items.put("make_distillery",PSItems.DISTILLERY.get());items.put("cannabis_buds",PSItems.CANNABIS_BUDS.get());items.put("dry_cannabis_buds",PSItems.DRIED_CANNABIS_BUDS.get());
        items.put("dry_brown_mushroom",PSItems.BROWN_MAGIC_MUSHROOMS.get());items.put("dry_red_mushroom",PSItems.RED_MAGIC_MUSHROOMS.get());items.put("hash_a_muffin",PSItems.HASH_MUFFIN.get());
        items.put("roll_joint",PSItems.JOINT.get());items.put("grapes",PSItems.WINE_GRAPES.get());items.put("hop_cones",PSItems.HOP_CONES.get());
        for (var target : items.entrySet()) {
            var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIRT));p.inventoryMenu.broadcastChanges();
            for(String name:items.keySet())check(!done(h,p,name),"unrelated dirt cannot award " + name);
            p.getInventory().setItem(0,new ItemStack(target.getValue()));p.inventoryMenu.broadcastChanges();check(done(h,p,target.getKey()),"native inventory award " + target.getKey());
            for(String name:items.keySet())if(!name.equals(target.getKey()))check(!done(h,p,name),"specific item cannot award " + name);
        }
        var iron=player(h);iron.getInventory().setItem(0,new ItemStack(PSItems.IRON_DRYING_TABLE.get()));iron.inventoryMenu.broadcastChanges();check(done(h,iron,"make_drying_table"),"either table satisfies OR requirement");
        h.succeed();
    }
    @GameTest(template="empty") public static void actualDrinkServingsMatchBrewStylesAndRejectVinegar(GameTestHelper h) {
        record Drink(String id, FluidStack contents) {}
        for (var target : List.of(new Drink("drink_beer",brew(PSFluids.WHEAT_HOP,2,0,0)),new Drink("drink_wine",brew(PSFluids.RED_GRAPES,2,7,0)),
                new Drink("drink_brandy",brew(PSFluids.RED_GRAPES,1,0,7)),new Drink("drink_basi",brew(PSFluids.SUGAR_CANE,2,0,0)),new Drink("drink_rum",brew(PSFluids.SUGAR_CANE,1,0,7)))) {
            var p=player(h);var vinegar=target.contents.copy();vinegar.getOrCreateTag().putBoolean("vinegar",true);drink(h,p,vinegar);
            check(!done(h,p,target.id),"vinegar cannot satisfy " + target.id);drink(h,p,target.contents);check(done(h,p,target.id),"native serving awards " + target.id);
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void matureBeerRequiresSevenMaturationSteps(GameTestHelper h) {
        var p=player(h);drink(h,p,brew(PSFluids.WHEAT_HOP,2,6,0));check(!done(h,p,"drink_mature_beer"),"six stages is not mature enough");
        drink(h,p,brew(PSFluids.WHEAT_HOP,2,7,0));check(done(h,p,"drink_mature_beer"),"seven stages awards mature beer");h.succeed();
    }
    @GameTest(template="empty") public static void mashWithdrawalAwardsTheMatchingFluidState(GameTestHelper h) {
        var pos=new BlockPos(1,2,1);h.setBlock(pos,PSBlocks.MASH_TUB.get());var tub=(MashTubBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        record Mash(String id,FluidStack fluid) {}
        for(var target:List.of(new Mash("wash_grapes",brew(PSFluids.RED_GRAPES,0,0,0)),new Mash("wash_beer",brew(PSFluids.WHEAT,2,0,0)),new Mash("wash_sugar_cane",brew(PSFluids.SUGAR_CANE,2,0,0)))) {
            var p=player(h);tub.getTank().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);tub.getTank().fill(target.fluid,FluidAction.EXECUTE);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.WOODEN_MUG.get()));check(tub.interactWithVessel(p,InteractionHand.MAIN_HAND),"real held-vessel withdrawal");
            check(tub.getTank().getFluidAmount()==500&&done(h,p,target.id),"withdrawal conserves contents and awards " + target.id);
        }
        var p=player(h);tub.getTank().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);var vinegar=brew(PSFluids.WHEAT,2,0,0);vinegar.getOrCreateTag().putBoolean("vinegar",true);tub.getTank().fill(vinegar,FluidAction.EXECUTE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.WOODEN_MUG.get()));tub.interactWithVessel(p,InteractionHand.MAIN_HAND);check(!done(h,p,"wash_beer"),"vinegar withdrawal is not beer wash");h.succeed();
    }
    @GameTest(template="empty") public static void nativeMuffinFeedingAwardsOnlySuccessfulConversion(GameTestHelper h) {
        var p=player(h);var baby=VillagerGameTests.villager(h,VillagerProfession.NONE);baby.setBaby(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PSItems.HASH_MUFFIN.get(),2));p.interactOn(baby,InteractionHand.MAIN_HAND);check(!done(h,p,"feed_a_villager"),"baby refusal gives no feed credit");baby.discard();
        var adult=VillagerGameTests.villager(h,VillagerProfession.NONE);p.interactOn(adult,InteractionHand.MAIN_HAND);check(done(h,p,"feed_a_villager")&&p.getMainHandItem().getCount()==1,"native adult feeding spends one muffin and awards");adult.discard();h.succeed();
    }
    @GameTest(template="empty") public static void wakingWithAlcoholAwardsTheHangoverEvent(GameTestHelper h) {
        var p=player(h);var props=DrugCapability.get(p);active(props,DrugType.ALCOHOL,1);props.setDrugLocked(DrugType.ALCOHOL,false);p.getRandom().setSeed(123);
        var pos=h.absolutePos(new BlockPos(1,2,1));h.setBlock(new BlockPos(1,2,1),Blocks.RED_BED);p.startSleeping(pos);p.stopSleepInBed(true,true);
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(done(h,p,"hangover"),"native wake event and properties tick award hangover");h.succeed();
    }
    @GameTest(template="empty") public static void allDrugsRequiresSimultaneousActiveValuesAtTheNormalTickHook(GameTestHelper h) {
        var p=player(h);var props=DrugCapability.get(p);var condition=advancement(h,"all_drugs").getCriteria().get("have_all_effects").getTrigger().serializeToJson(SerializationContext.INSTANCE);
        var types=condition.getAsJsonArray("drugs").asList().stream().map(e->DrugType.byId(e.getAsJsonObject().get("id").getAsString()).orElseThrow()).toList();check(types.size()==16,"accepted challenge contains sixteen named drugs");
        for(var type:types){props.setDrugLocked(type,true);props.setDrugValue(type,.2);}props.setDrugValue(DrugType.LSD,0);
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(!done(h,p,"all_drugs"),"one missing drug prevents challenge");
        props.setDrugValue(DrugType.LSD,1.0E-6);MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(!done(h,p,"all_drugs"),"sub-threshold intensity prevents challenge");
        props.setDrugValue(DrugType.LSD,.2);MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));check(done(h,p,"all_drugs"),"normal server tick awards simultaneous active challenge");h.succeed();
    }
    @SuppressWarnings({"rawtypes","unchecked"}) @GameTest(template="empty") public static void datapackDrugRangesRoundTripAndRejectUnknownIds(GameTestHelper h) {
        var trigger=(CriterionTrigger)CriteriaTriggers.getCriterion(Psychedelicraft.id("drug_effects_changed"));var context=new DeserializationContext(Psychedelicraft.id("range_test"),h.getLevel().getServer().getLootData());
        var json=JsonParser.parseString("{\"drugs\":[{\"id\":\"psychedelicraft:lsd\",\"value\":{\"min\":0.25,\"max\":0.5}}]}").getAsJsonObject();var condition=trigger.createInstance(json,context);
        var serialized=condition.serializeToJson(SerializationContext.INSTANCE);check(serialized.getAsJsonArray("drugs").get(0).getAsJsonObject().get("value").equals(json.getAsJsonArray("drugs").get(0).getAsJsonObject().get("value")),"numeric range is serialized intact");
        var advancement=Advancement.Builder.advancement().addCriterion("ranged",condition).build(Psychedelicraft.id("range_test"));var p=player(h);trigger.addPlayerListener(p.getAdvancements(),new CriterionTrigger.Listener(condition,advancement,"ranged"));var props=DrugCapability.get(p);
        active(props,DrugType.LSD,.51);PSCriteria.drugsChanged(p,props);check(!p.getAdvancements().getOrStartProgress(advancement).isDone(),"upper bound rejects stronger value");
        active(props,DrugType.LSD,.24);PSCriteria.drugsChanged(p,props);check(!p.getAdvancements().getOrStartProgress(advancement).isDone(),"lower bound rejects weaker value");
        active(props,DrugType.LSD,.5);PSCriteria.drugsChanged(p,props);check(p.getAdvancements().getOrStartProgress(advancement).isDone(),"inclusive bound awards range criterion");
        boolean rejected=false;try{trigger.createInstance(JsonParser.parseString("{\"drugs\":[\"psychedelicraft:missing\"]}").getAsJsonObject(),context);}catch(JsonSyntaxException expected){rejected=true;}check(rejected,"unknown drug is a real resource error");h.succeed();
    }
    private static void active(DrugProperties props,DrugType type,double value){
        props.setDrugLocked(type,true);props.setDrugValue(type,value);for(int i=0;i<100;i++)props.tick(i);
        check(Math.abs(props.getActiveValue(type)-value)<1.0E-12,"fixture reaches actual active intensity");
    }
}
