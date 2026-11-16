package ivorius.psychedelicraft.entity.drug;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SmokeStateTest {
    @Test public void savedSmokeColorAndCooldownResumeAndExpire(){
        CompoundTag saved=new CompoundTag();saved.putInt("smokeTicks",23);saved.putInt("smokeColor",0x80e666);
        DrugProperties properties=new DrugProperties();properties.fromNbt(saved);
        assertTrue(properties.isBreathingSmoke());assertEquals(0x80e666,properties.toNbt().getInt("smokeColor"));
        for(int i=0;i<22;i++)properties.tick(i,null);assertTrue(properties.isBreathingSmoke());
        properties.tick(22,null);assertFalse(properties.isBreathingSmoke());assertFalse(properties.toNbt().contains("smokeTicks"));
    }
    @Test public void malformedSmokeSavesHaveBoundedCooldownAndRgb(){
        CompoundTag saved=new CompoundTag();saved.putInt("smokeTicks",Integer.MAX_VALUE);saved.putInt("smokeColor",-1);
        DrugProperties properties=new DrugProperties();properties.fromNbt(saved);
        assertEquals(29,properties.toNbt().getInt("smokeTicks"));assertEquals(0xffffff,properties.toNbt().getInt("smokeColor"));
        saved.putInt("smokeTicks",-1);properties.fromNbt(saved);assertFalse(properties.isBreathingSmoke());
    }
}
