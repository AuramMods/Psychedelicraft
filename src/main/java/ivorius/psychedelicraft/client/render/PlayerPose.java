/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Scope the pose after native animation, including armor layers, then restore shared models. */
public final class PlayerPose implements AutoCloseable {
    public static final PlayerPose EMPTY = new PlayerPose(new ModelPart[0]);
    private final ModelPart[] parts;
    private final float[] transforms;

    PlayerPose(ModelPart... parts) {
        this.parts = parts;
        transforms = new float[parts.length * 9];
        for (int i = 0; i < parts.length; i++) {
            var p = parts[i]; int j = i * 9;
            transforms[j] = p.xRot; transforms[j + 1] = p.yRot; transforms[j + 2] = p.zRot;
            transforms[j + 3] = p.x; transforms[j + 4] = p.y; transforms[j + 5] = p.z;
            transforms[j + 6] = p.xScale; transforms[j + 7] = p.yScale; transforms[j + 8] = p.zScale;
        }
    }

    public static PlayerPose apply(EntityModel<?> model, LivingEntity entity, float tick) {
        if (!(entity instanceof Player player)
                || !(model instanceof HumanoidModel<?> humanoid)) return EMPTY;
        var drugs = DrugCapability.get(player);
        float view = drugs.blendStrength(0, Drug::viewTrembleStrength);
        float hand = drugs.blendStrength(0, Drug::handTrembleStrength);
        float alcohol = Math.min(1, (float)(drugs.getActiveValue(DrugType.ALCOHOL) + drugs.getActiveValue(DrugType.KAVA)));
        if (view == 0 && hand == 0 && alcohol == 0) return EMPTY;
        PlayerPose saved = model instanceof PlayerModel<?> skin
                ? new PlayerPose(humanoid.head, humanoid.hat, humanoid.leftArm, humanoid.rightArm, skin.leftSleeve, skin.rightSleeve)
                : new PlayerPose(humanoid.head, humanoid.hat, humanoid.leftArm, humanoid.rightArm);
        float noise = CameraMotion.noise(tick), shift = CameraMotion.vertical(tick, view);
        humanoid.head.xRot += noise * .05F * view;
        humanoid.head.yRot += shift;
        humanoid.head.zRot = alcohol;
        humanoid.leftArm.xRot += noise * .03F * hand;
        humanoid.rightArm.xRot -= noise * .03F * hand;
        humanoid.leftArm.zRot += shift * .3F;
        humanoid.rightArm.zRot -= shift * .3F;
        humanoid.hat.copyFrom(humanoid.head);
        if (model instanceof PlayerModel<?> skin) {
            skin.leftSleeve.copyFrom(humanoid.leftArm);
            skin.rightSleeve.copyFrom(humanoid.rightArm);
        }
        return saved;
    }

    @Override public void close() {
        for (int i = 0; i < parts.length; i++) {
            var p = parts[i]; int j = i * 9;
            p.xRot = transforms[j]; p.yRot = transforms[j + 1]; p.zRot = transforms[j + 2];
            p.x = transforms[j + 3]; p.y = transforms[j + 4]; p.z = transforms[j + 5];
            p.xScale = transforms[j + 6]; p.yScale = transforms[j + 7]; p.zScale = transforms[j + 8];
        }
    }
}
