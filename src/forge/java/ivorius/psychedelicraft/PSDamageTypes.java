package ivorius.psychedelicraft;

import net.minecraft.util.DamageSource;

public final class PSDamageTypes {
    public static final DamageSource STROKE = new DamageSource("psychedelicraft.stroke").setDamageBypassesArmor();
    public static final DamageSource ALCOHOL_POISONING = new DamageSource("psychedelicraft.alcohol_poisoning").setDamageBypassesArmor();
    public static final DamageSource HEART_FAILURE = new DamageSource("psychedelicraft.heart_failure").setDamageBypassesArmor();
    public static final DamageSource RESPIRATORY_FAILURE = new DamageSource("psychedelicraft.respiratory_failure").setDamageBypassesArmor();
    public static final DamageSource KIDNEY_FAILURE = new DamageSource("psychedelicraft.kidney_failure").setDamageBypassesArmor();
    public static final DamageSource IN_SLEEP = new DamageSource("psychedelicraft.in_sleep").setDamageBypassesArmor();

    private PSDamageTypes() {
    }
}
