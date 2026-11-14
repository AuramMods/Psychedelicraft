package ivorius.psychedelicraft.client.render.block;

import net.minecraft.util.math.Vec3d;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RiftGlyphPathTest {
    @Test
    public void spiralUsesTheDonorAxisOffsetsAndWrapsAtTheEnd() {
        RiftGlyphPath path = RiftGlyphPath.spiral(new Vec3d(-2, 6, 4), 0.1, 0.5, 8);
        Vec3d end = path.at(1 - 1e-9);

        assertEquals(-0.1, end.x, 1e-6);
        assertEquals(6, end.y, 1e-6);
        assertEquals(-0.15, end.z, 1e-6);
        assertEquals(path.at(0.9), path.at(-0.1));
        assertEquals(path.at(0), path.at(1));
    }

    @Test
    public void levelConnectionHasFinitePositions() {
        RiftGlyphPath path = RiftGlyphPath.spiral(new Vec3d(0, 0, 0), 0.1, 0.5, 8);
        for (int step = 0; step < 23; step++) {
            Vec3d point = path.at(step / 23.0);
            assertTrue(Double.isFinite(point.x));
            assertTrue(Double.isFinite(point.z));
            assertEquals(0, point.y, 0);
        }
    }
}
