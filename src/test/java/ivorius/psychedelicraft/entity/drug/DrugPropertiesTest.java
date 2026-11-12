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
