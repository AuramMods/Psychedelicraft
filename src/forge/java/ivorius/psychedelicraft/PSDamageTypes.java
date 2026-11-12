package ivorius.psychedelicraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;

public final class PSDamageTypes {
    public static final DamageSource STROKE = new DamageSource("psychedelicraft.stroke").setDamageBypassesArmor();
    public static final DamageSource ALCOHOL_POISONING = new DamageSource("psychedelicraft.alcohol_poisoning").setDamageBypassesArmor();
    public static final DamageSource HEART_FAILURE = new DamageSource("psychedelicraft.heart_failure").setDamageBypassesArmor();
    public static final DamageSource RESPIRATORY_FAILURE = new DamageSource("psychedelicraft.respiratory_failure").setDamageBypassesArmor();
    public static final DamageSource KIDNEY_FAILURE = new DamageSource("psychedelicraft.kidney_failure").setDamageBypassesArmor();
    public static final DamageSource IN_SLEEP = new DamageSource("psychedelicraft.in_sleep").setDamageBypassesArmor();
    public static final DamageSource OVER_EATING = new DamageSource("psychedelicraft.over_eating").setDamageBypassesArmor();

    public static DamageSource molotov(Entity projectile, @Nullable EntityLivingBase owner, Entity target, ItemStack stack) {
        String type = target == owner ? "psychedelicraft.self_molotov" : "psychedelicraft.molotov";
        return new EntityDamageSourceIndirect(type, projectile, owner) {
            @Override
            public ITextComponent getDeathMessage(EntityLivingBase victim) {
                String key = "death.attack." + damageType;
                ITextComponent attacker = owner == null ? projectile.getDisplayName() : owner.getDisplayName();
                return stack.hasDisplayName()
                        ? new TextComponentTranslation(key + ".item", victim.getDisplayName(), attacker, stack.getTextComponent())
                        : new TextComponentTranslation(key, victim.getDisplayName(), attacker);
            }
        }.setDamageBypassesArmor();
    }

    private PSDamageTypes() {
    }
}
