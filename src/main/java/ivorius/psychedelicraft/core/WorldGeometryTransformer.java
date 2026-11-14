package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;
import static ivorius.psychedelicraft.core.EntityGeometryTransformer.patchCalls;

public final class WorldGeometryTransformer implements IClassTransformer {
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String FORGE_CLOUDS = "net.minecraftforge.client.CloudRenderer";
    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String PARTICLE_MANAGER = "net.minecraft.client.particle.ParticleManager";
    private static final String TESSELLATOR = "net/minecraft/client/renderer/Tessellator";
    private static final String VERTEX_BUFFER = "net/minecraft/client/renderer/vertex/VertexBuffer";
    private static final String GL_STATE = "net/minecraft/client/renderer/GlStateManager";
    private static final String RAY_TRACE = "Lnet/minecraft/util/math/RayTraceResult;";
    private static final String BILLBOARD_RENDER = "(Lnet/minecraft/client/renderer/BufferBuilder;"
            + "Lnet/minecraft/entity/Entity;FFFFFF)V";
    private static final String LAYER_DRAW = "(Lnet/minecraft/util/BlockRenderLayer;)V";
    private static final String VANILLA_CLOUD_DRAW = "(FIDDD)V";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/DrugRenderer";
    private static final String SCOPE = "ivorius/psychedelicraft/client/render/shader/WorldGeometryShader$Scope";
    private static final String SCOPE_TYPE = "L" + SCOPE + ";";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !RENDER_GLOBAL.equals(transformedName) && !FORGE_CLOUDS.equals(transformedName)
                && !ENTITY_RENDERER.equals(transformedName) && !PARTICLE_MANAGER.equals(transformedName)
                && !isStandaloneBillboard(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (RENDER_GLOBAL.equals(transformedName)) {
            patchTerrain(method(type, "renderBlockLayer", "func_174982_a", LAYER_DRAW));
            patchOverworldSky(method(type, "renderSky", "func_174976_a", "(FI)V"));
            patchSkyDraw(method(type, "renderSkyEnd", "func_180448_r", "()V"), true);
            patchBlockOverlays(type);
            patchTessellatorDraws(method(type, "renderClouds", "func_180447_b", VANILLA_CLOUD_DRAW), true);
            wrapClouds(type, method(type, "renderCloudsFancy", "func_180445_c", VANILLA_CLOUD_DRAW), false);
        } else if (FORGE_CLOUDS.equals(transformedName)) {
            wrapClouds(type, method(type, "render", "render", "(IF)Z"), true);
        } else if (ENTITY_RENDERER.equals(transformedName)) {
            patchSkyView(method(type, "renderWorldPass", "func_175068_a", "(IFJ)V"));
            patchTessellatorDraws(method(type, "renderRainSnow", "func_78474_d", "(F)V"), false);
        } else if (PARTICLE_MANAGER.equals(transformedName)) {
            patchTessellatorDraws(method(type, "renderParticles", "func_78874_a", "(Lnet/minecraft/entity/Entity;F)V"), false);
        } else {
            patchTessellatorDraws(method(type, "renderParticle", "func_180434_a", BILLBOARD_RENDER), false);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchTerrain(MethodNode layer) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : layer.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if ("net/minecraft/client/renderer/ChunkRenderContainer".equals(remapper.map(draw.owner))
                    && ("renderChunkLayer".equals(name) || "func_178001_a".equals(name))
                    && LAYER_DRAW.equals(remapper.mapMethodDesc(draw.desc))) {
                String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(draw.owner),
                        Type.getArgumentTypes(draw.desc)[0]);
                layer.instructions.set(draw, hook("drawTerrain", descriptor));
                return;
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 chunk layer draw");
    }

    private static boolean isStandaloneBillboard(String name) {
        return "net.minecraft.client.particle.ParticleExplosionLarge".equals(name)
                || "net.minecraft.client.particle.ParticleSweepAttack".equals(name)
                || "net.minecraft.client.particle.ParticleFootStep".equals(name);
    }

    private static void patchBlockOverlays(ClassNode type) {
        MethodNode damage = method(type, "drawBlockDamageTexture", "func_174981_a", "(L" + TESSELLATOR
                + ";Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;F)V");
        Type tessellator = Type.getArgumentTypes(damage.desc)[0];
        replaceOverlayCall(damage, "net/minecraft/client/renderer/BlockRendererDispatcher", "renderBlockDamage",
                "func_175020_a", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;"
                        + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/world/IBlockAccess;)V",
                "drawBlockCracks", tessellator, 1);

        MethodNode outline = method(type, "drawSelectionBox", "func_72731_b",
                "(Lnet/minecraft/entity/player/EntityPlayer;" + RAY_TRACE + "IF)V");
        replaceOverlayCall(outline, RENDER_GLOBAL.replace('.', '/'), "drawSelectionBoundingBox", "func_189697_a",
                "(Lnet/minecraft/util/math/AxisAlignedBB;FFFF)V", "drawBlockOutline",
                Type.getArgumentTypes(outline.desc)[1], 2);

        MethodNode border = method(type, "renderWorldBorder", "func_180449_a", "(Lnet/minecraft/entity/Entity;F)V");
        patchCalls(Collections.singletonList(border), TESSELLATOR, "draw", "func_78381_a", "()V", "drawWorldBorder");
    }

    private static void replaceOverlayCall(MethodNode render, String owner, String name, String srg,
                                           String descriptor, String hookName, Type extraArgument, int slot) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!owner.equals(remapper.map(call.owner)) || !(name.equals(mapped) || srg.equals(mapped))
                    || !descriptor.equals(remapper.mapMethodDesc(call.desc))) continue;
            String receiver = call.getOpcode() == Opcodes.INVOKESTATIC ? "" : "L" + call.owner + ";";
            render.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, slot));
            render.instructions.set(call, hook(hookName,
                    "(" + receiver + call.desc.substring(1, call.desc.length() - 2) + extraArgument.getDescriptor() + ")V"));
            return;
        }
        throw new IllegalStateException("Could not locate " + name + " in " + render.name);
    }

    private static void patchSkyView(MethodNode render) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if ("net/minecraft/client/renderer/RenderGlobal".equals(remapper.map(draw.owner))
                    && ("renderSky".equals(name) || "func_174976_a".equals(name)) && "(FI)V".equals(draw.desc)) {
                render.instructions.set(draw, hook("renderSky", "(L" + draw.owner + ";FI)V"));
                return;
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 sky render call");
    }

    private static void patchSkyDraw(MethodNode render, boolean textured) {
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (!isTessellatorDraw(instruction)) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            // The first Overworld flush draws the sunrise gradient. The End flush repeats for each face.
            render.instructions.insertBefore(draw, new InsnNode(textured ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            render.instructions.set(draw, hook("drawSky", "(L" + draw.owner + ";Z)V"));
            return;
        }
        throw new IllegalStateException("Could not locate the sky draw in " + render.name);
    }

    private static void patchOverworldSky(MethodNode render) {
        patchSkyDraw(render, false);
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        // The sun and moon are the next two tessellator draws after the sunrise fan.
        int celestial = 0;
        boolean stars = false;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode && instruction.getOpcode() == Opcodes.GETFIELD) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (!RENDER_GLOBAL.replace('.', '/').equals(remapper.map(field.owner))) continue;
                String name = remapper.mapFieldName(field.owner, field.name, field.desc);
                if (name.equals("starVBO") || name.equals("field_175013_s")
                        || name.equals("starGLCallList") || name.equals("field_72772_v")) {
                    stars = true;
                } else if (name.equals("skyVBO") || name.equals("field_175012_t")
                        || name.equals("sky2VBO") || name.equals("field_175011_u")
                        || name.equals("glSkyList") || name.equals("field_72771_w")
                        || name.equals("glSkyList2") || name.equals("field_72781_x")) {
                    stars = false;
                }
            }
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            if (celestial < 2 && isTessellatorDraw(draw)) {
                render.instructions.set(draw, hook("drawCelestial", "(L" + draw.owner + ";)V"));
                celestial++;
                continue;
            }
            String owner = remapper.map(draw.owner);
            String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if (!"(I)V".equals(draw.desc)) continue;
            if (VERTEX_BUFFER.equals(owner) && (name.equals("drawArrays") || name.equals("func_177358_a"))) {
                render.instructions.insertBefore(draw, new InsnNode(stars ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                render.instructions.set(draw, hook("drawSkyBuffer", "(L" + draw.owner + ";IZ)V"));
            } else if (GL_STATE.equals(owner) && (name.equals("callList") || name.equals("func_179148_o"))) {
                render.instructions.insertBefore(draw, new InsnNode(stars ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                render.instructions.set(draw, hook("drawSkyList", "(IZ)V"));
            }
        }
    }

    private static boolean isTessellatorDraw(AbstractInsnNode instruction) {
        if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) return false;
        MethodInsnNode draw = (MethodInsnNode) instruction;
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
        return TESSELLATOR.equals(remapper.map(draw.owner))
                && ("draw".equals(name) || "func_78381_a".equals(name)) && "()V".equals(draw.desc);
    }

    private static void patchTessellatorDraws(MethodNode render, boolean fastClouds) {
        boolean patched = false;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (!isTessellatorDraw(instruction)) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            if (fastClouds) {
                render.instructions.insertBefore(draw, new VarInsnNode(Opcodes.DLOAD, 5));
                render.instructions.set(draw, hook("drawFastClouds", "(L" + draw.owner + ";D)V"));
            } else {
                render.instructions.set(draw, hook("drawParticles", "(L" + draw.owner + ";)V"));
            }
            patched = true;
        }
        if (!patched) throw new IllegalStateException("Could not locate the tessellator draw in " + render.name);
    }

    private static void wrapClouds(ClassNode type, MethodNode render, boolean forge) {
        MethodNode wrapper = new MethodNode(Opcodes.ASM5, render.access, render.name, render.desc, null, null);
        type.methods.add(wrapper);
        render.name = "psychedelicraft$renderClouds";
        render.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC;
        InsnList code = wrapper.instructions;
        int scopeSlot = forge ? 3 : 9;
        if (forge) {
            code.add(new VarInsnNode(Opcodes.FLOAD, 2));
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            code.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, "cloudMode", "I"));
            code.add(hook("beginForgeClouds", "(FI)" + SCOPE_TYPE));
        } else {
            code.add(new VarInsnNode(Opcodes.DLOAD, 5));
            code.add(new InsnNode(Opcodes.ICONST_1));
            code.add(hook("beginVanillaClouds", "(DZ)" + SCOPE_TYPE));
        }
        code.add(new VarInsnNode(Opcodes.ASTORE, scopeSlot));

        // Scope both color and depth draws together, including restoration when rendering throws.
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode handler = new LabelNode();
        code.add(start);
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (Type arg : Type.getArgumentTypes(render.desc)) {
            code.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
            slot += arg.getSize();
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, type.name, render.name, render.desc, false));
        if (forge) code.add(new VarInsnNode(Opcodes.ISTORE, scopeSlot + 1));
        code.add(end);
        closeClouds(code, scopeSlot);
        if (forge) code.add(new VarInsnNode(Opcodes.ILOAD, scopeSlot + 1));
        code.add(new InsnNode(forge ? Opcodes.IRETURN : Opcodes.RETURN));
        code.add(handler);
        Object[] locals = forge ? new Object[]{type.name, Opcodes.INTEGER, Opcodes.FLOAT, SCOPE}
                : new Object[]{type.name, Opcodes.FLOAT, Opcodes.INTEGER, Opcodes.DOUBLE, Opcodes.DOUBLE, Opcodes.DOUBLE, SCOPE};
        code.add(new FrameNode(Opcodes.F_FULL, locals.length, locals, 1, new Object[]{"java/lang/Throwable"}));
        closeClouds(code, scopeSlot);
        code.add(new InsnNode(Opcodes.ATHROW));
        wrapper.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, null));
    }

    private static void closeClouds(InsnList code, int scopeSlot) {
        code.add(new VarInsnNode(Opcodes.ALOAD, scopeSlot));
        code.add(hook("endClouds", "(" + SCOPE_TYPE + ")V"));
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}
