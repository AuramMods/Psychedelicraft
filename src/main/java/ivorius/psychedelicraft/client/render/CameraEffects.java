/* Copyright (c) 2014, Lukas Tenbrink. Updated by Sollace on 4 Jan 2023. */
package ivorius.psychedelicraft.client.render;
import ivorius.psychedelicraft.client.ClientDrugState;

import com.mojang.blaze3d.vertex.PoseStack;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;

/** One connection's camera state. Replaced on world/player changes and logout. */
public final class CameraEffects {
    private final DriftingCamera drift = new DriftingCamera();
    private final CameraInertia inertia = new CameraInertia();

    public void tick(Minecraft client) {
        var player = client.player;
        if (player == null) return;
        var drugs = ClientDrugState.get(player);
        if (!client.isPaused()) {
            var movement = player.getDeltaMovement();
            drift.update(drugs.getModifier(Drug::weightlessness), player.tickCount, movement.x, movement.z, player.getRandom());
        }
        boolean acceptsInput = !client.isPaused() && client.isWindowActive() && client.mouseHandler.isMouseGrabbed();
        inertia.tick(acceptsInput ? drugs.getModifier(Drug::headMotionInertia) : 0, sensitivity(client));
    }

    public void view(PoseStack pose, Camera camera, float partial) {
        var client = Minecraft.getInstance();
        if (!ClientDrugState.alive(client.player) || camera.getEntity() != client.player) return;
        var player = client.player;
        var drugs = ClientDrugState.get(player);
        var position = drift.position(partial);
        var rotation = drift.rotation(partial);
        float wobble = Math.min(1, drugs.getModifier(Drug::viewWobblyness));
        float tremble = drugs.blendStrength(0, Drug::viewTrembleStrength);
        if (position.lengthSqr() == 0 && rotation.lengthSqr() == 0 && wobble == 0 && tremble == 0) return;
        var view = CameraMotion.view(pose.last().pose(), position, rotation, wobble, tremble,
                player.tickCount + partial, client.options.getCameraType().isMirrored() && !player.isSleeping());
        pose.last().pose().set(view);
        // The normal matrix and Forge's inverse view rotation must see the same transform.
        pose.last().normal().set(new Matrix3f(view).invert().transpose());
    }

    public void turn(LocalPlayer player, double nativeX, double nativeY, double rawX, double rawY) {
        var client = Minecraft.getInstance();
        if (player == client.player && !client.isPaused() && ClientDrugState.get(player).getModifier(Drug::headMotionInertia) > 0) {
            float speed = sensitivity(client);
            var change = inertia.sample((float)rawX * speed, (float)rawY * speed, client.getFrameTime());
            int invert = client.options.invertYMouse().get() ? -1 : 1;
            if (!client.options.smoothCamera) nativeX = nativeY = 0;
            nativeX += change.x;
            nativeY += change.y * invert;
        }
        player.turn(nativeX, nativeY);
    }

    public boolean outOfBody(Entity cameraEntity) {
        var client = Minecraft.getInstance();
        return client.player != null && cameraEntity == client.player && client.getCameraEntity() == client.player
                && ClientDrugState.get(client.player).getModifier(Drug::weightlessness) > 0 && drift.isOutOfBody();
    }

    public static void hand(PoseStack pose, LocalPlayer player, float partial) {
        var drugs = ClientDrugState.get(player);
        float tick = player.tickCount + partial;
        pose.translate(CameraMotion.noise(tick) * .015F * drugs.blendStrength(0, Drug::handTrembleStrength),
                CameraMotion.vertical(tick, drugs.blendStrength(0, Drug::viewTrembleStrength)) * .3F, 0);
    }

    private static float sensitivity(Minecraft client) {
        float speed = CameraInertia.sensitivity(client.options.sensitivity().get().floatValue());
        return client.options.getCameraType().isFirstPerson() && client.player.isScoping() ? speed / 8 : speed;
    }
}
