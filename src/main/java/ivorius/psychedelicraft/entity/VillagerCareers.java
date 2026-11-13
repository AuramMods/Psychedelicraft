/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Based on the 1.20.1 villager professions and offers by Sollace.
 */

package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.advancement.PSCriteria;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.network.Channel;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class VillagerCareers {
    public static final VillagerProfession UNEMPLOYED = profession("unemployed");
    public static final VillagerProfession DEALER = profession("drug_dealer");
    public static final VillagerProfession ADDICT = profession("drug_addict");
    private static final DamageSource OVERDOSE = new DamageSource("psychedelicraft.overdose");
    private static final String CAREER = "psychedelicraftCareer";
    private static final String INFECTED_TRADES = "psychedelicraftInfectedTrades";

    private VillagerCareers() {
    }

    private static VillagerProfession profession(String name) {
        return new VillagerProfession(Psychedelicraft.id(name).toString(), "minecraft:textures/entity/villager/villager.png",
                "minecraft:textures/entity/zombie_villager/zombie_villager.png");
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<VillagerProfession> event) {
        new VillagerCareer(UNEMPLOYED, "psychedelicraft.unemployed");
        new VillagerCareer(DEALER, "psychedelicraft.drug_dealer")
                .addTrade(1, (merchant, recipes, random) -> appendOffers(DEALER, 0, recipes, random));
        new VillagerCareer(ADDICT, "psychedelicraft.drug_addict")
                .addTrade(1, (merchant, recipes, random) -> appendOffers(ADDICT, 0, recipes, random));
        event.getRegistry().registerAll(UNEMPLOYED, DEALER, ADDICT);
    }

    public static void setRandomProfession(EntityVillager villager, Random random) {
        villager.setProfession(randomProfession(random));
    }

    public static void setRandomProfession(EntityZombieVillager villager, Random random) {
        villager.setForgeProfession(randomProfession(random));
    }

    private static VillagerProfession randomProfession(Random random) {
        List<VillagerProfession> professions = new ArrayList<>(ForgeRegistries.VILLAGER_PROFESSIONS.getValuesCollection());
        professions.remove(DEALER);
        professions.remove(ADDICT);
        professions.remove(UNEMPLOYED);
        VillagerProfession profession = professions.get(random.nextInt(professions.size()));
        // 1.12 has only a nitwit slot for villagers without a job.
        return isNitwit(profession) && random.nextBoolean() ? UNEMPLOYED : profession;
    }

    private static boolean isNitwit(VillagerProfession profession) {
        return "minecraft:nitwit".equals(profession.getRegistryName().toString());
    }

    @SubscribeEvent
    public static void addTasks(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityVillager) {
            EntityVillager villager = (EntityVillager) event.getEntity();
            VillagerWork.rememberWorksiteDimension(villager);
            if (villager.tasks.taskEntries.stream().noneMatch(entry -> entry.action instanceof VillagerWork)) {
                VillagerTradePreview preview = new VillagerTradePreview(villager);
                villager.tasks.addTask(7, new VillagerWork(villager, preview));
                villager.tasks.addTask(8, preview);
            }
        }
    }

    @SubscribeEvent
    public static void syncTradePreview(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EntityVillager) || !(event.getEntityPlayer() instanceof EntityPlayerMP)) return;
        EntityVillager villager = (EntityVillager) event.getTarget();
        for (EntityAITasks.EntityAITaskEntry entry : villager.tasks.taskEntries) {
            if (entry.action instanceof VillagerTradePreview) {
                ItemStack shown = ((VillagerTradePreview) entry.action).shown();
                if (!shown.isEmpty()) Channel.showVillagerOffer(villager, shown, (EntityPlayerMP) event.getEntityPlayer());
                return;
            }
        }
    }

    @SubscribeEvent
    public static void stopDisabledPreview(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityVillager) || event.getEntityLiving().world.isRemote) return;
        EntityVillager villager = (EntityVillager) event.getEntityLiving();
        if (!villager.isAIDisabled()) return;
        for (EntityAITasks.EntityAITaskEntry entry : villager.tasks.taskEntries) {
            if (entry.using && entry.action instanceof VillagerTradePreview) {
                entry.action.resetTask();
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void releaseWorksite(LivingDeathEvent event) {
        if (event.getEntity().world.isRemote || !(event.getEntity() instanceof EntityVillager)) return;
        releaseWorksite((EntityVillager) event.getEntity());
    }

    public static void releaseWorksite(EntityVillager villager) {
        if (!(villager.world instanceof WorldServer) || villager.getProfessionForge() != DEALER) return;
        BlockPos pos = VillagerWork.worksite(villager);
        if (pos == null) return;
        WorksiteReleases.release((WorldServer) villager.world, VillagerWork.worksiteDimension(villager),
                pos, villager.getUniqueID());
        VillagerWork.forgetWorksite(villager);
    }

    @SubscribeEvent
    public static void releaseLoadedWorksites(ChunkEvent.Load event) {
        if (event.getWorld() instanceof WorldServer) {
            WorksiteReleases.onChunkLoad((WorldServer) event.getWorld(), event.getChunk());
        }
    }

    public static boolean collectExtra(EntityVillager villager, EntityItem entity) {
        if (villager.getProfessionForge() != DEALER) return false;
        ItemStack stack = entity.getItem();
        Item item = stack.getItem();
        if (!(VillagerWork.isSeed(item) || item == PSItems.CANNABIS_BUDS || item == PSItems.CANNABIS_LEAF
                || item == PSItems.TOBACCO_LEAVES || item == PSItems.COCA_LEAVES || item == PSItems.PEYOTE
                || item == Items.DYE && stack.getMetadata() == EnumDyeColor.WHITE.getDyeDamage())) return false;
        ItemStack remainder = villager.getVillagerInventory().addItem(stack);
        if (remainder.isEmpty()) entity.setDead();
        else entity.setItem(remainder);
        return true;
    }

    @SubscribeEvent
    public static void feedVillager(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityVillager) || event.getItemStack().getItem() != PSItems.HASH_MUFFIN) return;
        EntityVillager villager = (EntityVillager) event.getTarget();
        if (villager.isChild() || !villager.isEntityAlive()) return;
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        VillagerProfession profession = villager.getProfessionForge();
        if (!event.getWorld().isRemote && (profession == UNEMPLOYED || isNitwit(profession))) {
            if (!event.getEntityPlayer().isCreative()) event.getItemStack().shrink(1);
            Channel.playEntitySound(villager, SoundEvents.ENTITY_GENERIC_EAT, 1F + villager.getRNG().nextFloat(),
                    0.3F + villager.getRNG().nextFloat() * 0.7F);
            changeCareer(villager, ADDICT);
            PSCriteria.feedVillager(event.getEntityPlayer());
        }
    }

    static NBTTagCompound data(EntityVillager villager) {
        NBTTagCompound tag = villager.getEntityData();
        if (!tag.hasKey(CAREER, 10)) tag.setTag(CAREER, new NBTTagCompound());
        return tag.getCompoundTag(CAREER);
    }

    static NBTTagCompound savedData(EntityVillager villager) {
        return villager.getEntityData().getCompoundTag(CAREER);
    }

    public static boolean spawnInfected(World world, Entity replacement, EntityLivingBase original) {
        EntityVillager villager = (EntityVillager) original;
        EntityZombieVillager infected = (EntityZombieVillager) replacement;
        NBTTagCompound trades = new NBTTagCompound();
        trades.setTag("Offers", storedOffers(villager).getRecipiesAsTags());
        try {
            trades.setInteger("Career", TradeStorage.CAREER_ID.getInt(villager));
            trades.setInteger("CareerLevel", TradeStorage.CAREER_LEVEL.getInt(villager));
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not read the infected villager's career", error);
        }
        NBTTagCompound originalCareer = villager.getEntityData().getCompoundTag(CAREER);
        if (originalCareer.hasKey("Experience", 99)) trades.setInteger("Experience", originalCareer.getInteger("Experience"));
        if (originalCareer.hasKey("Demand", 10)) trades.setTag("Demand", originalCareer.getCompoundTag("Demand").copy());
        infected.getEntityData().setTag(INFECTED_TRADES, trades);
        VillagerReputation.copy(villager, infected);
        return world.spawnEntity(infected);
    }

    public static boolean spawnCured(World world, Entity replacement, EntityZombieVillager infected, @Nullable UUID healer) {
        EntityVillager villager = (EntityVillager) replacement;
        NBTTagCompound trades = infected.getEntityData().getCompoundTag(INFECTED_TRADES);
        if (trades.hasKey("Offers", 10)) {
            try {
                TradeStorage.OFFERS.set(villager, new MerchantRecipeList(trades.getCompoundTag("Offers")));
                TradeStorage.CAREER_ID.setInt(villager, trades.getInteger("Career"));
                TradeStorage.CAREER_LEVEL.setInt(villager, trades.getInteger("CareerLevel"));
            } catch (IllegalAccessException error) {
                throw new IllegalStateException("Could not restore the cured villager's trades", error);
            }
            if (trades.hasKey("Experience", 99)) data(villager).setInteger("Experience", trades.getInteger("Experience"));
            if (trades.hasKey("Demand", 10)) data(villager).setTag("Demand", trades.getCompoundTag("Demand").copy());
        }
        VillagerReputation.copy(infected, villager);
        VillagerReputation.cured(villager, healer);
        return world.spawnEntity(villager);
    }

    static void changeCareer(EntityVillager villager, VillagerProfession profession) {
        MerchantRecipeList recipes = storedOffers(villager);
        recipes.clear();
        villager.getEntityData().removeTag(CAREER);
        villager.setProfession(profession);
        if (profession == DEALER || profession == ADDICT) appendOffers(profession, 0, recipes, villager.getRNG());
    }

    public static boolean useRecipe(EntityVillager villager, MerchantRecipe recipe) {
        VillagerProfession profession = villager.getProfessionForge();
        if (!villager.world.isRemote) VillagerReputation.traded(villager);
        if (profession != DEALER && profession != ADDICT) return false;
        if (villager.world.isRemote) return true;

        recipe.incrementToolUses();
        villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1F, 1F);
        Offer offer = findOffer(profession, recipe);
        NBTTagCompound data = data(villager);
        int previous = data.getInteger("Experience");
        int experience = previous + (offer == null ? 0 : offer.experience);
        data.setInteger("Experience", experience);
        boolean levelUp = profession == DEALER && tier(previous) < tier(experience);
        if (levelUp) {
            appendOffers(profession, tier(experience), storedOffers(villager), villager.getRNG());
        }
        if (recipe.getRewardsExp()) {
            int reward = 3 + villager.getRNG().nextInt(4) + (levelUp ? 5 : 0);
            villager.world.spawnEntity(new EntityXPOrb(villager.world, villager.posX, villager.posY + 0.5D, villager.posZ, reward));
        }
        if (villager.getCustomer() instanceof EntityPlayerMP) {
            CriteriaTriggers.VILLAGER_TRADE.trigger((EntityPlayerMP) villager.getCustomer(), villager, recipe.getItemToSell());
        }
        if (profession == ADDICT) villager.attackEntityFrom(OVERDOSE, recipe.getToolUses() * recipe.getItemToSell().getCount() + 1F);
        return true;
    }

    static int tier(int experience) {
        return experience >= 70 ? 2 : experience >= 10 ? 1 : 0;
    }

    static boolean restock(EntityVillager villager) {
        if (villager.isTrading()) return false;
        VillagerProfession profession = villager.getProfessionForge();
        NBTTagCompound data = data(villager);
        long time = villager.world.getTotalWorldTime();
        long day = villager.world.getWorldTime() / 24000L;
        long lastRestock = data.getLong("LastRestock");
        int restocks = data.getInteger("Restocks");
        boolean reset = time - lastRestock > 12000L || data.hasKey("RestockDay", 99) && day > data.getLong("RestockDay");
        data.setLong("RestockDay", day);
        MerchantRecipeList recipes = storedOffers(villager);
        if (reset) {
            int missed = 2 - restocks;
            if (missed > 0) updateStock(data, recipes, profession, missed);
            lastRestock = time;
            restocks = 0;
            data.setLong("LastRestock", lastRestock);
            data.setInteger("Restocks", restocks);
        }
        if (restocks >= 2 || restocks > 0 && time - lastRestock <= 2400L) return false;
        if (recipes.stream().noneMatch(recipe -> recipe.getToolUses() > 0 && findOffer(profession, recipe) != null)) return false;

        updateStock(data, recipes, profession, 0);
        data.setLong("LastRestock", time);
        data.setInteger("Restocks", restocks + 1);
        if (profession == ADDICT) villager.attackEntityFrom(OVERDOSE, 1F + villager.getRNG().nextFloat() * 5F);
        return true;
    }

    static void updateStock(NBTTagCompound data, MerchantRecipeList recipes, VillagerProfession profession, int missedRestocks) {
        NBTTagCompound demand = data.getCompoundTag("Demand");
        for (int slot = 0; slot < recipes.size(); slot++) {
            MerchantRecipe recipe = recipes.get(slot);
            Offer offer = findOffer(profession, recipe);
            if (offer == null) continue;
            String id = offer.item.getRegistryName().toString();
            // The donor treats a missed restock as a restock with zero uses.
            int change = missedRestocks > 0 ? -missedRestocks * recipe.getMaxTradeUses()
                    : recipe.getToolUses() * 2 - recipe.getMaxTradeUses();
            int value = demand.getInteger(id) + change;
            demand.setInteger(id, value);
            ItemStack buy = recipe.getItemToBuy().copy();
            buy.setCount(MathHelper.clamp(demandPrice(offer, value), 1, buy.getMaxStackSize()));
            NBTTagCompound offerTag = recipe.writeToTags();
            offerTag.setTag("buy", buy.writeToNBT(new NBTTagCompound()));
            offerTag.setInteger("uses", 0);
            recipes.set(slot, new MerchantRecipe(offerTag));
        }
        data.setTag("Demand", demand);
    }

    private static MerchantRecipeList storedOffers(EntityVillager villager) {
        // Forge posts MerchantTradeOffersEvent with a copy of the list returned by getRecipes.
        try {
            MerchantRecipeList offers = (MerchantRecipeList) TradeStorage.OFFERS.get(villager);
            if (offers != null) return offers;
            villager.getRecipes(null);
            return (MerchantRecipeList) TradeStorage.OFFERS.get(villager);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Could not access the villager's stored trades", error);
        }
    }

    private static final class TradeStorage {
        private static final Field OFFERS = ReflectionHelper.findField(EntityVillager.class, "buyingList", "field_70963_i");
        private static final Field CAREER_ID = ReflectionHelper.findField(EntityVillager.class, "careerId", "field_175563_bv");
        private static final Field CAREER_LEVEL = ReflectionHelper.findField(EntityVillager.class, "careerLevel", "field_175562_bw");
    }

    private static void appendOffers(VillagerProfession profession, int tier, MerchantRecipeList recipes, Random random) {
        List<Offer> pool = new ArrayList<>(Arrays.asList(profession == DEALER ? Offers.DEALER[tier] : Offers.ADDICT));
        if (profession == DEALER && tier == 2 && PSConfig.balancing.enableHarmonium) pool.add(Offers.HARMONIUM);
        for (int i = 0; i < 2 && !pool.isEmpty(); i++) {
            Offer offer = pool.remove(random.nextInt(pool.size()));
            recipes.add(offer.create());
        }
    }

    private static Offer findOffer(VillagerProfession profession, MerchantRecipe recipe) {
        if (profession == DEALER) {
            if (Offers.HARMONIUM.matches(recipe)) return Offers.HARMONIUM;
            for (Offer[] tier : Offers.DEALER) for (Offer offer : tier) if (offer.matches(recipe)) return offer;
        } else if (profession == ADDICT) {
            for (Offer offer : Offers.ADDICT) if (offer.matches(recipe)) return offer;
        }
        return null;
    }

    static int customerPrice(EntityVillager villager, MerchantRecipe recipe, int reputation, int hero) {
        VillagerProfession profession = villager.getProfessionForge();
        ItemStack buy = recipe.getItemToBuy();
        int price = buy.getCount();
        if (buy.isEmpty()) return price;
        if (profession == VillagerRegistry.FARMER) {
            float multiplier = FarmerTrades.priceMultiplier(recipe);
            return multiplier < 0 ? price : VillagerPrices.adjustedCost(price, price, buy.getMaxStackSize(), multiplier, reputation, hero);
        }
        Offer offer = findOffer(profession, recipe);
        if (offer == null) return price;
        NBTTagCompound demand = data(villager).getCompoundTag("Demand");
        String key = offer.item.getRegistryName().toString();
        if (demand.hasKey(key)) {
            int withDemand = demandPrice(offer, demand.getInteger(key));
            if (price == MathHelper.clamp(withDemand, 1, buy.getMaxStackSize())) price = withDemand;
        }
        return VillagerPrices.adjustedCost(price, offer.inputCount(), buy.getMaxStackSize(), offer.priceMultiplier, reputation, hero);
    }

    private static int demandPrice(Offer offer, int demand) {
        return offer.inputCount() + Math.max(0, MathHelper.floor(offer.inputCount() * demand * offer.priceMultiplier));
    }

    private static final class Offer {
        final Item item;
        final int cost;
        final int quantity;
        final int maxUses;
        final int experience;
        final float priceMultiplier;
        final boolean buying;
        final int papers;

        Offer(Item item, int cost, int quantity, int maxUses, int experience, float priceMultiplier, boolean buying, int papers) {
            this.item = item;
            this.cost = cost;
            this.quantity = quantity;
            this.maxUses = maxUses;
            this.experience = experience;
            this.priceMultiplier = priceMultiplier;
            this.buying = buying;
            this.papers = papers;
        }

        int inputCount() {
            return buying ? quantity : cost;
        }

        boolean matches(MerchantRecipe recipe) {
            ItemStack buy = recipe.getItemToBuy();
            ItemStack sell = recipe.getItemToSell();
            ItemStack second = recipe.getSecondItemToBuy();
            return buy.getItem() == (buying ? item : Items.EMERALD) && buy.getMetadata() == 0
                    && sell.getItem() == (buying ? Items.EMERALD : item) && sell.getMetadata() == 0
                    && sell.getCount() == (buying ? cost : quantity)
                    && (papers == 0 ? second.isEmpty() : second.getItem() == Items.PAPER && second.getCount() == papers);
        }

        MerchantRecipe create() {
            ItemStack itemStack = new ItemStack(item, quantity);
            ItemStack emeralds = new ItemStack(Items.EMERALD, cost);
            return new MerchantRecipe(buying ? itemStack : emeralds, papers == 0 ? ItemStack.EMPTY : new ItemStack(Items.PAPER, papers),
                    buying ? emeralds : itemStack, 0, maxUses);
        }
    }

    private static Offer sell(Item item, int cost, int count, int uses, int xp, float multiplier) {
        return new Offer(item, cost, count, uses, xp, multiplier, false, 0);
    }

    private static Offer buy(Item item, int count, int emeralds, int uses, int xp, float multiplier) {
        return new Offer(item, emeralds, count, uses, xp, multiplier, true, 0);
    }

    private static final class Offers {
        static final Offer HARMONIUM = sell(PSItems.HARMONIUM, 3, 1, 7, 2, 0.2F);
        static final Offer[][] DEALER = {
                {sell(PSItems.CANNABIS_LEAF, 1, 1, 9, 1, 0.7F), sell(PSItems.CANNABIS_SEEDS, 1, 5, 12, 2, 0.5F),
                        sell(PSItems.HASH_MUFFIN, 1, 2, 3, 1, 0.7F), sell(PSItems.COCA_LEAVES, 1, 4, 4, 1, 0.5F),
                        sell(PSItems.COCA_SEEDS, 2, 4, 4, 1, 0.5F), sell(PSItems.PEYOTE, 1, 5, 4, 4, 0.5F),
                        sell(PSItems.CIGARETTE, 1, 4, 2, 3, 0.8F)},
                {sell(PSItems.DRIED_CANNABIS_BUDS, 2, 2, 8, 2, 0.9F), sell(PSItems.DRIED_CANNABIS_LEAF, 2, 2, 5, 3, 0.8F),
                        sell(PSItems.DRIED_PEYOTE, 3, 10, 2, 2, 0.5F), sell(PSItems.DRIED_COCA_LEAVES, 3, 20, 3, 2, 0.5F),
                        sell(PSItems.CIGAR, 1, 5, 2, 3, 0.5F), sell(PSItems.SMOKING_PIPE, 1, 5, 2, 3, 0.5F),
                        sell(PSItems.DRYING_TABLE, 6, 1, 2, 3, 0.5F)},
                {sell(PSItems.BROWN_MAGIC_MUSHROOMS, 5, 8, 3, 3, 0.5F), sell(PSItems.RED_MAGIC_MUSHROOMS, 2, 8, 3, 3, 0.5F),
                        sell(PSItems.SYRINGE, 3, 4, 3, 1, 0.5F), sell(PSItems.BONG, 3, 4, 3, 1, 0.5F),
                        sell(PSItems.PEYOTE_JOINT, 1, 3, 2, 3, 0.5F), sell(PSItems.LSD_PILL, 2, 3, 2, 3, 0.5F),
                        new Offer(PSItems.LSA_SQUARE, 3, 3, 2, 3, 0.5F, false, 2),
                        sell(PSItems.JOINT, 1, 2, 2, 3, 0.5F)}
        };
        static final Offer[] ADDICT = {
                buy(PSItems.BROWN_MAGIC_MUSHROOMS, 8, 5, 3, 3, 0.5F), buy(PSItems.RED_MAGIC_MUSHROOMS, 8, 2, 3, 3, 0.5F),
                buy(PSItems.LSD_PILL, 3, 2, 2, 3, 0.5F), buy(PSItems.JOINT, 2, 1, 2, 3, 0.5F),
                buy(PSItems.CIGARETTE, 4, 1, 2, 3, 0.8F), buy(PSItems.CIGAR, 5, 1, 2, 3, 0.5F),
                buy(PSItems.HASH_MUFFIN, 2, 1, 3, 1, 0.7F), buy(PSItems.DRIED_CANNABIS_BUDS, 2, 4, 8, 2, 0.9F),
                buy(PSItems.DRIED_CANNABIS_LEAF, 2, 4, 5, 3, 0.8F), buy(PSItems.DRIED_PEYOTE, 10, 5, 2, 2, 0.5F),
                buy(PSItems.DRIED_COCA_LEAVES, 20, 5, 3, 2, 0.5F)
        };
    }
}
