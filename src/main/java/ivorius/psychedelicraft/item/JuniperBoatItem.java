/* Copyright (c) 2014, Lukas Tenbrink. Adapted to native 1.20.1 boat placement and dispensing. */
package ivorius.psychedelicraft.item;
import ivorius.psychedelicraft.entity.PSEntities;
import net.minecraft.core.*;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

public final class JuniperBoatItem extends Item {
    private final boolean chest;
    public JuniperBoatItem(boolean chest){super(new Properties().stacksTo(1));this.chest=chest;}
    private Boat create(Level level,double x,double y,double z,float yaw){Boat boat=chest?PSEntities.JUNIPER_CHEST_BOAT.get().create(level):PSEntities.JUNIPER_BOAT.get().create(level);boat.setPos(x,y,z);boat.setYRot(yaw);return boat;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);HitResult hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.ANY);
        if(hit.getType()==HitResult.Type.MISS)return InteractionResultHolder.pass(stack);
        Vec3 eyes=player.getEyePosition(),look=player.getViewVector(1);
        for(Entity entity:level.getEntities(player,player.getBoundingBox().expandTowards(look.scale(5)).inflate(1),EntitySelector.NO_SPECTATORS.and(Entity::isPickable)))
            if(entity.getBoundingBox().inflate(entity.getPickRadius()).contains(eyes))return InteractionResultHolder.pass(stack);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        Vec3 point=hit.getLocation();Boat boat=create(level,point.x,point.y,point.z,player.getYRot());
        if(!level.noCollision(boat,boat.getBoundingBox()))return InteractionResultHolder.fail(stack);
        if(!level.isClientSide){
            if(!level.addFreshEntity(boat))return InteractionResultHolder.fail(stack);
            level.gameEvent(player,GameEvent.ENTITY_PLACE,point);if(!player.getAbilities().instabuild)stack.shrink(1);player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public void registerDispenser(){DispenserBlock.registerBehavior(this,new DefaultDispenseItemBehavior(){
        @Override public ItemStack execute(BlockSource source,ItemStack stack){
            Direction facing=source.getBlockState().getValue(DispenserBlock.FACING);BlockPos front=source.getPos().relative(facing);var level=source.getLevel();double lift;
            if(level.getFluidState(front).is(FluidTags.WATER))lift=1;
            else if(level.getBlockState(front).isAir()&&level.getFluidState(front.below()).is(FluidTags.WATER))lift=0;
            else return super.execute(source,stack);
            var boat=create(level,source.x()+facing.getStepX()*1.125,source.y()+facing.getStepY()*1.125+lift,source.z()+facing.getStepZ()*1.125,facing.toYRot());
            if(level.addFreshEntity(boat))stack.shrink(1);return stack;
        }
    });}
}
