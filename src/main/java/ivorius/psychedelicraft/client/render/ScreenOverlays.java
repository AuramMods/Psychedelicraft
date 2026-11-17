/* Copyright (c) 2014, Lukas Tenbrink. Target rendering updated by Sollace. */
package ivorius.psychedelicraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.OverlayMesh.*;
import ivorius.psychedelicraft.client.render.shader.PostRenderState;
import ivorius.psychedelicraft.config.PSClientConfig;
import ivorius.psychedelicraft.entity.drug.*;
import ivorius.psychedelicraft.mixin.client.ShaderBlendAccessor;
import ivorius.psychedelicraft.mixin.client.BlendModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL14;
import java.util.List;

/** Overlays cover the finished world and hands before post processing and the native GUI. */
public final class ScreenOverlays {
    private static final ResourceLocation WARMTH=Psychedelicraft.id("textures/drug/coffee/overlay.png");
    private static final ResourceLocation HURT=Psychedelicraft.id("textures/environment/hurt_overlay.png");
    private static final ResourceLocation EYELIDS=Psychedelicraft.id("textures/environment/eyelid_overlay.png");
    private static final ResourceLocation PARTICLE=Psychedelicraft.id("textures/drug/power/particle.png");
    private static final ResourceLocation GLARE=Psychedelicraft.id("textures/environment/lense_flare/sun_blindness.png");
    private static final float[] FLARE_SIZES={.15F,.24F,.12F,.036F,.06F,.048F,.006F,.012F,.5F,.09F,.036F,.09F,.06F,.05F,.6F};
    private static final float[] FLARE_OFFSETS={-1.3F,-2F,.2F,.4F,.25F,-.25F,-.7F,-1F,1F,1.4F,-1.31F,-1.2F,-1.5F,-1.55F,-3F};
    private static final ResourceLocation[] FLARES=new ResourceLocation[15],LIGHTNING=new ResourceLocation[4];
    static{
        for(int i=0;i<FLARES.length;i++)FLARES[i]=Psychedelicraft.id("textures/environment/lense_flare/flare"+i+".png");
        for(int i=0;i<LIGHTNING.length;i++)LIGHTNING[i]=Psychedelicraft.id("textures/drug/power/lightning_"+i+".png");
    }
    private final Eyelids eyelids=new Eyelids();
    private final SunFlare sun=new SunFlare();
    private final Heartbeat heartbeat=new Heartbeat();

    public void tick(Minecraft client,DrugProperties drugs){
        var player=client.player;
        if(heartbeat.tick(drugs.getModifier(Drug::heartbeatVolume),drugs.getModifier(Drug::heartbeatSpeed)))
            sound(client,drugs.getModifier(Drug::heartbeatVolume),1+drugs.getModifier(Drug::heartbeatSpeed));
        var events=eyelids.tick(drugs.getModifier(Drug::drowsiness),player.tickCount,player.getRandom());
        if(events.heartbeat())sound(client,events.tiredness(),.3F);
        if(events.warning())player.displayClientMessage(Component.translatable("psychedelicraft.sleep.tired"),true);
        var camera=client.getCameraEntity();float target=0;
        if(PSClientConfig.SUN_FLARE.get()>0&&camera!=null&&client.level.dimensionType().hasSkyLight()&&!client.level.dimensionType().hasCeiling()){
            var view=client.gameRenderer.getMainCamera();
            var eye=view.getPosition();var look=view.getLookVector();
            boolean clear=client.level.clip(new ClipContext(eye,eye.add(look.x()*100,look.y()*100,look.z()*100),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,camera)).getType()==HitResult.Type.MISS;
            target=SunFlare.visibility(direction(client,1),client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),client.level.getRainLevel(1),clear);
        }
        sun.tick(target);
    }

    private static void sound(Minecraft client,float volume,float pitch){
        var p=client.player;client.level.playLocalSound(p.getX(),p.getY(),p.getZ(),PSSounds.HEARTBEAT.get(),SoundSource.AMBIENT,volume,pitch,false);
    }

    public void render(Minecraft client,DrugProperties drugs,EnvironmentEffects environment,float partial){
        var player=client.player;float tick=player.tickCount+partial;
        float alcohol=(float)drugs.getActiveValue(DrugType.ALCOHOL),warmth=(float)drugs.getActiveValue(DrugType.WARMTH)*.5F,power=(float)drugs.getActiveValue(DrugType.POWER);
        float blink=eyelids.opacity(partial),hurt=environment.hurtOverlay(player.hurtTime,player.hurtDuration,heartbeat.pulse(partial));
        boolean flare=PSClientConfig.SUN_FLARE.get()>0&&sun.alpha()>0;
        if(!flare&&alcohol<=0&&warmth<=0&&power<=0&&hurt<=0&&blink<=0)return;
        int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight();
        try(var state=new PostRenderState()){
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,width,height,0,-1000,1000),VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().setIdentity();RenderSystem.applyModelViewMatrix();
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            if(flare)flare(client,width,height,partial);
            if(warmth>0)batch(WARMTH,OverlayMesh.warmth(width,height,player.tickCount,warmth),false);
            if(alcohol>0){
                var sprite=client.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
                batch(InventoryMenu.BLOCK_ATLAS,List.of(OverlayMesh.rectangle(0,0,width,height,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),new Color(1,1,1,OverlayMesh.alcoholAlpha(tick,alcohol)))),false);
            }
            var frame=OverlayMesh.power(width,height,player.tickCount,partial,power);
            batch(PARTICLE,frame.particles(),false);
            for(var bolt:frame.lightning())batch(LIGHTNING[bolt.texture()],List.of(bolt.quad()),true);
            if(hurt>0)vignette(width,height,HURT,hurt,(int)((1-hurt)*40));
            if(blink>0)vignette(width,height,EYELIDS,blink*.8F,(int)(blink*5.8F));
        }
    }

    private void flare(Minecraft client,int width,int height,float partial){
        Vec3 direction=direction(client,partial);if(direction.z<=0)return;
        float intensity=PSClientConfig.SUN_FLARE.get().floatValue(),size=Math.max(width,height);
        float x=(float)direction.x*width,y=(float)-direction.y*height;
        float[] fog=RenderSystem.getShaderFogColor();
        float red=Mth.clamp(fog[0]-.1F,0,1),green=Mth.clamp(fog[1]-.1F,0,1),blue=Mth.clamp(fog[2]-.1F,0,1);
        for(int i=0;i<FLARES.length;i++){
            float half=FLARE_SIZES[i]*size*.5F,cx=width*.5F+x*FLARE_OFFSETS[i],cy=height*.5F+y*FLARE_OFFSETS[i];
            batch(FLARES[i],List.of(OverlayMesh.rectangle(cx-half,cy-half,cx+half,cy+half,0,0,1,1,new Color(red,green,blue,(i==8?1:.5F)*sun.alpha()*intensity))),true);
        }
        float glare=(float)(direction.z*direction.z-.1F)*intensity*250;
        if(glare>0){float half=glare*size*.5F,cx=width*.5F+x,cy=height*.5F+y;batch(GLARE,List.of(OverlayMesh.rectangle(cx-half,cy-half,cx+half,cy+half,0,0,1,1,new Color(red,green,blue,Math.min(1,glare/150)*sun.alpha()))),true);}
    }

    private static Vec3 direction(Minecraft client,float partial){
        var camera=client.getCameraEntity();
        boolean front=client.options.getCameraType().isMirrored()&&!(camera instanceof LivingEntity living&&living.isSleeping());
        return SunFlare.direction(client.level.getSunAngle(partial),Mth.lerp(partial,camera.yRotO,camera.getYRot()),Mth.lerp(partial,camera.xRotO,camera.getXRot()),front);
    }

    private static void vignette(int width,int height,ResourceLocation texture,float alpha,int margin){
        batch(texture,List.of(OverlayMesh.rectangle(-margin,-margin,width+margin,height+margin,0,0,1,1,new Color(1,1,1,alpha))),false);
    }

    private static void batch(ResourceLocation texture,List<Quad> quads,boolean additive){
        if(quads.isEmpty())return;
        RenderSystem.setShader(OverlayShader::get);RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShaderTexture(0,texture);
        // Prime the native cache before overriding its factors, so ShaderInstance.apply retains this batch's blend.
        var blend=((ShaderBlendAccessor)RenderSystem.getShader()).psychedelicraft$getBlend();
        blend.apply();
        // Vanilla only updates lastApplied when opacity changes, not between two translucent modes.
        BlendModeAccessor.psychedelicraft$setLastApplied(blend);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);
        // Preserve the opaque scene alpha used by temporal history, including transparent texels.
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA,additive?GL11.GL_ONE:GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer=Tesselator.getInstance().getBuilder();buffer.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        for(var quad:quads){vertex(buffer,quad.bottomLeft());vertex(buffer,quad.bottomRight());vertex(buffer,quad.topRight());vertex(buffer,quad.topLeft());}
        BufferUploader.drawWithShader(buffer.end());
    }
    private static void vertex(BufferBuilder buffer,Vertex v){var color=v.color();buffer.vertex(v.x(),v.y(),0).uv(v.u(),v.v()).color(color.r(),color.g(),color.b(),color.a()).endVertex();}
}
