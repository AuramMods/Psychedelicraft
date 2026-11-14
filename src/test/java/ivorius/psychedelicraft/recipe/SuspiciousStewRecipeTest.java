package ivorius.psychedelicraft.recipe;

import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.item.StewFlower;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SuspiciousStewRecipeTest {
    private final SuspiciousStewRecipe recipe = new SuspiciousStewRecipe();

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void everySmallFlowerWritesItsDonorEffectAndDuration() {
        Potion[] effects = {MobEffects.SATURATION, MobEffects.NIGHT_VISION, MobEffects.SATURATION,
                MobEffects.FIRE_RESISTANCE, MobEffects.BLINDNESS, MobEffects.WEAKNESS, MobEffects.WEAKNESS,
                MobEffects.WEAKNESS, MobEffects.WEAKNESS, MobEffects.REGENERATION, MobEffects.JUMP_BOOST,
                MobEffects.POISON, MobEffects.WITHER, MobEffects.NIGHT_VISION};
        int[] durations = {7, 100, 7, 80, 160, 180, 180, 180, 180, 160, 120, 240, 160, 100};

        StewFlower[] flowers = StewFlower.values();
        assertEquals(effects.length, flowers.length);
        for (int index = 0; index < flowers.length; index++) {
            ItemStack ingredient = flowers[index].ingredient().setStackDisplayName("Named flower");
            InventoryCrafting inventory = grid(2, 2, ingredient);
            assertTrue(recipe.matches(inventory, null));
            ItemStack result = recipe.getCraftingResult(inventory);
            assertSame(PSItems.SUSPICIOUS_STEW, result.getItem());
            assertEquals(1, result.getCount());
            List<PotionEffect> saved = PotionUtils.getFullEffectsFromItem(result.copy());
            assertEquals(1, saved.size());
            assertSame(effects[index], saved.get(0).getPotion());
            assertEquals(durations[index], saved.get(0).getDuration());
            assertEquals(0, saved.get(0).getAmplifier());
            assertTrue(recipe.getRemainingItems(inventory).stream().allMatch(ItemStack::isEmpty));
            assertEquals("Named flower", ingredient.getDisplayName());
        }
    }

    @Test
    public void rejectsNonflowersDuplicatesBadMetadataAndOneRowGrids() {
        assertFalse(recipe.matches(grid(2, 2, new ItemStack(PSItems.MORNING_GLORY)), null));
        assertFalse(recipe.matches(grid(2, 2, new ItemStack(PSItems.PITCHER_PLANT)), null));
        assertFalse(recipe.matches(grid(2, 2, new ItemStack(Blocks.RED_FLOWER, 1, 9)), null));
        assertFalse(recipe.matches(grid(4, 1, StewFlower.POPPY.ingredient()), null));
        assertFalse(recipe.matches(grid(2, 2, ItemStack.EMPTY), null));
        InventoryCrafting larger = grid(3, 3, StewFlower.POPPY.ingredient());
        larger.setInventorySlotContents(5, StewFlower.DANDELION.ingredient());
        assertTrue(recipe.getCraftingResult(larger).isEmpty());
        larger.setInventorySlotContents(5, new ItemStack(Items.BOWL));
        assertFalse(recipe.matches(larger, null));
    }

    private static InventoryCrafting grid(int width, int height, ItemStack flower) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, width, height);
        grid.setInventorySlotContents(0, new ItemStack(Items.BOWL));
        grid.setInventorySlotContents(1, new ItemStack(Blocks.BROWN_MUSHROOM));
        grid.setInventorySlotContents(2, flower);
        grid.setInventorySlotContents(3, new ItemStack(Blocks.RED_MUSHROOM));
        return grid;
    }
}
