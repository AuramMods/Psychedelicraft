package ivorius.psychedelicraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

/** A native core shader retains the accepted overlays' faint alpha instead of cutout discarding it. */
@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class OverlayShader {
    private static ShaderInstance shader;
    private OverlayShader() {}
    public static ShaderInstance get(){return shader;}

    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{
        event.registerShader(new ShaderInstance(event.getResourceProvider(),Psychedelicraft.id("overlay"),DefaultVertexFormat.POSITION_TEX_COLOR),loaded->shader=loaded);
    }
}
