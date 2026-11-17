package ivorius.psychedelicraft.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;
import java.util.function.Predicate;

/** Observe actual native spawns without forcing chunks, changing items or replacing the world query. */
final class NativeItemTrace implements AutoCloseable {
    private final ServerLevel level;
    private final Predicate<ItemEntity> filter;
    private final List<ItemEntity> spawned=new ArrayList<>();
    NativeItemTrace(ServerLevel level,Predicate<ItemEntity> filter){this.level=level;this.filter=filter;MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent public void joined(EntityJoinLevelEvent event){if(event.getLevel()==level&&event.getEntity() instanceof ItemEntity item&&filter.test(item))spawned.add(item);}
    List<ItemEntity> spawned(){return List.copyOf(spawned);}
    void log(String phase,AABB box){
        LogUtils.getLogger().info("NATIVE_ITEM_TRACE phase={} time={} box={} spawned={} queried={}",phase,level.getGameTime(),box,
                spawned.stream().map(this::describe).toList(),level.getEntitiesOfClass(ItemEntity.class,box,filter).stream().map(this::describe).toList());
    }
    private String describe(ItemEntity item){var pos=item.blockPosition();var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return item.getUUID()+"/item="+item.getItem().save(new net.minecraft.nbt.CompoundTag())+"/pos="+item.position()+"/bbox="+item.getBoundingBox()+"/removed="+item.isRemoved()+"/added="+item.isAddedToWorld()+"/lookup="+(level.getEntity(item.getUUID())==item)+"/status="+(chunk==null?"not resident":chunk.getFullStatus());}
    @Override public void close(){MinecraftForge.EVENT_BUS.unregister(this);}
}
