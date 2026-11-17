package ivorius.psychedelicraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.stream.Collectors;

/** Actual accepted entities remain conserved while native chunk visibility catches up. */
@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class NativeItemVisibilityGameTests {
    private static ChunkPos remoteChunk;
    private static final List<ItemEntity> queued=new ArrayList<>();
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    @GameTest(template="empty",batch="native_item_visibility",timeoutTicks=1200)
    public static void acceptedBagDropsBecomeVisibleWithTheSameIdentityAfterNativeChunkLoading(GameTestHelper h){
        var level=h.getLevel();var origin=h.absolutePos(BlockPos.ZERO);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"VisibilityTest"));player.setGameMode(GameType.SURVIVAL);
        player.setPos(origin.getX()+1000000.5,300,origin.getZ()+1000000.5);
        var prototype=new ItemStack(Items.COOKIE);prototype.setHoverName(Component.literal("Queued units "+player.getUUID()));
        var bag=new ItemStack(PSItems.PAPER_BAG.get());PaperBagItem.setContents(bag,new PaperBagItem.Contents(prototype,100));player.setItemInHand(InteractionHand.MAIN_HAND,bag);
        var box=player.getBoundingBox().inflate(5);var trace=new NativeItemTrace(level,e->ItemStack.isSameItemSameTags(e.getItem(),prototype));
        try(trace){
            var normal=PSItems.PAPER_BAG.get().use(level,player,InteractionHand.MAIN_HAND);check(normal.getResult().consumesAction()&&PaperBagItem.getContents(bag).count()==36,"normal release commits exactly sixty-four");
            player.setShiftKeyDown(true);var single=PSItems.PAPER_BAG.get().use(level,player,InteractionHand.MAIN_HAND);check(single.getResult().consumesAction()&&PaperBagItem.getContents(bag).count()==35,"crouched release commits exactly one");
            queued.addAll(trace.spawned());check(queued.stream().mapToInt(e->e.getItem().getCount()).sum()==65&&queued.stream().allMatch(e->e.isAddedToWorld()&&!e.isRemoved()),"actual native acceptance retains all sixty-five live units");
            trace.log("queued-before-native-load",box);
        }
        check(level.getEntitiesOfClass(ItemEntity.class,box,e->ItemStack.isSameItemSameTags(e.getItem(),prototype)).isEmpty(),"the initially unloaded chunk has no query-visible entities yet");
        check(queued.stream().allMatch(e->level.getEntity(e.getUUID())==null),"native lookup also excludes the hidden entities");
        var identities=queued.stream().map(ItemEntity::getUUID).collect(Collectors.toSet());
        remoteChunk=new ChunkPos(player.blockPosition());long before=level.getGameTime();
        // Run the real operation after the actual release, without simulating visibility.
        level.setChunkForced(remoteChunk.x,remoteChunk.z,true);
        h.startSequence().thenWaitUntil(()->{
            var visible=level.getEntitiesOfClass(ItemEntity.class,box,e->ItemStack.isSameItemSameTags(e.getItem(),prototype));
            check(visible.stream().mapToInt(e->e.getItem().getCount()).sum()==65,"normal native loading exposes exactly sixty-five released units");
            check(visible.stream().map(ItemEntity::getUUID).collect(Collectors.toSet()).equals(identities),"loading preserves every emitted identity without replacement or duplication");
            check(queued.stream().allMatch(e->level.getEntity(e.getUUID())==e&&!e.isRemoved()),"direct native lookup resolves the same live entity objects");
            check(PaperBagItem.getContents(bag).count()==35,"all retained contents stay intact during loading");
        }).thenExecute(()->{
            trace.log("queued-after-native-load",box);
            LogUtils.getLogger().info("NATIVE_ITEM_VISIBILITY elapsedTicks={} released=65 retained=35 sameIdentities=true",level.getGameTime()-before);
        }).thenSucceed();
    }
    @AfterBatch(batch="native_item_visibility") public static void cleanup(ServerLevel level){
        queued.forEach(ItemEntity::discard);queued.clear();
        if(remoteChunk!=null){level.setChunkForced(remoteChunk.x,remoteChunk.z,false);remoteChunk=null;}
    }
}
