package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.village.MerchantTradeOffersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class VillagerPrices {
    private static final Map<EntityVillager, CustomerPrices> SESSIONS = new WeakHashMap<>();
    private static final Potion HERO = new HeroEffect().setPotionName("effect.psychedelicraft.hero_of_the_village");

    private VillagerPrices() {}

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Potion> event) {
        event.getRegistry().register(HERO.setRegistryName(Psychedelicraft.id("hero_of_the_village")));
    }

    public static void customerChanged(EntityVillager villager, @Nullable EntityPlayer player) {
        if (villager.world.isRemote || villager.getCustomer() == player) return;
        if (player == null) {
            SESSIONS.remove(villager);
        } else {
            PotionEffect effect = player.getActivePotionEffect(HERO);
            SESSIONS.put(villager, new CustomerPrices(VillagerReputation.reputation(villager, player),
                    effect == null ? -1 : effect.getAmplifier()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void adjustOffers(MerchantTradeOffersEvent event) {
        if (!(event.getMerchant() instanceof EntityVillager) || event.getPlayer() == null || event.getList() == null) return;
        EntityVillager villager = (EntityVillager) event.getMerchant();
        if (villager.world.isRemote || villager.getCustomer() != event.getPlayer()) return;
        CustomerPrices prices = SESSIONS.get(villager);
        if (prices == null || prices.reputation == 0 && prices.hero < 0) return;
        MerchantRecipeList offers = event.getList();
        for (int index = 0; index < offers.size(); index++) {
            MerchantRecipe offer = offers.get(index);
            int price = VillagerCareers.customerPrice(villager, offer, prices.reputation, prices.hero);
            if (price != offer.getItemToBuy().getCount()) offers.set(index, new PricedOffer(offer, price));
        }
    }

    static int adjustedCost(int price, int baseCost, int maxStack, float multiplier, int reputation, int hero) {
        int adjustment = -MathHelper.floor(reputation * multiplier);
        if (hero >= 0) adjustment -= Math.max(1, MathHelper.floor(baseCost * (0.3D + 0.0625D * hero)));
        return MathHelper.clamp(price + adjustment, 1, maxStack);
    }

    private static final class CustomerPrices {
        final int reputation;
        final int hero;

        CustomerPrices(int reputation, int hero) {
            this.reputation = reputation;
            this.hero = hero;
        }
    }

    // The native merchant inventory consumes this price and the original offer retains its stock.
    private static final class PricedOffer extends MerchantRecipe {
        private final MerchantRecipe stock;

        PricedOffer(MerchantRecipe stock, int price) {
            super(stock.getItemToBuy().copy(), stock.getSecondItemToBuy().copy(), stock.getItemToSell().copy());
            getItemToBuy().setCount(price);
            this.stock = stock;
        }

        @Override public int getToolUses() { return stock.getToolUses(); }
        @Override public int getMaxTradeUses() { return stock.getMaxTradeUses(); }
        @Override public void incrementToolUses() { stock.incrementToolUses(); }
        @Override public void increaseMaxTradeUses(int amount) { stock.increaseMaxTradeUses(amount); }
        @Override public boolean isRecipeDisabled() { return stock.isRecipeDisabled(); }
        @Override public boolean getRewardsExp() { return stock.getRewardsExp(); }

        @Override
        public NBTTagCompound writeToTags() {
            NBTTagCompound tag = stock.writeToTags();
            tag.setTag("buy", getItemToBuy().writeToNBT(new NBTTagCompound()));
            return tag;
        }
    }

    private static final class HeroEffect extends Potion {
        private static final ResourceLocation ICON = new ResourceLocation("textures/items/emerald.png");

        HeroEffect() {
            super(false, 4521796);
            setBeneficial();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
            drawIcon(x + 6, y + 7);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
            drawIcon(x + 3, y + 3);
        }

        @SideOnly(Side.CLIENT)
        private void drawIcon(int x, int y) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ICON);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
        }
    }
}
