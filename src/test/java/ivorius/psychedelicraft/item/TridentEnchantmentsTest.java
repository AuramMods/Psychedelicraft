package ivorius.psychedelicraft.item;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static ivorius.psychedelicraft.item.TridentEnchantments.CHANNELING;
import static ivorius.psychedelicraft.item.TridentEnchantments.IMPALING;
import static ivorius.psychedelicraft.item.TridentEnchantments.LOYALTY;
import static ivorius.psychedelicraft.item.TridentEnchantments.RIPTIDE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TridentEnchantmentsTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void riptideExcludesLoyaltyAndChannelingInEitherOrder() {
        assertFalse(RIPTIDE.isCompatibleWith(LOYALTY));
        assertFalse(LOYALTY.isCompatibleWith(RIPTIDE));
        assertFalse(RIPTIDE.isCompatibleWith(CHANNELING));
        assertFalse(CHANNELING.isCompatibleWith(RIPTIDE));
        assertTrue(RIPTIDE.isCompatibleWith(IMPALING));
        assertTrue(LOYALTY.isCompatibleWith(CHANNELING));
    }

    @Test
    public void enchantingUsesTheTridentCategoryAndDonorRanges() {
        assertTrue(LOYALTY.type.canEnchantItem(new TridentItem()));
        assertFalse(LOYALTY.canApply(new ItemStack(Items.IRON_SWORD)));
        assertEquals(26, LOYALTY.getMinEnchantability(LOYALTY.getMaxLevel()));
        assertEquals(33, IMPALING.getMinEnchantability(IMPALING.getMaxLevel()));
        assertEquals(53, IMPALING.getMaxEnchantability(IMPALING.getMaxLevel()));
        assertEquals(31, RIPTIDE.getMinEnchantability(RIPTIDE.getMaxLevel()));
        assertEquals(25, CHANNELING.getMinEnchantability(CHANNELING.getMaxLevel()));
    }
}
