package ivorius.psychedelicraft.entity;

import ivorius.psychedelicraft.PSSounds;
import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.item.StewFlower;
import ivorius.psychedelicraft.item.SuspiciousStewItem;
import net.minecraft.block.BlockBush;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Psychedelicraft.MOD_ID)
public final class MooshroomVariants {
    private static final DataParameter<Boolean> BROWN = EntityDataManager.createKey(EntityMooshroom.class, DataSerializers.BOOLEAN);
    private static final String BROWN_TAG = "psychedelicraftBrownMooshroom";
    private static final String LAST_BOLT = "psychedelicraftLastMooshroomBolt";
    private static final String STEW_EFFECT = "psychedelicraftMooshroomStew";
    private static final ResourceLocation BROWN_TEXTURE = Psychedelicraft.id("textures/entity/brown_mooshroom.png");

    private MooshroomVariants() {}

    @SubscribeEvent
    public static void registerVariant(EntityEvent.EntityConstructing event) {
        if (event.getEntity() instanceof EntityMooshroom) event.getEntity().getDataManager().register(BROWN, false);
    }

    @SubscribeEvent
    public static void loadVariant(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityMooshroom) {
            EntityMooshroom cow = (EntityMooshroom) event.getEntity();
            setBrown(cow, cow.getEntityData().getBoolean(BROWN_TAG));
        }
    }

    public static boolean isBrown(EntityMooshroom cow) {
        return cow.getDataManager().get(BROWN);
    }

    private static void setBrown(EntityMooshroom cow, boolean brown) {
        cow.getDataManager().set(BROWN, brown);
        if (brown) cow.getEntityData().setBoolean(BROWN_TAG, true);
        else cow.getEntityData().removeTag(BROWN_TAG);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void struckByLightning(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof EntityMooshroom) || event.getEntity().world.isRemote) return;
        EntityMooshroom cow = (EntityMooshroom) event.getEntity();
        event.setCanceled(true);
        UUID bolt = event.getLightning().getUniqueID();
        NBTTagCompound data = cow.getEntityData();
        if (bolt.equals(data.getUniqueId(LAST_BOLT))) return;
        setBrown(cow, !isBrown(cow));
        data.setUniqueId(LAST_BOLT, bolt);
        cow.playSound(PSSounds.MOOSHROOM_CONVERT, 2F, 1F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void inheritVariant(BabyEntitySpawnEvent event) {
        EntityAgeable child = event.getChild();
        if (!(child instanceof EntityMooshroom) || !(event.getParentA() instanceof EntityMooshroom)
                || !(event.getParentB() instanceof EntityMooshroom) || child.world.isRemote) return;
        EntityMooshroom first = (EntityMooshroom) event.getParentA();
        EntityMooshroom second = (EntityMooshroom) event.getParentB();
        setBrown((EntityMooshroom) child, offspringBrown(isBrown(first), isBrown(second), first.getRNG()));
    }

    static boolean offspringBrown(boolean first, boolean second, Random random) {
        if (first == second && random.nextInt(1024) == 0) return !first;
        return random.nextBoolean() ? first : second;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof EntityMooshroom) || !event.getTarget().isEntityAlive()) return;
        EntityMooshroom cow = (EntityMooshroom) event.getTarget();
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        if (held.getItem() == Items.BOWL && !cow.isChild()) {
            if (!cow.world.isRemote) fillBowl(event, cow, player, held);
        } else {
            StewFlower flower = isBrown(cow) ? StewFlower.fromStack(held) : null;
            if (flower == null) return;
            if (!cow.world.isRemote) feed(cow, player, held, flower);
        }
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void fillBowl(PlayerInteractEvent.EntityInteract event, EntityMooshroom cow, EntityPlayer player, ItemStack bowl) {
        NBTTagCompound data = cow.getEntityData();
        PotionEffect effect = data.hasKey(STEW_EFFECT, Constants.NBT.TAG_COMPOUND)
                ? PotionEffect.readCustomPotionEffectFromNBT(data.getCompoundTag(STEW_EFFECT)) : null;
        ItemStack stew = effect == null ? new ItemStack(Items.MUSHROOM_STEW) : SuspiciousStewItem.withEffect(effect);
        data.removeTag(STEW_EFFECT);
        if (!player.isCreative()) bowl.shrink(1);
        if (bowl.isEmpty()) player.setHeldItem(event.getHand(), stew);
        else if (!player.inventory.addItemStackToInventory(stew)) player.dropItem(stew, false);
        cow.playSound(effect == null ? PSSounds.MOOSHROOM_MILK : PSSounds.MOOSHROOM_SUSPICIOUS_MILK, 1F, 1F);
    }

    private static void feed(EntityMooshroom cow, EntityPlayer player, ItemStack stack, StewFlower flower) {
        NBTTagCompound data = cow.getEntityData();
        if (data.hasKey(STEW_EFFECT, Constants.NBT.TAG_COMPOUND)) {
            flowerParticles(cow, EnumParticleTypes.SMOKE_NORMAL, 2);
            return;
        }
        if (!player.isCreative()) stack.shrink(1);
        data.setTag(STEW_EFFECT, flower.createEffect().writeCustomPotionEffectToNBT(new NBTTagCompound()));
        flowerParticles(cow, EnumParticleTypes.SPELL, 4);
        cow.playSound(PSSounds.MOOSHROOM_EAT, 2F, 1F);
    }

    private static void flowerParticles(EntityMooshroom cow, EnumParticleTypes kind, int count) {
        WorldServer world = (WorldServer) cow.world;
        Random random = cow.getRNG();
        for (int i = 0; i < count; i++) {
            double x = cow.posX + random.nextDouble() / 2;
            double z = cow.posZ + random.nextDouble() / 2;
            world.spawnParticle(kind, false, x, cow.posY + cow.height / 2, z,
                    0, 0, random.nextDouble() / 5, 0, 1D);
        }
    }

    public static BlockBush mushroom(EntityMooshroom cow) {
        return isBrown(cow) ? Blocks.BROWN_MUSHROOM : Blocks.RED_MUSHROOM;
    }

    public static ResourceLocation texture(ResourceLocation legacy, EntityMooshroom cow) {
        return isBrown(cow) ? BROWN_TEXTURE : legacy;
    }

    public static void copyShearingState(EntityCow cow, EntityMooshroom original) {
        if (original.isNoDespawnRequired()) cow.enablePersistence();
        cow.setAlwaysRenderNameTag(original.getAlwaysRenderNameTag());
        cow.setEntityInvulnerable(original.getIsInvulnerable());
    }
}
