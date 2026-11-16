/* Copyright (c) 2014, Lukas Tenbrink. Native selection design by Sollace. */
package ivorius.psychedelicraft.client;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.entity.PlacedDrinkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT)
public final class ClientDrinkHighlights {
    private ClientDrinkHighlights(){}
    @SubscribeEvent public static void highlight(RenderHighlightEvent.Block event){
        var client=Minecraft.getInstance();var hit=event.getTarget();var pos=hit.getBlockPos();if(client.level==null||!(client.level.getBlockEntity(pos) instanceof PlacedDrinkBlockEntity display))return;
        event.setCanceled(true);if(!hit.getDirection().getAxis().isVertical())return;
        int cell=PlacedDrinkBlockEntity.cell((float)(hit.getLocation().x-pos.getX()),(float)(hit.getLocation().z-pos.getZ()));double x=pos.getX()+PlacedDrinkBlockEntity.x(cell),z=pos.getZ()+PlacedDrinkBlockEntity.z(cell),y=pos.getY();
        AABB box=new AABB(x-.125,y,z-.125,x+.125,y+(display.findColumn(cell)<0?.01/16:.25),z+.125).move(event.getCamera().getPosition().scale(-1));
        LevelRenderer.renderLineBox(event.getPoseStack(),event.getMultiBufferSource().getBuffer(RenderType.lines()),box,0,0,0,.4F);
    }
}
