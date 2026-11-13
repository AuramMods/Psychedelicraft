package ivorius.psychedelicraft.world;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.MolotovCocktailEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityMinecartTNT;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class ExplosionDropRules {
    static final String BLOCK = "blockExplosionDropDecay";
    static final String MOB = "mobExplosionDropDecay";
    static final String TNT = "tntExplosionDropDecay";

    private ExplosionDropRules() {
    }

    @SubscribeEvent
    public static void register(WorldEvent.Load event) {
        register(event.getWorld().getGameRules());
    }

    static void register(GameRules rules) {
        register(rules, BLOCK, true);
        register(rules, MOB, true);
        register(rules, TNT, false);
    }

    private static void register(GameRules rules, String name, boolean defaultValue) {
        if (!rules.areSameType(name, GameRules.ValueType.BOOLEAN_VALUE)) {
            boolean value = rules.hasRule(name) ? rules.getBoolean(name) : defaultValue;
            rules.addGameRule(name, Boolean.toString(value), GameRules.ValueType.BOOLEAN_VALUE);
        }
    }

    private static String ruleFor(@Nullable Entity source) {
        if (source instanceof EntityTNTPrimed || source instanceof EntityMinecartTNT) return TNT;
        if (source instanceof EntityLiving || source instanceof EntityLargeFireball
                || source instanceof EntityWitherSkull || source instanceof MolotovCocktailEntity) return MOB;
        return BLOCK;
    }

    @Nullable
    public static Entity nativeSource(@Nullable Entity source) {
        // Forge 1.12's large fireball uses a null exploder for damage and destruction callbacks.
        return source instanceof EntityLargeFireball ? null : source;
    }

    public static boolean decays(World world, @Nullable Entity source) {
        String rule = ruleFor(source);
        GameRules rules = world.getGameRules();
        return rules.hasRule(rule) ? rules.getBoolean(rule) : !TNT.equals(rule);
    }

    public static float dropChance(float vanilla, boolean decays) {
        return decays ? vanilla : 1F;
    }
}
