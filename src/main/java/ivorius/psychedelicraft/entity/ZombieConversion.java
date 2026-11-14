package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class ZombieConversion {
    private static final String SUBMERGED = "psychedelicraftZombieSubmergedTicks";
    private static final String REMAINING = "psychedelicraftZombieConversionTicks";
    private static DataParameter<Boolean> shaking;

    private ZombieConversion() {}

    private static boolean convertsInWater(Entity entity) {
        return entity.getClass() == EntityZombie.class || entity.getClass() == EntityHusk.class;
    }

    public static void reserveSyncedState() {
        shaking = EntityDataManager.createKey(EntityZombie.class, DataSerializers.BOOLEAN);
    }

    public static boolean breathesUnderwater(EntityLivingBase entity) {
        return convertsInWater(entity) || entity instanceof EntityZombieVillager;
    }

    @SubscribeEvent
    public static void registerState(EntityEvent.EntityConstructing event) {
        Entity entity = event.getEntity();
        if (convertsInWater(entity)) entity.getDataManager().register(shaking, false);
    }

    @SubscribeEvent
    public static void restoreState(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!event.getWorld().isRemote && convertsInWater(entity)) {
            entity.getDataManager().set(shaking, entity.getEntityData().hasKey(REMAINING));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || !convertsInWater(entity) || !entity.isEntityAlive()) return;
        EntityZombie zombie = (EntityZombie) entity;
        if (zombie.isAIDisabled()) return;

        NBTTagCompound state = zombie.getEntityData();
        boolean wasConverting = state.hasKey(REMAINING);
        if (advance(state, wasConverting || zombie.isInsideOfMaterial(Material.WATER))) {
            if (convert(zombie)) event.setCanceled(true);
        } else if (!wasConverting && state.hasKey(REMAINING)) {
            zombie.getDataManager().set(shaking, true);
        }
    }

    static boolean advance(NBTTagCompound state, boolean submerged) {
        if (state.hasKey(REMAINING)) {
            int remaining = state.getInteger(REMAINING);
            if (remaining == 0) return true;
            state.setInteger(REMAINING, remaining - 1);
        } else if (submerged) {
            int submergedTicks = state.getInteger(SUBMERGED) + 1;
            if (submergedTicks >= 600) {
                state.removeTag(SUBMERGED);
                state.setInteger(REMAINING, 300);
            } else {
                state.setInteger(SUBMERGED, submergedTicks);
            }
        } else {
            state.removeTag(SUBMERGED);
        }
        return false;
    }

    public static float shake(float yaw, EntityLivingBase entity) {
        if (!convertsInWater(entity) || !entity.getDataManager().get(shaking)) return yaw;
        return yaw + (float) (Math.cos(entity.ticksExisted * 3.25) * Math.PI * 0.4);
    }

    private static boolean convert(EntityZombie zombie) {
        World world = zombie.world;
        boolean husk = zombie instanceof EntityHusk;
        EntityZombie replacement = husk ? new EntityZombie(world) : new DrownedEntity(world);
        replacement.copyLocationAndAnglesFrom(zombie);
        replacement.setChild(zombie.isChild());
        replacement.setCanPickUpLoot(zombie.canPickUpLoot());
        replacement.setEntityInvulnerable(zombie.getIsInvulnerable());
        if (zombie.isNoDespawnRequired()) replacement.enablePersistence();
        if (zombie.hasCustomName()) {
            replacement.setCustomNameTag(zombie.getCustomNameTag());
            replacement.setAlwaysRenderNameTag(zombie.getAlwaysRenderNameTag());
        }
        replacement.setBreakDoorsAItask(husk && zombie.isBreakDoorsTaskSet());

        NBTTagCompound saved = new NBTTagCompound();
        zombie.writeEntityToNBT(saved);
        NBTTagList handChances = saved.getTagList("HandDropChances", Constants.NBT.TAG_FLOAT);
        NBTTagList armorChances = saved.getTagList("ArmorDropChances", Constants.NBT.TAG_FLOAT);
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (zombie.getItemStackFromSlot(slot).isEmpty()) continue;
            replacement.setItemStackToSlot(slot, zombie.getItemStackFromSlot(slot).copy());
            NBTTagList chances = slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? handChances : armorChances;
            replacement.setDropChance(slot, chances.getFloatAt(slot.getIndex()));
        }
        // Conversion uses the new mob's attributes and keeps the old gear without re-rolling it.
        replacement.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE)
                .applyModifier(new AttributeModifier("Random spawn bonus", world.rand.nextDouble() * 0.05, 0));
        float localDifficulty = world.getDifficultyForLocation(zombie.getPosition()).getClampedAdditionalDifficulty();
        double rangeBonus = world.rand.nextDouble() * 1.5 * localDifficulty;
        if (rangeBonus > 1) {
            replacement.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE)
                    .applyModifier(new AttributeModifier("Random zombie-spawn bonus", rangeBonus, 2));
        }
        if (world.rand.nextFloat() < localDifficulty * 0.05F) {
            replacement.getAttributeMap().getAttributeInstanceByName("zombie.spawnReinforcements")
                    .applyModifier(new AttributeModifier("Leader zombie bonus", world.rand.nextDouble() * 0.25 + 0.5, 0));
            replacement.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                    .applyModifier(new AttributeModifier("Leader zombie bonus", world.rand.nextDouble() * 3 + 1, 2));
        }
        if (!world.spawnEntity(replacement)) return false;

        Entity vehicle = zombie.getRidingEntity();
        if (vehicle != null) {
            zombie.dismountRidingEntity();
            replacement.startRiding(vehicle, true);
        }
        zombie.setDead();
        if (!zombie.isSilent()) {
            float pitch = 1 + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F;
            replacement.playSound(husk ? PSSounds.HUSK_CONVERT : PSSounds.ZOMBIE_CONVERT, 2, pitch);
        }
        return true;
    }
}
