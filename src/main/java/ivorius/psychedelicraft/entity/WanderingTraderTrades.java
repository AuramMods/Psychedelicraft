package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.BitSet;
import java.util.Random;

final class WanderingTraderTrades {
    // Keep every donor slot so unavailable items do not shift the odds of the ported offers.
    private static final Offer[] COMMON = {
            offer("sea_pickle", 2, 1, 5),
            offer("slime_ball", 4, 1, 5),
            offer("glowstone", 2, 1, 5),
            modOffer("nautilus_shell", 5, 1, 5),
            offer("tallgrass", 1, 1, 12, 2),
            offer("reeds", 1, 1, 8),
            offer("pumpkin", 1, 1, 4),
            modOffer("kelp", 3, 1, 12),
            offer("cactus", 3, 1, 8),
            offer("yellow_flower", 1, 1, 12),
            offer("red_flower", 1, 1, 12),
            offer("red_flower", 1, 1, 8, 1),
            offer("red_flower", 1, 1, 12, 2),
            offer("red_flower", 1, 1, 12, 3),
            offer("red_flower", 1, 1, 12, 4),
            offer("red_flower", 1, 1, 12, 5),
            offer("red_flower", 1, 1, 12, 6),
            offer("red_flower", 1, 1, 12, 7),
            offer("red_flower", 1, 1, 12, 8),
            modOffer("cornflower", 1, 1, 12),
            modOffer("lily_of_the_valley", 1, 1, 7),
            offer("wheat_seeds", 1, 1, 12),
            offer("beetroot_seeds", 1, 1, 12),
            offer("pumpkin_seeds", 1, 1, 12),
            offer("melon_seeds", 1, 1, 12),
            offer("sapling", 5, 1, 8, 4),
            offer("sapling", 5, 1, 8, 2),
            offer("sapling", 5, 1, 8, 5),
            offer("sapling", 5, 1, 8, 3),
            offer("sapling", 5, 1, 8),
            offer("sapling", 5, 1, 8, 1),
            offer("cherry_sapling", 5, 1, 8),
            offer("mangrove_propagule", 5, 1, 8),
            offer("dye", 1, 3, 12, 1),
            offer("dye", 1, 3, 12, 15),
            offer("dye", 1, 3, 12, 4),
            offer("dye", 1, 3, 12, 9),
            offer("dye", 1, 3, 12),
            offer("dye", 1, 3, 12, 2),
            offer("dye", 1, 3, 12, 7),
            offer("dye", 1, 3, 12, 13),
            offer("dye", 1, 3, 12, 11),
            offer("dye", 1, 3, 12, 8),
            offer("dye", 1, 3, 12, 5),
            offer("dye", 1, 3, 12, 12),
            offer("dye", 1, 3, 12, 10),
            offer("dye", 1, 3, 12, 14),
            offer("dye", 1, 3, 12, 3),
            offer("dye", 1, 3, 12, 6),
            offer("brain_coral_block", 3, 1, 8),
            offer("bubble_coral_block", 3, 1, 8),
            offer("fire_coral_block", 3, 1, 8),
            offer("horn_coral_block", 3, 1, 8),
            offer("tube_coral_block", 3, 1, 8),
            offer("vine", 1, 1, 12),
            offer("brown_mushroom", 1, 1, 12),
            offer("red_mushroom", 1, 1, 12),
            offer("waterlily", 1, 2, 5),
            offer("small_dripleaf", 1, 2, 5),
            offer("sand", 1, 8, 8),
            offer("sand", 1, 4, 6, 1),
            offer("pointed_dripstone", 1, 2, 5),
            modOffer("rooted_dirt", 1, 2, 5),
            modOffer("moss_block", 1, 2, 5)
    };
    private static final Offer[] RARE = {
            offer("tropical_fish_bucket", 5, 1, 4),
            offer("pufferfish_bucket", 5, 1, 4),
            offer("packed_ice", 3, 1, 6),
            offer("blue_ice", 6, 1, 6),
            offer("gunpowder", 1, 1, 8),
            offer("dirt", 3, 3, 6, 2)
    };

    private WanderingTraderTrades() {
    }

    static MerchantRecipeList draw(Random random) {
        BitSet slots = new BitSet(COMMON.length);
        while (slots.cardinality() < 5) slots.set(random.nextInt(COMMON.length));
        MerchantRecipeList recipes = new MerchantRecipeList();
        for (int slot = slots.nextSetBit(0); slot >= 0; slot = slots.nextSetBit(slot + 1)) {
            COMMON[slot].addTo(recipes);
        }
        RARE[random.nextInt(RARE.length)].addTo(recipes);
        return recipes;
    }

    private static Offer offer(String path, int cost, int count, int uses) {
        return offer(path, cost, count, uses, 0);
    }

    private static Offer offer(String path, int cost, int count, int uses, int metadata) {
        return new Offer(new ResourceLocation("minecraft", path), metadata, cost, count, uses);
    }

    private static Offer modOffer(String path, int cost, int count, int uses) {
        return new Offer(Psychedelicraft.id(path), 0, cost, count, uses);
    }

    private static final class Offer {
        private final ResourceLocation itemId;
        private final int metadata;
        private final int cost;
        private final int count;
        private final int uses;

        private Offer(ResourceLocation itemId, int metadata, int cost, int count, int uses) {
            this.itemId = itemId;
            this.metadata = metadata;
            this.cost = cost;
            this.count = count;
            this.uses = uses;
        }

        private void addTo(MerchantRecipeList recipes) {
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null && item != Items.AIR) {
                recipes.add(new MerchantRecipe(new ItemStack(Items.EMERALD, cost), ItemStack.EMPTY,
                        new ItemStack(item, count, metadata), 0, uses));
            }
        }
    }
}
