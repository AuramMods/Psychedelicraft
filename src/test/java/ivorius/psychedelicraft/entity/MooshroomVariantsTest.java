package ivorius.psychedelicraft.entity;

import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MooshroomVariantsTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void matchingParentsCanMutateAtOneIn1024() {
        Random mutation = new Random() {
            @Override
            public int nextInt(int bound) {
                assertEquals(1024, bound);
                return 0;
            }
        };
        assertTrue(MooshroomVariants.offspringBrown(false, false, mutation));
        assertFalse(MooshroomVariants.offspringBrown(true, true, mutation));
    }

    @Test
    public void mixedParentsPassOnOneOfTheirColorsWithoutRerolling() {
        Random first = new Random() {
            @Override
            public int nextInt(int bound) {
                throw new AssertionError("Mixed parents do not roll a mutation");
            }

            @Override
            public boolean nextBoolean() {
                return true;
            }
        };
        assertTrue(MooshroomVariants.offspringBrown(true, false, first));
        assertFalse(MooshroomVariants.offspringBrown(false, true, first));
    }
}
