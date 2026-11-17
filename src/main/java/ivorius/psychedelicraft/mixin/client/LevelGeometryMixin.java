package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.client.render.RenderPhase;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelGeometryMixin {
    @Shadow private VertexBuffer starBuffer;
    @Unique private RenderPhase.Scope psychedelicraft$skyScope,psychedelicraft$cloudScope,psychedelicraft$endSkyScope,psychedelicraft$borderScope;
    @Inject(method="renderLevel",at=@At("HEAD"))
    private void psychedelicraft$view(PoseStack pose,float partial,long finish,boolean outline,Camera camera,GameRenderer game,LightTexture light,Matrix4f projection,CallbackInfo ci){GeometryEffects.captureView(pose.last().pose());}
    @Inject(method="renderSky",at=@At("HEAD"))
    private void psychedelicraft$sky(PoseStack pose,Matrix4f projection,float partial,Camera camera,boolean fog,Runnable setupFog,CallbackInfo ci){psychedelicraft$skyScope=RenderPhase.SKY.enter();}
    @Inject(method="renderSky",at=@At("RETURN"))
    private void psychedelicraft$skyEnd(PoseStack pose,Matrix4f projection,float partial,Camera camera,boolean fog,Runnable setupFog,CallbackInfo ci){psychedelicraft$skyScope.close();}
    @Inject(method="renderEndSky",at=@At("HEAD"))
    private void psychedelicraft$endSky(PoseStack pose,CallbackInfo ci){psychedelicraft$endSkyScope=RenderPhase.END_SKY.enter();}
    @Inject(method="renderEndSky",at=@At("RETURN"))
    private void psychedelicraft$endSkyEnd(PoseStack pose,CallbackInfo ci){psychedelicraft$endSkyScope.close();}
    @Redirect(method="renderSky",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"))
    private void psychedelicraft$skyBuffer(VertexBuffer buffer,Matrix4f view,Matrix4f projection,ShaderInstance shader){try(var scope=(buffer==starBuffer?RenderPhase.STARS:RenderPhase.current()).enter()){buffer.drawWithShader(view,projection,shader);}}
    @Inject(method="renderClouds",at=@At("HEAD"))
    private void psychedelicraft$clouds(PoseStack pose,Matrix4f projection,float partial,double x,double y,double z,CallbackInfo ci){psychedelicraft$cloudScope=RenderPhase.CLOUDS.enter();}
    @Inject(method="renderClouds",at=@At("RETURN"))
    private void psychedelicraft$cloudEnd(PoseStack pose,Matrix4f projection,float partial,double x,double y,double z,CallbackInfo ci){psychedelicraft$cloudScope.close();}
    @Inject(method="renderWorldBorder",at=@At("HEAD"))
    private void psychedelicraft$border(Camera camera,CallbackInfo ci){psychedelicraft$borderScope=RenderPhase.BORDER.enter();}
    @Inject(method="renderWorldBorder",at=@At("RETURN"))
    private void psychedelicraft$borderEnd(Camera camera,CallbackInfo ci){psychedelicraft$borderScope.close();}
}
