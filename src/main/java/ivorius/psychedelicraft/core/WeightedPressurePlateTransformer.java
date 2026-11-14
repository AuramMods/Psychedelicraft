package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class WeightedPressurePlateTransformer implements IClassTransformer {
    private static final String WORLD = "net/minecraft/world/World";
    private static final String QUERY = "(Ljava/lang/Class;Lnet/minecraft/util/math/AxisAlignedBB;)Ljava/util/List;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.block.BlockPressurePlateWeighted".equals(transformedName)) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        MethodNode strength = ClientEffectsTransformer.method(type, "computeRedstoneStrength", "func_180669_e",
                "(L" + WORLD + ";Lnet/minecraft/util/math/BlockPos;)I");
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : strength.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String method = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (WORLD.equals(remapper.map(call.owner)) && QUERY.equals(remapper.mapMethodDesc(call.desc))
                    && ("getEntitiesWithinAABB".equals(method) || "func_72872_a".equals(method))) {
                strength.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "ivorius/psychedelicraft/block/WeightedPressurePlateEntities", "find",
                        "(L" + call.owner + ";" + call.desc.substring(1), false));
                ClassWriter writer = new ClassWriter(0);
                type.accept(writer);
                return writer.toByteArray();
            }
        }
        throw new IllegalStateException("Could not locate the weighted pressure plate entity query");
    }
}
