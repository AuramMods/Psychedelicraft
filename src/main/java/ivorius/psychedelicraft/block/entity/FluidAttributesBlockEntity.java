package ivorius.psychedelicraft.block.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Attribute snapshots never expose the mutable saved batch to callers. */
public final class FluidAttributesBlockEntity extends BlockEntity {
    private CompoundTag attributes=new CompoundTag();
    public FluidAttributesBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.FLUID_ATTRIBUTES.get(),pos,state);}
    public CompoundTag attributes(){return attributes.copy();}
    public boolean setAttributes(CompoundTag value){if(attributes.equals(value))return false;attributes=value.copy();setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);return true;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("attributes",attributes.copy());}
    @Override public void load(CompoundTag tag){super.load(tag);attributes=tag.getCompound("attributes").copy();}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(Connection connection,ClientboundBlockEntityDataPacket packet){super.onDataPacket(connection,packet);if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_ALL);}
}
