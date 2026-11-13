package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.HarmoniumItem;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.DyeUtils;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class HarmoniumDyeRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new HarmoniumDyeRecipe().setRegistryName(Psychedelicraft.id("dye_harmonium")));
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return !getCraftingResult(inventory).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        ItemStack harmonium = ItemStack.EMPTY;
        int[] channels = new int[3];
        int brightness = 0;
        int samples = 0;
        boolean hasDye = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            int color;
            if (stack.getItem() == PSItems.HARMONIUM) {
                if (!harmonium.isEmpty()) return ItemStack.EMPTY;
                harmonium = stack;
                if (!HarmoniumItem.hasColor(stack)) continue;
                color = HarmoniumItem.getColor(stack);
            } else {
                EnumDyeColor dye = DyeUtils.colorFromStack(stack).orElse(null);
                if (dye == null) return ItemStack.EMPTY;
                color = HarmoniumItem.packColor(dye.getColorComponentValues());
                hasDye = true;
            }
            int red = color >> 16 & 255;
            int green = color >> 8 & 255;
            int blue = color & 255;
            brightness += Math.max(red, Math.max(green, blue));
            channels[0] += red;
            channels[1] += green;
            channels[2] += blue;
            samples++;
        }
        if (harmonium.isEmpty() || !hasDye) return ItemStack.EMPTY;
        int red = channels[0] / samples;
        int green = channels[1] / samples;
        int blue = channels[2] / samples;
        int peak = Math.max(red, Math.max(green, blue));
        float scale = peak == 0 ? 0 : (float) brightness / samples / peak;
        ItemStack result = harmonium.copy();
        result.setCount(1);
        HarmoniumItem.setColor(result, (int) (red * scale) << 16 | (int) (green * scale) << 8 | (int) (blue * scale));
        return result;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
