package ivorius.psychedelicraft.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MsgEntitySoundTest {
    @Test
    public void roundTripsTheSourceAndSoundParameters() {
        ResourceLocation sound = new ResourceLocation("minecraft", "entity.sheep.shear");
        PacketBuffer encoded = new PacketBuffer(Unpooled.buffer());
        PacketBuffer decoded = new PacketBuffer(Unpooled.buffer());
        try {
            new MsgEntitySound(32784, -1, sound, SoundCategory.PLAYERS, 0.75F, 1.25F).toBytes(encoded);
            MsgEntitySound copy = new MsgEntitySound();
            copy.fromBytes(encoded);
            copy.toBytes(decoded);
            assertEquals(32784, decoded.readVarInt());
            assertEquals(-1, decoded.readInt());
            assertEquals(sound, decoded.readResourceLocation());
            assertEquals(SoundCategory.PLAYERS, decoded.readEnumValue(SoundCategory.class));
            assertEquals(0.75F, decoded.readFloat(), 0F);
            assertEquals(1.25F, decoded.readFloat(), 0F);
            assertFalse(decoded.isReadable());
        } finally {
            encoded.release();
            decoded.release();
        }
    }
}
