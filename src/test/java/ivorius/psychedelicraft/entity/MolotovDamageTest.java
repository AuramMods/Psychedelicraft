package ivorius.psychedelicraft.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.PSDamageTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

    @Test
    public void launchUsesTriangularSpreadAndInheritsOwnerMotion() {
        TestWorld world = new TestWorld();
        EntityPig owner = new EntityPig(world);
        owner.motionX = 0.2;
        owner.motionY = 0.35;
        owner.motionZ = -0.15;
        checkLaunch(world, owner, true);
        checkLaunch(world, owner, false);
    }

    @Test
    public void projectileClipsPhysicalShapesAndHonorsSegmentBounds() {
        TestWorld world = new TestWorld() {
            @Override
            public IBlockState getBlockState(BlockPos pos) {
                if (pos.equals(BlockPos.ORIGIN)) return Blocks.TALLGRASS.getStateFromMeta(1);
                if (pos.equals(BlockPos.ORIGIN.east())) return Blocks.STONE.getDefaultState();
                return Blocks.AIR.getDefaultState();
            }
        };
        MolotovCocktailEntity projectile = new MolotovCocktailEntity(world);
        Vec3d start = new Vec3d(-0.5, 0.25, 0.5);
        assertNull(ProjectileBlockCollision.trace(projectile, start, new Vec3d(1, 0.25, 0.5)));
        RayTraceResult throughGrass = ProjectileBlockCollision.trace(projectile, start, new Vec3d(1.5, 0.25, 0.5));
        assertEquals(BlockPos.ORIGIN.east(), throughGrass.getBlockPos());
        assertEquals(EnumFacing.WEST, throughGrass.sideHit);
        assertEquals(1D, throughGrass.hitVec.x, 0);

        RayTraceResult inside = ProjectileBlockCollision.trace(projectile, new Vec3d(1.5, 0.25, 0.5), start);
        assertEquals(BlockPos.ORIGIN.east(), inside.getBlockPos());
        assertEquals(EnumFacing.EAST, inside.sideHit);
        assertEquals(1.498, inside.hitVec.x, 1E-12);
    }

    @Test
    public void projectileEntityRayAcceptsOnlyEntriesWithinTheSegment() {
        AxisAlignedBB target = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
        Vec3d before = new Vec3d(-1, 0.5, 0.5);
        Vec3d face = new Vec3d(0, 0.5, 0.5);
        Vec3d after = new Vec3d(2, 0.5, 0.5);
        assertNull(ProjectileBlockCollision.entry(target, new Vec3d(0.5, 0.5, 0.5), after));
        assertNull(ProjectileBlockCollision.entry(target, before, face));
        assertNull(ProjectileBlockCollision.entry(target, face, after));
        assertEquals(face, ProjectileBlockCollision.entry(target, before, after).hitVec);
    }

    @Test
    public void spawnDataPreservesVelocityWhenOwnerIdIsZero() {
        TestWorld world = new TestWorld();
        EntityPig owner = new EntityPig(world);
        owner.setEntityId(0);
        world.trackedEntity = owner;
        MolotovCocktailEntity source = new MolotovCocktailEntity(world, owner, ItemStack.EMPTY);
        source.motionX = 0.164732;
        source.motionY = -4;
        source.motionZ = 4;

        MolotovCocktailEntity received = new MolotovCocktailEntity(world);
        ByteBuf payload = Unpooled.buffer();
        try {
            source.writeSpawnData(payload);
            received.readSpawnData(payload);
            assertSame(owner, received.getThrower());
            assertEquals(0.164625, received.motionX, 0);
            assertEquals(-3.9, received.motionY, 0);
            assertEquals(3.9, received.motionZ, 0);
            assertFalse(payload.isReadable());

            payload.clear();
            new MolotovCocktailEntity(world).writeSpawnData(payload);
            received.readSpawnData(payload);
            assertNull(received.getThrower());
            assertEquals(0, received.motionX, 0);
            received.readSpawnData(payload);
            assertFalse(payload.isReadable());
        } finally {
            payload.release();
        }
    }

    private static void checkLaunch(World world, EntityPig owner, boolean onGround) {
        owner.onGround = onGround;
        MolotovCocktailEntity projectile = new MolotovCocktailEntity(world, owner, ItemStack.EMPTY);
        long seed = 160L;
        Random projectileRandom = ReflectionHelper.getPrivateValue(Entity.class, projectile, "rand");
        projectileRandom.setSeed(seed);
        Random random = new Random(seed);
        float speed = 0.375F;
        projectile.shoot(owner, 0, 0, 0, speed, 1);
        double spread = 0.0172275;
        double x = spread * (random.nextDouble() - random.nextDouble()) * speed;
        double y = spread * (random.nextDouble() - random.nextDouble()) * speed;
        double z = (1 + spread * (random.nextDouble() - random.nextDouble())) * speed;
        assertEquals(x + owner.motionX, projectile.motionX, 1E-12);
        assertEquals(y + (onGround ? 0 : owner.motionY), projectile.motionY, 1E-12);
        assertEquals(z + owner.motionZ, projectile.motionZ, 1E-12);
        assertEquals((float) Math.toDegrees(MathHelper.atan2(x, z)), projectile.rotationYaw, 1E-5F);
        assertEquals((float) Math.toDegrees(MathHelper.atan2(y, Math.sqrt(x * x + z * z))), projectile.rotationPitch, 1E-5F);
    }

    private static class TestWorld extends World {
        private Entity trackedEntity;

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

        @Override
        public Entity getEntityByID(int id) {
            return trackedEntity != null && trackedEntity.getEntityId() == id ? trackedEntity : null;
        }
    }
}
