package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.*;
import ivorius.psychedelicraft.entity.drug.type.HarmoniumDrug;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.Test;
import static org.junit.Assert.*;

public class HallucinationManagerTest {
    static DrugProperties lsd(){var properties=new DrugProperties();var root=properties.toNbt();var drug=new CompoundTag();drug.putDouble("effect",1);drug.putDouble("effectActive",1);root.getCompound("Drugs").put(DrugType.LSD.id(),drug);properties.fromNbt(root);return properties;}
    @Test public void soberInputsNeverSelectOrRetainAnyVisualChannel(){
        var h=new HallucinationManager();var properties=new DrugProperties();var random=RandomSource.create(18);for(int i=0;i<1000;i++)h.update(properties,random,i);for(var effect:HallucinationManager.Effect.values())assertEquals(0,h.strength(effect),0);assertNull(h.contrastColor(null));assertNull(h.bloomColor(null));
    }
    @Test public void realDrugProfilesSelectWeightedChannelsAndThenReturnToNeutral(){
        var h=new HallucinationManager();var properties=lsd();var random=RandomSource.create(19);for(int i=0;i<600;i++)h.update(properties,random,i);float total=0;for(var effect:HallucinationManager.Effect.values()){float strength=h.strength(effect);assertTrue(Float.isFinite(strength)&&strength>=0);total+=strength;}assertTrue(total>0);properties.fromNbt(new CompoundTag());for(int i=600;i<2600;i++)h.update(properties,random,i);for(var effect:HallucinationManager.Effect.values())assertEquals(0,h.strength(effect),0);
    }

    @Test public void aSingleBrownMushroomReservesSmallWavesAcrossRandomSelections() {
        var properties = new DrugProperties();
        var root = properties.toNbt();
        var drug = new CompoundTag();
        drug.putDouble("effect", 0.38);
        drug.putDouble("effectActive", 0.38);
        root.getCompound("Drugs").put(DrugType.BROWN_SHROOMS.id(), drug);
        properties.fromNbt(root);

        for (int seed = 0; seed < 64; seed++) {
            var manager = new HallucinationManager();
            var random = RandomSource.create(seed);
            for (int tick = 0; tick < 240; tick++) manager.update(properties, random, tick);
            assertTrue("terrain wave for seed " + seed, manager.strength(HallucinationManager.Effect.SMALL_WAVES) > 0.25F);
        }
    }
    @Test public void harmoniumSuppliesItsActualTintWithoutRandomHallucinations(){
        var h=new HallucinationManager();var drug=new HarmoniumDrug();var tag=new CompoundTag();tag.putDouble("effect",.5);tag.putDouble("effectActive",.5);tag.putFloat("currentColor[0]",.1F);tag.putFloat("currentColor[1]",.5F);tag.putFloat("currentColor[2]",.9F);drug.fromNbt(tag);assertArrayEquals(new float[]{.1F,.5F,.9F,.5F},h.contrastColor(drug),.00001F);assertArrayEquals(new float[]{.1F,.5F,.9F,1.5F},h.bloomColor(drug),.00001F);
    }
}
