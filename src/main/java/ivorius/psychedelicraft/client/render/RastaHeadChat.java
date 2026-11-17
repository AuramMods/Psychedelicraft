/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import java.util.*;

final class RastaHeadChat {
    private static final String NAME = "Reggie";

    private final LocalPlayer player;
    private final RandomSource random;
    private final List<Voice> voices = new ArrayList<>();

    RastaHeadChat(LocalPlayer player) {
        this.player = player;
        random = player.getRandom();
    }

    void tick(boolean visible) {
        if (voices.size() < 3 && random.nextInt(200) == 0) voices.add(new Voice());
        voices.removeIf(voice -> voice.tick(visible));
    }

    void receive(String sender, Component message) {
        if (voices.isEmpty() || NAME.equals(sender)) return;
        if (message.getString().contains(NAME)) {
            voices.forEach(Voice::respond);
        } else {
            voices.get(random.nextInt(voices.size())).respond();
        }
    }

    private void emit(String line) {
        Minecraft.getInstance().gui.getChat().addMessage(ivorius.psychedelicraft.client.ClientChatEffects.distortIncoming(Component.translatable("chat.type.text", Component.literal(NAME), Component.literal(line))));
        RandomSource worldRandom=player.level().random;
        if(worldRandom.nextFloat()<.3F||line.contains("!")){
            double x=worldRandom.nextFloat(),z=worldRandom.nextFloat();
            player.animateHurt((float)(Mth.atan2(z,x)*180/Math.PI-player.getYRot()));
            player.playSound(SoundEvents.PLAYER_HURT,1,1);
            player.knockback(.2F,x,z);
        }
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
