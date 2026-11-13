package ivorius.psychedelicraft.world;

import net.minecraft.world.GameRules;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExplosionDropRulesTest {
    @Test
    public void defaultsAndSavedOverridesRemainBoolean() {
        GameRules fresh = new GameRules();
        ExplosionDropRules.register(fresh);
        assertTrue(fresh.getBoolean(ExplosionDropRules.BLOCK));
        assertTrue(fresh.getBoolean(ExplosionDropRules.MOB));
        assertFalse(fresh.getBoolean(ExplosionDropRules.TNT));

        fresh.setOrCreateGameRule(ExplosionDropRules.BLOCK, "false");
        fresh.setOrCreateGameRule(ExplosionDropRules.MOB, "false");
        fresh.setOrCreateGameRule(ExplosionDropRules.TNT, "true");
        GameRules loaded = new GameRules();
        loaded.readFromNBT(fresh.writeToNBT());
        ExplosionDropRules.register(loaded);
        assertFalse(loaded.getBoolean(ExplosionDropRules.BLOCK));
        assertFalse(loaded.getBoolean(ExplosionDropRules.MOB));
        assertTrue(loaded.getBoolean(ExplosionDropRules.TNT));
        for (String name : new String[]{ExplosionDropRules.BLOCK, ExplosionDropRules.MOB, ExplosionDropRules.TNT}) {
            assertTrue(loaded.areSameType(name, GameRules.ValueType.BOOLEAN_VALUE));
        }
    }
}
