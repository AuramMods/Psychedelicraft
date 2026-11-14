/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

final class RastaHeadChat {
    private static final String NAME = "Reggie";

    private final EntityPlayerSP player;
    private final Random random;
    private final List<Voice> voices = new ArrayList<>();

    RastaHeadChat(EntityPlayerSP player) {
        this.player = player;
        random = player.getRNG();
    }

    void tick(boolean visible) {
        if (voices.size() < 3 && random.nextInt(200) == 0) voices.add(new Voice());
        voices.removeIf(voice -> voice.tick(visible));
    }

    void receive(String sender, ITextComponent message) {
        if (voices.isEmpty() || NAME.equals(sender)) return;
        if (message.getUnformattedText().contains(NAME)) {
            voices.forEach(Voice::respond);
        } else {
            voices.get(random.nextInt(voices.size())).respond();
        }
    }

    private void emit(String line) {
        ITextComponent message = new TextComponentTranslation("chat.type.text",
                new TextComponentString(NAME), new TextComponentString(line));
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(message);
        Random worldRandom = player.world.rand;
        if (worldRandom.nextFloat() < 0.3F || line.contains("!")) {
            double x = worldRandom.nextFloat();
            double z = worldRandom.nextFloat();
            player.performHurtAnimation();
            player.attackedAtYaw = (float) (MathHelper.atan2(z, x) * (180F / (float) Math.PI) - player.rotationYaw);
            player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1F, 1F);
            pushPlayer(x, z);
        }
    }

    private void pushPlayer(double x, double z) {
        // In 1.20.1 knockback resistance scales the impulse. The legacy method rolls a chance to cancel it.
        double strength = 0.2F * (1D - player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE)
                .getAttributeValue());
        if (strength <= 0) return;
        Vec3d push = new Vec3d(x, 0, z).normalize().scale(strength);
        player.isAirBorne = true;
        player.motionX = player.motionX / 2D - push.x;
        player.motionZ = player.motionZ / 2D - push.z;
        if (player.onGround) player.motionY = Math.min(0.4D, player.motionY / 2D + strength);
    }

    private final class Voice {
        private final Deque<DelayedLine> lines = new ArrayDeque<>();
        private int idleTicks;
        private int sleepTicks;

        boolean tick(boolean visible) {
            if (sleepTicks-- > 0) return false;
            if (lines.isEmpty() && idleTicks++ > 300 && random.nextInt(300) == 0) {
                if (random.nextInt(120) == 0) return true;
                queue(RastaHeadDialogue.statement(random));
                sleepTicks = 5 + random.nextInt(96);
            }
            DelayedLine line = lines.peekFirst();
            if (line != null) {
                if (--line.delay <= 0) {
                    if (visible) emit(line.text);
                    lines.removeFirst();
                }
                sleepTicks = 2 + random.nextInt(19);
            }
            return false;
        }

        void respond() {
            lines.clear();
            queue(RastaHeadDialogue.response(random));
            sleepTicks = 1 + random.nextInt(5);
        }

        private void queue(String[] script) {
            for (String line : script) lines.addLast(new DelayedLine(line, 2 + random.nextInt(19)));
            idleTicks = 0;
        }
    }

    private static final class DelayedLine {
        private final String text;
        private int delay;

        DelayedLine(String text, int delay) {
            this.text = text;
            this.delay = delay;
        }
    }
}
