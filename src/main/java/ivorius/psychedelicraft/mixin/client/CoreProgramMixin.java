package ivorius.psychedelicraft.mixin.client;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import ivorius.psychedelicraft.client.render.shader.GeometryEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.io.*;

/** EffectProgram has its own entry point, so post programs do not receive core geometry code. */
@Mixin(Program.class)
abstract class CoreProgramMixin {
    @ModifyArg(method="compileShader",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/shaders/Program;compileShaderInternal(Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;Ljava/io/InputStream;Ljava/lang/String;Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;)I"),index=2)
    private static InputStream psychedelicraft$coreSource(Program.Type type,String name,InputStream input,String pack,GlslPreprocessor imports)throws IOException{return GeometryEffects.inject(type,name,input);}
}
