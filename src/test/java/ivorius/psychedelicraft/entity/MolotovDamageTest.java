package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSDamageTypes;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MolotovDamageTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void blastUsesThrownDamageAndScalesForAMobOwner() {
        TestWorld world = new TestWorld();
        EntityPig owner = new EntityPig(world);
        MolotovCocktailEntity projectile = new MolotovCocktailEntity(world, owner, ItemStack.EMPTY);
        Explosion blast = new Explosion(world, projectile, 0, 0, 0, 1, false, false) {
            @Override
            public EntityLivingBase getExplosivePlacedBy() {
                return owner;
            }
        };

        DamageSource damage = PSDamageTypes.explosionDamage(blast, projectile);
        assertEquals("thrown", damage.getDamageType());
        assertSame(projectile, damage.getImmediateSource());
        assertSame(owner, damage.getTrueSource());
        assertTrue(damage.isProjectile());
        assertTrue(damage.isDifficultyScaled());
        assertFalse(damage.isExplosion());

        DamageSource vanilla = PSDamageTypes.explosionDamage(new Explosion(world, owner, 0, 0, 0, 1, false, false), owner);
        assertTrue(vanilla.isExplosion());
        assertFalse(vanilla.isProjectile());
        assertSame(owner, vanilla.getTrueSource());
    }

    private static final class TestWorld extends World {
        private TestWorld() {
            super(null, null, new WorldProviderSurface(), new Profiler(), false);
        }

        @Override
        protected IChunkProvider createChunkProvider() {
            return null;
        }

        @Override
        protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
            return false;
        }
    }
}
