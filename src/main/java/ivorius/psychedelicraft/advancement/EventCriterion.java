package ivorius.psychedelicraft.advancement;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class EventCriterion<T> implements ICriterionTrigger<EventCriterion.Condition<T>> {
    private final ResourceLocation id;
    private final Function<JsonObject, Predicate<T>> parse;
    private final Map<PlayerAdvancements, Set<Listener<Condition<T>>>> listeners = new HashMap<>();

    EventCriterion(ResourceLocation id, Function<JsonObject, Predicate<T>> parse) {
        this.id = id;
        this.parse = parse;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Condition<T> deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Condition<>(id, EntityPredicate.deserialize(json.get("player")), parse.apply(json));
    }

    @Override
    public void addListener(PlayerAdvancements advancements, Listener<Condition<T>> listener) {
        listeners.computeIfAbsent(advancements, key -> new HashSet<>()).add(listener);
    }

    @Override
    public void removeListener(PlayerAdvancements advancements, Listener<Condition<T>> listener) {
        Set<Listener<Condition<T>>> registered = listeners.get(advancements);
        if (registered != null && registered.remove(listener) && registered.isEmpty()) listeners.remove(advancements);
    }

    @Override
    public void removeAllListeners(PlayerAdvancements advancements) {
        listeners.remove(advancements);
    }

    void trigger(EntityPlayer player, T value) {
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        PlayerAdvancements advancements = serverPlayer.getAdvancements();
        Set<Listener<Condition<T>>> registered = listeners.get(advancements);
        if (registered == null) return;
        List<Listener<Condition<T>>> matching = null;
        for (Listener<Condition<T>> listener : registered) {
            Condition<T> condition = listener.getCriterionInstance();
            if (condition.player.test(serverPlayer, serverPlayer) && condition.matches.test(value)) {
                if (matching == null) matching = new ArrayList<>();
                matching.add(listener);
            }
        }
        // Granting criteria can remove listeners from the registered set.
        if (matching != null) matching.forEach(listener -> listener.grantCriterion(advancements));
    }

    static final class Condition<T> extends AbstractCriterionInstance {
        private final EntityPredicate player;
        private final Predicate<T> matches;

        private Condition(ResourceLocation id, EntityPredicate player, Predicate<T> matches) {
            super(id);
            this.player = player;
            this.matches = matches;
        }
    }
}
