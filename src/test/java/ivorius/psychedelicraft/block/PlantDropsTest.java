package ivorius.psychedelicraft.block;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlantDropsTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void decayCanKeepOnlyPartOfAStack() {
        ItemStack stack = new ItemStack(Items.WHEAT_SEEDS, 4);
        PlantDrops.decay(stack, samples(0.4F, 0.5F, 0.1F, 0.9F), 0.5F);
        assertEquals(3, stack.getCount());
    }

    @Test
    public void decayCanBeAppliedTwiceAndLeavesCertainDropsAlone() {
        ItemStack stack = new ItemStack(Items.STICK, 3);
        PlantDrops.decay(stack, samples(), 1F);
        assertEquals(3, stack.getCount());
        PlantDrops.decay(stack, samples(0.1F, 0.2F, 0.9F), 0.5F);
        assertEquals(2, stack.getCount());
        PlantDrops.decay(stack, samples(0.8F, 0.7F), 0.5F);
        assertTrue(stack.isEmpty());
    }

    private static Random samples(float... values) {
        return new Random(0) {
            private int index;

            @Override
            public float nextFloat() {
                return values[index++];
            }
        };
    }
}
