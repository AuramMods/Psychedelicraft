package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class FurnaceRecipeTransformer implements IClassTransformer {
    private static final String FURNACE_RECIPES = "net.minecraft.item.crafting.FurnaceRecipes";
    private static final String FURNACE_TILE = "net.minecraft.tileentity.TileEntityFurnace";
    private static final String RESULT_DESCRIPTOR = "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;";
    private static final String OUTPUT_SLOT = "net.minecraft.inventory.SlotFurnaceOutput";
    private static final String RECIPES = "ivorius/psychedelicraft/recipe/FurnaceRecipe";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(FURNACE_RECIPES.equals(transformedName) || FURNACE_TILE.equals(transformedName)
                || OUTPUT_SLOT.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (FURNACE_RECIPES.equals(transformedName)) {
            addRecipeLookup(ClientEffectsTransformer.method(type, "getSmeltingResult", "func_151395_a", RESULT_DESCRIPTOR));
            filterReturns(ClientEffectsTransformer.method(type, "getSmeltingExperience", "func_151398_b",
                    "(Lnet/minecraft/item/ItemStack;)F"), Opcodes.FRETURN, Type.FLOAT_TYPE, "recipeExperience");
        } else if (FURNACE_TILE.equals(transformedName)) {
            patchFuelConsumption(type);
            patchFurnace(type);
        } else {
            patchOutputSlot(type);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchFurnace(ClassNode type) {
        MethodNode duration = ClientEffectsTransformer.method(type, "getCookTime", "func_174904_a", "(Lnet/minecraft/item/ItemStack;)I");
        filterReturns(duration, Opcodes.IRETURN, Type.INT_TYPE, "cookTime");
        MethodNode read = ClientEffectsTransformer.method(type, "readFromNBT", "func_145839_a", "(Lnet/minecraft/nbt/NBTTagCompound;)V");
        for (AbstractInsnNode instruction : read.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.RETURN) {
                read.instructions.insertBefore(instruction, furnaceHook(type, "restoreDuration"));
            }
        }

        MethodNode canSmelt = ClientEffectsTransformer.method(type, "canSmelt", "func_145948_k", "()Z");
        MethodInsnNode equal = findCall(canSmelt, "net/minecraft/item/ItemStack", "isItemEqual", "func_77969_a",
                "(Lnet/minecraft/item/ItemStack;)Z");
        canSmelt.instructions.insertBefore(equal, new VarInsnNode(Opcodes.ALOAD, 0));
        canSmelt.instructions.set(equal, new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPES, "canMerge",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getObjectType(equal.owner),
                        Type.getArgumentTypes(equal.desc)[0], Type.getObjectType(type.name)), false));

        MethodNode smelt = ClientEffectsTransformer.method(type, "smeltItem", "func_145949_j", "()V");
        MethodInsnNode shrink = findCall(smelt, "net/minecraft/item/ItemStack", "shrink", "func_190918_g", "(I)V");
        smelt.instructions.insertBefore(shrink, furnaceHook(type, "recordOutput"));
    }

    private static void patchOutputSlot(ClassNode type) {
        MethodNode take = ClientEffectsTransformer.method(type, "onCrafting", "func_75208_c", "(Lnet/minecraft/item/ItemStack;)V");
        MethodInsnNode experience = findCall(take, "net/minecraft/item/crafting/FurnaceRecipes", "getSmeltingExperience", "func_151398_b",
                "(Lnet/minecraft/item/ItemStack;)F");
        String inventory = "net/minecraft/inventory/IInventory";
        String field = Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment")) ? "inventory" : "field_75224_c";
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, field, "L" + inventory + ";"));
        take.instructions.insertBefore(experience, hook);
        take.instructions.set(experience, new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPES, "takenExperience",
                Type.getMethodDescriptor(Type.FLOAT_TYPE, Type.getObjectType(experience.owner),
                        Type.getArgumentTypes(experience.desc)[0], Type.getObjectType(inventory)), false));
    }

    private static InsnList furnaceHook(ClassNode type, String name) {
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPES, name, "(L" + type.name + ";)V", false));
        return hook;
    }

    private static void filterReturns(MethodNode method, int opcode, Type result, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != opcode) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
            hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPES, name,
                    Type.getMethodDescriptor(result, result, Type.getArgumentTypes(method.desc)[0]), false));
            method.instructions.insertBefore(instruction, hook);
        }
    }

    private static MethodInsnNode findCall(MethodNode method, String owner, String mcp, String srg, String descriptor) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (owner.equals(remapper.map(call.owner)) && descriptor.equals(remapper.mapMethodDesc(call.desc))
                    && (mcp.equals(name) || srg.equals(name))) return call;
        }
        throw new IllegalStateException("Could not locate " + mcp + " in " + method.name);
    }

    private static void patchFuelConsumption(ClassNode type) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            String name = remapper.mapMethodName(type.name, method.name, method.desc);
            if (!"()V".equals(method.desc) || !("update".equals(name) || "func_73660_a".equals(name))) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                String target = remapper.mapMethodName(call.owner, call.name, call.desc);
                if ("net/minecraft/item/ItemStack".equals(remapper.map(call.owner)) && "(I)V".equals(call.desc)
                        && ("shrink".equals(target) || "func_190918_g".equals(target))) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/entity/drug/SmokeEffects",
                            "fuelConsumed", "(L" + type.name + ";)V", false));
                    method.instructions.insertBefore(instruction, hook);
                    return;
                }
            }
        }
        throw new IllegalStateException("Could not locate Forge 1.12 furnace fuel consumption");
    }

    private static void addRecipeLookup(MethodNode method) {
        LabelNode vanilla = new LabelNode();
        String stackType = Type.getReturnType(method.desc).getInternalName();
        InsnList lookup = new InsnList();
        lookup.add(new VarInsnNode(Opcodes.ALOAD, 1));
        lookup.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPES, "lookup", method.desc, false));
        lookup.add(new InsnNode(Opcodes.DUP));
        lookup.add(new JumpInsnNode(Opcodes.IFNULL, vanilla));
        lookup.add(new InsnNode(Opcodes.ARETURN));
        lookup.add(vanilla);
        lookup.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{stackType}));
        lookup.add(new InsnNode(Opcodes.POP));
        method.instructions.insert(lookup);
    }
}
