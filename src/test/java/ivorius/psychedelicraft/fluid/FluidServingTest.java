package ivorius.psychedelicraft.fluid;

import com.google.gson.JsonParser;
import ivorius.psychedelicraft.entity.drug.Drug;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.recipe.FurnaceRecipe;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FluidServingTest {
    private static FurnaceRecipe heating;

    @BeforeClass
    public static void bootstrap() throws Exception {
        Bootstrap.register();
        PSFluids.register();
        try (InputStreamReader json = new InputStreamReader(FluidServingTest.class.getResourceAsStream(
                "/assets/psychedelicraft/recipes/hot_coffee.json"), StandardCharsets.UTF_8)) {
            heating = (FurnaceRecipe) new FurnaceRecipe.Factory().parse(new JsonContext("psychedelicraft"),
                    new JsonParser().parse(json).getAsJsonObject());
        }
    }

    @Test
    public void heatingPreservesTheVesselVolumeAndUnrelatedTags() {
        ItemStack cup = new ItemStack(PSItems.STONE_CUP);
        cup.setStackDisplayName("Evening cup");
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setString("batch", "winter");
        cup.setTagInfo("fluid", FluidContents.write(new FluidStack(PSFluids.COFFEE, 125, attributes)));

        ItemStack warm = heating.resultFor(cup);
        ItemStack hot = heating.resultFor(warm);
        assertSame(PSItems.STONE_CUP, hot.getItem());
        assertEquals("Evening cup", hot.getDisplayName());
        FluidStack contents = PSItems.STONE_CUP.contents(hot);
        assertEquals(125, contents.amount);
        assertEquals("winter", contents.tag.getString("batch"));
        assertEquals(0, PSFluids.warmth(PSItems.STONE_CUP.contents(cup)));
        assertEquals(1, PSFluids.warmth(PSItems.STONE_CUP.contents(warm)));
        assertEquals(2, PSFluids.warmth(contents));
        assertTrue(heating.resultFor(hot).isEmpty());
        assertTrue(heating.resultFor(new ItemStack(PSItems.STONE_CUP)).isEmpty());
        assertEquals(0.2F, heating.getExperience(), 0.0001F);
    }

    @Test
    public void flaskHeatingAlsoKeepsItsFluidAttributes() {
        ItemStack flask = new ItemStack(PSItems.FLASK);
        flask.setTagInfo("fluid", FluidContents.write(new FluidStack(PSFluids.COFFEE, 8000)));
        ItemStack warmed = heating.resultFor(flask);
        assertSame(PSItems.FLASK, warmed.getItem());
        assertEquals(8000, PSItems.FLASK.contents(warmed).amount);
        assertEquals(1, PSFluids.warmth(PSItems.FLASK.contents(warmed)));

        flask.setTagInfo("fluid", FluidContents.write(new FluidStack(FluidRegistry.WATER, 8000)));
        assertTrue(heating.resultFor(flask).isEmpty());
    }

    @Test
    public void drinkingAndInjectionUseTheirOwnEligibleFluids() {
        for (Fluid fluid : new Fluid[]{PSFluids.COFFEE, PSFluids.PEYOTE_JUICE, PSFluids.CANNABIS_TEA,
                PSFluids.COCA_TEA, PSFluids.KAVA, PSFluids.RED_GRAPES, PSFluids.JIMSONWEED_EXTRACT,
                PSFluids.BELLADONNA_EXTRACT}) {
            assertTrue(PSFluids.canConsume(fluid, false));
            assertFalse(PSFluids.canConsume(fluid, true));
        }
        for (Fluid fluid : new Fluid[]{PSFluids.CAFFEINE, PSFluids.COCAINE, PSFluids.BATH_SALTS}) {
            assertTrue(PSFluids.canConsume(fluid, true));
            assertFalse(PSFluids.canConsume(fluid, false));
        }
        assertTrue(PSFluids.canConsume(PSFluids.MORNING_GLORY_EXTRACT, false));
        assertTrue(PSFluids.canConsume(PSFluids.MORNING_GLORY_EXTRACT, true));
        assertFalse(PSFluids.canConsume(PSFluids.SLURRY, false));
    }

    @Test
    public void brewAndExtractStagesScaleTheirInGameEffectsByServing() {
        NBTTagCompound attributes = new NBTTagCompound();
        attributes.setInteger(BrewFluid.FERMENTATION, 2);
        attributes.setInteger(BrewFluid.DISTILLATION, 2);
        attributes.setInteger(BrewFluid.MATURATION, 5);
        FluidStack cider = new FluidStack(PSFluids.APPLE, 250, attributes);
        NBTTagCompound influence = pending(cider).getCompoundTagAt(0);
        assertEquals(DrugType.ALCOHOL.id(), influence.getString("drugType"));
        assertEquals(20, influence.getInteger("delay"));
        assertEquals((0.35 + 1.7 * (1 - 1D / 3) + 0.05) / 4, influence.getDouble("maxInfluence"), 0.000001);
        cider.tag.setBoolean(BrewFluid.VINEGAR, true);
        assertEquals(0, pending(cider).tagCount());
        assertEquals(0, pending(new FluidStack(PSFluids.RED_GRAPES, 250)).tagCount());

        FluidStack extract = new FluidStack(PSFluids.MORNING_GLORY_EXTRACT, 10, attributes);
        NBTTagCompound concentrate = pending(extract).getCompoundTagAt(0);
        assertEquals(DrugType.LSD.id(), concentrate.getString("drugType"));
        assertEquals(96 * 96 / 100D, concentrate.getDouble("maxInfluence"), 0.000001);
        assertEquals(3, concentrate.getInteger("delay"));
        assertEquals(DrugType.ATROPINE.id(), pending(new FluidStack(PSFluids.BELLADONNA_EXTRACT, 250))
                .getCompoundTagAt(0).getString("drugType"));
    }

    @Test
    public void newEffectsTickStackAndRoundTripInTheExistingPlayerState() {
        DrugProperties properties = new DrugProperties();
        PSFluids.addServingInfluences(new FluidStack(PSFluids.CANNABIS_TEA, 250), properties);
        PSFluids.addServingInfluences(new FluidStack(PSFluids.BATH_SALTS, 10), properties);
        for (int tick = 0; tick < 450; tick++) properties.tick(tick);
        assertTrue(properties.getActiveValue(DrugType.CANNABIS) > 0);
        assertTrue(properties.getActiveValue(DrugType.BATH_SALTS) > 0);
        assertTrue(properties.getMultiplier(Drug::digSpeedModifier) < 1);
        properties.addToDrug(DrugType.COCAINE, 0.8);
        for (int tick = 450; tick < 530; tick++) properties.tick(tick);
        assertEquals("psychedelicraft.sleep.fail.coccaine", properties.sleepWarning());
        DrugProperties restored = new DrugProperties();
        restored.fromNbt(properties.toNbt());
        assertEquals(properties.getActiveValue(DrugType.BATH_SALTS), restored.getActiveValue(DrugType.BATH_SALTS), 0);
        assertEquals(properties.getMultiplier(Drug::speedModifier), restored.getMultiplier(Drug::speedModifier), 0);
        assertEquals(properties.sleepWarning(), restored.sleepWarning());
    }

    private static NBTTagList pending(FluidStack stack) {
        DrugProperties properties = new DrugProperties();
        PSFluids.addServingInfluences(stack, properties);
        return properties.toNbt().getTagList("drugInfluences", 10);
    }
}
