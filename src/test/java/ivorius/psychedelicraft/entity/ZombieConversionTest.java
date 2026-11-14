package ivorius.psychedelicraft.entity;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZombieConversionTest {
    @Test
    public void breathingAirResetsUnfinishedSubmersionAndTheCountdownContinuesOutOfWater() {
        NBTTagCompound state = new NBTTagCompound();
        for (int tick = 0; tick < 599; tick++) assertFalse(ZombieConversion.advance(state, true));
        assertFalse(ZombieConversion.advance(state, false));
        assertTrue(state.hasNoTags());

        for (int tick = 0; tick < 600; tick++) assertFalse(ZombieConversion.advance(state, true));
        for (int tick = 0; tick < 300; tick++) assertFalse(ZombieConversion.advance(state, false));
        assertTrue(ZombieConversion.advance(state, false));
    }

    @Test
    public void countdownStaysReadyAfterARefusedConversionAndSurvivesANbtCopy() {
        NBTTagCompound state = new NBTTagCompound();
        for (int tick = 0; tick < 750; tick++) assertFalse(ZombieConversion.advance(state, true));
        NBTTagCompound reloaded = state.copy();
        for (int tick = 0; tick < 150; tick++) assertFalse(ZombieConversion.advance(reloaded, false));
        assertTrue(ZombieConversion.advance(reloaded, false));
        assertTrue(ZombieConversion.advance(reloaded, true));
    }
}
