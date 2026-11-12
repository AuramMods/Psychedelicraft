package ivorius.psychedelicraft.network;

import io.netty.buffer.Unpooled;
import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MsgHallucinateTest {
    @Test
    public void preservesTargetDimensionAndOptionalPosition() {
        BlockPos position = new BlockPos(-19, 245, 17200);
        assertRoundTrip(position);
        assertRoundTrip(null);
    }

    private static void assertRoundTrip(BlockPos position) {
        PacketBuffer encoded = new PacketBuffer(Unpooled.buffer());
        PacketBuffer decoded = new PacketBuffer(Unpooled.buffer());
        try {
            new MsgHallucinate(32784, -1, HallucinationType.RASTA_HEAD, position).toBytes(encoded);
            MsgHallucinate copy = new MsgHallucinate();
            copy.fromBytes(encoded);
            copy.toBytes(decoded);
            assertEquals(32784, decoded.readVarInt());
            assertEquals(-1, decoded.readInt());
            assertEquals("psychedelicraft:rasta_head", decoded.readString(64));
            if (position == null) {
                assertFalse(decoded.readBoolean());
            } else {
                assertTrue(decoded.readBoolean());
                assertEquals(position, decoded.readBlockPos());
            }
            assertFalse(decoded.isReadable());
        } finally {
            encoded.release();
            decoded.release();
        }
    }
}
