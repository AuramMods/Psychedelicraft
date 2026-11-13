/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import java.util.Random;

final class RastaHeadDialogue {
    private static final String[][] RANDOM_STATEMENTS = {
            {"Did you ever notice, like...", "Sorry, I forgot what I was trying to say"},
            {"Want another joint? I got so many, like, 2 or 3 more..."},
            {"Duuuude...", "DuuuuuuuuuuuuuuuuuuDe!", "DuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuDe!", "Yo dudeliodude!"},
            {"Woah, like, woah", "That's, like, so, rad, dude"},
            {"You know what would go excellently with this?", "DIRT"},
            {"Like, hear me out","Just this once","Come on, come oon"},
            {"I am SO high right now","Look at me flyyyyy..."},
            {"OoOoOOowOoOoOo", "SpooOoOoOoOky floating Head"},
            {"Creeper!"},
            {"Behind you"},
            {"Get him"},
            {"Ahahahahaaaa"},
            {"My minds is, like, so opened right now", "I'm like a can of beans","If you... yknow..Used a can opener on me","I'm like. My mind is like an opened can of beans","Like a can of beans you opened"},
            {"You not gonna eat my beans, are you?"},
            {"Don't look now, but I think this guy's a little bit sus"},
            {"It's aaaaalll natural, babey"}
    };

    private static final String[][] RESPONSES_TO_PLAYER = {
            {"Haha, you're so right."},
            {"Haha, he's so right."},
            {"Yes."},
            {"Great idea"},
            {"Wooooooooah", "That's, like, dued", "Are you, like, a genie or something?"},
            {"Wooooooah"},
            {"My minds is, like, so opened right now"}
    };

    private RastaHeadDialogue() {
    }

    static String[] statement(Random random) {
        return RANDOM_STATEMENTS[random.nextInt(RANDOM_STATEMENTS.length)];
    }

    static String[] response(Random random) {
        return random.nextFloat() < 0.4F ? RESPONSES_TO_PLAYER[random.nextInt(RESPONSES_TO_PLAYER.length)] : new String[0];
    }
}
