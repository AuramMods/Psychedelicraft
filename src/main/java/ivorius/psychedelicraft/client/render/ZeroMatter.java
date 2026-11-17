/* Copyright (c) 2014, Lukas Tenbrink. Native shader/layer design by Sollace. */
package ivorius.psychedelicraft.client.render;

import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

@Mod.EventBusSubscriber(modid=Psychedelicraft.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ZeroMatter extends RenderType {
    private static ShaderInstance shader;
    public static final ResourceLocation[] TEXTURES=new ResourceLocation[8];
    private static final RenderType[] LAYERS=new RenderType[8];
    static {for(int i=0;i<8;i++){
        TEXTURES[i]=Psychedelicraft.id("textures/entity/reality_rift/zero_screen_"+i+".png");
        LAYERS[i]=create("psychedelicraft_zero_matter_"+i,DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
            .setShaderState(new ShaderStateShard(()->shader)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(NO_LIGHTMAP).setCullState(NO_CULL)
            .setTextureState(MultiTextureStateShard.builder().add(TEXTURES[i],false,false).add(new ResourceLocation("textures/entity/end_portal.png"),false,false).build()).createCompositeState(false));
    }}
    private ZeroMatter(){super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{});}
    public static RenderType layer(float ticks){return LAYERS[Math.floorMod(Mth.floor(ticks*.5F),8)];}
    public static ShaderInstance shader(){return shader;}
    @SubscribeEvent public static void register(RegisterShadersEvent event)throws IOException{event.registerShader(new ShaderInstance(event.getResourceProvider(),Psychedelicraft.id("rendertype_zero_matter"),DefaultVertexFormat.POSITION_COLOR),program->shader=program);}
}
