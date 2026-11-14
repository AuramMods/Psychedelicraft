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
import org.objectweb.asm.tree.TypeInsnNode;
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
    private static final String BLOCK = "net.minecraft.block.Block";
    private static final String ENTITY_CLASS = "net.minecraft.entity.Entity";
    private static final String WORLD_SERVER = "net.minecraft.world.WorldServer";
    private static final String WORLD_CLASS = "net.minecraft.world.World";
    private static final String SHEARS = "net.minecraft.item.ItemShears";
    private static final String FARMLAND = "net.minecraft.block.BlockFarmland";
    private static final String SUSPENDED = "net.minecraft.client.particle.ParticleSuspend";
    private static final String WORLD_CLIENT = "net.minecraft.client.multiplayer.WorldClient";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String ACCESS = "Lnet/minecraft/world/IBlockAccess;";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String MATERIAL = "Lnet/minecraft/block/material/Material;";
    private static final String ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String PLAYER = "Lnet/minecraft/entity/player/EntityPlayer;";
    private static final String AABB = "Lnet/minecraft/util/math/AxisAlignedBB;";
    private static final String VECTOR = "Lnet/minecraft/util/math/Vec3d;";
    private static final String FACING = "Lnet/minecraft/util/EnumFacing;";
    private static final String RANDOM = "Ljava/util/Random;";
    private static final String HOOKS = "ivorius/psychedelicraft/fluid/physical/Waterlogging";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(DISPATCHER.equals(transformedName) || FLUID_RENDERER.equals(transformedName)
                || LIQUID.equals(transformedName) || FLOWING.equals(transformedName)
                || PISTON.equals(transformedName) || VIEWPOINT.equals(transformedName)
                || BLOCK.equals(transformedName) || ENTITY_CLASS.equals(transformedName) || WORLD_SERVER.equals(transformedName)
                || WORLD_CLASS.equals(transformedName) || SHEARS.equals(transformedName)
                || FARMLAND.equals(transformedName) || SUSPENDED.equals(transformedName)
                || WORLD_CLIENT.equals(transformedName))) return bytes;
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
                substituteFluidStates(method(type, "checkForMixing", "func_176365_e", "(" + WORLD + POS + STATE + ")Z"),
                        1, "waterState");
                break;
            case FLOWING:
                keepSourceInBlock(type);
                guardWaterOutlets(type);
                allowVatInflow(type);
                substituteFluidStates(method(type, "checkAdjacentBlock", "func_176371_a", "(" + WORLD + POS + "I)I"), 1);
                substituteFluidStates(method(type, "updateTick", "func_180650_b",
                        "(" + WORLD + POS + STATE + "Ljava/util/Random;)V"), 1);
                break;
            case PISTON:
                patchPiston(type);
                break;
            case BLOCK:
                patchBlock(type);
                break;
            case ENTITY_CLASS:
                patchPhysicalFluidEyes(type);
                break;
            case WORLD_SERVER:
                routeScheduledTicks(type);
                break;
            case WORLD_CLASS:
                scheduleOnNeighborUpdate(type);
                releaseWater(method(type, "setBlockToAir", "func_175698_g", "(" + POS + ")Z"));
                releaseWater(method(type, "destroyBlock", "func_175655_b", "(" + POS + "Z)Z"));
                break;
            case SHEARS:
                releaseWater(method(type, "onBlockStartBreak", "onBlockStartBreak",
                        "(Lnet/minecraft/item/ItemStack;" + POS + PLAYER + ")Z"));
                break;
            case FARMLAND:
                substituteFluidStates(method(type, "hasWater", "func_176530_e", "(" + WORLD + POS + ")Z"), 1, "waterState");
                break;
            case SUSPENDED:
                substituteFluidStates(method(type, "onUpdate", "func_189213_a", "()V"), 1, "waterState");
                break;
            case WORLD_CLIENT:
                patchDisplayTick(type);
                break;
            default:
                throw new AssertionError(transformedName);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchDisplayTick(ClassNode type) {
        MethodNode display = method(type, "showBarrierParticles", "func_184153_a",
                "(IIII" + RANDOM + "ZLnet/minecraft/util/math/BlockPos$MutableBlockPos;)V");
        for (AbstractInsnNode instruction : display.instructions.toArray()) {
            if (!matches(instruction, "randomDisplayTick", "func_180655_c", "(" + STATE + WORLD + POS + RANDOM + ")V")) continue;
            replaceWorldCall(display, (MethodInsnNode) instruction, "displayTick");
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 random display tick");
    }

    private static void patchPhysicalFluidEyes(ClassNode type) {
        MethodNode method = method(type, "isInsideOfMaterial", "func_70055_a", "(" + MATERIAL + ")Z");
        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "physicalFluidAtEyes",
                Type.getMethodDescriptor(Type.getType(Boolean.class), Type.getObjectType(type.name),
                        Type.getArgumentTypes(method.desc)[0]), false));
        hook.add(new InsnNode(Opcodes.DUP));
        hook.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
        hook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
        hook.add(new InsnNode(Opcodes.IRETURN));
        hook.add(vanilla);
        hook.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Boolean"}));
        hook.add(new InsnNode(Opcodes.POP));
        method.instructions.insert(hook);
    }

    private static void patchBlock(ClassNode type) {
        MethodNode hasTile = method(type, "hasTileEntity", "hasTileEntity", "(" + STATE + ")Z");
        returnWhenSupported(hasTile, new InsnNode(Opcodes.ICONST_1), Opcodes.IRETURN);
        MethodNode createTile = method(type, "createTileEntity", "createTileEntity",
                "(" + WORLD + STATE + ")Lnet/minecraft/tileentity/TileEntity;");
        String tile = "ivorius/psychedelicraft/block/entity/WaterloggedBlockEntity";
        InsnList create = new InsnList();
        create.add(new TypeInsnNode(Opcodes.NEW, tile));
        create.add(new InsnNode(Opcodes.DUP));
        create.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, tile, "<init>", "()V", false));
        create.add(new InsnNode(Opcodes.ARETURN));
        returnWhen(createTile, create, 2, "needsWaterTile");
        MethodNode layer = method(type, "canRenderInLayer", "canRenderInLayer",
                "(" + STATE + "Lnet/minecraft/util/BlockRenderLayer;)Z");
        Type[] args = Type.getArgumentTypes(layer.desc);
        InsnList render = new InsnList();
        render.add(new VarInsnNode(Opcodes.ALOAD, 1));
        render.add(new VarInsnNode(Opcodes.ALOAD, 2));
        render.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/client/render/WaterloggedRenderer",
                "renderLayer", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, args), false));
        render.add(new InsnNode(Opcodes.IRETURN));
        returnWhen(layer, render, 1, "supports");

        delegateBlock(method(type, "isEntityInsideMaterial", "isEntityInsideMaterial",
                "(" + ACCESS + POS + STATE + ENTITY + "D" + MATERIAL + "Z)Ljava/lang/Boolean;"),
                "containsEntity", Opcodes.ARETURN);
        delegateBlock(method(type, "isAABBInsideMaterial", "isAABBInsideMaterial",
                "(" + WORLD + POS + AABB + MATERIAL + ")Ljava/lang/Boolean;"), "containsMaterial", Opcodes.ARETURN);
        delegateBlock(method(type, "isAABBInsideLiquid", "isAABBInsideLiquid",
                "(" + WORLD + POS + AABB + ")Ljava/lang/Boolean;"), "containsLiquid", Opcodes.ARETURN);
        delegateBlock(method(type, "getBlockLiquidHeight", "getBlockLiquidHeight",
                "(" + WORLD + POS + STATE + MATERIAL + ")F"), "liquidHeight", Opcodes.FRETURN);
        delegateBlock(method(type, "modifyAcceleration", "func_176197_a",
                "(" + WORLD + POS + ENTITY + VECTOR + ")" + VECTOR), "acceleration", Opcodes.ARETURN);
        patchFireValue(method(type, "getFlammability", "getFlammability", "(" + ACCESS + POS + FACING + ")I"));
        patchFireValue(method(type, "getFireSpreadSpeed", "getFireSpreadSpeed", "(" + ACCESS + POS + FACING + ")I"));
        releaseWater(method(type, "removedByPlayer", "removedByPlayer", "(" + STATE + WORLD + POS + PLAYER + "Z)Z"));
    }

    private static void returnWhenSupported(MethodNode method, AbstractInsnNode value, int opcode) {
        InsnList result = new InsnList();
        result.add(value);
        result.add(new InsnNode(opcode));
        returnWhen(method, result, 1, "supports");
    }

    private static void returnWhen(MethodNode method, InsnList result, int state, String predicate) {
        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, state));
        String descriptor = "(" + Type.getArgumentTypes(method.desc)[state - 1].getDescriptor() + ")Z";
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, predicate, descriptor, false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        hook.add(result);
        hook.add(vanilla);
        hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(hook);
    }

    private static void delegateBlock(MethodNode method, String hook, int opcode) {
        InsnList body = new InsnList();
        int local = 1;
        for (Type arg : Type.getArgumentTypes(method.desc)) {
            body.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), local));
            local += arg.getSize();
        }
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, method.desc, false));
        body.add(new InsnNode(opcode));
        method.instructions.clear();
        method.localVariables = null;
        method.instructions.add(body);
    }

    private static void patchFireValue(MethodNode method) {
        Type[] args = Type.getArgumentTypes(method.desc);
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != Opcodes.IRETURN) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "fireValue",
                    Type.getMethodDescriptor(Type.INT_TYPE, Type.INT_TYPE, args[0], args[1]), false));
            method.instructions.insertBefore(instruction, hook);
        }
    }

    private static void releaseWater(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!matches(instruction, "setBlockState", "func_180501_a", "(" + POS + STATE + "I)Z")) continue;
            replaceWorldCall(method, (MethodInsnNode) instruction, "remove");
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 block removal in " + method.name);
    }

    private static void routeScheduledTicks(ClassNode type) {
        MethodNode ticks = method(type, "tickUpdates", "func_72955_a", "(Z)Z");
        for (AbstractInsnNode instruction : ticks.instructions.toArray()) {
            if (!matches(instruction, "updateTick", "func_180650_b", "(" + WORLD + POS + STATE + RANDOM + ")V")) continue;
            replaceWorldCall(ticks, (MethodInsnNode) instruction, "tickBlock");
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 scheduled block update");
    }

    private static void scheduleOnNeighborUpdate(ClassNode type) {
        MethodNode neighbor = method(type, "neighborChanged", "func_190524_a",
                "(" + POS + "Lnet/minecraft/block/Block;" + POS + ")V");
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "scheduleIfWet",
                "(L" + type.name + ";" + Type.getArgumentTypes(neighbor.desc)[0] + ")V", false));
        neighbor.instructions.insert(hook);
    }

    private static void guardWaterOutlets(ClassNode type) {
        MethodNode tick = method(type, "updateTick", "func_180650_b", "(" + WORLD + POS + STATE + RANDOM + ")V");
        guardDownwardDestination(tick);
        for (AbstractInsnNode instruction : tick.instructions.toArray()) {
            if (!matches(instruction, "tryFlowInto", "func_176375_a", "(" + WORLD + POS + STATE + "I)V")) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            // The source position is local 2. Use a synthetic bridge in the transformed class
            // so the native private method still owns water placement.
            tick.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 2));
            tick.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESPECIAL, type.name,
                    "psychedelicraft$flowThroughFace", appendSource(call.desc), false));
        }
        MethodNode nativeFlow = method(type, "tryFlowInto", "func_176375_a", "(" + WORLD + POS + STATE + "I)V");
        MethodNode bridge = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                "psychedelicraft$flowThroughFace", appendSource(nativeFlow.desc), null, null);
        Type[] args = Type.getArgumentTypes(nativeFlow.desc);
        InsnList body = bridge.instructions;
        LabelNode open = new LabelNode();
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new VarInsnNode(Opcodes.ALOAD, 5));
        body.add(new VarInsnNode(Opcodes.ALOAD, 2));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "isOutletClosed",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, args[0], args[1], args[1]), false));
        body.add(new JumpInsnNode(Opcodes.IFEQ, open));
        body.add(new InsnNode(Opcodes.RETURN));
        body.add(open);
        body.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        for (int local = 0; local <= 4; local++) {
            body.add(new VarInsnNode(local == 4 ? Opcodes.ILOAD : Opcodes.ALOAD, local));
        }
        body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, type.name, nativeFlow.name, nativeFlow.desc, false));
        body.add(new InsnNode(Opcodes.RETURN));
        type.methods.add(bridge);
    }

    private static void guardDownwardDestination(MethodNode tick) {
        int downReads = 0;
        for (AbstractInsnNode instruction : tick.instructions.toArray()) {
            if (!matches(instruction, "getBlockState", "func_180495_p", "(" + POS + ")" + STATE)
                    || !matches(instruction.getPrevious(), "down", "func_177977_b", "()" + POS)
                    || ++downReads != 2) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            Type pos = Type.getArgumentTypes(call.desc)[0];
            String descriptor = Type.getMethodDescriptor(Type.getReturnType(call.desc), Type.getObjectType(call.owner), pos, pos);
            tick.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 2));
            tick.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "flowDestination", descriptor, false));
            return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 downward flow destination");
    }

    private static String appendSource(String descriptor) {
        return descriptor.substring(0, descriptor.indexOf(')')) + Type.getArgumentTypes(descriptor)[1] + ")V";
    }

    private static void allowVatInflow(ClassNode type) {
        String destination = "(" + WORLD + POS + STATE;
        MethodNode blocked = method(type, "isBlocked", "func_176372_g", destination + ")Z");
        MethodNode spread = method(type, "tryFlowInto", "func_176375_a", destination + "I)V");
        InsnList open = new InsnList();
        open.add(new InsnNode(Opcodes.ICONST_0));
        open.add(new InsnNode(Opcodes.IRETURN));
        hookVatDestination(type, blocked, "canAcceptVanilla", open);
        InsnList filled = new InsnList();
        filled.add(new InsnNode(Opcodes.RETURN));
        hookVatDestination(type, spread, "fillIfVanillaVat", filled);
    }

    private static void hookVatDestination(ClassNode type, MethodNode method, String hookName, InsnList accepted) {
        Type[] args = Type.getArgumentTypes(method.desc);
        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        for (int local = 0; local <= 3; local++) hook.add(new VarInsnNode(Opcodes.ALOAD, local));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/fluid/physical/VatInflow", hookName,
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getObjectType(type.name), args[0], args[1], args[2]), false));
        hook.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        hook.add(accepted);
        hook.add(vanilla);
        hook.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(hook);
    }

    private static boolean matches(AbstractInsnNode instruction, String name, String srg, String descriptor) {
        if (!(instruction instanceof MethodInsnNode)) return false;
        MethodInsnNode call = (MethodInsnNode) instruction;
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
        return (name.equals(mapped) || srg.equals(mapped)) && descriptor.equals(remapper.mapMethodDesc(call.desc));
    }

    private static void replaceWorldCall(MethodNode method, MethodInsnNode call, String hook) {
        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook,
                "(L" + call.owner + ";" + call.desc.substring(1), false));
    }

    private static void keepSourceInBlock(ClassNode type) {
        MethodNode settle = method(type, "placeStaticBlock", "func_180690_f", "(" + WORLD + POS + STATE + ")V");
        Type[] args = Type.getArgumentTypes(settle.desc);
        LabelNode vanilla = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 2));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "keepsWaterSource",
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
        substituteFluidStates(method, limit, "fluidState");
    }

    private static void substituteFluidStates(MethodNode method, int limit, String hook) {
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
                    hook, "(L" + call.owner + ";" + call.desc.substring(1), false));
            if (++replaced == limit) return;
        }
        throw new IllegalStateException("Could not locate Forge 1.12 liquid block state reads in " + method.name);
    }
}
