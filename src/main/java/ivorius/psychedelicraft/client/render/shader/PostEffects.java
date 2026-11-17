/* Copyright (c) 2014, Lukas Tenbrink. Target shaders and effects updated by Sollace. */
package ivorius.psychedelicraft.client.render.shader;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.logging.LogUtils;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.client.render.*;
import ivorius.psychedelicraft.client.render.HallucinationManager.Effect;
import ivorius.psychedelicraft.config.PSClientConfig;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/** Native post passes use owned color targets; the final blit never clears the world's depth. */
public final class PostEffects {
    private static final Logger LOGGER=LogUtils.getLogger();
    private static final ResourceLocation HEAT=Psychedelicraft.id("textures/effect/heat_distortion_noise.png"),WATER=Psychedelicraft.id("textures/effect/water_distortion.png"),BITS=Psychedelicraft.id("textures/effect/bits.png");
    private final Map<Key,PostPass> passes=new HashMap<>();
    private final MotionHistory history=new MotionHistory();
    private final List<String> lastPasses=new ArrayList<>();
    private RenderTarget a,b,depth,main;
    private ShaderResources resources;
    private int width,height;
    private boolean stencil,failed;
    private boolean worldDepthCaptured;
    private Throwable lastFailure;
    private long resourceGeneration;
    private record Key(String name,RenderTarget in,RenderTarget out){}

    public void beginFrame(){worldDepthCaptured=false;}

    /** Preserve scene depth before the native first-person pass clears the main depth buffer. */
    public void captureWorldDepth(Minecraft client,HallucinationManager h,EnvironmentEffects environment){
        worldDepthCaptured=false;
        if(failed||!PSClientConfig.SHADERS_2D.get())return;
        var drugs=DrugCapability.get(client.player);
        if(h.contrastColor(drugs.getHarmonium())==null&&PSClientConfig.BLUR_NEAR.get()<=0&&PSClientConfig.BLUR_FAR.get()<=0
                &&environment.heatDistortion()<=0&&environment.waterDistortion((float)drugs.getActiveValue(DrugType.PEYOTE))<=0&&drugs.getActiveValue(DrugType.ZERO)<=0)return;
        try(var state=new PostRenderState()){prepare(client,true);depth.copyDepthFrom(main);worldDepthCaptured=true;}
    }

    public void render(Minecraft client,float partial,Matrix4f projection,HallucinationManager h,EnvironmentEffects environment,float pauseProgress){
        lastPasses.clear();if(failed)return;
        var drugs=DrugCapability.get(client.player);float ticks=client.player.tickCount+partial;
        float slow=h.strength(Effect.SLOW_COLOR_ROTATION),quick=h.strength(Effect.QUICK_COLOR_ROTATION),intensify=drugs.getColorIntensification(h.strength(Effect.SUPER_SATURATION)),desaturate=drugs.getDesaturation(h.strength(Effect.DESATURATION)),inversion=drugs.getModifier(Drug::inversionStrength);
        float[] contrast=h.contrastColor(drugs.getHarmonium()),colorBloom=h.bloomColor(drugs.getHarmonium());
        float bloom=h.strength(Effect.BLOOM)+drugs.getModifier(Drug::bloomStrength),vision=drugs.blendStrength(0,Drug::doubleVision),power=(float)drugs.getActiveValue(DrugType.POWER),digital=(float)drugs.getActiveValue(DrugType.ZERO);
        float commonBlur=(float)PSClientConfig.PAUSE_BLUR.get().doubleValue()*pauseProgress*pauseProgress*pauseProgress+Math.max(0,(float)drugs.getActiveValue(DrugType.SLEEP_DEPRIVATION)-.7F)*partial*15;
        float horizontal=commonBlur+(float)drugs.getActiveValue(DrugType.BATH_SALTS)*(6+ticks%5),vertical=commonBlur+power;
        float heat=environment.heatDistortion(),water=environment.waterDistortion((float)drugs.getActiveValue(DrugType.PEYOTE)),wet=environment.wetOverlay();
        float nearBlur=PSClientConfig.BLUR_NEAR.get().floatValue(),farBlur=PSClientConfig.BLUR_FAR.get().floatValue();
        boolean colors=slow!=0||quick!=0||intensify!=0||desaturate!=0||inversion!=0;
        boolean focus=nearBlur>0||farBlur>0;
        boolean post=PSClientConfig.SHADERS_2D.get()&&(colors||contrast!=null||colorBloom!=null||bloom>0||vision>0||horizontal>0||vertical>0||focus||heat>0||water>0||wet>0||digital>0);
        float motion=PSClientConfig.MOTION_BLUR.get()?drugs.blendStrength(0,Drug::motionBlur):0;
        if(!post&&motion<=0&&history.empty())return;
        try(var state=new PostRenderState()){
            if(motion>0){history.render(client,ticks,motion);lastPasses.add("motion_history");}else history.release();
            if(!post)return;
            boolean needsDepth=contrast!=null||focus||heat>0||water>0||digital>0;
            prepare(client,needsDepth);
            if(needsDepth&&!worldDepthCaptured)depth.copyDepthFrom(main);
            RenderTarget current=main;
            if(heat>0)current=distort(current,heat,ticks*.15F,partial);
            if(water>0)current=distort(current,water,ticks*.03F,partial);
            if(colors)current=draw("simple_effects",current,partial,e->{set(e,"ticks",ticks);set(e,"slowColorRotation",slow);set(e,"quickColorRotation",quick);set(e,"colorIntensification",intensify);set(e,"desaturation",desaturate);set(e,"inversion",inversion);});
            if(contrast!=null)current=draw("simple_effects_depth",current,partial,e->{set(e,"ticks",ticks);set(e,"colorSafeMode",0);set(e,"worldColorization",contrast[0],contrast[1],contrast[2],contrast[3]);});
            if(horizontal>0||vertical>0)current=draw("ps_blur",current,partial,e->{set(e,"pixelSize",1F/width,1F/height);set(e,"hBlur",horizontal);set(e,"vBlur",vertical);set(e,"repeats",Mth.ceil(Math.max(horizontal,vertical)));});
            if(focus){
                float near=PSClientConfig.FOCUS_NEAR.get().floatValue(),far=PSClientConfig.FOCUS_FAR.get().floatValue();float low=Math.max(0,Math.min(near,far)),high=Math.max(0,Math.max(near,far));
                for(int n=0;n<Mth.ceil(Math.max(nearBlur,farBlur));n++)for(int axis=0;axis<2;axis++){
                    float activeNear=Mth.clamp(nearBlur-n,0,1),activeFar=Mth.clamp(farBlur-n,0,1);int direction=axis;
                    current=draw("depth_of_field",current,partial,e->{set(e,"pixelSize",1F/width,1F/height);set(e,"vertical",direction);set(e,"focalPointNear",low);set(e,"focalPointFar",high);set(e,"focalBlurNear",activeNear);set(e,"focalBlurFar",activeFar);set(e,"ProjectionDepth",projection.m22(),projection.m32());});
                }
            }
            if(bloom>0)current=bloom(current,bloom,null,partial);
            if(colorBloom!=null)current=bloom(current,colorBloom[3],colorBloom,partial);
            // The accepted implementation does not couple double vision to environmental water settings.
            if(vision>0)current=draw("double_vision",current,partial,e->{set(e,"totalAlpha",vision);set(e,"distance",Mth.sin(ticks/20)*.05F*vision);set(e,"stretch",1+vision);});
            if(power>0)current=draw("ps_blur_noise",current,partial,e->{set(e,"pixelSize",1F/width,1F/height);set(e,"strength",power*.6F);set(e,"seed",new Random((long)(ticks*1000)).nextFloat()*9+1);});
            if(wet>0)current=draw("distortion_map",current,partial,e->{set(e,"totalAlpha",wet);set(e,"strength",wet*.2F);set(e,"texTranslation0",0,ticks*.005F);set(e,"texTranslation1",.5F,ticks*.007F);});
            if(digital>0){
                float amount=ease(Math.min(digital*3,1))*.95F+digital*.05F;
                float resolutionX=width*(1+(PSClientConfig.DIGITAL_X.get().floatValue()-1)*amount),resolutionY=height*(1+(PSClientConfig.DIGITAL_Y.get().floatValue()-1)*amount);
                current=draw("digital_depth",current,partial,e->{set(e,"newResolution",resolutionX,resolutionY);set(e,"totalAlpha",1);set(e,"textProgress",ease((digital-.2F)*5)+ease((digital-.8F)*10));set(e,"maxColors",digital>.4F?Math.max(256/((digital-.4F)*640+1),2):-1);set(e,"saturation",1-ease((digital-.6F)*5));set(e,"depthRange",.05F,client.options.getEffectiveRenderDistance()*16F);});
            }
            main.bindWrite(true);current.blitToScreen(main.viewWidth,main.viewHeight,true);
        }catch(IOException error){failed=true;lastFailure=error;try(var state=new PostRenderState()){releaseResources();history.release();}LOGGER.error("Could not load Psychedelicraft post effect",error);}
    }
    private static float ease(float value){float x=Mth.clamp(value,0,1);return (1-Mth.cos(x*Mth.PI))*.5F;}
    private void prepare(Minecraft client,boolean needsDepth){
        var target=client.getMainRenderTarget();
        if(a==null||main!=target||target.width!=width||target.height!=height||target.isStencilEnabled()!=stencil){releaseResources();main=target;width=main.width;height=main.height;stencil=main.isStencilEnabled();a=new TextureTarget(width,height,false,Minecraft.ON_OSX);b=new TextureTarget(width,height,false,Minecraft.ON_OSX);resources=new ShaderResources(client.getResourceManager());resourceGeneration++;}
        if(needsDepth&&depth==null){depth=new TextureTarget(width,height,true,Minecraft.ON_OSX);if(stencil)depth.enableStencil();}
    }
    private RenderTarget distort(RenderTarget current,float strength,float ticks,float partial)throws IOException{
        return draw("heat_distortion",current,partial,e->{set(e,"strength",strength);set(e,"ticks",ticks);set(e,"totalAlpha",1);});
    }
    private RenderTarget bloom(RenderTarget current,float strength,float[] color,float partial)throws IOException{
        for(int n=0;n<Mth.ceil(strength);n++)for(int axis=0;axis<2;axis++){
            float alpha=color==null?Math.min(1,strength-n):Math.max(1,strength-n),pixels=color==null?2:1;int direction=axis;
            current=draw(color==null?"ps_bloom":"ps_colored_bloom",current,partial,e->{set(e,"pixelSize",pixels/width,pixels/height);set(e,"vertical",direction);set(e,"totalAlpha",alpha);if(color!=null)set(e,"bloomColor",color[0],color[1],color[2]);});
        }
        return current;
    }
    private RenderTarget draw(String name,RenderTarget source,float partial,Consumer<EffectInstance> uniforms)throws IOException{
        RenderTarget out=source==a?b:a;var key=new Key(name,source,out);var pass=passes.get(key);
        if(pass==null){
            pass=new PostPass(resources,Psychedelicraft.id(name).toString(),source,out);pass.setOrthoMatrix(new Matrix4f().setOrtho(0,width,0,height,.1F,1000));
            if(name.equals("heat_distortion")||name.equals("simple_effects_depth")||name.equals("depth_of_field")||name.equals("digital_depth"))pass.addAuxAsset("DepthSampler",()->depth.getDepthTextureId(),width,height);
            if(name.equals("heat_distortion"))texture(pass,"NoiseSampler",HEAT,128,128);
            if(name.equals("distortion_map")){texture(pass,"OverlaySampler1",WATER,800,600);texture(pass,"OverlaySampler2",WATER,800,600);}
            if(name.equals("digital_depth"))texture(pass,"AsciiSampler",BITS,128,128);
            passes.put(key,pass);
        }
        RenderSystem.disableBlend();RenderSystem.colorMask(true,true,true,true);uniforms.accept(pass.getEffect());pass.process(partial);lastPasses.add(name);return out;
    }
    private static void texture(PostPass pass,String sampler,ResourceLocation location,int width,int height){pass.addAuxAsset(sampler,()->Minecraft.getInstance().getTextureManager().getTexture(location).getId(),width,height);}
    private static void set(EffectInstance effect,String name,float... values){AbstractUniform uniform=effect.safeGetUniform(name);switch(values.length){case 1->uniform.set(values[0]);case 2->uniform.set(values[0],values[1]);case 3->uniform.set(values[0],values[1],values[2]);case 4->uniform.set(values[0],values[1],values[2],values[3]);default->throw new IllegalArgumentException(name);}}
    private void releaseResources(){passes.values().forEach(PostPass::close);passes.clear();if(a!=null)a.destroyBuffers();if(b!=null)b.destroyBuffers();if(depth!=null)depth.destroyBuffers();a=b=depth=main=null;width=height=0;resources=null;worldDepthCaptured=false;}
    public void release(){RenderSystem.assertOnRenderThread();try(var state=new PostRenderState()){releaseResources();history.release();}failed=false;lastFailure=null;lastPasses.clear();}
}
