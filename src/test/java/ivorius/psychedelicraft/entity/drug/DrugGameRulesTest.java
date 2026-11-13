package ivorius.psychedelicraft.entity.drug;

import ivorius.psychedelicraft.entity.drug.type.SleepDeprivationDrug;
import net.minecraft.world.GameRules;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrugGameRulesTest {
    private static final String RULE = SleepDeprivationDrug.GAME_RULE;

    @Test
    public void newWorldsHaveAnOptInBooleanRule() {
        GameRules rules = new GameRules();
        DrugEvents.registerGameRules(rules);
        assertTrue(rules.areSameType(RULE, GameRules.ValueType.BOOLEAN_VALUE));
        assertFalse(rules.getBoolean(RULE));
    }

    @Test
    public void registrationRestoresTheBooleanTypeAndSavedValue() {
        for (boolean enabled : new boolean[]{false, true}) {
            GameRules rules = new GameRules();
            DrugEvents.registerGameRules(rules);
            rules.setOrCreateGameRule(RULE, Boolean.toString(enabled));

            GameRules reopened = new GameRules();
            reopened.readFromNBT(rules.writeToNBT());
            assertTrue(reopened.areSameType(RULE, GameRules.ValueType.ANY_VALUE));
            assertFalse(reopened.areSameType(RULE, GameRules.ValueType.BOOLEAN_VALUE));

            DrugEvents.registerGameRules(reopened);
            assertTrue(reopened.areSameType(RULE, GameRules.ValueType.BOOLEAN_VALUE));
            assertEquals(enabled, reopened.getBoolean(RULE));
        }
    }
}
