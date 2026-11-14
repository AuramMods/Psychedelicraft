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

public final class VillagerCareersTransformer implements IClassTransformer {
    private static final String VILLAGER = "net.minecraft.entity.passive.EntityVillager";
    private static final String ZOMBIE = "net.minecraft.entity.monster.EntityZombieVillager";
    private static final String ZOMBIE_ATTACKER = "net.minecraft.entity.monster.EntityZombie";
    private static final String VILLAGE = "net.minecraft.world.gen.structure.StructureVillagePieces$Village";
    private static final String RENDERER = "net.minecraft.client.renderer.entity.RenderLivingBase";
    private static final String CONVERSATION = "net.minecraft.entity.ai.EntityAIVillagerInteract";
    private static final String SIEGE = "net.minecraft.village.VillageSiege";
    private static final String FORGE_REGISTRY = "net/minecraftforge/fml/common/registry/VillagerRegistry";
    private static final String CAREERS = "ivorius/psychedelicraft/entity/VillagerCareers";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !(VILLAGER.equals(transformedName) || ZOMBIE.equals(transformedName)
                || ZOMBIE_ATTACKER.equals(transformedName) || VILLAGE.equals(transformedName)
                || RENDERER.equals(transformedName) || CONVERSATION.equals(transformedName)
                || SIEGE.equals(transformedName))) return bytes;
        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        if (RENDERER.equals(transformedName)) {
            patchShake(type);
        } else if (CONVERSATION.equals(transformedName)) {
            patchConversation(type);
        } else if (SIEGE.equals(transformedName)) {
            patchSiege(type);
        } else if (ZOMBIE_ATTACKER.equals(transformedName)) {
            MethodNode kill = find(type, "onKillEntity", "func_70074_a", "(Lnet/minecraft/entity/EntityLivingBase;)V");
            patchConversionSpawn(type, kill, 1, "spawnInfected", null);
        } else {
            patchRandomSpawns(type);
            if (VILLAGER.equals(transformedName)) {
                prependHandler(type, "useRecipe", "func_70933_a", "(Lnet/minecraft/village/MerchantRecipe;)V", "useRecipe");
                prependHandler(type, "updateEquipmentIfNeeded", "func_175445_a", "(Lnet/minecraft/entity/item/EntityItem;)V", "collectDealerItem");
                patchCustomer(type);
                patchLightningConversion(type);
            }
            if (ZOMBIE.equals(transformedName)) {
                MethodNode cure = find(type, "finishConversion", "func_190738_dp", "()V");
                FieldNode healer = ClientEffectsTransformer.field(type, "converstionStarter", "field_191992_by");
                patchConversionSpawn(type, cure, 0, "spawnCured", healer);
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchLightningConversion(ClassNode type) {
        MethodNode lightning = find(type, "onStruckByLightning", "func_70077_a",
                "(Lnet/minecraft/entity/effect/EntityLightningBolt;)V");
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : lightning.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!("setDead".equals(name) || "func_70106_y".equals(name)) || !"()V".equals(call.desc)) continue;
            InsnList release = new InsnList();
            release.add(new VarInsnNode(Opcodes.ALOAD, 0));
            release.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CAREERS, "releaseWorksite",
                    "(L" + type.name + ";)V", false));
            lightning.instructions.insert(call, release);
            return;
        }
        throw new IllegalStateException("Could not locate the villager removal after lightning conversion");
    }

    private static void patchSiege(ClassNode type) {
        MethodNode method = find(type, "spawnZombie", "func_75530_c", "()Z");
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            String name = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!("spawnEntity".equals(name) || "func_72838_d".equals(name))
                    || !"(Lnet/minecraft/entity/Entity;)Z".equals(remapper.mapMethodDesc(call.desc))) continue;
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "ivorius/psychedelicraft/entity/VillageSiegeReward", "spawnAttacker",
                    "(L" + call.owner + ";" + call.desc.substring(1), false));
            return;
        }
        throw new IllegalStateException("Could not locate the village siege spawn");
    }

    private static void patchCustomer(ClassNode type) {
        MethodNode method = find(type, "setCustomer", "func_70932_a_", "(Lnet/minecraft/entity/player/EntityPlayer;)V");
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/entity/VillagerPrices", "customerChanged",
                "(L" + type.name + ";" + method.desc.substring(1), false));
        method.instructions.insert(hook);
    }

    private static void patchConversation(ClassNode type) {
        FieldNode villager = ClientEffectsTransformer.field(type, "villager", "field_179477_f");
        MethodNode start = find(type, "startExecuting", "func_75249_e", "()V");
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, villager.name, villager.desc));
        hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/entity/VillagerReputation", "talk",
                "(" + villager.desc + "L" + type.name + ";)V", false));
        start.instructions.insert(hook);
    }

    private static void patchConversionSpawn(ClassNode type, MethodNode convert, int sourceSlot, String hook, FieldNode healer) {
        Type source = sourceSlot == 0 ? Type.getObjectType(type.name) : Type.getArgumentTypes(convert.desc)[0];
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (AbstractInsnNode insn : convert.instructions.toArray()) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) insn;
            String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
            if (!("spawnEntity".equals(mapped) || "func_72838_d".equals(mapped))
                    || !"(Lnet/minecraft/entity/Entity;)Z".equals(remapper.mapMethodDesc(call.desc))) continue;
            InsnList arguments = new InsnList();
            arguments.add(new VarInsnNode(Opcodes.ALOAD, sourceSlot));
            Type world = Type.getObjectType(call.owner);
            Type replacement = Type.getArgumentTypes(call.desc)[0];
            String descriptor;
            if (healer == null) {
                descriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, world, replacement, source);
            } else {
                arguments.add(new VarInsnNode(Opcodes.ALOAD, sourceSlot));
                arguments.add(new FieldInsnNode(Opcodes.GETFIELD, type.name, healer.name, healer.desc));
                descriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, world, replacement, source, Type.getType(healer.desc));
            }
            convert.instructions.insertBefore(call, arguments);
            convert.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, CAREERS, hook, descriptor, false));
            return;
        }
        throw new IllegalStateException("Could not locate the conversion spawn in " + type.name + "." + convert.name);
    }

    private static void patchRandomSpawns(ClassNode type) {
        for (MethodNode method : type.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (FORGE_REGISTRY.equals(call.owner) && "setRandomProfession".equals(call.name)) call.owner = CAREERS;
            }
        }
    }

    private static void prependHandler(ClassNode type, String name, String srg, String descriptor, String hook) {
        MethodNode method = find(type, name, srg, descriptor);
        String handler = "(L" + type.name + ";" + method.desc.substring(1, method.desc.length() - 1) + "Z";
        LabelNode vanilla = new LabelNode();
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CAREERS, hook, handler, false));
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        instructions.add(new InsnNode(Opcodes.RETURN));
        instructions.add(vanilla);
        instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(instructions);
    }

    private static void patchShake(ClassNode type) {
        MethodNode method = find(type, "applyRotations", "func_77043_a", "(Lnet/minecraft/entity/EntityLivingBase;FFF)V");
        Type entity = Type.getArgumentTypes(method.desc)[0];
        InsnList instructions = new InsnList();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new VarInsnNode(Opcodes.FLOAD, 3));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ivorius/psychedelicraft/client/render/VillagerClothes",
                "shake", Type.getMethodDescriptor(Type.FLOAT_TYPE, entity, Type.FLOAT_TYPE), false));
        instructions.add(new VarInsnNode(Opcodes.FSTORE, 3));
        method.instructions.insert(instructions);
    }

    private static MethodNode find(ClassNode type, String name, String srg, String descriptor) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        for (MethodNode method : type.methods) {
            String mapped = remapper.mapMethodName(type.name, method.name, method.desc);
            if ((name.equals(mapped) || srg.equals(mapped)) && descriptor.equals(remapper.mapMethodDesc(method.desc))) return method;
        }
        throw new IllegalStateException("Could not locate " + type.name + "." + name);
    }
}
