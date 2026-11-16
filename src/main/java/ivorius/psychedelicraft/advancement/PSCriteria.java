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
    private PSCriteria() {}
    public static void register() { CriteriaTriggers.register(CUSTOM);CriteriaTriggers.register(DRANK_BREW); }
    public static void drankBrew(Player player,FluidStack contents){if(player instanceof ServerPlayer server&&PSFluids.brew(contents.getFluid())!=null)DRANK_BREW.fire(server,contents);}
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
}
