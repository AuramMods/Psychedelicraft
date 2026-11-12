package ivorius.psychedelicraft.client;

import ivorius.psychedelicraft.entity.drug.DrugProperties;
import ivorius.psychedelicraft.entity.drug.DrugType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ClientDrugMusicTest {
    @Test
    public void selectsStrongestEnabledThemeWithStableTies() {
        DrugProperties drugs = new DrugProperties();
        set(drugs, DrugType.CANNABIS, 0.4);
        set(drugs, DrugType.CAFFEINE, 0.4);
        set(drugs, DrugType.ALCOHOL, 0.008);
        for (int tick = 1; tick <= 100; tick++) drugs.tick(tick);

        assertSame(DrugType.CAFFEINE, ClientDrugMusic.strongestTheme(drugs, type -> true));
        assertSame(DrugType.CANNABIS, ClientDrugMusic.strongestTheme(drugs, type -> type != DrugType.CAFFEINE));
        assertNull(ClientDrugMusic.strongestTheme(drugs, type -> type == DrugType.ALCOHOL));
    }

    @Test
    public void themeGainRisesToFullVolumeAtPointFour() {
        assertEquals(0F, ClientDrugMusic.themeVolume(0.01), 0F);
        assertEquals(0.25F, ClientDrugMusic.themeVolume(0.1), 0.00001F);
        assertEquals(1F, ClientDrugMusic.themeVolume(0.4), 0.00001F);
        assertEquals(1F, ClientDrugMusic.themeVolume(1), 0.00001F);
    }

    private static void set(DrugProperties drugs, DrugType type, double value) {
        drugs.setDrugLocked(type, true);
        drugs.setDrugValue(type, value);
    }
}
