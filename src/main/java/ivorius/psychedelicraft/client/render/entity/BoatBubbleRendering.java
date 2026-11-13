package ivorius.psychedelicraft.client.render.entity;

import ivorius.psychedelicraft.entity.BoatBubbleColumn;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityBoat;

public final class BoatBubbleRendering {
    private BoatBubbleRendering() {
    }

    public static void rotate(EntityBoat boat, float yaw, float partialTicks) {
        float angle = ((BoatBubbleColumn.Carrier) boat).psychedelicraft$bubbleColumn().angle(partialTicks);
        if (angle != 0) {
            GlStateManager.rotate(180 - yaw, 0, 1, 0);
            GlStateManager.rotate(angle, 1, 0, 1);
            GlStateManager.rotate(yaw - 180, 0, 1, 0);
        }
    }
}
