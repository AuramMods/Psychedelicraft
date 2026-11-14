package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.field;
import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class NetherSurfaceTransformer implements IClassTransformer {
    private static final String GENERATOR = "net.minecraft.world.gen.ChunkGeneratorHell";
    private static final String HOOKS = "ivorius/psychedelicraft/world/gen/SoulSoilWorldGenerator";
    private static final String CAVE_SIGNATURE = "(Lnet/minecraft/world/World;IILnet/minecraft/world/chunk/ChunkPrimer;)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !GENERATOR.equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode generate = method(type, "generateChunk", "func_185932_a", "(II)Lnet/minecraft/world/chunk/Chunk;");
        FieldNode caves = field(type, "genNetherCaves", "field_185939_I");
        FieldNode world = field(type, "world", "field_185952_n");
        patchAfterCaves(type, generate, caves, world);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchAfterCaves(ClassNode type, MethodNode generate, FieldNode caves, FieldNode world) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        boolean readsCaves = false;
        for (AbstractInsnNode instruction : generate.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode access = (FieldInsnNode) instruction;
                if (type.name.equals(access.owner) && caves.name.equals(access.name)) readsCaves = true;
            }
            if (!readsCaves || !(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!("generate".equals(name) || "func_186125_a".equals(name))
                    || !CAVE_SIGNATURE.equals(remapper.mapMethodDesc(call.desc))) continue;
            AbstractInsnNode primer = call.getPrevious();
            while (primer.getOpcode() < 0) primer = primer.getPrevious();
            if (!(primer instanceof VarInsnNode) || primer.getOpcode() != Opcodes.ALOAD) break;

            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
            hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, world.name, world.desc));
            hook.add(new VarInsnNode(Opcodes.ILOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ILOAD, 2));
            hook.add(new VarInsnNode(Opcodes.ALOAD, ((VarInsnNode) primer).var));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "buildSurfaces",
                    call.desc, false));
            generate.instructions.insert(call, hook);
            return;
        }
        throw new IllegalStateException("Could not locate Nether cave generation");
    }
}
