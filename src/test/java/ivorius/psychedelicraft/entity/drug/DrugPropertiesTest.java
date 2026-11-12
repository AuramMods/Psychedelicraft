package ivorius.psychedelicraft.entity.drug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ivorius.psychedelicraft.entity.drug.influence.DrugInfluence;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DrugPropertiesTest {
    private static final double EPSILON = 1.0e-8;

    @Test
    public void lockedRiftEffectsSurviveReloadAndOnlyAcceptExplicitSets() {
        DrugProperties drugs = new DrugProperties();
        drugs.setDrugValue(DrugType.POWER, 0.7);
        drugs.setDrugLocked(DrugType.POWER, true);
        drugs.setDrugValue(DrugType.ZERO, 0.7);
        drugs.setDrugLocked(DrugType.ZERO, true);
        DrugProperties restored = new DrugProperties();
        restored.fromNbt(drugs.toNbt());
        restored.addToDrug(DrugType.POWER, 0.3);
        restored.addToDrug(DrugType.ZERO, 0.3);
        tick(restored, 1, 120);

        assertEquals(0.7, restored.getActiveValue(DrugType.POWER), EPSILON);
        assertEquals(0.7, restored.getActiveValue(DrugType.ZERO), EPSILON);
        restored.setDrugValue(DrugType.ZERO, 0.4);
        restored.setDrugLocked(DrugType.POWER, false);
        tick(restored, 121, 180);
        assertTrue(restored.getActiveValue(DrugType.POWER) < 0.2);
        assertEquals(0.4, restored.getActiveValue(DrugType.ZERO), EPSILON);
    }

    @Test
    public void registeredAlcoholAndKavaStackDoubleVisionThroughTheRemainingStrength() {
        NBTTagCompound snapshot = new NBTTagCompound();
        NBTTagCompound drugs = new NBTTagCompound();
        NBTTagCompound state = new NBTTagCompound();
        state.setDouble("effect", 0.625);
        state.setDouble("effectActive", 0.625);
        drugs.setTag(DrugType.ALCOHOL.id(), state);
        drugs.setTag(DrugType.KAVA.id(), state.copy());
        snapshot.setTag("Drugs", drugs);
        DrugProperties properties = new DrugProperties();
        properties.fromNbt(snapshot);

        assertEquals(0.75F, properties.blendStrength(0F, Drug::doubleVision), 0.00001F);
        assertEquals(0F, properties.getModifier(Drug::heartbeatVolume), 0F);
    }

    @Test
    public void powerAlcoholAndFatigueStackMotionBlurThroughTheRemainingStrength() {
        DrugProperties properties = new DrugProperties();
        properties.setDrugValue(DrugType.POWER, 0.8);
        properties.setDrugValue(DrugType.ALCOHOL, 0.75);
        properties.setDrugValue(DrugType.SLEEP_DEPRIVATION, 0.8);
        properties.setDrugLocked(DrugType.POWER, true);
        properties.setDrugLocked(DrugType.ALCOHOL, true);
        properties.setDrugLocked(DrugType.SLEEP_DEPRIVATION, true);
        tick(properties, 1, 120);

        assertEquals(1F - 0.76F * 0.85F * 0.4F, properties.blendStrength(0F, Drug::motionBlur), 0.00001F);
    }

    @Test
    public void soundModifiersMultiplyAfterTheLsdRamp() {
        DrugProperties properties = new DrugProperties();
        properties.setDrugValue(DrugType.POWER, 0.4);
        properties.setDrugValue(DrugType.LSD, 0.8);
        properties.setDrugValue(DrugType.SLEEP_DEPRIVATION, 0.75);
        properties.setDrugLocked(DrugType.POWER, true);
        properties.setDrugLocked(DrugType.LSD, true);
        properties.setDrugLocked(DrugType.SLEEP_DEPRIVATION, true);
        tick(properties, 1, 50);
        assertEquals(0.6F * 1.5F, properties.getMultiplier(Drug::soundVolumeModifier), 0.00001F);

        tick(properties, 51, 300);
        assertEquals(0.6F * 1.5F * 2.4F, properties.getMultiplier(Drug::soundVolumeModifier), 0.00001F);
    }

    @Test
    public void ingestionStartsOnTheFifteenthFiveTickUpdate() {
        DrugProperties properties = withMushroom(DrugType.BROWN_SHROOMS);

        tick(properties, 1, 74);
        assertEquals(0, properties.getModifier(Drug::movementHallucinationStrength), EPSILON);

        properties.tick(75);
        assertEquals(0.005265, properties.getModifier(Drug::movementHallucinationStrength), EPSILON);
    }

    @Test
    public void savedDelayAndActiveEffectsResumeAfterLoading() throws IOException {
        DrugProperties before = withMushroom(DrugType.BROWN_SHROOMS);
        tick(before, 1, 150);
        before.addInfluence(mushroom(DrugType.RED_SHROOMS));
        tick(before, 151, 175);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(before.toNbt(), bytes);
        DrugProperties after = new DrugProperties();
        after.fromNbt(CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes.toByteArray())));

        assertTrue(after.getModifier(Drug::colorHallucinationStrength) > 0);
        for (int age = 176; age <= 300; age++) {
            before.tick(age);
            after.tick(age);
            assertEquals(before.getModifier(Drug::colorHallucinationStrength),
                    after.getModifier(Drug::colorHallucinationStrength), EPSILON);
        }
    }

    @Test
    public void redAndBrownHaveDifferentModifiersAtTheSameStrength() {
        DrugProperties brown = withMushroom(DrugType.BROWN_SHROOMS);
        DrugProperties red = withMushroom(DrugType.RED_SHROOMS);
        tick(brown, 1, 100);
        tick(red, 1, 100);

        assertTrue(red.getModifier(Drug::colorHallucinationStrength) > brown.getModifier(Drug::colorHallucinationStrength));
        assertTrue(brown.getModifier(Drug::movementHallucinationStrength) > red.getModifier(Drug::movementHallucinationStrength));
        assertEquals(brown.getModifier(Drug::viewWobblyness), red.getModifier(Drug::viewWobblyness), EPSILON);
    }

    @Test
    public void wakingUpCancelsPendingInfluencesAndFadesActiveEffects() {
        DrugProperties properties = withMushroom(DrugType.BROWN_SHROOMS);
        tick(properties, 1, 150);
        properties.addInfluence(mushroom(DrugType.RED_SHROOMS));
        properties.onAwoken();

        tick(properties, 151, 350);
        assertEquals(0, properties.getModifier(Drug::colorHallucinationStrength), EPSILON);
    }

    @Test
    public void effectsExpireAfterTheDoseIsSpent() {
        DrugProperties properties = withMushroom(DrugType.BROWN_SHROOMS);
        tick(properties, 1, 250);
        assertTrue(properties.getModifier(Drug::movementHallucinationStrength) > 0);

        tick(properties, 251, 4000);
        assertEquals(0, properties.getModifier(Drug::movementHallucinationStrength), EPSILON);
    }

    @Test
    public void malformedAndUnknownInfluencesDoNotPoisonAnActiveEffect() {
        NBTTagCompound saved = withMushroom(DrugType.BROWN_SHROOMS).toNbt();
        NBTTagList influences = saved.getTagList("drugInfluences", Constants.NBT.TAG_COMPOUND);
        NBTTagCompound invalid = mushroom(DrugType.BROWN_SHROOMS).toNbt();
        invalid.setDouble("maxInfluence", Double.POSITIVE_INFINITY);
        influences.appendTag(invalid);
        NBTTagCompound unknown = mushroom(DrugType.BROWN_SHROOMS).toNbt();
        unknown.setString("drugType", "psychedelicraft:unregistered");
        influences.appendTag(unknown);

        DrugProperties actual = new DrugProperties();
        actual.fromNbt(saved);
        DrugProperties expected = withMushroom(DrugType.BROWN_SHROOMS);
        tick(actual, 1, 120);
        tick(expected, 1, 120);
        assertEquals(expected.getModifier(Drug::movementHallucinationStrength),
                actual.getModifier(Drug::movementHallucinationStrength), EPSILON);
    }

    private static DrugProperties withMushroom(DrugType type) {
        DrugProperties properties = new DrugProperties();
        properties.addInfluence(mushroom(type));
        return properties;
    }

    private static DrugInfluence mushroom(DrugType type) {
        return new DrugInfluence(type, DrugInfluence.INGESTED_DELAY, 0.005, 0.003, 0.5);
    }

    private static void tick(DrugProperties properties, int start, int end) {
        for (int age = start; age <= end; age++) {
            properties.tick(age);
        }
    }
}
