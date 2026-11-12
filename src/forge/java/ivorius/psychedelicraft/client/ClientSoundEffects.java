package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import paulscode.sound.SoundSystem;

import java.util.Map;

public final class ClientSoundEffects {
    private static float appliedMultiplier = 1;

    private ClientSoundEffects() {
    }

    private static float currentMultiplier() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        DrugProperties drugs = player == null ? null : DrugCapability.get(player);
        return drugs == null ? 1 : drugs.getMultiplier(Drug::soundVolumeModifier);
    }

    public static float scaleVolume(float volume) {
        return MathHelper.clamp(volume * currentMultiplier(), 0, 1);
    }

    public static void refreshPlaying(SoundSystem engine, GameSettings settings, Map<String, ISound> playing) {
        if (engine == null) return;
        float multiplier = currentMultiplier();
        if (multiplier == appliedMultiplier) return;
        appliedMultiplier = multiplier;

        for (Map.Entry<String, ISound> channel : playing.entrySet()) {
            ISound sound = channel.getValue();
            // SoundManager already updates tickable sounds through its scaled volume calculation.
            if (sound instanceof ITickableSound) continue;
            SoundCategory category = sound.getCategory();
            float categoryVolume = category == null || category == SoundCategory.MASTER ? 1 : settings.getSoundLevel(category);
            float volume = sound.getVolume() * MathHelper.clamp(categoryVolume * multiplier, 0, 1);
            engine.setVolume(channel.getKey(), MathHelper.clamp(volume, 0, 1));
        }
    }
}
