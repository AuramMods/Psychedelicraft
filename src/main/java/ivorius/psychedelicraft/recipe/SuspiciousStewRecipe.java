package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.StewFlower;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class SuspiciousStewRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new SuspiciousStewRecipe().setRegistryName(Psychedelicraft.id("suspicious_stew")));
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return findFlower(inventory) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        StewFlower flower = findFlower(inventory);
        return flower == null ? ItemStack.EMPTY : flower.stew();
    }

    @Nullable
    private static StewFlower findFlower(InventoryCrafting inventory) {
        if (!fits(inventory.getWidth(), inventory.getHeight())) return null;
        Item redMushroom = Item.getItemFromBlock(Blocks.RED_MUSHROOM);
        Item brownMushroom = Item.getItemFromBlock(Blocks.BROWN_MUSHROOM);
        boolean red = false;
        boolean brown = false;
        boolean bowl = false;
        StewFlower flower = null;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!red && item == redMushroom) red = true;
            else if (!brown && item == brownMushroom) brown = true;
            else if (!bowl && item == Items.BOWL) bowl = true;
            else if (flower == null) {
                flower = StewFlower.fromStack(stack);
                if (flower == null) return null;
            } else return null;
        }
        return red && brown && bowl ? flower : null;
    }

    private static boolean fits(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public boolean canFit(int width, int height) {
        return fits(width, height);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
