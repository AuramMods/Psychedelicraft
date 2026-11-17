package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class PlayerLifecycleGameTests {
    @GameTest(template="empty") public static void finalDeadPlayerTickDoesNotReadInvalidatedCapabilities(GameTestHelper helper){
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RemovedPlayer"));
        helper.assertTrue(player.getCapability(DrugCapability.TYPE).isPresent(),"normal player has its attached drug capability");
        player.setHealth(0);player.remove(Entity.RemovalReason.KILLED);
        helper.assertTrue(!player.getCapability(DrugCapability.TYPE).isPresent(),"native removal invalidates the player capability");
        DrugEvents.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START,player));
        DrugEvents.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,player));
        helper.assertTrue(!player.getCapability(DrugCapability.TYPE).isPresent(),"final tick leaves native capability invalidation intact");helper.succeed();
    }
}
