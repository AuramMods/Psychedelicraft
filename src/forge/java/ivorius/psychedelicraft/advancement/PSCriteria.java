package ivorius.psychedelicraft.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.fluid.BrewFluid;
import ivorius.psychedelicraft.fluid.FluidVesselHandler;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class PSCriteria {
    private static final EventCriterion<String> CUSTOM = new EventCriterion<>(Psychedelicraft.id("custom"),
            json -> JsonUtils.getString(json, "event")::equals);
    private static final EventCriterion<DrugProperties> DRUG_EFFECTS_CHANGED = new EventCriterion<>(
            Psychedelicraft.id("drug_effects_changed"), PSCriteria::drugEffects);
    private static final EventCriterion<FluidStack> MASHED_ITEM = new EventCriterion<>(
            Psychedelicraft.id("mashed_item"), PSCriteria::mashedFluid);
    private static final EventCriterion<FluidStack> DRANK_BREW = new EventCriterion<>(
            Psychedelicraft.id("drank_brew"), PSCriteria::brew);

    private PSCriteria() {
    }

    public static void register() {
        CriteriaTriggers.register(CUSTOM);
        CriteriaTriggers.register(DRUG_EFFECTS_CHANGED);
        CriteriaTriggers.register(MASHED_ITEM);
        CriteriaTriggers.register(DRANK_BREW);
    }

    public static void feedVillager(EntityPlayer player) {
        CUSTOM.trigger(player, "feed_villager");
    }

    public static void hangover(EntityPlayer player) {
        CUSTOM.trigger(player, "get_hangover");
    }

    public static void drugsChanged(EntityPlayer player, DrugProperties properties) {
        DRUG_EFFECTS_CHANGED.trigger(player, properties);
    }

    public static void tookMash(EntityPlayer player, ItemStack stack) {
        IFluidHandlerItem handler = FluidVesselHandler.forTransfer(stack);
        if (handler != null) tookMash(player, handler.drain(Integer.MAX_VALUE, false));
    }

    public static void tookMash(EntityPlayer player, FluidStack fluid) {
        if (fluid != null) MASHED_ITEM.trigger(player, fluid);
    }

    public static void drankBrew(EntityPlayer player, FluidStack fluid) {
        if (fluid.getFluid() instanceof BrewFluid) DRANK_BREW.trigger(player, fluid);
    }

    private static Predicate<DrugProperties> drugEffects(JsonObject json) {
        List<Predicate<DrugProperties>> effects = new ArrayList<>();
        for (JsonElement element : JsonUtils.getJsonArray(json, "drugs")) {
            JsonObject condition = element.isJsonObject() ? element.getAsJsonObject() : null;
            String id = condition == null ? JsonUtils.getString(element, "drug") : JsonUtils.getString(condition, "id");
            DrugType type = DrugType.byId(id).orElseThrow(() -> new JsonSyntaxException("Unknown drug " + id));
            MinMaxBounds range = condition == null ? new MinMaxBounds(1.0E-5F, null)
                    : MinMaxBounds.deserialize(condition.get("value"));
            effects.add(properties -> range.test((float) properties.getActiveValue(type)));
        }
        return properties -> effects.stream().allMatch(effect -> effect.test(properties));
    }

    private static Predicate<FluidStack> mashedFluid(JsonObject json) {
        Fluid fluid = fluid(json);
        MinMaxBounds fermentation = MinMaxBounds.deserialize(json.get(BrewFluid.FERMENTATION));
        MinMaxBounds maturation = MinMaxBounds.deserialize(json.get(BrewFluid.MATURATION));
        MinMaxBounds distillation = MinMaxBounds.deserialize(json.get(BrewFluid.DISTILLATION));
        return stack -> stack.getFluid() == fluid && !BrewFluid.isVinegar(stack)
                && fermentation.test(BrewFluid.stage(stack, BrewFluid.FERMENTATION, BrewFluid.FERMENTATION_STEPS))
                && maturation.test(BrewFluid.stage(stack, BrewFluid.MATURATION, BrewFluid.MATURATION_STEPS))
                && distillation.test(BrewFluid.stage(stack, BrewFluid.DISTILLATION, 16));
    }

    private static Predicate<FluidStack> brew(JsonObject json) {
        BrewFluid fluid = fluid(json);
        String drink = JsonUtils.getString(json, "drink");
        MinMaxBounds maturation = MinMaxBounds.deserialize(json.get(BrewFluid.MATURATION));
        return stack -> stack.getFluid() == fluid && fluid.isDrinkType(stack, drink)
                && maturation.test(BrewFluid.stage(stack, BrewFluid.MATURATION, BrewFluid.MATURATION_STEPS));
    }

    private static BrewFluid fluid(JsonObject json) {
        String id = JsonUtils.getString(json, "fluid");
        Fluid fluid = PSFluids.resolve(id);
        if (!(fluid instanceof BrewFluid)) throw new JsonSyntaxException("Unknown brew fluid " + id);
        return (BrewFluid) fluid;
    }
}
