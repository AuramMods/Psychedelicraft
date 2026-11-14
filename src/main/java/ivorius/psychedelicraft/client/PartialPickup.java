package ivorius.psychedelicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleItemPickup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PartialPickup {
    private PartialPickup() {
    }

    public static void receive(INetHandler sender, int villagerId, int itemId, int dimension, int count) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.getConnection() != sender || client.world == null
                || client.world.provider.getDimension() != dimension) return;
        Entity villager = client.world.getEntityByID(villagerId);
        Entity source = client.world.getEntityByID(itemId);
        if (!(villager instanceof EntityVillager) || !(source instanceof EntityItem)) return;

        // The 1.12 collect packet removes the source even for partial pickups.
        ItemStack pickedUp = ((EntityItem) source).getItem().copy();
        pickedUp.setCount(count);
        EntityItem visual = new EntityItem(client.world, source.posX, source.posY, source.posZ, pickedUp);
        client.effectRenderer.addEffect(new ParticleItemPickup(client.world, visual, villager, 0.5F));
        float pitch = (client.world.rand.nextFloat() - client.world.rand.nextFloat()) * 1.4F + 2.0F;
        client.world.playSound(source.posX, source.posY, source.posZ, SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS, 0.2F, pitch, false);
    }
}
