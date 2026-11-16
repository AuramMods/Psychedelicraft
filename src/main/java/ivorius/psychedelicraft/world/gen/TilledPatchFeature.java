/* Copyright (c) 2014, Lukas Tenbrink. Target-version donor by Sollace. */
package ivorius.psychedelicraft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ivorius.psychedelicraft.block.CannabisPlantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public final class TilledPatchFeature extends Feature<TilledPatchFeature.Config> {
    public TilledPatchFeature(){super(Config.CODEC);}
    @Override public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel world=context.level();
        RandomSource random=context.random();
        Config config=context.config();
        BlockPos.MutableBlockPos cursor=context.origin().mutable();
        findTerrain(world,cursor);
        if(world.isOutsideBuildHeight(cursor)||!replaceable(world,cursor))return false;
        BlockPos origin=cursor.immutable();
        cursor.move(Direction.DOWN);
        if(!soil(world.getBlockState(cursor))||config.needsWater&&!waterNearby(world,cursor))return false;
        int radius=1+random.nextInt(3);
        boolean placed=false;
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x*x+z*z>radius*radius)continue;
            cursor.set(origin).move(x,0,z);
            findTerrain(world,cursor);
            if(world.isOutsideBuildHeight(cursor)||!replaceable(world,cursor)||!soil(world.getBlockState(cursor.below()))||random.nextInt(3)!=0)continue;
            int height=Math.min(2+random.nextInt(random.nextInt(3)+1),config.block.getMaxHeight());
            for(int i=0;i<height;i++) {
                BlockState section=config.block.getStateForHeight(i).setValue(CannabisPlantBlock.NATURAL,true);
                int age=i==height-1?random.nextInt(config.block.getMaxAge(section)+1):config.block.getMaxAge(section);
                section=section.setValue(config.block.getAgeProperty(),age);
                if(world.isOutsideBuildHeight(cursor)||!replaceable(world,cursor)||!section.canSurvive(world,cursor))break;
                setBlock(world,cursor,section);
                placed=true;
                if(i<height-1)cursor.move(Direction.UP);
            }
            BlockPos roots=cursor.below();
            BlockState floor=world.getBlockState(roots);
            if(soil(floor)||floor.is(BlockTags.BASE_STONE_OVERWORLD))setBlock(world,roots,Blocks.ROOTED_DIRT.defaultBlockState());
        }
        return placed;
    }
    private static boolean soil(BlockState state){return state.is(BlockTags.DIRT)||state.is(Blocks.GRASS_BLOCK);}
    private static boolean replaceable(WorldGenLevel world,BlockPos pos){return world.isEmptyBlock(pos)||world.getBlockState(pos).is(BlockTags.REPLACEABLE_BY_TREES);}
    private static boolean waterNearby(WorldGenLevel world,BlockPos pos){
        for(BlockPos candidate:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(world.getFluidState(candidate).is(FluidTags.WATER))return true;
        return false;
    }
    private static void findTerrain(WorldGenLevel world,BlockPos.MutableBlockPos cursor){
        if(replaceable(world,cursor)){
            while(!world.isOutsideBuildHeight(cursor)&&replaceable(world,cursor))cursor.move(Direction.DOWN);
            cursor.move(Direction.UP);
        }else while(!world.isOutsideBuildHeight(cursor)&&!replaceable(world,cursor))cursor.move(Direction.UP);
    }
    public record Config(boolean needsWater,CannabisPlantBlock block) implements FeatureConfiguration {
        private static final Codec<CannabisPlantBlock> PLANT=BuiltInRegistries.BLOCK.byNameCodec().comapFlatMap(
                block -> block instanceof CannabisPlantBlock plant?DataResult.success(plant):DataResult.error(()->"Tilled patch needs a tall crop block"),plant->plant);
        public static final Codec<Config> CODEC=RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("needsWater").orElse(true).forGetter(Config::needsWater),
                PLANT.fieldOf("block").forGetter(Config::block)).apply(instance,Config::new));
    }
}
