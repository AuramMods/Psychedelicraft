/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.block.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
public final class BarrelBlockEntity extends FluidProcessingBlockEntity {
    public static final int CAPACITY=16000;
    private int tapTicks;private float tapAngle,previousTapAngle;
    public BarrelBlockEntity(BlockPos pos,BlockState state){super(PSBlockEntities.BARREL.get(),pos,state,CAPACITY);}
    public boolean dispense(Player player,InteractionHand hand){if(!transferHeldFluid(player,hand,1000,true))return false;tapTicks=20;changed();return true;}
    public int getTapTicks(){return tapTicks;}
    public float getTapAngle(float partial){return Mth.lerp(partial,previousTapAngle,tapAngle);}
    @Override protected void tickProcessing(){
        processFluid(true);if(tapTicks>0&&tapTicks%5==0)level.playSound(null,worldPosition,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,.025F,.5F);animateTap();
    }
    @Override protected void tickClient(){animateTap();}
    private void animateTap(){previousTapAngle=tapAngle;tapAngle=Mth.clamp(tapAngle+(tapTicks>0?18:-18),0,90);if(tapTicks>0)tapTicks--;}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putInt("timeLeftTapOpen",tapTicks);}
    @Override public void load(CompoundTag tag){super.load(tag);tapTicks=Math.max(0,tag.getInt("timeLeftTapOpen"));}
}
