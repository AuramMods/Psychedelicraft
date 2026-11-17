package ivorius.psychedelicraft.entity.vehicle;
import ivorius.psychedelicraft.item.PSItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;
/** Native chest-boat inventory, capability, menu, passenger, bubble and destruction behavior. */
public final class JuniperChestBoat extends ChestBoat {
    private int cargoEpoch;
    private LazyOptional<IItemHandler> cargoCapability=createCargoCapability();
    public JuniperChestBoat(EntityType<? extends Boat> type,Level level){super(type,level);}
    private LazyOptional<IItemHandler> createCargoCapability(){
        int epoch=cargoEpoch;
        return LazyOptional.of(()->new InvWrapper(this){
            private boolean active(){return !isRemoved()&&epoch==cargoEpoch;}
            @Override public ItemStack getStackInSlot(int slot){return active()?super.getStackInSlot(slot).copy():ItemStack.EMPTY;}
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return active()?super.insertItem(slot,stack,simulate):stack;}
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return active()?super.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
            @Override public boolean isItemValid(int slot,ItemStack stack){return active()&&super.isItemValid(slot,stack);}
        });
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,@Nullable Direction side){
        if(capability==ForgeCapabilities.ITEM_HANDLER)return isRemoved()?LazyOptional.empty():cargoCapability.cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();cargoEpoch++;cargoCapability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();cargoCapability=createCargoCapability();}
    @Override public Type getVariant(){return Type.OAK;}
    @Override public Item getDropItem(){return PSItems.JUNIPER_CHEST_BOAT.get();}
    @Override public ItemEntity spawnAtLocation(ItemStack stack,float offset){return super.spawnAtLocation(JuniperBoat.hullDrop(stack),offset);}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
