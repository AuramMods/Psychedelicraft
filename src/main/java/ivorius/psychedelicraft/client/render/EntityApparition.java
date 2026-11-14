/*
 * Copyright (c) 2014, Lukas Tenbrink.
 */

package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.entity.drug.HallucinationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

final class EntityApparition {
    private final ResourceLocation species;
    private final Entity entity;
    private final ApparitionModels model;
    private final Vec3d velocity;
    private final Vec3d[] offsets;
    private final float rotationSpeed;
    private final float red;
    private final float green;
    private final float blue;
    private final float scale;
    private final int maxAge;
    private Vec3d previousPosition;
    private Vec3d position;
    private float previousYaw;
    private float yaw;
    private int age;

    EntityApparition(EntityPlayerSP player, HallucinationType type, @Nullable Vec3d commandedPosition) {
        boolean grouped = type == HallucinationType.MULTIPLE_ENTITY;
        species = ApparitionPool.choose(grouped, player.world.rand);
        model = ApparitionModels.forId(species);
        entity = model == null ? createEntity(player, species) : null;

        Random random = player.getRNG();
        position = player.getPositionVector().addVector(random.nextDouble() * 50D - 25D,
                random.nextDouble() * 10D - 5D, random.nextDouble() * 50D - 25D);
        if (commandedPosition != null) position = commandedPosition;
        previousPosition = position;
        velocity = new Vec3d((random.nextDouble() - 0.5D) / 10D, (random.nextDouble() - 0.5D) / 10D,
                (random.nextDouble() - 0.5D) / 10D);
        previousYaw = yaw = random.nextInt(360);
        maxAge = (random.nextInt(59) + 3) * 20;
        rotationSpeed = random.nextFloat() * 10 * (random.nextBoolean() ? 0 : 1);
        red = random.nextFloat();
        green = random.nextFloat();
        blue = random.nextFloat();
        float size = 1F;
        while (random.nextFloat() < 0.3F) size *= random.nextFloat() * 2.7F + 0.3F;
        scale = Math.min(size, 20F);

        offsets = new Vec3d[grouped ? random.nextInt(11) + 5 : 1];
        float width = model == null ? entity.width : model.width;
        for (int i = 0; i < offsets.length; i++) {
            double spread = Math.max(2, i / 10F) * width;
            offsets[i] = grouped ? new Vec3d(triangle(random, spread), triangle(random, spread), triangle(random, spread)) : Vec3d.ZERO;
        }
        if (entity != null) {
            entity.motionX = velocity.x;
            entity.motionY = velocity.y;
            entity.motionZ = velocity.z;
            updateEntityTransform();
        }
    }

    private static Entity createEntity(EntityPlayerSP player, ResourceLocation id) {
        Entity result = EntityList.createEntityByIDFromName(ApparitionPool.legacyId(id), player.world);
        return result == null ? new EntityPig(player.world) : result;
    }

    private static double triangle(Random random, double spread) {
        return (random.nextDouble() - random.nextDouble()) * spread;
    }

    boolean tick() {
        age++;
        previousPosition = position;
        position = position.add(velocity);
        previousYaw = yaw;
        yaw = MathHelper.wrapDegrees(yaw + rotationSpeed);
        if (entity != null) updateEntity();
        return age >= maxAge;
    }

    private void updateEntityTransform() {
        entity.ticksExisted = age;
        entity.setPosition(position.x, position.y, position.z);
        entity.prevPosX = entity.lastTickPosX = previousPosition.x;
        entity.prevPosY = entity.lastTickPosY = previousPosition.y;
        entity.prevPosZ = entity.lastTickPosZ = previousPosition.z;
        entity.prevRotationYaw = previousYaw;
        entity.rotationYaw = yaw;
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            living.prevRenderYawOffset = living.prevRotationYawHead = previousYaw;
            living.renderYawOffset = living.rotationYawHead = yaw;
        }
    }

    private void updateEntity() {
        updateEntityTransform();
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            double dx = entity.posX - entity.prevPosX;
            double dz = entity.posZ - entity.prevPosZ;
            float stride = Math.min(1F, MathHelper.sqrt(dx * dx + dz * dz) * 4F);
            living.prevLimbSwingAmount = living.limbSwingAmount;
            living.limbSwingAmount += (stride - living.limbSwingAmount) * 0.4F;
            living.limbSwing += living.limbSwingAmount;
        }
    }

    void render(Minecraft client, float partialTicks, float strength) {
        float progress = (float) Math.min(age, maxAge - 2) / (maxAge - 2);
        float alpha = Math.min(1F, MathHelper.sin(progress * (float) Math.PI) * 18F) * strength;
        if (alpha <= 0.001F) return;

        RenderManager manager = client.getRenderManager();
        Vec3d center = previousPosition.add(position.subtract(previousPosition).scale(partialTicks))
                .subtract(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ);
        float facing = previousYaw + MathHelper.wrapDegrees(yaw - previousYaw) * partialTicks;
        if (offsets.length > 1) {
            Arrays.sort(offsets, Comparator.comparingDouble((Vec3d offset) -> distanceSquared(center, offset)).reversed());
        }
        if (entity == null) ApparitionRender.drawModel(model, offsets, center, facing, age + partialTicks,
                velocity.x * velocity.x + velocity.z * velocity.z, scale, red, green, blue, alpha);
        else ApparitionRender.drawEntity(client, entity, offsets, center, facing, partialTicks, scale, red, green, blue, alpha);
    }

    private double distanceSquared(Vec3d center, Vec3d offset) {
        double x = center.x + offset.x * scale;
        double y = center.y + offset.y * scale;
        double z = center.z + offset.z * scale;
        return x * x + y * y + z * z;
    }
}
