/* Copyright (c) 2014, Lukas Tenbrink. Native placement design by Sollace. */
package ivorius.psychedelicraft.block;
import ivorius.psychedelicraft.*;
import ivorius.psychedelicraft.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.items.ItemHandlerHelper;
public final class PlacedDrinksBlock extends BaseEntityBlock {
    private static final VoxelShape SELECTION=Block.box(0,0,0,16,1,16);
    public PlacedDrinksBlock(){super(Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.GLASS).noLootTable().noParticlesOnBreak().pushReaction(PushReaction.DESTROY));}
    public static boolean canPlace(ItemStack stack){return !stack.isEmpty()&&stack.is(PSTags.PLACEABLE);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PlacedDrinkBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){return SELECTION;}
    public static InteractionResult placeOnBlock(UseOnContext context){
        Player player=context.getPlayer();ItemStack held=context.getItemInHand();if(player==null||!canPlace(held))return InteractionResult.PASS;
        Level world=context.getLevel();BlockPos clicked=context.getClickedPos();BlockState state=world.getBlockState(clicked);
        if(state.is(PSBlocks.PLACED_DRINK.get()))return state.use(world,player,context.getHand(),new BlockHitResult(context.getClickLocation(),context.getClickedFace(),clicked,false));
        boolean replaceable=state.canBeReplaced(new BlockPlaceContext(context));if(!replaceable&&context.getClickedFace()!=Direction.UP)return InteractionResult.PASS;
        BlockPos target=replaceable?clicked:clicked.relative(context.getClickedFace());
        if(world.isOutsideBuildHeight(target)||!world.getWorldBorder().isWithinBounds(target)||!world.mayInteract(player,clicked)||!world.mayInteract(player,target)||!player.mayUseItemAt(target,context.getClickedFace(),held))return InteractionResult.FAIL;
        if(!replaceable&&!world.getBlockState(target).isAir())return InteractionResult.PASS;if(world.isClientSide)return InteractionResult.SUCCESS;
        BlockState previous=world.getBlockState(target);if(!world.setBlock(target,PSBlocks.PLACED_DRINK.get().defaultBlockState(),Block.UPDATE_ALL))return InteractionResult.FAIL;
        int cell=PlacedDrinkBlockEntity.cell((float)(context.getClickLocation().x-target.getX()),(float)(context.getClickLocation().z-target.getZ()));
        if(world.getBlockEntity(target) instanceof PlacedDrinkBlockEntity display&&display.place(cell,held,context.getRotation())){
            if(!player.getAbilities().instabuild)held.shrink(1);sound(world,target);return InteractionResult.CONSUME;
        }world.setBlock(target,previous,Block.UPDATE_ALL);return InteractionResult.FAIL;
    }
    @Override public InteractionResult use(BlockState state,Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!hit.getDirection().getAxis().isVertical()||!world.mayInteract(player,pos)||!(world.getBlockEntity(pos) instanceof PlacedDrinkBlockEntity display))return InteractionResult.PASS;
        int cell=PlacedDrinkBlockEntity.cell((float)(hit.getLocation().x-pos.getX()),(float)(hit.getLocation().z-pos.getZ()));ItemStack held=player.getItemInHand(hand);
        if(held.isEmpty()){
            if(display.peek(cell).isEmpty())return InteractionResult.PASS;if(!world.isClientSide)ItemHandlerHelper.giveItemToPlayer(player,display.take(cell));return InteractionResult.sidedSuccess(world.isClientSide);
        }
        if(!canPlace(held)||!player.mayUseItemAt(pos,hit.getDirection(),held)||!display.canPlace(cell))return InteractionResult.PASS;
        if(!world.isClientSide&&display.place(cell,held,player.getYHeadRot())){if(!player.getAbilities().instabuild)held.shrink(1);sound(world,pos);}return InteractionResult.sidedSuccess(world.isClientSide);
    }
    @Override public void onRemove(BlockState state,Level world,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&!world.isClientSide&&world.getBlockEntity(pos) instanceof PlacedDrinkBlockEntity display)for(ItemStack stack:display.takeAll())Block.popResource(world,pos,stack);
        super.onRemove(state,world,pos,replacement,moving);
    }
    @Override public void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){
        if(!world.isClientSide&&entity instanceof LivingEntity&&(entity.getY()>pos.getY()||entity.getY()>=pos.getY()&&!entity.isShiftKeyDown())&&Math.max(Math.abs(entity.getX()-entity.xo),Math.abs(entity.getZ()-entity.zo))>=.003){world.destroyBlock(pos,true,entity);sound(world,pos);}
    }
    private static void sound(Level world,BlockPos pos){world.playSound(null,pos,PSSounds.PLACED_DRINK_SET_DOWN.get(),SoundSource.BLOCKS,1,1);}
    @Override public ItemStack getCloneItemStack(BlockState state,HitResult target,BlockGetter world,BlockPos pos,Player player){
        if(!(target instanceof BlockHitResult hit)||!hit.getDirection().getAxis().isVertical()||!(world.getBlockEntity(pos) instanceof PlacedDrinkBlockEntity display))return ItemStack.EMPTY;
        return display.peek(PlacedDrinkBlockEntity.cell((float)(hit.getLocation().x-pos.getX()),(float)(hit.getLocation().z-pos.getZ())));
    }
}
