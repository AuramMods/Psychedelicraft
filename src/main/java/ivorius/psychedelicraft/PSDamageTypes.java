package ivorius.psychedelicraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class PSDamageTypes {
    private static final ResourceKey<DamageType> HEART_ATTACK = ResourceKey.create(
            Registries.DAMAGE_TYPE, Psychedelicraft.id("heart_attack"));

    private PSDamageTypes() {}

    public static DamageSource heartAttack(Player player) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(HEART_ATTACK));
    }
}
