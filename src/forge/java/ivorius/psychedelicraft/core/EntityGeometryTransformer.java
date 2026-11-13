package ivorius.psychedelicraft.core;

import com.google.common.collect.ImmutableSet;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;
import java.util.Set;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class EntityGeometryTransformer implements IClassTransformer {
    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String ENTITY_RENDERERS = "net.minecraft.client.renderer.entity.";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String BLOCK_STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String MODEL_BASE = "net/minecraft/client/model/ModelBase";
    private static final String MULTIPASS_MODEL = "net/minecraft/client/model/IMultipassModel";
    private static final String TESSELLATOR = "net/minecraft/client/renderer/Tessellator";
    private static final String MODEL_RENDER = "(" + ENTITY + "FFFFFF)V";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/DrugRenderer";
    private static final Set<String> MODELS = ImmutableSet.of(
            "RenderLivingBase", "RenderBoat", "RenderDragon", "RenderMinecart", "RenderEnderCrystal",
            "RenderShulkerBullet", "RenderLeashKnot", "RenderEvokerFangs", "RenderWitherSkull", "RenderLlamaSpit",
            "layers.LayerArmorBase", "layers.LayerCreeperCharge", "layers.LayerElytra", "layers.LayerEnderDragonEyes",
            "layers.LayerEndermanEyes", "layers.LayerEntityOnShoulder", "layers.LayerLlamaDecor", "layers.LayerSaddle",
            "layers.LayerSheepWool", "layers.LayerSlimeGel", "layers.LayerSpiderEyes", "layers.LayerStrayClothing",
            "layers.LayerWitherAura", "layers.LayerWolfCollar");
    private static final Set<String> QUADS = ImmutableSet.of(
            "RenderArrow", "RenderDragon", "RenderDragonFireball", "RenderFallingBlock", "RenderFireball", "RenderFish",
            "RenderGuardian", "RenderLightningBolt", "RenderLiving", "RenderPainting", "RenderXPOrb", "layers.LayerEnderDragonDeath");
    private static final Set<String> BLOCKS = ImmutableSet.of("RenderMinecart", "RenderTntMinecart", "RenderTNTPrimed", "layers.LayerHeldBlock");
    private static final String SHULKER_HEAD = "RenderShulker$HeadLayer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) return bytes;
        String renderer = transformedName.startsWith(ENTITY_RENDERERS) ? transformedName.substring(ENTITY_RENDERERS.length()) : "";
        if (!ENTITY_RENDERER.equals(transformedName) && !RENDER_GLOBAL.equals(transformedName)
                && !MODELS.contains(renderer) && !QUADS.contains(renderer) && !BLOCKS.contains(renderer)
                && !"Render".equals(renderer) && !SHULKER_HEAD.equals(renderer)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (ENTITY_RENDERER.equals(transformedName)) {
            patchWorld(method(type, "updateCameraAndRender", "func_181560_a", "(FJ)V"));
        } else if (RENDER_GLOBAL.equals(transformedName)) {
            method(type, "renderEntities", "func_180446_a",
                    "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V")
                    .instructions.insert(hook("captureEntityView", "()V"));
        } else {
            if (MODELS.contains(renderer)) patchModels(type);
            if (QUADS.contains(renderer)) patchQuads(type.methods, "drawWorldQuads");
            if (BLOCKS.contains(renderer)) patchCalls(type.methods, "net/minecraft/client/renderer/BlockRendererDispatcher",
                    "renderBlockBrightness", "func_175016_a", "(" + BLOCK_STATE + "F)V", "drawWorldBlock");
            if ("RenderBoat".equals(renderer)) patchCalls(type.methods, MULTIPASS_MODEL,
                    "renderMultipass", "func_187054_b", MODEL_RENDER, "drawBoatWaterMask");
            if (SHULKER_HEAD.equals(renderer)) patchCalls(type.methods, "net/minecraft/client/model/ModelRenderer",
                    "render", "func_78785_a", "(F)V", "drawWorldPart");
            if ("Render".equals(renderer)) patchShadowAndFire(type);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchWorld(MethodNode render) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : render.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode draw = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
            if ("(FJ)V".equals(draw.desc) && ("renderWorld".equals(name) || "func_78471_a".equals(name))) {
                render.instructions.set(draw, hook("renderWorld", "(L" + draw.owner + ";FJ)V"));
                return;
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 world render call");
    }

    private static void patchModels(ClassNode type) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        boolean patched = false;
        for (MethodNode render : type.methods) {
            String method = remapper.mapMethodName(type.name, render.name, render.desc);
            boolean glint = "renderEnchantedGlint".equals(method) || "func_188364_a".equals(method);
            for (AbstractInsnNode instruction : render.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
                MethodInsnNode draw = (MethodInsnNode) instruction;
                String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
                String owner = remapper.map(draw.owner);
                if (!owner.startsWith("net/minecraft/client/model/")
                        || !("render".equals(name) || "func_78088_a".equals(name))
                        || !MODEL_RENDER.equals(remapper.mapMethodDesc(draw.desc))) continue;
                String model = owner.equals(draw.owner) ? MODEL_BASE : remapper.unmap(MODEL_BASE);
                render.instructions.insertBefore(draw, new InsnNode(glint ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
                render.instructions.set(draw, hook("drawEntityModel",
                        "(L" + model + ";" + draw.desc.substring(1, draw.desc.length() - 2) + "Z)V"));
                patched = true;
            }
        }
        if (!patched) throw new IllegalStateException("Could not locate model draws in " + type.name);
    }

    private static void patchShadowAndFire(ClassNode type) {
        MethodNode shadow = method(type, "renderShadow", "func_76975_c", "(" + ENTITY + "DDDFF)V");
        patchQuads(Collections.singletonList(shadow), "drawShadow");
        MethodNode fire = method(type, "renderEntityOnFire", "func_76977_a", "(" + ENTITY + "DDDF)V");
        patchQuads(Collections.singletonList(fire), "drawWorldQuads");

        MethodNode surface = method(type, "renderShadowSingle", "func_188299_a", "(" + BLOCK_STATE + "DDD" + BLOCK_POS + "FFDDD)V");
        String descriptor = Type.getArgumentTypes(surface.desc)[4].getDescriptor();
        InsnList section = new InsnList();
        section.add(new VarInsnNode(Opcodes.ALOAD, 8));
        section.add(hook("shadowSurface", "(" + descriptor + ")V"));
        surface.instructions.insert(section);
    }

    private static void patchQuads(Iterable<MethodNode> methods, String target) {
        patchCalls(methods, TESSELLATOR, "draw", "func_78381_a", "()V", target);
    }

    private static void patchCalls(Iterable<MethodNode> methods, String owner, String name, String srg,
                                   String descriptor, String target) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        boolean patched = false;
        for (MethodNode render : methods) {
            for (AbstractInsnNode instruction : render.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode draw = (MethodInsnNode) instruction;
                String mapped = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
                if (!owner.equals(remapper.map(draw.owner)) || !(name.equals(mapped) || srg.equals(mapped))
                        || !descriptor.equals(remapper.mapMethodDesc(draw.desc))) continue;
                render.instructions.set(draw, hook(target, "(L" + draw.owner + ";" + draw.desc.substring(1)));
                patched = true;
            }
        }
        if (!patched) throw new IllegalStateException("Could not locate " + owner + "." + name + " for " + target);
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}
