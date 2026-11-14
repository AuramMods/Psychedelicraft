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

public final class BeeNestTreeTransformer implements IClassTransformer {
    private static final String GENERATOR = "net.minecraft.world.gen.feature.WorldGenerator";
    private static final String DECORATOR = "net.minecraft.world.biome.BiomeDecorator";
    private static final String SAPLING = "net.minecraft.block.BlockSapling";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String RANDOM = "Ljava/util/Random;";
    private static final String HOOKS = "ivorius/psychedelicraft/world/gen/BeeNestWorldGenerator";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(GENERATOR.equals(transformedName) || DECORATOR.equals(transformedName)
                || SAPLING.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        switch (transformedName) {
            case GENERATOR:
                MethodNode place = ClientEffectsTransformer.method(type, "setBlockAndNotifyAdequately", "func_175903_a",
                        "(" + WORLD + POS + STATE + ")V");
                replaceCalls(place, "net/minecraft/world/World", "setBlockState", "func_180501_a",
                        "(" + POS + STATE + "I)Z", "setTreeBlock");
                break;
            case DECORATOR:
                MethodNode decorate = ClientEffectsTransformer.method(type, "genDecorations", "func_150513_a",
                        "(Lnet/minecraft/world/biome/Biome;" + WORLD + RANDOM + ")V");
                replaceCalls(decorate, "net/minecraft/world/gen/feature/WorldGenAbstractTree", "generate", "func_180709_b",
                        "(" + WORLD + RANDOM + POS + ")Z", "generateNatural");
                break;
            case SAPLING:
                MethodNode grow = ClientEffectsTransformer.method(type, "generateTree", "func_176476_e",
                        "(" + WORLD + POS + STATE + RANDOM + ")V");
                replaceCalls(grow, "net/minecraft/world/gen/feature/WorldGenerator", "generate", "func_180709_b",
                        "(" + WORLD + RANDOM + POS + ")Z", "generateFromSapling");
                break;
        }
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void replaceCalls(MethodNode method, String owner, String mcp, String srg, String descriptor, String hook) {
        FMLDeobfuscatingRemapper mapper = FMLDeobfuscatingRemapper.INSTANCE;
        boolean replaced = false;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String mapped = mapper.mapMethodName(call.owner, call.name, call.desc);
            if (!owner.equals(mapper.map(call.owner)) || !descriptor.equals(mapper.mapMethodDesc(call.desc))
                    || !(mcp.equals(mapped) || srg.equals(mapped))) continue;
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook,
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
            replaced = true;
        }
        if (!replaced) throw new IllegalStateException("Could not locate Forge 1.12 call for " + hook);
    }
}
