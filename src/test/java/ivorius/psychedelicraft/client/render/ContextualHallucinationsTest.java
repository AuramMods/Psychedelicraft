package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ContextualHallucinationsTest {
    @Test
    public void naturalSelectionRespectsCannabisAndAvoidsADuplicateHead() {
        assertSame(HallucinationType.RASTA_HEAD, ContextualHallucinations.pickNatural(
                rolls(false, 0.05F), 0.4F, 0.41, false));
        assertSame(HallucinationType.MULTIPLE_ENTITY, ContextualHallucinations.pickNatural(
                rolls(false, 0.05F, 0.1F), 0.4F, 0.41, true));
        assertSame(HallucinationType.SINGLE_ENTITY, ContextualHallucinations.pickNatural(
                rolls(false, 0.05F, 0.7F), 0.4F, 0.4, false));
    }

    @Test
    public void identitySwapsRequireTheAuthoredStrength() {
        assertSame(HallucinationType.SINGLE_ENTITY, ContextualHallucinations.pickNatural(
                rolls(true, 0.6F, 0.7F), 0.79F, 0, false));
        assertSame(HallucinationType.HOSTILE_VILLAGERS, ContextualHallucinations.pickNatural(
                rolls(true, 0.6F, 0.7F), 0.8F, 0, false));
        assertSame(HallucinationType.FRIENDLY_ZOMBIES, ContextualHallucinations.pickNatural(
                rolls(false, 0.6F, 0.7F), 0.8F, 0, false));
    }

    @Test
    public void preservesTheDonorIdsAndResolvesLegacyAliases() {
        for (String id : new String[]{"allay", "bee", "axolotl", "camel", "dolphin", "warden", "glow_squid",
                "cod", "salmon", "tropical_fish"}) {
            assertNotNull(id, ApparitionModels.forId(new ResourceLocation(id)));
        }
        assertNull(ApparitionModels.forId(new ResourceLocation("anothermod", "allay")));
        assertEquals(new ResourceLocation("snowman"), ApparitionPool.legacyId(new ResourceLocation("snow_golem")));
        assertEquals(new ResourceLocation("villager_golem"), ApparitionPool.legacyId(new ResourceLocation("iron_golem")));
    }

    private static Random rolls(boolean choice, float... floats) {
        return new Random() {
            private int index;

            @Override
            public float nextFloat() {
                return floats[index++];
            }

            @Override
            public boolean nextBoolean() {
                return choice;
            }
        };
    }
}
