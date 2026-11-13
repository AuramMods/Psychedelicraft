package ivorius.psychedelicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.network.INetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class EntitySound extends MovingSound {
    private final Entity source;

    private EntitySound(Entity source, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        super(sound, category);
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
        update();
    }

    public static void receive(INetHandler sender, int entityId, int dimension, ResourceLocation soundId,
                               SoundCategory category, float volume, float pitch) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() != sender || client.player == null || client.world == null
                || client.world.provider.getDimension() != dimension) return;
        Entity source = client.world.getEntityByID(entityId);
        SoundEvent sound = SoundEvent.REGISTRY.getObject(soundId);
        play(source, sound, category, volume, pitch);
    }

    public static void playLocal(Entity source, SoundEvent sound, float volume, float pitch) {
        if (source != Minecraft.getMinecraft().player) return;
        PlaySoundAtEntityEvent event = ForgeEventFactory.onPlaySoundAtEntity(source, sound, source.getSoundCategory(), volume, pitch);
        if (!event.isCanceled()) play(source, event.getSound(), event.getCategory(), event.getVolume(), event.getPitch());
    }

    private static void play(Entity source, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        if (source != null && !source.isDead && !source.isSilent() && sound != null) {
            Minecraft.getMinecraft().getSoundHandler().playSound(new EntitySound(source, sound, category, volume, pitch));
        }
    }

    @Override
    public void update() {
        if (source.isDead) {
            donePlaying = true;
        } else {
            xPosF = (float) source.posX;
            yPosF = (float) source.posY;
            zPosF = (float) source.posZ;
        }
    }
}
