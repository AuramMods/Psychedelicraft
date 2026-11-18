/* Copyright (c) 2014, Lukas Tenbrink. Modern shader integration adapted from Sollace. */
package ivorius.psychedelicraft.client.render.shader;
import ivorius.psychedelicraft.client.ClientDrugState;

import com.mojang.blaze3d.shaders.*;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.ClientEffects;
import ivorius.psychedelicraft.client.render.*;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.config.PSClientConfig;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class GeometryEffects {
    private static final Matrix4f viewToWorld=new Matrix4f();
    private static float partial;
    private static boolean sphericalFog;
    private GeometryEffects(){}
    public static void beginFrame(float tickDelta){partial=tickDelta;sphericalFog=false;}
    public static void captureView(Matrix4f view){
        viewToWorld.set(view).invert();
        var client=Minecraft.getInstance();var camera=client.gameRenderer.getMainCamera();
        var entity=camera.getEntity();
        boolean impaired=entity instanceof net.minecraft.world.entity.LivingEntity living
                &&(living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)||living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS));
        // Match the accepted fog policy through native 1.20 camera/dimension/boss APIs.
        sphericalFog=client.level!=null&&(impaired||camera.getFluidInCamera()!=net.minecraft.world.level.material.FogType.NONE
                ||client.level.effects().isFoggyAt(camera.getBlockPosition().getX(),camera.getBlockPosition().getZ())
                ||client.gui.getBossOverlay().shouldCreateWorldFog());
    }
    private static String extension(boolean vertex)throws IOException{
        var id=Psychedelicraft.id("shaders/geometry/basic."+(vertex?"gvsh":"gfsh"));
        var manager=Minecraft.getInstance().getResourceManager();
        var resource=manager==null?java.util.Optional.<net.minecraft.server.packs.resources.Resource>empty():manager.getResource(id);
        try(var stream=resource.isPresent()?resource.get().open():GeometryEffects.class.getResourceAsStream("/assets/"+id.getNamespace()+"/"+id.getPath())){
            if(stream==null)throw new FileNotFoundException(id.toString());return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    public static InputStream inject(Program.Type type,String name,InputStream input)throws IOException{
        // The native final framebuffer blit is a screen copy, including outside active worlds.
        if(new ResourceLocation(name).getPath().equals("blit_screen"))return input;
        String source=new String(input.readAllBytes(),StandardCharsets.UTF_8);
        return new ByteArrayInputStream(GeometrySource.combine(source,extension(type==Program.Type.VERTEX),type==Program.Type.VERTEX).getBytes(StandardCharsets.UTF_8));
    }
    public static void register(ShaderInstance shader,List<Uniform> uniforms,List<String> samplers){
        add(shader,uniforms,"PS_Enabled",4,1);add(shader,uniforms,"PS_WorldTicks",4,1);add(shader,uniforms,"PS_WavesMatrix",6,3);
        add(shader,uniforms,"PS_ApparitionTint",7,4);
        add(shader,uniforms,"PS_DistantWorldDeformation",4,1);add(shader,uniforms,"PS_SurfaceFractalCoords",7,4);
        add(shader,uniforms,"PS_SurfaceFractalStrength",4,1);add(shader,uniforms,"PS_FractalFractureStrength",4,1);
        add(shader,uniforms,"PS_PlayerPosition",6,3);add(shader,uniforms,"PS_CameraPosition",6,3);
        add(shader,uniforms,"PS_ViewToWorld",10,16);add(shader,uniforms,"PS_Pulses",7,4);add(shader,uniforms,"PS_PulseShape",4,1);
        if(Uniform.glGetUniformLocation(shader.getId(),"PS_SurfaceFractalSampler")!=-1&&!samplers.contains("PS_SurfaceFractalSampler")){samplers.add("PS_SurfaceFractalSampler");shader.setSampler("PS_SurfaceFractalSampler",null);}
    }
    private static void add(ShaderInstance shader,List<Uniform> uniforms,String name,int type,int count){
        if(Uniform.glGetUniformLocation(shader.getId(),name)!=-1&&uniforms.stream().noneMatch(uniform->uniform.getName().equals(name)))uniforms.add(new Uniform(name,type,count,shader));
    }
    public static void apply(ShaderInstance shader){
        float[] tint=ApparitionRender.tint();set(shader,"PS_ApparitionTint",tint[0],tint[1],tint[2],tint[3]);
        var client=Minecraft.getInstance();var phase=RenderPhase.current();
        boolean active=phase.active()&&client.level!=null&&ClientDrugState.alive(client.player)&&PSClientConfig.SHADERS_3D.get();
        var h=ClientEffects.hallucinations();var drugs=active?ClientDrugState.get(client.player):null;
        boolean deform=active&&phase.deforms();String name=shader.getName();
        boolean surface=active&&GeometryMaterial.surface(phase,name);
        float[] pulse=active&&GeometryMaterial.pulses(name)?h.pulseColor():null;
        boolean any=active&&(deform&&(h.strength(Effect.SMALL_WAVES)>0||h.strength(Effect.BIG_WAVES)>0||h.strength(Effect.WIGGLE_WAVES)>0||h.strength(Effect.DISTANT_WORLD_DEFORMATION)>0||drugs.getActiveValue(DrugType.LSD)>0)||surface&&h.strength(Effect.SURFACE_FRACTALS)>0||pulse!=null);
        set(shader,"PS_Enabled",any?1:0);
        set(shader,"PS_WorldTicks",active?client.player.tickCount+partial:0);
        set(shader,"PS_WavesMatrix",deform?h.strength(Effect.SMALL_WAVES)*.5F:0,deform?h.strength(Effect.BIG_WAVES)*.6F:0,deform?h.strength(Effect.WIGGLE_WAVES)*.7F:0);
        set(shader,"PS_DistantWorldDeformation",deform?h.strength(Effect.DISTANT_WORLD_DEFORMATION):0);
        set(shader,"PS_FractalFractureStrength",deform?(float)drugs.getActiveValue(DrugType.LSD)*.06F:0);
        set(shader,"PS_SurfaceFractalStrength",surface?net.minecraft.util.Mth.clamp(h.strength(Effect.SURFACE_FRACTALS),0,1):0);
        set(shader,"PS_Pulses",pulse==null?0:pulse[0],pulse==null?0:pulse[1],pulse==null?0:pulse[2],pulse==null?0:pulse[3]);
        set(shader,"PS_PulseShape",GeometryMaterial.pulseShape(phase,name,sphericalFog));
        shader.safeGetUniform("PS_ViewToWorld").set(viewToWorld);
        var camera=active?client.gameRenderer.getMainCamera().getPosition():net.minecraft.world.phys.Vec3.ZERO;set(shader,"PS_CameraPosition",(float)camera.x,(float)camera.y,(float)camera.z);
        set(shader,"PS_PlayerPosition",active?(float)client.player.getX():0,active?(float)client.player.getY():0,active?(float)client.player.getZ():0);
        if(surface){
            var sprite=client.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
            set(shader,"PS_SurfaceFractalCoords",sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1());
        }
        shader.setSampler("PS_SurfaceFractalSampler",surface&&h.strength(Effect.SURFACE_FRACTALS)>0?client.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS):com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0));
    }
    private static void set(ShaderInstance shader,String name,float... values){AbstractUniform u=shader.safeGetUniform(name);switch(values.length){case 1->u.set(values[0]);case 3->u.set(values[0],values[1],values[2]);case 4->u.set(values[0],values[1],values[2],values[3]);default->throw new IllegalArgumentException(name);}}
}
