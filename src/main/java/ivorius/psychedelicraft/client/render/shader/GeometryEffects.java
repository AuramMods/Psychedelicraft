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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeometryEffects {
    public static final String IRIS_BLOCK_ATLAS = "PS_IrisBlockAtlas";
    public enum IrisTerrainPass { NEAR, DISTANT, SHADOW }
    private static final Matrix4f viewToWorld=new Matrix4f();
    private static final Matrix4f worldToClip = new Matrix4f();
    private static final Matrix4f clipToWorld = new Matrix4f();
    private static float partial;
    private static boolean sphericalFog;
    private GeometryEffects(){}
    public static void beginFrame(float tickDelta){partial=tickDelta;sphericalFog=false;}
    public static void captureView(Matrix4f view, Matrix4f projection){
        viewToWorld.set(view).invert();
        worldToClip.set(projection).mul(view);
        clipToWorld.set(worldToClip).invert();
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
        String source = geometryResource(vertex ? "basic.gvsh" : "basic.gfsh");
        return vertex ? source.replace("__PS_DEFORMATION__", geometryResource("deform.glsl")) : source;
    }
    private static String geometryResource(String name) throws IOException {
        var id=Psychedelicraft.id("shaders/geometry/"+name);
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
    public static String injectTerrain(String source, boolean vertex) throws IOException {
        return GeometrySource.combineTerrain(source, extension(vertex), vertex);
    }

    private static String projectedDeformation() throws IOException {
        return geometryResource("deform.glsl") + geometryResource("iris_position.glsl");
    }

    public static String injectDistantTerrain(String source) throws IOException {
        return GeometrySource.deformDistantTerrain(source, projectedDeformation() + geometryResource("lod_position.gvsh"));
    }

    private record IrisStages<K>(K vertex, K fragment, boolean intermediate) {}

    private static <K> IrisStages<K> identifyIrisStages(Map<K, String> stages) {
        K vertex = null;
        K fragment = null;
        boolean intermediate = false;
        for (var stage : stages.entrySet()) {
            if (!(stage.getKey() instanceof Enum<?> type) || stage.getValue() == null) continue;
            switch (type.name()) {
                case "VERTEX" -> vertex = stage.getKey();
                case "FRAGMENT" -> fragment = stage.getKey();
                default -> intermediate = true;
            }
        }
        return new IrisStages<>(vertex, fragment, intermediate);
    }

    public static <K> Map<K, String> injectIrisTerrain(Map<K, String> stages, IrisTerrainPass pass) throws IOException {
        if (stages == null) return null;
        var layout = identifyIrisStages(stages);
        if (layout.vertex == null) return stages;
        String source = stages.get(layout.vertex);
        String replacement = GeometrySource.deformIrisTerrain(source, projectedDeformation());
        if (source.equals(replacement)) return stages;
        var patched = new LinkedHashMap<>(stages);
        boolean distant = pass == IrisTerrainPass.DISTANT;
        if (pass != IrisTerrainPass.SHADOW && !layout.intermediate && layout.fragment != null
                && GeometrySource.supportsIrisMaterial(source, stages.get(layout.fragment), distant)) {
            replacement = GeometrySource.irisTerrainVertexMaterial(replacement, geometryResource("iris_material.gvsh"), distant);
            patched.put(layout.fragment, GeometrySource.irisColorFragment(stages.get(layout.fragment), extension(false), false));
        }
        patched.put(layout.vertex, replacement);
        return patched;
    }

    public static <K> Map<K, String> injectIrisModel(Map<K, String> stages) throws IOException {
        if (stages == null) return null;
        var layout = identifyIrisStages(stages);
        if (layout.vertex == null || layout.fragment == null || layout.intermediate) return stages;
        String vertex = stages.get(layout.vertex);
        String fragment = stages.get(layout.fragment);
        if (!GeometrySource.hasIrisColor(fragment)) return stages;
        String replacement = GeometrySource.irisModelVertex(vertex, geometryResource("deform.glsl") + geometryResource("iris_model.gvsh"));
        if (vertex.equals(replacement)) return stages;
        var patched = new LinkedHashMap<>(stages);
        patched.put(layout.vertex, replacement);
        patched.put(layout.fragment, GeometrySource.irisColorFragment(fragment, extension(false), !GeometrySource.irisModelVertexColor(vertex)));
        return patched;
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
        boolean useAtlas = applyValues(shader.getName(), (name, values) -> set(shader, name, values), true);
        shader.safeGetUniform("PS_ViewToWorld").set(viewToWorld);
        shader.setSampler("PS_SurfaceFractalSampler", useAtlas
                ? Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS)
                : com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0));
    }

    public static void applyTerrain(GeometryProgram program) {
        if (!program.hasGeometry()) return;
        applyValues("terrain", program::set, true);
        program.setMatrix("PS_ViewToWorld", viewToWorld);
        program.setMatrix("PS_WorldToClip", worldToClip);
        program.setMatrix("PS_ClipToWorld", clipToWorld);
    }

    public static void applyIrisModel(String shaderName, GeometryProgram program) {
        if (program.hasGeometry()) {
            applyValues(shaderName, program::set, program.hasUniform(IRIS_BLOCK_ATLAS));
            program.setMatrix("PS_ViewToWorld", viewToWorld);
        } else {
            float[] tint = ApparitionRender.tint();
            program.set("PS_ApparitionTint", tint[0], tint[1], tint[2], tint[3]);
        }
    }

    public static int blockAtlasId() {
        return Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
    }

    private interface UniformValues {
        void set(String name, float... values);
    }

    private static boolean applyValues(String name, UniformValues uniforms, boolean atlas) {
        float[] tint = ApparitionRender.tint();
        uniforms.set("PS_ApparitionTint", tint[0], tint[1], tint[2], tint[3]);
        var client=Minecraft.getInstance();var phase=RenderPhase.current();
        boolean active=phase.active()&&client.level!=null&&ClientDrugState.alive(client.player)&&PSClientConfig.SHADERS_3D.get();
        var h=ClientEffects.hallucinations();var drugs=active?ClientDrugState.get(client.player):null;
        boolean deform=active&&phase.deforms();
        boolean surface=active&&atlas&&GeometryMaterial.surface(phase,name);
        float[] pulse=active&&GeometryMaterial.pulses(name)?h.pulseColor():null;
        boolean any=active&&(deform&&(h.strength(Effect.SMALL_WAVES)>0||h.strength(Effect.BIG_WAVES)>0||h.strength(Effect.WIGGLE_WAVES)>0||h.strength(Effect.DISTANT_WORLD_DEFORMATION)>0||drugs.getActiveValue(DrugType.LSD)>0)||surface&&h.strength(Effect.SURFACE_FRACTALS)>0||pulse!=null);
        uniforms.set("PS_Enabled",any?1:0);
        uniforms.set("PS_WorldTicks",active?client.player.tickCount+partial:0);
        uniforms.set("PS_WavesMatrix",deform?h.strength(Effect.SMALL_WAVES)*.5F:0,deform?h.strength(Effect.BIG_WAVES)*.6F:0,deform?h.strength(Effect.WIGGLE_WAVES)*.7F:0);
        uniforms.set("PS_DistantWorldDeformation",deform?h.strength(Effect.DISTANT_WORLD_DEFORMATION):0);
        uniforms.set("PS_FractalFractureStrength",deform?(float)drugs.getActiveValue(DrugType.LSD)*.06F:0);
        uniforms.set("PS_SurfaceFractalStrength",surface?net.minecraft.util.Mth.clamp(h.strength(Effect.SURFACE_FRACTALS),0,1):0);
        uniforms.set("PS_Pulses",pulse==null?0:pulse[0],pulse==null?0:pulse[1],pulse==null?0:pulse[2],pulse==null?0:pulse[3]);
        uniforms.set("PS_PulseShape",GeometryMaterial.pulseShape(phase,name,sphericalFog));
        var camera=active?client.gameRenderer.getMainCamera().getPosition():net.minecraft.world.phys.Vec3.ZERO;
        uniforms.set("PS_CameraPosition",(float)camera.x,(float)camera.y,(float)camera.z);
        uniforms.set("PS_PlayerPosition",active?(float)client.player.getX():0,active?(float)client.player.getY():0,active?(float)client.player.getZ():0);
        if(surface){
            var sprite=client.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
            uniforms.set("PS_SurfaceFractalCoords",sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1());
        }
        return surface&&h.strength(Effect.SURFACE_FRACTALS)>0;
    }
    private static void set(ShaderInstance shader,String name,float... values){AbstractUniform u=shader.safeGetUniform(name);switch(values.length){case 1->u.set(values[0]);case 3->u.set(values[0],values[1],values[2]);case 4->u.set(values[0],values[1],values[2],values[3]);default->throw new IllegalArgumentException(name);}}
}
