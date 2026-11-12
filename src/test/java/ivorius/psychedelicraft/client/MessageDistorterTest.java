package ivorius.psychedelicraft.client;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageDistorterTest {
    @Test
    public void fullZeroUsesOneBinaryDigitPerVisibleCodePoint() {
        MessageDistorter effect = new MessageDistorter(new Random(821), 0, 1, 0);
        String text = "\u00a7aA\uD83D\uDE00B\nC";
        assertTrue(effect.distort(text, true).matches("\u00a7a[01]{3}\n[01]"));
    }

    @Test
    public void distortedOutgoingTextCannotBecomeACommandOrSplitAUnicodeCharacter() {
        assertEquals(" \t?list", ClientChatEffects.keepAsChat(" \t/list"));
        assertEquals("\u00a0?list", ClientChatEffects.keepAsChat("\u00a0/list"));
        assertEquals("ordinary /chat", ClientChatEffects.keepAsChat("ordinary /chat"));

        StringBuilder message = new StringBuilder();
        for (int index = 0; index < 255; index++) message.append('a');
        String prefix = message.toString();
        assertEquals(prefix, ClientChatEffects.keepAsChat(prefix + "\uD83D\uDE00b"));
    }
}
