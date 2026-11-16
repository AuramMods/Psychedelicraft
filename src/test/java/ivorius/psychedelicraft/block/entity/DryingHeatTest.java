package ivorius.psychedelicraft.block.entity;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DryingHeatTest {
    @Test public void acceptedHeatCurveUsesBothLightAndBiomeTemperature(){
        assertEquals(0,DryingHeat.at(0,2),0);
        assertEquals(.7225F,DryingHeat.at(15,.8F),1e-6F);
        assertEquals(.0625F,DryingHeat.at(15,0),1e-6F);
        assertEquals(.1296F,DryingHeat.at(9,1),1e-6F);
        assertEquals(1,DryingHeat.at(15,2),0);
    }
}
