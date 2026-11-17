/* Copyright (c) 2014, Lukas Tenbrink. Native properties adapted from Sollace's 1.20.1 implementation. */
package ivorius.psychedelicraft.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
public final class GlitchedBlock extends Block {
    public GlitchedBlock(){super(Properties.of().mapColor(MapColor.COLOR_BLACK).instabreak().emissiveRendering((s,l,p)->true).air().noOcclusion().noParticlesOnBreak().noLootTable());}
    private void clear(Level level,BlockPos pos){if(!level.isClientSide&&level.getBlockState(pos).is(this))level.removeBlock(pos,false);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){clear(level,pos);return InteractionResult.PASS;}
    @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){clear(level,pos);}
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,Entity entity,float distance){super.fallOn(level,state,pos,entity,distance);clear(level,pos);}
}
