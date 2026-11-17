/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.recipe;

import com.google.gson.*;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import java.util.*;
import java.util.function.Predicate;

/** Assign each occupied slot once, then drain that same ingredient's remainder on a copy. */
public final class FluidAwareShapelessRecipe extends CustomRecipe {
    private final String group;
    private final ItemStack output;
    private final List<Input> inputs;
    public FluidAwareShapelessRecipe(ResourceLocation id, String group, CraftingBookCategory category, ItemStack output, List<Input> inputs) {
        super(id, category);
        this.group = group; this.output = output.copy(); this.inputs = List.copyOf(inputs);
    }
    public List<Input> getFluidAwareIngredients() { return inputs; }
    private static List<ItemStack> occupied(CraftingContainer inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) if (!inventory.getItem(i).isEmpty()) stacks.add(inventory.getItem(i));
        return stacks;
    }
    @Override public boolean matches(CraftingContainer inventory, Level level) { return RecipeMatcher.findMatches(occupied(inventory), inputs) != null; }
    @Override public ItemStack assemble(CraftingContainer inventory, RegistryAccess registries) { return output.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return output.copy(); }
    @Override public String getGroup() { return group; }
    @Override public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> result = NonNullList.create();
        for (Input input : inputs) result.add(input.displayIngredient());
        return result;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingContainer inventory) {
        NonNullList<ItemStack> result = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
        int[] assignment = RecipeMatcher.findMatches(occupied(inventory), inputs);
        if (assignment == null) return result;
        int index = 0;
        for (int slot = 0; slot < result.size(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) result.set(slot, inputs.get(assignment[index++]).remainder(stack));
        }
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= inputs.size(); }
    @Override public RecipeSerializer<?> getSerializer() { return PSRecipes.SHAPELESS_FLUID.get(); }

    /** A null fluid means an ordinary ingredient; an empty vessel means any fluid-capable item. */
    public record Input(Ingredient vessel, Fluid fluid, int amount, CompoundTag attributes, boolean consumeContainer) implements Predicate<ItemStack> {
        public Input(Ingredient vessel,Fluid fluid,int amount,CompoundTag attributes){this(vessel,fluid,amount,attributes,false);}
        public Input { attributes = attributes.copy(); }
        @Override public CompoundTag attributes() { return attributes.copy(); }
        @Override public boolean test(ItemStack stack) {
            if (stack.isEmpty() || !vessel.isEmpty() && !vessel.test(stack)) return false;
            if (fluid == null) return vessel.test(stack);
            var handler = FluidVesselHandler.forTransfer(stack);
            return handler != null && !matching(handler).isEmpty();
        }
        private FluidStack matching(IFluidHandlerItem handler) {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack contents = FluidContents.normalize(handler.getFluidInTank(tank));
                if (contents.isEmpty() || contents.getFluid() != fluid || contents.getAmount() < amount
                        || !NbtUtils.compareNbt(attributes, contents.getTag() == null ? new CompoundTag() : contents.getTag(), true)) continue;
                if (amount <= 0) return contents;
                FluidStack request = contents.copy(); request.setAmount(amount);
                if (handler.drain(request, FluidAction.SIMULATE).isFluidStackIdentical(request)) return contents;
            }
            return FluidStack.EMPTY;
        }
        public ItemStack remainder(ItemStack stack) {
            // Some products incorporate the vessel itself (lava glass -> solid obsidian bottle).
            if (consumeContainer) return ItemStack.EMPTY;
            if (fluid == null || amount <= 0) return stack.getCraftingRemainingItem();
            var handler = FluidVesselHandler.forTransfer(stack);
            if (handler == null) return ItemStack.EMPTY;
            FluidStack contents = matching(handler);
            if (contents.isEmpty()) return ItemStack.EMPTY;
            contents.setAmount(amount);
            return handler.drain(contents, FluidAction.EXECUTE).isFluidStackIdentical(contents) ? handler.getContainer() : ItemStack.EMPTY;
        }
        public Ingredient displayIngredient() {
            if (fluid == null) return vessel;
            List<ItemStack> candidates = new ArrayList<>();
            if (!vessel.isEmpty()) candidates.addAll(Arrays.asList(vessel.getItems()));
            else { PSItems.ITEMS.getEntries().forEach(item -> candidates.add(new ItemStack(item.get()))); candidates.addAll(List.of(new ItemStack(Items.BUCKET), new ItemStack(Items.BOWL), new ItemStack(Items.GLASS_BOTTLE))); }
            List<ItemStack> samples = new ArrayList<>();
            for (ItemStack candidate : candidates) {
                var handler = FluidVesselHandler.forCrafting(candidate);
                if (handler == null || amount > handler.getTankCapacity(0)) continue;
                var sample = handler.replaceContents(new FluidStack(fluid, amount > 0 ? amount : handler.getTankCapacity(0), attributes));
                if (test(sample)) samples.add(sample);
            }
            return Ingredient.of(samples.stream());
        }
        static Input parse(JsonElement element) {
            if (!element.isJsonObject() || !element.getAsJsonObject().has("fluid")) return new Input(Ingredient.fromJson(element), null, 0, new CompoundTag(),
                    element.isJsonObject() && GsonHelper.getAsBoolean(element.getAsJsonObject(),"consume_container",false));
            JsonObject json = element.getAsJsonObject();
            JsonObject vesselSpec=json.deepCopy();vesselSpec.remove("fluid");vesselSpec.remove("consume_container");
            Ingredient vessel = vesselSpec.size() > 0 ? Ingredient.fromJson(vesselSpec) : Ingredient.EMPTY;
            JsonElement spec = json.get("fluid");
            JsonObject value = spec.isJsonObject() ? spec.getAsJsonObject() : new JsonObject();
            String id = spec.isJsonObject() ? GsonHelper.getAsString(value, "fluid") : spec.getAsString();
            Fluid fluid = PSFluids.resolve(id);
            if (fluid == null) throw new JsonSyntaxException("Unknown crafting fluid " + id);
            CompoundTag attributes = new CompoundTag();
            if (value.has("attributes")) try {
                JsonElement tag = value.get("attributes"); attributes = TagParser.parseTag(tag.isJsonPrimitive() ? tag.getAsString() : tag.toString());
            } catch (Exception error) { throw new JsonSyntaxException("Invalid crafting fluid attributes", error); }
            return new Input(vessel, fluid, GsonHelper.getAsInt(value, "level", -1), attributes,GsonHelper.getAsBoolean(json,"consume_container",false));
        }
    }
    public static final class Serializer implements RecipeSerializer<FluidAwareShapelessRecipe> {
        @Override public FluidAwareShapelessRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<Input> inputs = new ArrayList<>();
            for (JsonElement entry : GsonHelper.getAsJsonArray(json, "ingredients")) inputs.add(Input.parse(entry));
            if (inputs.isEmpty() || inputs.size() > 9) throw new JsonSyntaxException("Expected one to nine shapeless ingredients");
            return new FluidAwareShapelessRecipe(id, GsonHelper.getAsString(json, "group", ""),
                    CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", "misc"), CraftingBookCategory.MISC),
                    CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true), inputs);
        }
        @Override public FluidAwareShapelessRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String group = buffer.readUtf(); CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class); ItemStack output = buffer.readItem();
            int count = buffer.readVarInt(); List<Input> inputs = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Ingredient vessel = Ingredient.fromNetwork(buffer);
                Fluid fluid = buffer.readBoolean() ? Objects.requireNonNull(PSFluids.resolve(buffer.readResourceLocation().toString())) : null;
                inputs.add(new Input(vessel, fluid, buffer.readVarInt(), Objects.requireNonNullElseGet(buffer.readNbt(), CompoundTag::new),buffer.readBoolean()));
            }
            return new FluidAwareShapelessRecipe(id, group, category, output, inputs);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, FluidAwareShapelessRecipe recipe) {
            buffer.writeUtf(recipe.group); buffer.writeEnum(recipe.category()); buffer.writeItem(recipe.output); buffer.writeVarInt(recipe.inputs.size());
            for (Input input : recipe.inputs) {
                input.vessel.toNetwork(buffer); buffer.writeBoolean(input.fluid != null);
                if (input.fluid != null) buffer.writeResourceLocation(new ResourceLocation(PSFluids.id(input.fluid)));
                buffer.writeVarInt(input.amount); buffer.writeNbt(input.attributes);
                buffer.writeBoolean(input.consumeContainer);
            }
        }
    }
}
