/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Background music selection by Sollace.
 */

package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID, value = Side.CLIENT)
public final class ClientDrugMusic {
    private static final double PLAY_THRESHOLD = 0.01;
    private static DrugTrack playing;
    private static int retryTicks;

    private ClientDrugMusic() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        EntityPlayerSP player = client.player;
        SoundHandler sounds = client.getSoundHandler();
        DrugProperties drugs = player == null ? null : DrugCapability.get(player);
        if (client.world == null || drugs == null) {
            stop(sounds);
            return;
        }
        if (client.isGamePaused()) return;

        DrugType strongest = strongestTheme(drugs, ClientMusicConfig::enabled);
        if (playing != null && (playing.player != player || playing.drugs != drugs || playing.type != strongest)) {
            stop(sounds);
        }
        if (strongest == null) return;
        if (retryTicks > 0) {
            retryTicks--;
            return;
        }
        if (playing != null && sounds.isSoundPlaying(playing)) return;
        if (client.gameSettings.getSoundLevel(SoundCategory.MASTER) <= 0
                || ClientSoundEffects.scaleVolume(client.gameSettings.getSoundLevel(SoundCategory.AMBIENT)) <= 0) return;

        stop(sounds);
        playing = new DrugTrack(themeSound(sounds, strongest), player, drugs, strongest);
        sounds.playSound(playing);
        // A new or reloaded sound engine can take several ticks to start accepting sources.
        retryTicks = 20;
    }

    private static void stop(SoundHandler sounds) {
        if (playing != null) {
            playing.finish();
            sounds.stopSound(playing);
            playing = null;
        }
        retryTicks = 0;
    }

    @Nullable
    static DrugType strongestTheme(DrugProperties drugs, Predicate<DrugType> enabled) {
        DrugType selected = null;
        double strongest = PLAY_THRESHOLD;
        for (DrugType type : DrugType.values()) {
            double value = drugs.getActiveValue(type);
            if (value > strongest && enabled.test(type)) {
                strongest = value;
                selected = type;
            }
        }
        return selected;
    }

    static float themeVolume(double activeValue) {
        return activeValue <= PLAY_THRESHOLD ? 0F : MathHelper.clamp((float) activeValue / 0.4F, 0F, 1F);
    }

    private static SoundEvent themeSound(SoundHandler sounds, DrugType type) {
        ResourceLocation drugId = new ResourceLocation(type.id());
        ResourceLocation themeId = Psychedelicraft.id("drug." + drugId.getResourcePath());
        SoundEventAccessor theme = sounds.getAccessor(themeId);
        return theme != null && theme.cloneEntry() != SoundHandler.MISSING_SOUND
                ? new SoundEvent(themeId) : PSSounds.DRUG_GENERIC;
    }

    private static final class DrugTrack extends MovingSound {
        private final EntityPlayerSP player;
        private final DrugProperties drugs;
        private final DrugType type;

        private DrugTrack(SoundEvent event, EntityPlayerSP player, DrugProperties drugs, DrugType type) {
            super(event, SoundCategory.AMBIENT);
            this.player = player;
            this.drugs = drugs;
            this.type = type;
            repeat = true;
            update();
        }

        @Override
        public void update() {
            volume = themeVolume(drugs.getActiveValue(type));
            if (volume <= 0 || player.isDead) finish();
            xPosF = (float) player.posX;
            yPosF = (float) player.posY;
            zPosF = (float) player.posZ;
        }

        private void finish() {
            donePlaying = true;
        }
    }
}
