package ivorius.psychedelicraft.entity.vehicle;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
public final class JuniperBoat extends Boat {
    public JuniperBoat(EntityType<? extends Boat> type,Level level){super(type,level);}
    @Override public Type getVariant(){return Type.OAK;}
    @Override public Item getDropItem(){return PSItems.JUNIPER_BOAT.get();}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float offset){return super.spawnAtLocation(hullDrop(stack),offset);}
    static ItemStack hullDrop(ItemStack stack){if(!stack.is(Items.OAK_PLANKS))return stack;var result=new ItemStack(PSItems.JUNIPER_PLANKS.get(),stack.getCount());if(stack.hasTag())result.setTag(stack.getTag().copy());return result;}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
