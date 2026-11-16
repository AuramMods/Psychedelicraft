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

public final class PSCriteria {
    private static final Custom CUSTOM = new Custom();
    private PSCriteria() {}
    public static void register() { CriteriaTriggers.register(CUSTOM); }
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
}
