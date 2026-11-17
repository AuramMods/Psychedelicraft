package ivorius.psychedelicraft.block;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public final class JuniperSaplingBlock extends SaplingBlock {
    public static final ResourceKey<ConfiguredFeature<?,?>> TREE=ResourceKey.create(Registries.CONFIGURED_FEATURE,Psychedelicraft.id("juniper_tree"));
    public JuniperSaplingBlock(Properties properties){super(new AbstractTreeGrower(){@Override protected ResourceKey<ConfiguredFeature<?,?>> getConfiguredFeature(RandomSource random,boolean bees){return TREE;}},properties);}
    @Override public void advanceTree(ServerLevel level,BlockPos pos,BlockState state,RandomSource random){
        if(state.getValue(STAGE)==0||level.hasChunksAt(pos.offset(-6,0,-6),pos.offset(6,0,6)))super.advanceTree(level,pos,state,random);
    }
}
