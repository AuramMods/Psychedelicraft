package ivorius.psychedelicraft.recipe;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class CampfireRecipe {
    public static final int COOK_TICKS = 600;

    private CampfireRecipe() {
    }

    public static ItemStack cook(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.BEEF) return new ItemStack(Items.COOKED_BEEF);
        if (item == Items.CHICKEN) return new ItemStack(Items.COOKED_CHICKEN);
        if (item == Items.MUTTON) return new ItemStack(Items.COOKED_MUTTON);
        if (item == Items.PORKCHOP) return new ItemStack(Items.COOKED_PORKCHOP);
        if (item == Items.RABBIT) return new ItemStack(Items.COOKED_RABBIT);
        if (item == Items.POTATO) return new ItemStack(Items.BAKED_POTATO);
        if (item == Items.FISH && (stack.getMetadata() == 0 || stack.getMetadata() == 1)) {
            return new ItemStack(Items.COOKED_FISH, 1, stack.getMetadata());
        }
        return ItemStack.EMPTY;
    }
}
