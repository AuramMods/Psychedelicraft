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
    @GameTest(template="empty") public static void nativePlayerRevivalRetainsDataAndNeverRevivesStaleHandles(GameTestHelper helper){
        var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"RevivedPlayer"));
        var properties=DrugCapability.get(player);properties.addToDrug(DrugType.LSD,.75);properties.setDrugLocked(DrugType.LSD,true);var saved=properties.toNbt();
        for(int i=0;i<2;i++){
            var previous=player.getCapability(DrugCapability.TYPE);player.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            helper.assertTrue(!previous.isPresent()&&!player.getCapability(DrugCapability.TYPE).isPresent(),"native removal invalidates cached and owner-gated handles");
            player.revive();var current=player.getCapability(DrugCapability.TYPE);
            helper.assertTrue(current.isPresent()&&current!=previous&&!previous.isPresent(),"revival supplies a fresh handle and leaves stale references invalid");
            helper.assertTrue(DrugCapability.get(player)==properties&&DrugCapability.get(player).toNbt().equals(saved),"same-player revival preserves exact drug data and identity");
        }
        player.remove(Entity.RemovalReason.DISCARDED);helper.succeed();
    }
    @GameTest(template="empty") public static void nonDeathCloneCanCopyRemovedPlayersAndReinvalidatesOriginal(GameTestHelper helper){
        var original=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"OriginalPlayer"));
        var replacement=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ClonedPlayer"));
        DrugCapability.get(original).addToDrug(DrugType.CAFFEINE,.5);var saved=DrugCapability.get(original).toNbt();var previous=original.getCapability(DrugCapability.TYPE);original.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        DrugEvents.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(replacement,original,false));
        helper.assertTrue(DrugCapability.get(replacement).toNbt().equals(saved),"native non-death clone copies exact retained drug data");
        helper.assertTrue(!previous.isPresent()&&!original.getCapability(DrugCapability.TYPE).isPresent(),"copy finishes with original and stale handles invalidated");
        replacement.remove(Entity.RemovalReason.DISCARDED);helper.succeed();
    }
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
