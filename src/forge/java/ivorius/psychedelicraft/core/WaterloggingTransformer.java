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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.field;
import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class WaterloggingTransformer implements IClassTransformer {
    private static final String DISPATCHER = "net.minecraft.client.renderer.BlockRendererDispatcher";
    private static final String FLUID_RENDERER = "net.minecraft.client.renderer.BlockFluidRenderer";
    private static final String VIEWPOINT = "net.minecraft.client.renderer.ActiveRenderInfo";
    private static final String LIQUID = "net.minecraft.block.BlockLiquid";
    private static final String FLOWING = "net.minecraft.block.BlockDynamicLiquid";
    private static final String PISTON = "net.minecraft.block.BlockPistonBase";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String ACCESS = "Lnet/minecraft/world/IBlockAccess;";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String MATERIAL = "Lnet/minecraft/block/material/Material;";
    private static final String HOOKS = "ivorius/psychedelicraft/fluid/physical/Waterlogging";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(DISPATCHER.equals(transformedName) || FLUID_RENDERER.equals(transformedName)
                || LIQUID.equals(transformedName) || FLOWING.equals(transformedName)
                || PISTON.equals(transformedName) || VIEWPOINT.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        switch (transformedName) {
            case DISPATCHER:
                patchDispatcher(type);
                break;
            case FLUID_RENDERER:
                substituteFluidStates(method(type, "getFluidHeight", "func_178269_a", "(" + ACCESS + POS + MATERIAL + ")F"), 2);
                break;
            case VIEWPOINT:
                substituteFluidStates(method(type, "getBlockStateAtEntityViewpoint", "func_186703_a",
                        "(" + WORLD + "Lnet/minecraft/entity/Entity;F)" + STATE), 2);
                break;
            case LIQUID:
                substituteFluidStates(method(type, "shouldSideBeRendered", "func_176225_a",
                        "(" + STATE + ACCESS + POS + "Lnet/minecraft/util/EnumFacing;)Z"), 1);
                substituteFluidStates(method(type, "getFlow", "func_189543_a",
                        "(" + ACCESS + POS + STATE + ")Lnet/minecraft/util/math/Vec3d;"), 3);
                substituteFluidStates(method(type, "modifyAcceleration", "func_176197_a",
                        "(" + WORLD + POS + "Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;)"
                                + "Lnet/minecraft/util/math/Vec3d;"), 1);
                substituteFluidStates(method(type, "checkForMixing", "func_176365_e", "(" + WORLD + POS + STATE + ")Z"), 1);
                break;
            case FLOWING:
                keepSourceInBlock(type);
                substituteFluidStates(method(type, "checkAdjacentBlock", "func_176371_a", "(" + WORLD + POS + "I)I"), 1);
                substituteFluidStates(method(type, "updateTick", "func_180650_b",
                        "(" + WORLD + POS + STATE + "Ljava/util/Random;)V"), 1);
                break;
            case PISTON:
                patchPiston(type);
                break;
            default:
                throw new AssertionError(transformedName);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void keepSourceInBlock(ClassNode type) {
        MethodNode settle = method(type, "placeStaticBlock", "func_180690_f", "(" + WORLD + POS + STATE + ")V");
        Type[] args = Type.getArgumentTypes(settle.desc);
        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "isWet",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, args[0], args[1]), false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        hook.add(new InsnNode(Opcodes.RETURN));
        hook.add(vanilla);
        hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        settle.instructions.insert(hook);
    }

    private static void patchDispatcher(ClassNode type) {
        MethodNode render = method(type, "renderBlock", "func_175018_a",
                "(" + STATE + POS + ACCESS + "Lnet/minecraft/client/renderer/BufferBuilder;)Z");
        FieldNode renderer = field(type, "fluidRenderer", "field_175025_e");
        String descriptor = Type.getMethodDescriptor(Type.getType(Boolean.class), Type.getType(renderer.desc),
                Type.getArgumentTypes(render.desc)[0], Type.getArgumentTypes(render.desc)[1],
                Type.getArgumentTypes(render.desc)[2], Type.getArgumentTypes(render.desc)[3]);
        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, renderer.name, renderer.desc));
        for (int arg = 1; arg <= 4; arg++) hook.add(new VarInsnNode(Opcodes.ALOAD, arg));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/client/render/WaterloggedRenderer",
                "render", descriptor, false));
        hook.add(new InsnNode(Opcodes.DUP));
        hook.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
        hook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
        hook.add(new InsnNode(Opcodes.IRETURN));
        hook.add(vanilla);
        hook.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Boolean"}));
        hook.add(new InsnNode(Opcodes.POP));
        render.instructions.insert(hook);
    }

    private static void patchPiston(ClassNode type) {
        String facing = "Lnet/minecraft/util/EnumFacing;";
        MethodNode push = method(type, "canPush", "func_185646_a", "(" + STATE + WORLD + POS + facing + "Z" + facing + ")Z");
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : push.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!"hasTileEntity".equals(call.name) || !("(" + STATE + ")Z").equals(remapper.mapMethodDesc(call.desc))) continue;
            push.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "blocksPiston",
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 piston tile check");
    }

    private static void substituteFluidStates(MethodNode method, int limit) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        int replaced = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            String owner = remapper.map(call.owner);
            if (!("getBlockState".equals(name) || "func_180495_p".equals(name))
                    || !("(" + POS + ")" + STATE).equals(remapper.mapMethodDesc(call.desc))
                    || !("net/minecraft/world/World".equals(owner) || "net/minecraft/world/IBlockAccess".equals(owner))) continue;
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS,
                    "fluidState", "(L" + call.owner + ";" + call.desc.substring(1), false));
            if (++replaced == limit) return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 liquid block state reads in " + method.name);
    }
}
