package ivorius.psychedelicraft.entity.drug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.DoubleSupplier;

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
    public void cocaineDesaturationScalesWithStrengthAndBlendsWithTobacco() {
        DrugProperties properties = new DrugProperties();
        assertEquals(0F, properties.getDesaturation(0F), 0F);

        lockAt(properties, DrugType.COCAINE, 0.4);
        tick(properties, 1, 120);
        assertEquals(0.3F, properties.getDesaturation(0F), 0.00001F);

        lockAt(properties, DrugType.TOBACCO, 0.5);
        tick(properties, 121, 240);
        assertEquals(1F - 0.7F * 0.9F * 0.8F, properties.getDesaturation(0.2F), 0.00001F);
    }

    @Test
    public void oneLsdPillPeaksNearThreeBlocksAndOverlappingPillsReachSeven() {
        DrugProperties properties = new DrugProperties();
        properties.addInfluence(new DrugInfluence(DrugType.LSD, DrugInfluence.INGESTED_DELAY, 0.05, 0.003, 0.6));
        float singlePeak = peak(properties, Drug::extraBlockReach);
        assertTrue(singlePeak > 2.6F && singlePeak < 3.4F);
        properties.addInfluence(new DrugInfluence(DrugType.LSD, DrugInfluence.INGESTED_DELAY, 0.05, 0.003, 0.6));
        properties.addInfluence(new DrugInfluence(DrugType.LSD, DrugInfluence.INGESTED_DELAY, 0.05, 0.003, 0.6));
        assertEquals(7F, peak(properties, Drug::extraBlockReach), 0.00001F);
        tick(properties, 901, 6000);
        assertEquals(0F, properties.getModifier(Drug::extraBlockReach), 0F);
    }

    @Test
    public void mushroomAvoidancePeaksScaleWithOverlappingRedAndBrownDoses() {
        for (int doses = 1; doses <= 4; doses++) {
            DrugProperties properties = new DrugProperties();
            for (int dose = 0; dose < doses; dose++) {
                properties.addInfluence(mushroom(DrugType.BROWN_SHROOMS));
            }
            assertEquals(doses / 4F, peak(properties, Drug::mobAvoidanceChance), 0.003F);
            assertTrue(properties.getActiveValue(DrugType.BROWN_SHROOMS) <= 1);
        }

        DrugProperties mixed = withMushroom(DrugType.RED_SHROOMS);
        mixed.addInfluence(mushroom(DrugType.BROWN_SHROOMS));
        assertEquals(0.5F, peak(mixed, Drug::mobAvoidanceChance), 0.003F);
        tick(mixed, 901, 6000);
        assertEquals(0F, mixed.getModifier(Drug::mobAvoidanceChance), 0F);
    }

    @Test
    public void eitherMushroomStartsLocalGrowthAtTwoBlocksAndThreeTicks() {
        for (DrugType type : new DrugType[]{DrugType.RED_SHROOMS, DrugType.BROWN_SHROOMS}) {
            DrugProperties properties = withMushroom(type);
            assertEquals(0, MushroomGrowth.tickCount(MushroomGrowth.strength(properties)));
            double peak = peakValue(properties, () -> MushroomGrowth.strength(properties));
            assertTrue(peak > 0);
            assertEquals(2, MushroomGrowth.radius(peak));
            assertEquals(3, MushroomGrowth.tickCount(peak));
        }
    }

    @Test
    public void overlappingMushroomsExpandGrowthAndStopAfterTheEffectExpires() {
        DrugProperties properties = withMushroom(DrugType.RED_SHROOMS);
        properties.addInfluence(mushroom(DrugType.BROWN_SHROOMS));
        double mixed = peakValue(properties, () -> MushroomGrowth.strength(properties));
        assertTrue(MushroomGrowth.radius(mixed) > 2 && MushroomGrowth.radius(mixed) < 5);
        assertTrue(MushroomGrowth.tickCount(mixed) > 3 && MushroomGrowth.tickCount(mixed) < 18);

        DrugProperties stacked = withMushroom(DrugType.RED_SHROOMS);
        stacked.addInfluence(mushroom(DrugType.RED_SHROOMS));
        stacked.addInfluence(mushroom(DrugType.BROWN_SHROOMS));
        stacked.addInfluence(mushroom(DrugType.BROWN_SHROOMS));
        double strongest = peakValue(stacked, () -> MushroomGrowth.strength(stacked));
        assertEquals(1, strongest, EPSILON);
        assertEquals(5, MushroomGrowth.radius(strongest));
        assertEquals(18, MushroomGrowth.tickCount(strongest));

        tick(stacked, 901, 18000);
        double expired = MushroomGrowth.strength(stacked);
        assertEquals(0, MushroomGrowth.radius(expired));
        assertEquals(0, MushroomGrowth.tickCount(expired));
    }

    @Test
    public void peyoteFlightCostsTwiceAsMuchUpToOnePointPerSecond() {
        assertEquals(20F, 1F / PeyoteFlight.saturationPerTick(0), 0.001F);
        assertEquals(20F, 1F / PeyoteFlight.saturationPerTick(0.1), 0.001F);
        assertEquals(55F, 1F / PeyoteFlight.saturationPerTick(0.5), 0.001F);
        assertEquals(100F, 1F / PeyoteFlight.saturationPerTick(1), 0.001F);
    }

    private static float peak(DrugProperties properties, Drug.Modifier modifier) {
        return (float) peakValue(properties, () -> properties.getModifier(modifier));
    }

    private static double peakValue(DrugProperties properties, DoubleSupplier value) {
        double maximum = 0;
        for (int age = 1; age <= 900; age++) {
            properties.tick(age);
            maximum = Math.max(maximum, value.getAsDouble());
        }
        return maximum;
    }

    @Test
    public void stimulantActionChancesUseTheirOwnThresholdsAndAddTogether() {
        DrugProperties properties = new DrugProperties();
        lockAt(properties, DrugType.CAFFEINE, 0.3);
        lockAt(properties, DrugType.SUGAR, 0.6);
        lockAt(properties, DrugType.BATH_SALTS, 0.5);
        lockAt(properties, DrugType.COCAINE, 0.5);
        tick(properties, 1, 120);

        assertEquals(0F, properties.getModifier(Drug::randomJumpChance), 0F);
        assertEquals(0.05F * (0.6F - 0.3F) / (1F - 0.3F),
                properties.getModifier(Drug::randomSwingChance), 0.00001F);

        for (DrugType type : new DrugType[]{DrugType.CAFFEINE, DrugType.SUGAR, DrugType.BATH_SALTS, DrugType.COCAINE}) {
            properties.setDrugValue(type, 1);
        }
        tick(properties, 121, 240);

        assertEquals(0.20F, properties.getModifier(Drug::randomJumpChance), 0.00001F);
        assertEquals(0.14F, properties.getModifier(Drug::randomSwingChance), 0.00001F);
    }

    @Test
    public void stimulantEnergySuppressesDrowsinessWithoutGivingKavaItsUnusedProfile() {
        DrugProperties properties = new DrugProperties();
        lockAt(properties, DrugType.KAVA, 1);
        tick(properties, 1, 120);
        assertEquals(0F, properties.getModifier(Drug::drowsiness), 0F);

        lockAt(properties, DrugType.SLEEP_DEPRIVATION, 0.8);
        tick(properties, 121, 240);
        assertEquals(0.8F, properties.getModifier(Drug::drowsiness), 0.00001F);

        lockAt(properties, DrugType.CAFFEINE, 0.05);
        tick(properties, 241, 360);
        assertEquals(0.3F, properties.getModifier(Drug::drowsiness), 0.00001F);
    }

    private static void lockAt(DrugProperties properties, DrugType type, double value) {
        properties.setDrugValue(type, value);
        properties.setDrugLocked(type, true);
    }

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
