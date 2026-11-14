package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class Riptide {
    private static final int SPIN_TICKS = 20;
    private static final float COMPACT_HEIGHT = 0.6F;
    private static final float RECOVERY_CROUCH_HEIGHT = 1.5F;
    private static final Map<EntityPlayer, Spin> active = new WeakHashMap<>();
    private static final Map<EntityPlayer, Integer> predicted = new WeakHashMap<>();
    private static DataParameter<Boolean> spinning;

    private Riptide() {}

    public static void reserveSyncedState() {
        spinning = EntityDataManager.createKey(EntityPlayer.class, DataSerializers.BOOLEAN);
    }

    @SubscribeEvent
    public static void registerState(EntityEvent.EntityConstructing event) {
        if (event.getEntity() instanceof EntityPlayer) event.getEntity().getDataManager().register(spinning, false);
    }

    public static boolean isSpinning(Entity entity) {
        return entity instanceof EntityPlayer && entity.getDataManager().get(spinning);
    }

    public static float eyeHeight(float vanilla, EntityPlayer player) {
        if (player.isPlayerSleeping()) return vanilla;
        if (player.height == COMPACT_HEIGHT) return 0.4F;
        return player.height == RECOVERY_CROUCH_HEIGHT ? 1.27F : vanilla;
    }

    public static float desiredHeight(EntityPlayer player, float width, float vanilla) {
        if (vanilla <= COMPACT_HEIGHT) return vanilla;
        if (isSpinning(player) || hasPredictedLaunch(player)) return COMPACT_HEIGHT;
        if (player.height == COMPACT_HEIGHT) {
            AxisAlignedBB bounds = player.getEntityBoundingBox();
            if (!blockedAtHeight(player.world, bounds, width, vanilla)) return vanilla;
            if (!blockedAtHeight(player.world, bounds, width, RECOVERY_CROUCH_HEIGHT)) return RECOVERY_CROUCH_HEIGHT;
            return COMPACT_HEIGHT;
        }
        return vanilla;
    }

    private static boolean blockedAtHeight(World world, AxisAlignedBB bounds, float width, float height) {
        return world.collidesWithAnyBlock(new AxisAlignedBB(bounds.minX, bounds.minY, bounds.minZ,
                bounds.minX + width, bounds.minY + height, bounds.minZ + width));
    }

    public static boolean isCrawling(Entity entity) {
        return entity instanceof EntityPlayer && entity.height == COMPACT_HEIGHT
                && !((EntityPlayer) entity).isElytraFlying() && !isSpinning(entity) && !hasPredictedLaunch((EntityPlayer) entity);
    }

    public static void launch(EntityPlayer player, int level) {
        if (player.height > COMPACT_HEIGHT) {
            // The local release also shrinks the box before applying the predicted movement.
            player.height = COMPACT_HEIGHT;
            player.setPosition(player.posX, player.posY, player.posZ);
        }
        Vec3d motion = player.getLookVec().scale(3 * (1 + level) / 4.0);
        player.addVelocity(motion.x, motion.y, motion.z);
        if (player.onGround) player.move(MoverType.SELF, 0, 1.2, 0);

        if (player.world.isRemote) {
            predicted.put(player, player.ticksExisted + 4);
        } else {
            player.getDataManager().set(spinning, true);
            active.put(player, new Spin(player.world, player.getEntityBoundingBox()));
            SoundEvent sound = level >= 3 ? PSSounds.TRIDENT_RIPTIDE_3 : level == 2 ? PSSounds.TRIDENT_RIPTIDE_2 : PSSounds.TRIDENT_RIPTIDE_1;
            player.playSound(sound, 1, 1);
        }
    }

    private static boolean hasPredictedLaunch(EntityPlayer player) {
        if (!player.world.isRemote) return false;
        Integer expiry = predicted.get(player);
        if (expiry == null) return false;
        if (player.ticksExisted < expiry) return true;
        predicted.remove(player);
        return false;
    }

    @SubscribeEvent
    public static void update(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase != TickEvent.Phase.END || player.world.isRemote) return;
        Spin spin = active.get(player);
        if (spin == null) return;
        AxisAlignedBB now = player.getEntityBoundingBox();
        Vec3d start = center(spin.bounds);
        // A teleport should end the dash and cannot hit mobs between the old and new positions.
        if (player.world == spin.world && player.isEntityAlive() && !player.isSpectator()
                && start.squareDistanceTo(center(now)) <= 16 * 16) {
            EntityLivingBase target = player.world.getEntitiesWithinAABB(EntityLivingBase.class, spin.bounds.union(now),
                            living -> living != player && living.isEntityAlive() && living.canBeCollidedWith()
                                    && (!(living instanceof EntityPlayer) || player.canAttackPlayer((EntityPlayer) living)))
                    .stream().min(Comparator.comparingDouble(living -> living.getDistanceSq(start.x, start.y, start.z)))
                    .orElse(null);
            if (target != null) {
                player.attackTargetEntityWithCurrentItem(target);
                player.motionX *= -0.2;
                player.motionY *= -0.2;
                player.motionZ *= -0.2;
                player.velocityChanged = true;
            } else if (--spin.remaining > 0 && !player.collidedHorizontally) {
                spin.bounds = now;
                return;
            }
        }
        active.remove(player);
        player.getDataManager().set(spinning, false);
    }

    private static Vec3d center(AxisAlignedBB bounds) {
        return new Vec3d((bounds.minX + bounds.maxX) / 2, (bounds.minY + bounds.maxY) / 2, (bounds.minZ + bounds.maxZ) / 2);
    }

    private static final class Spin {
        private final World world;
        private int remaining = SPIN_TICKS;
        private AxisAlignedBB bounds;

        private Spin(World world, AxisAlignedBB bounds) {
            this.world = world;
            this.bounds = bounds;
        }
    }
}
