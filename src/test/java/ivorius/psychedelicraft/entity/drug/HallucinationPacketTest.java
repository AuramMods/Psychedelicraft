package ivorius.psychedelicraft.entity.drug;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class HallucinationPacketTest {
    @Test public void everyTypeRoundTripsWithAndWithoutACommandedPosition(){
        for(var type:HallucinationType.values())for(var position:new BlockPos[]{null,new BlockPos(-132,72,250)}){
            var message=new DrugNetwork.ForcedHallucination(411,UUID.randomUUID(),new ResourceLocation("minecraft:the_nether"),type,position);
            var buffer=new FriendlyByteBuf(Unpooled.buffer());try{message.encode(buffer);assertEquals(message,DrugNetwork.ForcedHallucination.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
            assertSame(type,HallucinationType.byName(type.id().toString()));assertSame(type,HallucinationType.byName(type.id().getPath()));
        }
    }
    @Test public void unknownTypeCannotSelectAnotherNamespace(){
        assertNull(HallucinationType.byName("othermod:single_entity"));
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{buffer.writeVarInt(3);buffer.writeUUID(UUID.randomUUID());buffer.writeResourceLocation(new ResourceLocation("minecraft:overworld"));buffer.writeUtf("othermod:single_entity");buffer.writeBoolean(false);assertNull(DrugNetwork.ForcedHallucination.decode(buffer).type());}finally{buffer.release();}
    }
    @Test(expected=io.netty.handler.codec.DecoderException.class) public void oversizedTypeIsRejectedByTheBoundedCodec(){
        var buffer=new FriendlyByteBuf(Unpooled.buffer());try{buffer.writeVarInt(3);buffer.writeUUID(UUID.randomUUID());buffer.writeResourceLocation(new ResourceLocation("minecraft:overworld"));buffer.writeUtf("a".repeat(65));DrugNetwork.ForcedHallucination.decode(buffer);}finally{buffer.release();}
    }
}
