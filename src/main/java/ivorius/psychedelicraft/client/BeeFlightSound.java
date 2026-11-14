package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.entity.BeeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

@SideOnly(Side.CLIENT)
public final class BeeFlightSound extends MovingSound {
    private static final Map<BeeEntity, BeeFlightSound> LOOPS = new WeakHashMap<>();
    private final WeakReference<BeeEntity> bee;
    private final boolean angry;
    private final int startTick;

    private BeeFlightSound(BeeEntity bee) {
        super(bee.isAngry() ? PSSounds.BEE_LOOP_ANGRY : PSSounds.BEE_LOOP, SoundCategory.NEUTRAL);
        this.bee = new WeakReference<>(bee);
        angry = bee.isAngry();
        startTick = bee.ticksExisted;
        repeat = true;
        update();
    }

    public static void tick(BeeEntity bee) {
        Minecraft client = Minecraft.getMinecraft();
        SoundHandler handler = client.getSoundHandler();
        BeeFlightSound current = LOOPS.get(bee);
        if (!isActive(bee)) {
            stop(bee, current, handler);
            return;
        }
        if (current != null && (current.angry != bee.isAngry() || current.isDonePlaying())) {
            stop(bee, current, handler);
            current = null;
        }
        if (current != null && (bee.ticksExisted - current.startTick < 20 || handler.isSoundPlaying(current)
                || client.gameSettings.getSoundLevel(SoundCategory.MASTER) <= 0F
                || ClientSoundEffects.scaleVolume(client.gameSettings.getSoundLevel(SoundCategory.NEUTRAL)) <= 0F)) return;
        // Forge 1.12 drops requests that start with zero volume.
        if (horizontalSpeed(bee) < 0.01F) return;
        BeeFlightSound sound = new BeeFlightSound(bee);
        LOOPS.put(bee, sound);
        handler.playSound(sound);
    }

    private static void stop(BeeEntity bee, BeeFlightSound sound, SoundHandler handler) {
        if (sound == null) return;
        sound.donePlaying = true;
        handler.stopSound(sound);
        LOOPS.remove(bee);
    }

    @Override
    public void update() {
        BeeEntity source = bee.get();
        if (source == null || !isActive(source)) {
            donePlaying = true;
            return;
        }
        xPosF = (float) source.posX;
        yPosF = (float) source.posY;
        zPosF = (float) source.posZ;
        float speed = horizontalSpeed(source);
        float minPitch = source.isChild() ? 1.1F : 0.7F;
        float maxPitch = source.isChild() ? 1.5F : 1.1F;
        pitch = speed < 0.01F ? 0F : minPitch + (maxPitch - minPitch) * MathHelper.clamp(speed, minPitch, maxPitch);
        volume = speed < 0.01F ? 0F : 1.2F * Math.min(speed, 0.5F);
    }

    private static boolean isActive(BeeEntity bee) {
        Minecraft client = Minecraft.getMinecraft();
        return client.world == bee.world && bee.isEntityAlive() && !bee.isSilent()
                && client.world.getEntityByID(bee.getEntityId()) == bee;
    }

    private static float horizontalSpeed(BeeEntity bee) {
        return MathHelper.sqrt(bee.motionX * bee.motionX + bee.motionZ * bee.motionZ);
    }
}
