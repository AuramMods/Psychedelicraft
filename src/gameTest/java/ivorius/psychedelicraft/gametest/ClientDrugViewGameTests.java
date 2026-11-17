package ivorius.psychedelicraft.gametest;
import com.mojang.authlib.GameProfile;
import ivorius.psychedelicraft.client.ClientDrugState;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.UUID;
@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class ClientDrugViewGameTests {
 @GameTest(template="empty") public static void invalidatedDeathViewIsNeutralWithoutRevivingTheProvider(GameTestHelper helper){
  var player=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"ClientView"));player.setHealth(20);var original=DrugCapability.get(player);var root=original.toNbt();var caffeine=new CompoundTag();caffeine.putDouble("effect",.8);caffeine.putDouble("effectActive",.8);root.getCompound("Drugs").put(DrugType.CAFFEINE.id(),caffeine);original.fromNbt(root);
  helper.assertTrue(ClientDrugState.get(player)==original&&ClientDrugState.get(player).getModifier(Drug::breathVolume)>0,"healthy view uses the actual capability");
  player.setHealth(0);player.invalidateCaps();helper.assertTrue(!player.getCapability(DrugCapability.TYPE).isPresent(),"death capability is invalid");var view=ClientDrugState.get(player);helper.assertTrue(view!=original&&view.getActiveValue(DrugType.CAFFEINE)==0&&view.getMultiplier(Drug::soundVolumeModifier)==1,"dead rendering and sound use neutral state");helper.assertTrue(!player.getCapability(DrugCapability.TYPE).isPresent(),"client view does not revive a dead provider");
  player.reviveCaps();player.setHealth(20);helper.assertTrue(ClientDrugState.get(player)==original&&ClientDrugState.get(player).getActiveValue(DrugType.CAFFEINE)==.8,"legitimate revive exposes retained data");helper.succeed();
 }
}
