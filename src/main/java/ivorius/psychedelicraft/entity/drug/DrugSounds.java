/*
 * Copyright (c) 2014, Lukas Tenbrink.
 * Updated by Sollace on 15 Jan 2023.
 */

package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.PSSounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;

final class DrugSounds {
    private int heartbeatDelay;
    private int breathDelay;
    private boolean inhaling;
    private float previousPulse;
    private float pulse;
    private float targetPulse;

    void tick(DrugProperties properties, EntityPlayer player) {
        if (player.world.isRemote) tickHeartbeat(properties, player);
        if (breathDelay > 0) breathDelay--;
        if (breathDelay == 0) {
            inhaling = !inhaling;
            float volume = properties.getModifier(Drug::breathVolume);
            if (volume > 0) {
                float speed = 1F + properties.getModifier(Drug::breathSpeed);
                breathDelay = MathHelper.floor(30F / speed);
                float pitch = speed * 0.05F + 0.9F + (inhaling ? 0.15F : 0F);
                player.world.playSound(player, player.posX, player.posY, player.posZ, PSSounds.BREATH,
                        SoundCategory.PLAYERS, volume, pitch);
            }
        }
    }

    private void tickHeartbeat(DrugProperties properties, EntityPlayer player) {
        previousPulse = pulse;
        pulse += MathHelper.clamp(targetPulse - pulse, -0.2F, 0.2F);
        targetPulse = Math.max(0F, targetPulse - 0.02F);
        if (heartbeatDelay > 0) heartbeatDelay--;
        if (heartbeatDelay == 0) {
            float volume = properties.getModifier(Drug::heartbeatVolume);
            if (volume > 0) {
                float speed = 1F + properties.getModifier(Drug::heartbeatSpeed);
                heartbeatDelay = speed <= 1F ? 1 : MathHelper.floor(35F / (speed - 1F));
                targetPulse = 1F;
                player.world.playSound(player.posX, player.posY, player.posZ, PSSounds.HEARTBEAT,
                        SoundCategory.AMBIENT, volume, speed, false);
            }
        }
    }

    float pulse(float partialTicks) {
        return previousPulse + (pulse - previousPulse) * partialTicks;
    }
}
