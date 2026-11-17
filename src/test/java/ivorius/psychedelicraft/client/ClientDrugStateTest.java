package ivorius.psychedelicraft.client;
import ivorius.psychedelicraft.entity.drug.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClientDrugStateTest {
 @Test public void missingPlayerHasNeutralClientEffects(){assertFalse(ClientDrugState.alive(null));var view=ClientDrugState.get(null);assertEquals(1,view.getMultiplier(Drug::soundVolumeModifier),0);assertEquals(0,view.getModifier(Drug::breathVolume),0);assertEquals(0,view.getModifier(Drug::weightlessness),0);for(var type:DrugType.values())assertEquals(0,view.getActiveValue(type),0);}
}
