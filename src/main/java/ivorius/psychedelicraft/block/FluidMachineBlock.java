/* Copyright (c) 2014, Lukas Tenbrink. Native block shapes and layout by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import ivorius.psychedelicraft.block.entity.BarrelBlockEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

public abstract class FluidMachineBlock extends BaseEntityBlock {
    protected FluidMachineBlock(Properties properties){super(properties.noOcclusion());}
    protected abstract BlockEntityType<?> machineType();
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world,BlockState state,BlockEntityType<T> type){return type==machineType()?(level,pos,current,entity)->((FluidMachineBlockEntity)entity).tick():null;}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(world.getBlockEntity(pos) instanceof FluidMachineBlockEntity machine))return InteractionResult.FAIL;
        if(!world.isClientSide){
            if(machine instanceof BarrelBlockEntity barrel&&state.getValue(BarrelBlock.TAPPED)&&state.getValue(BarrelBlock.FACING).getAxis().isHorizontal()&&barrel.dispense(player,hand))return InteractionResult.SUCCESS;
            if(player.getItemInHand(hand).isEmpty()&&machine instanceof FluidProcessingBlockEntity processor&&processor.collectSolidOutput(player))return InteractionResult.SUCCESS;
            if(machine.interactHeld(player,hand))return InteractionResult.SUCCESS;
            if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,machine,pos);
        }return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public void setPlacedBy(Level world,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){if(!world.isClientSide&&world.getBlockEntity(pos) instanceof FluidMachineBlockEntity machine)machine.placedFrom(stack);}
    @Override public List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        List<ItemStack> drops=super.getDrops(state,builder);var entity=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(entity instanceof FluidMachineBlockEntity machine)for(int i=0;i<drops.size();i++)if(drops.get(i).is(asItem()))drops.set(i,machine.portableStack());return drops;
    }
    @Override public ItemStack getCloneItemStack(BlockGetter world,BlockPos pos,BlockState state){return world.getBlockEntity(pos) instanceof FluidMachineBlockEntity machine?machine.portableStack():super.getCloneItemStack(world,pos,state);}
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())){
            if(!world.isClientSide&&world.getBlockEntity(pos) instanceof FluidMachineBlockEntity machine){
                Containers.dropContents(world,pos,machine);
                if(machine instanceof FluidProcessingBlockEntity processor)Block.popResource(world,pos,processor.removeSolidOutput());
            }super.onRemove(state,world,pos,replacement,moving);
        }
    }
}
