package ivorius.psychedelicraft.advancement;

import com.google.gson.JsonObject;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SerializationContext;
import ivorius.psychedelicraft.fluid.*;
import net.minecraftforge.fluids.FluidStack;

public final class PSCriteria {
    private static final Custom CUSTOM = new Custom();
    private static final DrankBrew DRANK_BREW=new DrankBrew();
    private static final MashedItem MASHED_ITEM=new MashedItem();
    private PSCriteria() {}
    public static void register() { CriteriaTriggers.register(CUSTOM);CriteriaTriggers.register(DRANK_BREW);CriteriaTriggers.register(MASHED_ITEM); }
    public static void drankBrew(Player player,FluidStack contents){if(player instanceof ServerPlayer server&&PSFluids.brew(contents.getFluid())!=null)DRANK_BREW.fire(server,contents);}
    public static void tookMash(Player player,net.minecraft.world.item.ItemStack stack){var handler=FluidVesselHandler.forTransfer(stack);if(handler!=null)tookMash(player,handler.drain(Integer.MAX_VALUE,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE));}
    public static void tookMash(Player player,FluidStack contents){if(!contents.isEmpty()&&player instanceof ServerPlayer server)MASHED_ITEM.fire(server,contents);}
    public static void hangover(Player player) {
        if (player instanceof ServerPlayer serverPlayer) CUSTOM.fire(serverPlayer, "get_hangover");
    }

    private static final class Custom extends SimpleCriterionTrigger<Condition> {
        @Override public ResourceLocation getId() { return Psychedelicraft.id("custom"); }
        @Override protected Condition createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
            return new Condition(player, GsonHelper.getAsString(json, "event"));
        }
        void fire(ServerPlayer player, String event) { trigger(player, condition -> condition.event.equals(event)); }
    }
    private static final class Condition extends AbstractCriterionTriggerInstance {
        private final String event;
        Condition(ContextAwarePredicate player, String event) {
            super(Psychedelicraft.id("custom"), player);
            this.event = event;
        }
    }
    private static final class DrankBrew extends SimpleCriterionTrigger<BrewCondition>{
        @Override public ResourceLocation getId(){return Psychedelicraft.id("drank_brew");}
        @Override protected BrewCondition createInstance(JsonObject json,ContextAwarePredicate player,DeserializationContext context){
            return new BrewCondition(player,GsonHelper.getAsString(json,"fluid"),GsonHelper.getAsString(json,"drink"),MinMaxBounds.Ints.fromJson(json.get("maturation")));
        }
        void fire(ServerPlayer player,FluidStack contents){trigger(player,condition->condition.fluid.equals(PSFluids.id(contents.getFluid()))&&PSFluids.brew(contents.getFluid()).isDrinkType(contents,condition.drink)&&condition.maturation.matches(BrewProfile.stage(contents,BrewProfile.MATURATION,16)));}
    }
    private static final class BrewCondition extends AbstractCriterionTriggerInstance{
        final String fluid,drink;final MinMaxBounds.Ints maturation;
        BrewCondition(ContextAwarePredicate player,String fluid,String drink,MinMaxBounds.Ints maturation){super(Psychedelicraft.id("drank_brew"),player);this.fluid=fluid;this.drink=drink;this.maturation=maturation;}
        @Override public JsonObject serializeToJson(SerializationContext context){JsonObject json=super.serializeToJson(context);json.addProperty("fluid",fluid);json.addProperty("drink",drink);json.add("maturation",maturation.serializeToJson());return json;}
    }
    private static final class MashedItem extends SimpleCriterionTrigger<MashCondition>{
        @Override public ResourceLocation getId(){return Psychedelicraft.id("mashed_item");}
        @Override protected MashCondition createInstance(JsonObject json,ContextAwarePredicate player,DeserializationContext context){return new MashCondition(player,GsonHelper.getAsString(json,"fluid"),MinMaxBounds.Ints.fromJson(json.get("fermentation")),MinMaxBounds.Ints.fromJson(json.get("maturation")),MinMaxBounds.Ints.fromJson(json.get("distillation")));}
        void fire(ServerPlayer player,FluidStack contents){trigger(player,condition->condition.fluid.equals(PSFluids.id(contents.getFluid()))&&!BrewProfile.isVinegar(contents)&&condition.fermentation.matches(BrewProfile.stage(contents,"fermentation",2))&&condition.maturation.matches(BrewProfile.stage(contents,"maturation",16))&&condition.distillation.matches(BrewProfile.stage(contents,"distillation",16)));}
    }
    private static final class MashCondition extends AbstractCriterionTriggerInstance{
        final String fluid;final MinMaxBounds.Ints fermentation,maturation,distillation;
        MashCondition(ContextAwarePredicate player,String fluid,MinMaxBounds.Ints fermentation,MinMaxBounds.Ints maturation,MinMaxBounds.Ints distillation){super(Psychedelicraft.id("mashed_item"),player);this.fluid=fluid;this.fermentation=fermentation;this.maturation=maturation;this.distillation=distillation;}
        @Override public JsonObject serializeToJson(SerializationContext context){JsonObject json=super.serializeToJson(context);json.addProperty("fluid",fluid);json.add("fermentation",fermentation.serializeToJson());json.add("maturation",maturation.serializeToJson());json.add("distillation",distillation.serializeToJson());return json;}
    }
}
