package ivorius.psychedelicraft.entity.drug;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlantDrugsTest {
    @Test
    public void peyoteUsesItsOwnColorAndContextualProfile() {
        DrugProperties properties = new DrugProperties();
        properties.addToDrug(DrugType.PEYOTE, 0.5);
        properties.tick(1);

        float active = (float) properties.getActiveValue(DrugType.PEYOTE);
        assertTrue(active > 0);
        assertEquals(active * 0.3F, properties.getModifier(Drug::colorHallucinationStrength), 0F);
        assertEquals(active * 0.6F, properties.getModifier(Drug::contextualHallucinationStrength), 0F);
    }

    @Test
    public void belladonnaDirectIntensificationCombinesWithSelectedEffects() {
        DrugProperties properties = new DrugProperties();
        properties.addToDrug(DrugType.ATROPINE, 0.5);
        properties.tick(1);

        float active = (float) properties.getActiveValue(DrugType.ATROPINE);
        assertEquals(0F, properties.getModifier(Drug::colorHallucinationStrength), 0F);
        assertEquals(active * 0.8F, properties.getColorIntensification(0F), 0F);
        assertEquals(0.4F + 0.6F * active * 0.8F, properties.getColorIntensification(0.4F), 1.0e-7F);
    }

    @Test
    public void caffeineHalvesOverdoseStrengthAndConsumesItsOwnDesiredValue() {
        DrugProperties properties = new DrugProperties();
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagCompound drugs = new NBTTagCompound();
        drugs.setTag(DrugType.ATROPINE.id(), state(1, 1));
        drugs.setTag(DrugType.CAFFEINE.id(), state(0.5, 0.25));
        saved.setTag("Drugs", drugs);
        properties.fromNbt(saved);
        properties.tick(1);

        NBTTagCompound updated = properties.toNbt().getCompoundTag("Drugs");
        assertEquals(0.4997, updated.getCompoundTag(DrugType.ATROPINE.id()).getDouble("effect"), 1.0e-8);
        assertEquals(0D, updated.getCompoundTag(DrugType.CAFFEINE.id()).getDouble("effect"), 0D);
        assertTrue(properties.getActiveValue(DrugType.ATROPINE) < 0.99F);
    }

    private static NBTTagCompound state(double desired, double active) {
        NBTTagCompound state = new NBTTagCompound();
        state.setDouble("effect", desired);
        state.setDouble("effectActive", active);
        return state;
    }
}
