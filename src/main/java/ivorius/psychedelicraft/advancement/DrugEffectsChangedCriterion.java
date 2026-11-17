/* Copyright (c) 2014, Lukas Tenbrink. Modern criterion implementation by Sollace. */
package ivorius.psychedelicraft.advancement;

import com.google.gson.*;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import java.util.*;

/** Match simultaneous active intensities, including datapack-defined numeric ranges. */
final class DrugEffectsChangedCriterion extends SimpleCriterionTrigger<DrugEffectsChangedCriterion.Condition> {
    private static final ResourceLocation ID = Psychedelicraft.id("drug_effects_changed");
    @Override public ResourceLocation getId() { return ID; }
    @Override protected Condition createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        List<Effect> effects = new ArrayList<>();
        for (JsonElement value : GsonHelper.getAsJsonArray(json, "drugs")) {
            JsonObject condition = value.isJsonObject() ? value.getAsJsonObject() : null;
            String id = condition == null ? GsonHelper.convertToString(value, "drug") : GsonHelper.getAsString(condition, "id");
            DrugType type = DrugType.byId(id).orElseThrow(() -> new JsonSyntaxException("Unknown drug " + id));
            effects.add(new Effect(type, condition == null ? MinMaxBounds.Doubles.atLeast(1.0E-5)
                    : MinMaxBounds.Doubles.fromJson(condition.get("value"))));
        }
        return new Condition(player, List.copyOf(effects));
    }
    void fire(ServerPlayer player, DrugProperties properties) {
        trigger(player, condition -> condition.effects.stream().allMatch(effect -> effect.range.matches(properties.getActiveValue(effect.type))));
    }
    private record Effect(DrugType type, MinMaxBounds.Doubles range) {}
    static final class Condition extends AbstractCriterionTriggerInstance {
        final List<Effect> effects;
        Condition(ContextAwarePredicate player, List<Effect> effects) { super(ID, player); this.effects = effects; }
        @Override public JsonObject serializeToJson(SerializationContext context) {
            JsonObject result = super.serializeToJson(context);
            JsonArray values = new JsonArray();
            for (Effect effect : effects) {
                JsonObject value = new JsonObject();
                value.addProperty("id", effect.type.id());
                value.add("value", effect.range.serializeToJson());
                values.add(value);
            }
            result.add("drugs", values);
            return result;
        }
    }
}
