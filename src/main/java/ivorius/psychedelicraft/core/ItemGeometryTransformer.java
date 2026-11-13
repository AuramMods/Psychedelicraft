package ivorius.psychedelicraft.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static ivorius.psychedelicraft.core.ClientEffectsTransformer.method;

public final class ItemGeometryTransformer implements IClassTransformer {
    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String ITEM_RENDERER = "net.minecraft.client.renderer.ItemRenderer";
    private static final String RENDER_ITEM = "net.minecraft.client.renderer.RenderItem";
    private static final String FORGE_HOOKS = "net.minecraftforge.client.ForgeHooksClient";
    private static final String RENDER_PLAYER = "net.minecraft.client.renderer.entity.RenderPlayer";
    private static final String MODEL_RENDERER = "net.minecraft.client.model.ModelRenderer";
    private static final String MAP_INSTANCE = "net.minecraft.client.gui.MapItemRenderer$Instance";
    private static final String STACK = "Lnet/minecraft/item/ItemStack;";
    private static final String BAKED_MODEL = "Lnet/minecraft/client/renderer/block/model/IBakedModel;";
    private static final String PLAYER = "Lnet/minecraft/client/entity/AbstractClientPlayer;";
    private static final String TESSELLATOR = "net/minecraft/client/renderer/Tessellator";
    private static final String HOOKS = "ivorius/psychedelicraft/client/render/DrugRenderer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) return bytes;
        switch (transformedName) {
            case ENTITY_RENDERER:
            case ITEM_RENDERER:
            case RENDER_ITEM:
            case FORGE_HOOKS:
            case RENDER_PLAYER:
            case MODEL_RENDERER:
            case MAP_INSTANCE:
                break;
            default:
                return bytes;
        }

        ClassNode type = new ClassNode(Opcodes.ASM5);
        new ClassReader(bytes).accept(type, 0);
        switch (transformedName) {
            case ENTITY_RENDERER:
                replaceDraws(method(type, "renderHand", "func_78476_b", "(FI)V"), ITEM_RENDERER.replace('.', '/'),
                        "renderItemInFirstPerson", "func_78440_a", "(F)V", "renderFirstPersonItems");
                break;
            case ITEM_RENDERER:
                replaceDraws(method(type, "renderMapFirstPerson", "func_187461_a", "(" + STACK + ")V"),
                        TESSELLATOR, "draw", "func_78381_a", "()V", "drawMapQuads");
                break;
            case RENDER_ITEM:
                patchItemBatch(method(type, "renderModel", "func_191967_a", "(" + BAKED_MODEL + "I" + STACK + ")V"), 3);
                replaceDraws(method(type, "renderItem", "func_180454_a", "(" + STACK + BAKED_MODEL + ")V"),
                        "net/minecraft/client/renderer/tileentity/TileEntityItemStackRenderer",
                        "renderByItem", "func_179022_a", "(" + STACK + ")V", "renderBuiltInItem");
                break;
            case FORGE_HOOKS:
                patchItemBatch(method(type, "drawSegment", "drawSegment",
                        "(Lnet/minecraft/client/renderer/RenderItem;I" + STACK + "Ljava/util/List;IIZZZ)V"), 2);
                break;
            case RENDER_PLAYER:
                patchArm(method(type, "renderRightArm", "func_177138_b", "(" + PLAYER + ")V"));
                patchArm(method(type, "renderLeftArm", "func_177139_c", "(" + PLAYER + ")V"));
                break;
            case MODEL_RENDERER:
                patchDisplayLists(method(type, "render", "func_78785_a", "(F)V"));
                patchDisplayLists(method(type, "renderWithRotation", "func_78791_b", "(F)V"));
                break;
            case MAP_INSTANCE:
                replaceDraws(method(type, "render", "func_148237_a", "(Z)V"),
                        TESSELLATOR, "draw", "func_78381_a", "()V", "drawMapQuads");
                break;
            default:
                throw new AssertionError(transformedName);
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        type.accept(writer);
        return writer.toByteArray();
    }

    private static void patchArm(MethodNode arm) {
        replaceDraws(arm, MODEL_RENDERER.replace('.', '/'), "render", "func_78785_a", "(F)V", "drawHandPart");
    }

    private static void patchDisplayLists(MethodNode model) {
        replaceDraws(model, "net/minecraft/client/renderer/GlStateManager", "callList", "func_179148_o",
                "(I)V", "drawBuiltInModelList");
    }

    private static void patchItemBatch(MethodNode batch, int stackSlot) {
        String stack = Type.getArgumentTypes(batch.desc)[2].getDescriptor();
        for (AbstractInsnNode instruction : batch.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!matches(call, TESSELLATOR, "draw", "func_78381_a", "()V")) continue;
            batch.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, stackSlot));
            batch.instructions.set(call, hook("drawItemQuads", "(L" + call.owner + ";" + stack + ")V"));
            return;
        }
        throw new IllegalStateException("Could not locate item batch draw in " + batch.name);
    }

    private static void replaceDraws(MethodNode method, String owner, String name, String srg, String descriptor, String hook) {
        boolean patched = false;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!matches(call, owner, name, srg, descriptor)) continue;
            String target = call.getOpcode() == Opcodes.INVOKESTATIC ? call.desc : "(L" + call.owner + ";" + call.desc.substring(1);
            method.instructions.set(call, hook(hook, target));
            patched = true;
        }
        if (!patched) throw new IllegalStateException("Could not locate " + name + " in " + method.name);
    }

    private static boolean matches(MethodInsnNode call, String owner, String name, String srg, String descriptor) {
        FMLDeobfuscatingRemapper remapper = FMLDeobfuscatingRemapper.INSTANCE;
        String mapped = remapper.mapMethodName(call.owner, call.name, call.desc);
        return owner.equals(remapper.map(call.owner)) && (name.equals(mapped) || srg.equals(mapped))
                && descriptor.equals(remapper.mapMethodDesc(call.desc));
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}
