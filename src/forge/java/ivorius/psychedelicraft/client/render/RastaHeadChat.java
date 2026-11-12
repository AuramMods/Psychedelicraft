/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.math.MathHelper;
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
        if (random.nextFloat() < 0.3F || line.contains("!")) {
            double x = random.nextFloat();
            double z = random.nextFloat();
            player.performHurtAnimation();
            player.attackedAtYaw = (float) (Math.toDegrees(MathHelper.atan2(z, x)) - player.rotationYaw);
            player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1F, 1F);
            if (x != 0 || z != 0) player.knockBack(player, 0.2F, x, z);
        }
    }

    private final class Voice {
        private final Deque<String> lines = new ArrayDeque<>();
        private int idleTicks;
        private int sleepTicks;
        private int delay;

        boolean tick(boolean visible) {
            if (sleepTicks-- > 0) return false;
            if (lines.isEmpty() && idleTicks++ > 300 && random.nextInt(300) == 0) {
                if (random.nextInt(120) == 0) return true;
                queue(RastaHeadDialogue.statement(random), 5 + random.nextInt(96));
            }
            if (!lines.isEmpty()) {
                if (--delay <= 0) {
                    String line = lines.removeFirst();
                    if (visible) emit(line);
                    delay = 2 + random.nextInt(19);
                }
                if (sleepTicks <= 0) sleepTicks = 2 + random.nextInt(19);
            }
            return false;
        }

        void respond() {
            queue(RastaHeadDialogue.response(random), 1 + random.nextInt(5));
        }

        private void queue(String[] script, int sleep) {
            lines.clear();
            for (String line : script) lines.addLast(line);
            idleTicks = 0;
            sleepTicks = sleep;
            delay = 2 + random.nextInt(19);
        }
    }
}
