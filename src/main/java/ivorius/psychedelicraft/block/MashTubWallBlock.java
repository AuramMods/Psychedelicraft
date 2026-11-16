/* Copyright (c) 2014, Lukas Tenbrink. Native wall design by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class MashTubWallBlock extends BaseEntityBlock implements LiquidBlockContainer,WorldlyContainerHolder {
    private static final WorldlyContainer EMPTY=new EmptyContainer();
    public MashTubWallBlock(){super(Properties.of().strength(2).sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(PushReaction.BLOCK).lightLevel(state->state.getValue(MashTubBlock.LIGHT)));registerDefaultState(stateDefinition.any().setValue(MashTubBlock.OFFSET,4).setValue(MashTubBlock.LIGHT,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(MashTubBlock.OFFSET,MashTubBlock.LIGHT);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MashTubEdgeBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return MashTubBlock.outline(state);}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return MashTubBlock.collision(state);}
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return MashTubBlock.interact(world,pos,player,hand);}
    @Override public WorldlyContainer getContainer(BlockState state,LevelAccessor world,BlockPos pos){var vat=MashTubBlock.getVat(world,pos);return vat==null?EMPTY:vat;}
    @Override public ItemStack getCloneItemStack(BlockGetter world,BlockPos pos,BlockState state){var vat=MashTubBlock.getVat(world,pos);return vat==null?new ItemStack(PSBlocks.MASH_TUB.get()):vat.portableStack();}
    @Override public void playerWillDestroy(Level world,BlockPos pos,BlockState state,Player player){if(!world.isClientSide&&player.getAbilities().instabuild){var vat=MashTubBlock.getVat(world,pos);if(vat!=null)world.destroyBlock(vat.getBlockPos(),false);}super.playerWillDestroy(world,pos,state,player);}
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&!world.isClientSide){var vat=MashTubBlock.getVat(world,MashTubBlock.center(pos,state));if(vat!=null)world.destroyBlock(vat.getBlockPos(),true);}super.onRemove(state,world,pos,replacement,moving);
    }
    @Override public boolean canPlaceLiquid(BlockGetter world,BlockPos pos,BlockState state,Fluid fluid){return MashTubBlock.canAcceptLiquid(world,pos,fluid);}
    @Override public boolean placeLiquid(LevelAccessor world,BlockPos pos,BlockState state,FluidState fluid){return MashTubBlock.acceptLiquid(world,pos,fluid);}
    private static final class EmptyContainer extends SimpleContainer implements WorldlyContainer {
        EmptyContainer(){super(0);}
        @Override public int[] getSlotsForFace(net.minecraft.core.Direction side){return new int[0];}
        @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,net.minecraft.core.Direction side){return false;}
        @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,net.minecraft.core.Direction side){return false;}
    }
}
