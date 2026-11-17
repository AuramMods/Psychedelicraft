package ivorius.psychedelicraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

public final class PSDamageTypes {
    public static final ResourceKey<DamageType> HEART_ATTACK = key("heart_attack");
    public static final ResourceKey<DamageType> STROKE = key("stroke");
    public static final ResourceKey<DamageType> ALCOHOL_POISONING = key("alcohol_poisoning");
    public static final ResourceKey<DamageType> HEART_FAILURE = key("heart_failure");
    public static final ResourceKey<DamageType> RESPIRATORY_FAILURE = key("respiratory_failure");
    public static final ResourceKey<DamageType> KIDNEY_FAILURE = key("kidney_failure");
    public static final ResourceKey<DamageType> IN_SLEEP = key("in_sleep");
    public static final ResourceKey<DamageType> OVER_EATING = key("over_eating");

    public static final ResourceKey<DamageType> MOLOTOV=key("molotov"),SELF_MOLOTOV=key("self_molotov");
    public static DamageSource molotov(net.minecraft.world.entity.Entity projectile,net.minecraft.world.entity.Entity owner,net.minecraft.world.entity.Entity target,net.minecraft.world.item.ItemStack stack){
        var type=target==owner?SELF_MOLOTOV:MOLOTOV;var item=stack.copy();
        return new DamageSource(projectile.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),projectile,owner){
            @Override public net.minecraft.network.chat.Component getLocalizedDeathMessage(net.minecraft.world.entity.LivingEntity victim){
                String key="death.attack."+getMsgId();var attacker=owner==null?projectile.getDisplayName():owner.getDisplayName();
                return item.hasCustomHoverName()?net.minecraft.network.chat.Component.translatable(key+".item",victim.getDisplayName(),attacker,item.getDisplayName()):net.minecraft.network.chat.Component.translatable(key,victim.getDisplayName(),attacker);
            }
        };
    }
    private PSDamageTypes() {}

    public static DamageSource heartAttack(Player player) {
        return of(player, HEART_ATTACK);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Psychedelicraft.id(path));
    }

    public static DamageSource of(Player player, ResourceKey<DamageType> type) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type));
    }
}
