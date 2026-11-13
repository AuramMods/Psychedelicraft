/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

final class MessageDistorter {
    private static final String TRANSLATION_PREFIX = "distortion.psychedelicraft.";
    private final Random random;
    private final float randomCase;
    private final float randomLetter;
    private final float slur;
    private final float longSlur;
    private final float hiccup;
    private final float rewind;
    private final float repeat;
    private final float binary;
    private final float randomCharacter;
    private final float filler;
    private final float startFiller;

    MessageDistorter(Random random, float alcohol, float zero, float cannabis) {
        this.random = random;
        randomCase = ramp(alcohol, 0.3F, 1) * 0.06F + ramp(zero, 0, 0.3F);
        randomLetter = ramp(alcohol, 0.5F, 1) * 0.015F;
        slur = ramp(alcohol, 0.2F, 0.6F);
        longSlur = alcohol * 0.8F;
        hiccup = ramp(alcohol, 0.5F, 1) * 0.04F;
        rewind = ramp(alcohol, 0.4F, 0.9F) * 0.03F;
        repeat = ramp(alcohol, 0.3F, 1) * 0.025F;
        binary = ramp(zero, 0.6F, 0.95F);
        randomCharacter = ramp(zero, 0.2F, 0.95F);
        filler = ramp(cannabis, 0.2F, 0.95F) * 0.1F;
        startFiller = ramp(cannabis, 0.2F, 0.95F) * 0.7F;
    }

    private static float ramp(float value, float start, float end) {
        return MathHelper.clamp((value - start) / (end - start), 0, 1);
    }

    String distort(String text, boolean startOfMessage) {
        StringBuilder result = new StringBuilder(text.length());
        int rewindsLeft = text.length();
        for (int index = 0; index < text.length();) {
            int start = index;
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (codePoint == '\u00a7' && index < text.length()) {
                result.appendCodePoint(codePoint).appendCodePoint(text.codePointAt(index));
                index += Character.charCount(text.codePointAt(index));
                continue;
            }
            if (codePoint == '\n' || codePoint == '\r') {
                result.appendCodePoint(codePoint);
                continue;
            }

            int distorted = distortCharacter(codePoint);
            if ((distorted == 's' || distorted == 'S') && random.nextFloat() < slur) {
                result.appendCodePoint(distorted).append(random.nextFloat() < longSlur ? "hh" : "h");
            } else if (distorted == ' ' && random.nextFloat() < filler) {
                result.append(translation("filler_words.")).append(' ');
            } else {
                if (startOfMessage && random.nextFloat() < startFiller) {
                    result.append(translation("start_filler_words.")).append(' ');
                }
                result.appendCodePoint(distorted);
            }
            startOfMessage = false;

            if (random.nextFloat() < repeat) {
                float more = 1.2F;
                do {
                    more *= 0.5F;
                    result.appendCodePoint(distorted);
                } while (random.nextFloat() < more);
            }
            if (random.nextFloat() < hiccup) result.append(translation("hics."));
            if (random.nextFloat() < rewind && rewindsLeft > 0) {
                result.append("... ");
                rewindsLeft--;
                int words = random.nextInt(5) + 1;
                int previousSpace = start;
                for (int count = 0; count < words && previousSpace >= 0; count++) {
                    previousSpace = text.lastIndexOf(' ', previousSpace - 1);
                }
                index = previousSpace < 0 ? Character.charCount(text.codePointAt(0)) : previousSpace + 1;
            }
        }
        return result.toString();
    }

    private int distortCharacter(int codePoint) {
        if (random.nextFloat() < binary) return random.nextBoolean() ? '0' : '1';
        if (random.nextFloat() < randomCharacter) return ' ' + random.nextInt('~' - ' ' + 1);
        if (random.nextFloat() < randomLetter) return (random.nextBoolean() ? 'a' : 'A') + random.nextInt(26);
        if (random.nextFloat() < randomCase && random.nextBoolean()) {
            return Character.isUpperCase(codePoint) ? Character.toLowerCase(codePoint) : Character.toUpperCase(codePoint);
        }
        return codePoint;
    }

    private String translation(String group) {
        String prefix = TRANSLATION_PREFIX + group;
        int variants = 0;
        while (I18n.hasKey(prefix + variants)) variants++;
        return I18n.format(prefix + (variants == 0 ? 0 : random.nextInt(variants)));
    }
}
