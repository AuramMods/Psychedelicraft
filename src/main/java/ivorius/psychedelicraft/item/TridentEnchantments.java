package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class TridentEnchantments {
    private static final EnumEnchantmentType TRIDENT = EnumHelper.addEnchantmentType("PS_TRIDENT", item -> item instanceof TridentItem);
    public static final Enchantment LOYALTY = enchantment("loyalty", Enchantment.Rarity.UNCOMMON, 3, 12, 7, 50, false);
    public static final Enchantment IMPALING = enchantment("impaling", Enchantment.Rarity.RARE, 5, 1, 8, 20, true);
    public static final Enchantment RIPTIDE = enchantment("riptide", Enchantment.Rarity.RARE, 3, 17, 7, 50, false);
    public static final Enchantment CHANNELING = enchantment("channeling", Enchantment.Rarity.VERY_RARE, 1, 25, 0, 50, false);

    private TridentEnchantments() {}

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Enchantment> event) {
        event.getRegistry().registerAll(LOYALTY, IMPALING, RIPTIDE, CHANNELING);
    }

    public static float impalingDamage(ItemStack stack, Entity target) {
        // Legacy guardians belong to the monster class, while squid and aquatic mod animals use EntityWaterMob.
        if (!(target instanceof EntityWaterMob) && !(target instanceof EntityGuardian)) return 0;
        return EnchantmentHelper.getEnchantmentLevel(IMPALING, stack) * 2.5F;
    }

    @SubscribeEvent
    public static void impalingMelee(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getImmediateSource();
        if (attacker != source.getTrueSource() || !(attacker instanceof EntityLivingBase)
                || !("player".equals(source.getDamageType()) || "mob".equals(source.getDamageType()))) return;
        ItemStack weapon = ((EntityLivingBase) attacker).getHeldItemMainhand();
        if (weapon.getItem() == PSItems.TRIDENT) {
            event.setAmount(event.getAmount() + impalingDamage(weapon, event.getEntityLiving()));
        }
    }

    private static Enchantment enchantment(String name, Enchantment.Rarity rarity, int levels, int firstCost,
                                            int step, int upperCost, boolean relativeUpperCost) {
        Enchantment enchantment = new Enchantment(rarity, TRIDENT, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}) {
            @Override
            public int getMinEnchantability(int level) {
                return firstCost + (level - 1) * step;
            }

            @Override
            public int getMaxEnchantability(int level) {
                return relativeUpperCost ? getMinEnchantability(level) + upperCost : upperCost;
            }

            @Override
            public int getMaxLevel() {
                return levels;
            }

            @Override
            protected boolean canApplyTogether(Enchantment other) {
                return super.canApplyTogether(other) && (this != RIPTIDE || other != LOYALTY && other != CHANNELING);
            }
        };
        return enchantment.setName(Psychedelicraft.MOD_ID + "." + name).setRegistryName(Psychedelicraft.id(name));
    }
}
