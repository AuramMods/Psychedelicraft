package ivorius.psychedelicraft.core;

import com.google.common.collect.ImmutableSet;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Set;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class EntityGeometryTransformer implements IClassTransformer {
    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String RENDER_LIVING = "net.minecraft.client.renderer.entity.RenderLivingBase";
    private static final String RENDER_DRAGON = "net.minecraft.client.renderer.entity.RenderDragon";
    private static final String RENDER_BOAT = "net.minecraft.client.renderer.entity.RenderBoat";
    private static final String LAYERS = "net.minecraft.client.renderer.entity.layers.";
    private static final String MODEL_BASE = "net/minecraft/client/model/ModelBase";
    private static final String MULTIPASS_MODEL = "net/minecraft/client/model/IMultipassModel";
    private static final String MODEL_RENDER = "(Lnet/minecraft/entity/Entity;FFFFFF)V";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/DrugRenderer";
    private static final Set<String> MODEL_LAYERS = ImmutableSet.of(
            "LayerArmorBase", "LayerCreeperCharge", "LayerElytra", "LayerEnderDragonEyes", "LayerEndermanEyes",
            "LayerEntityOnShoulder", "LayerLlamaDecor", "LayerSaddle", "LayerSheepWool", "LayerSlimeGel",
            "LayerSpiderEyes", "LayerStrayClothing", "LayerWitherAura", "LayerWolfCollar");

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !ENTITY_RENDERER.equals(transformedName) && !RENDER_GLOBAL.equals(transformedName)
                && !RENDER_LIVING.equals(transformedName) && !RENDER_DRAGON.equals(transformedName) && !RENDER_BOAT.equals(transformedName)
                && !isModelLayer(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (ENTITY_RENDERER.equals(transformedName)) {
            patchWorld(method(type, "updateCameraAndRender", "func_181560_a", "(FJ)V"));
        } else if (RENDER_GLOBAL.equals(transformedName)) {
            method(type, "renderEntities", "func_180446_a",
                    "(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V")
                    .instructions.insert(hook("captureEntityView", "()V"));
        } else {
            patchModels(type);
            if (RENDER_BOAT.equals(transformedName)) patchBoatMask(type);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isModelLayer(String name) {
        return name != null && name.startsWith(LAYERS) && MODEL_LAYERS.contains(name.substring(LAYERS.length()));
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

    private static void patchBoatMask(ClassNode type) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode render : type.methods) {
            for (AbstractInsnNode instruction : render.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.INVOKEINTERFACE) continue;
                MethodInsnNode draw = (MethodInsnNode) instruction;
                String name = remapper.mapMethodName(draw.owner, draw.name, draw.desc);
                if (!MULTIPASS_MODEL.equals(remapper.map(draw.owner))
                        || !("renderMultipass".equals(name) || "func_187054_b".equals(name))
                        || !MODEL_RENDER.equals(remapper.mapMethodDesc(draw.desc))) continue;
                render.instructions.set(draw, hook("drawBoatWaterMask", "(L" + draw.owner + ";" + draw.desc.substring(1)));
                return;
            }
        }
        throw new IllegalStateException("Could not locate the boat water mask draw");
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}
