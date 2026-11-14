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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class MooshroomVariantsTransformer implements IClassTransformer {
    private static final String MOOSHROOM = "net.minecraft.entity.passive.EntityMooshroom";
    private static final String RENDERER = "net.minecraft.client.renderer.entity.RenderMooshroom";
    private static final String LAYER = "net.minecraft.client.renderer.entity.layers.LayerMooshroomMushroom";
    private static final String MOOSHROOM_TYPE = "Lnet/minecraft/entity/passive/EntityMooshroom;";
    private static final String HOOKS = "ivorius/psychedelicraft/entity/MooshroomVariants";
    private static final FMLDeobfuscatingRemapper MAPPER = FMLDeobfuscatingRemapper.INSTANCE;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(MOOSHROOM.equals(transformedName) || RENDERER.equals(transformedName)
                || LAYER.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (MOOSHROOM.equals(transformedName)) {
            MethodNode shear = ClientEffectsTransformer.method(type, "onSheared", "onSheared",
                    "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;I)Ljava/util/List;");
            patchMushrooms(shear, Type.getObjectType(type.name), 0, 1);
            patchShearingState(shear, Type.getObjectType(type.name));
        } else if (LAYER.equals(transformedName)) {
            MethodNode layer = ClientEffectsTransformer.method(type, "doRenderLayer", "func_177141_a", "(" + MOOSHROOM_TYPE + "FFFFFFF)V");
            patchMushrooms(layer, Type.getArgumentTypes(layer.desc)[0], 1, 3);
        } else {
            MethodNode texture = ClientEffectsTransformer.method(type, "getEntityTexture", "func_110775_a",
                    "(" + MOOSHROOM_TYPE + ")Lnet/minecraft/util/ResourceLocation;");
            Type resource = Type.getReturnType(texture.desc);
            for (AbstractInsnNode instruction : texture.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN) continue;
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
                hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "texture",
                        Type.getMethodDescriptor(resource, resource, Type.getArgumentTypes(texture.desc)[0]), false));
                texture.instructions.insertBefore(instruction, hook);
            }
        }
        // The vanilla layer discards its arguments in the frame before drawing the mushrooms.
        int flags = LAYER.equals(transformedName) ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
        ClassWriter writer = new ClassWriter(flags);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchMushrooms(MethodNode method, Type cow, int local, int expected) {
        int replaced = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.GETSTATIC) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            String mappedName = MAPPER.mapFieldName(field.owner, field.name, field.desc);
            if (!"net/minecraft/init/Blocks".equals(MAPPER.map(field.owner))
                    || !("RED_MUSHROOM".equals(mappedName) || "field_150337_Q".equals(mappedName))) continue;
            method.instructions.insertBefore(field, new VarInsnNode(Opcodes.ALOAD, local));
            method.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "mushroom",
                    Type.getMethodDescriptor(Type.getType(field.desc), cow), false));
            replaced++;
        }
        if (replaced != expected) throw new IllegalStateException("Could not locate the mooshroom mushroom selections");
    }

    private static void patchShearingState(MethodNode method, Type mooshroom) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.INVOKESPECIAL) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!"net/minecraft/entity/passive/EntityCow".equals(MAPPER.map(call.owner))
                    || !"<init>".equals(call.name)) continue;
            InsnList copy = new InsnList();
            copy.add(new InsnNode(Opcodes.DUP));
            copy.add(new VarInsnNode(Opcodes.ALOAD, 0));
            copy.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "copyShearingState",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(call.owner), mooshroom), false));
            method.instructions.insert(call, copy);
            return;
        }
        throw new IllegalStateException("Could not locate the cow created by mooshroom shearing");
    }
}
