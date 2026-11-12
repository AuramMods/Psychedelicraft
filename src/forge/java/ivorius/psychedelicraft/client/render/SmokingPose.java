package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.item.PowderItem;
import ivorius.psychedelicraft.item.SmokingItem;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class SmokingPose {
    private SmokingPose() {
    }

    public static void apply(ModelBiped model, Entity entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (!player.isHandActive()) return;
        Item item = player.getActiveItemStack().getItem();
        if (!(item instanceof SmokingItem || item instanceof PowderItem)) return;

        boolean right = (player.getPrimaryHand() == EnumHandSide.RIGHT) == (player.getActiveHand() == EnumHand.MAIN_HAND);
        ModelRenderer arm = right ? model.bipedRightArm : model.bipedLeftArm;
        arm.rotateAngleX = MathHelper.clamp(model.bipedHead.rotateAngleX, -1.2F, 1.2F) - 1.4835298F;
        arm.rotateAngleY = model.bipedHead.rotateAngleY + (right ? -0.5235988F : 0.5235988F);
    }
}
