/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.config.PSConfig;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;

import java.util.Random;

public final class FarmerTrades {
    private static final int VANILLA_DONOR_OFFERS = 5;
    private static final Offer[] OFFERS = {
            new Offer("wine_grapes", 2, 3, 8, 0.5F),
            new Offer("hop_cones", 1, 1, 4, 0.6F),
            new Offer("hop_seeds", 1, 1, 4, 0.4F),
            new Offer("wooden_mug", 1, 1, 4, 0.5F),
            new Offer("dried_tobacco", 4, 1, 4, 0.3F),
            new Offer("cigarette", 2, 1, 4, 0.8F),
            new Offer("cigar", 2, 1, 4, 0.8F),
            new Offer("tobacco_seeds", 2, 1, 4, 0.3F),
            new Offer("coffee_beans", 1, 1, 4, 0.8F),
            new Offer("coffea_cherries", 1, 1, 4, 0.6F)
    };

    private FarmerTrades() {
    }

    public static void register() {
        VillagerRegistry.FARMER.getCareer(0).addTrade(1, (merchant, recipes, random) -> addOffers(recipes, random));
    }

    static void addOffers(MerchantRecipeList recipes, Random random) {
        if (!PSConfig.balancing.worldGeneration.farmerDrugDeals) {
            return;
        }
        // Modern farmers make two distinct draws from the combined novice pool.
        // Native 1.12 trades are installed by Forge, so only emit our side of those draws.
        int poolSize = VANILLA_DONOR_OFFERS + OFFERS.length;
        int first = random.nextInt(poolSize);
        int second = random.nextInt(poolSize - 1);
        addOffer(recipes, first);
        addOffer(recipes, second >= first ? second + 1 : second);
    }

    private static void addOffer(MerchantRecipeList recipes, int draw) {
        if (draw < VANILLA_DONOR_OFFERS) {
            return;
        }
        Offer offer = OFFERS[draw - VANILLA_DONOR_OFFERS];
        // A baseline item becomes available here as soon as its Forge registration is ported.
        Item item = ForgeRegistries.ITEMS.getValue(offer.item);
        if (item != null && item != Items.AIR) {
            recipes.add(new MerchantRecipe(new ItemStack(Items.EMERALD, offer.cost), ItemStack.EMPTY,
                    new ItemStack(item, offer.quantity), 0, offer.maxUses));
        }
    }

    static float priceMultiplier(MerchantRecipe recipe) {
        if (recipe.getItemToBuy().getItem() != Items.EMERALD || recipe.hasSecondItemToBuy()) return -1;
        ItemStack output = recipe.getItemToSell();
        for (Offer offer : OFFERS) {
            if (offer.item.equals(output.getItem().getRegistryName()) && offer.quantity == output.getCount()
                    && offer.cost == recipe.getItemToBuy().getCount()) return offer.multiplier;
        }
        return -1;
    }

    private static final class Offer {
        private final ResourceLocation item;
        private final int cost;
        private final int quantity;
        private final int maxUses;
        private final float multiplier;

        private Offer(String name, int cost, int quantity, int maxUses, float multiplier) {
            item = Psychedelicraft.id(name);
            this.cost = cost;
            this.quantity = quantity;
            this.maxUses = maxUses;
            this.multiplier = multiplier;
        }
    }
}
