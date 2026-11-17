package ivorius.psychedelicraft.gametest;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.block.PSBlocks;
import ivorius.psychedelicraft.block.entity.DryingTableBlockEntity;
import ivorius.psychedelicraft.config.PSConfig;
import ivorius.psychedelicraft.fluid.*;
import ivorius.psychedelicraft.item.PSItems;
import ivorius.psychedelicraft.world.gen.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class VillageAcquisitionGameTests {
    public static final List<String> BUILDINGS=List.of("plains/houses/plains_hot_house_1","plains/houses/plains_hot_house_2","plains/houses/plains_vineyard","plains/houses/plains_lattice_wall_1","desert/houses/desert_hot_house_1","desert/houses/desert_hot_house_2");
    static final List<String> CHESTS=List.of("abandoned_mineshaft","simple_dungeon","village/village_shepherd","village/village_tannery","village/village_temple","village/village_toolsmith","village/village_weaponsmith");
    static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    @GameTest(template="empty") public static void additivePoolsRetainNativeEntriesWeightsAndAreIdempotent(GameTestHelper h){
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        for(int i=0;i<VillagePools.PATHS.size();i++){
            var path=VillagePools.PATHS.get(i);var target=registry.get(new ResourceLocation("minecraft",path));var donor=registry.get(new ResourceLocation("psychedelicraftmc",path));
            check(target!=null&&donor!=null,"native and donor pools are registered");
            var counts=((VillagePoolExtension)target).psychedelicraft$entries();var source=((VillagePoolExtension)donor).psychedelicraft$entries();
            check(donor.size()==new int[]{140,75,11}[i],"original donor weights are retained");
            check(counts.size()>source.size()&&counts.containsAll(source)&&target.size()>donor.size(),"native graph and custom entries coexist");
            check(counts.stream().mapToInt(p->p.getSecond()).sum()==target.size(),"weighted serialization matches native sampling");
            int size=target.size();VillagePools.install(h.getLevel().registryAccess());check(target.size()==size,"repeated lifecycle calls never duplicate additions");
            var sampled=new HashSet<StructurePoolElement>();var random=RandomSource.create(711);for(int n=0;n<20000;n++)sampled.add(target.getRandomTemplate(random));
            check(source.stream().allMatch(p->sampled.contains(p.getFirst())),"every added entry participates in native random selection");
        }
        h.succeed();
    }
    @GameTest(template="empty",batch="village_graph",timeoutTicks=200) public static void nativeVillageGraphsAcceptAllSixBuildings(GameTestHelper h){
        var level=h.getLevel();var pools=level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);var seen=new HashSet<String>();
        for(String climate:List.of("plains","desert"))for(int seed=1;seed<=16;seed++){
            var root=pools.getHolderOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,new ResourceLocation("minecraft","village/"+climate+"/town_centers")));
            var generator=level.getChunkSource().getGenerator();var origin=new BlockPos(100,0,100);
            var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),seed,new net.minecraft.world.level.ChunkPos(origin),level,biome->true);
            var stub=JigsawPlacement.addPieces(context,root,Optional.empty(),6,origin,true,Optional.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG),80).orElseThrow();
            for(var piece:stub.getPiecesBuilder().build().pieces())if(piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool)seen.add(pool.getElement().toString());
        }
        for(String building:BUILDINGS)check(seen.stream().anyMatch(s->s.contains("psychedelicraft:village/"+building)),"native village graphs must accept "+building);h.succeed();
    }
    @GameTest(template="empty",batch="village_graph") public static void nativeInteriorStreetBoundsAcceptTheGreenhouseFloor(GameTestHelper h){
        var level=h.getLevel();var pools=level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        var root=new StructureTemplatePool(pools.getHolderOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL,new ResourceLocation("minecraft","empty"))),List.of(com.mojang.datafixers.util.Pair.of(StructurePoolElement.single("psychedelicrafttests:village_street"),1)),StructureTemplatePool.Projection.RIGID);
        var seen=new HashSet<String>();
        for(int seed=1;seed<=32;seed++){
            var generator=level.getChunkSource().getGenerator();var origin=new BlockPos(100,0,100);
            var context=new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(),generator,generator.getBiomeSource(),level.getChunkSource().randomState(),level.getStructureManager(),seed,new net.minecraft.world.level.ChunkPos(origin),level,biome->true);
            var stub=JigsawPlacement.addPieces(context,Holder.direct(root),Optional.empty(),1,origin,false,Optional.of(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG),80).orElseThrow();
            for(var piece:stub.getPiecesBuilder().build().pieces())if(piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool)seen.add(pool.getElement().toString());
        }
        check(seen.stream().anyMatch(s->s.contains("plains_hot_house_1"))&&seen.stream().anyMatch(s->s.contains("plains_hot_house_2"))&&seen.stream().anyMatch(s->s.contains("plains_vineyard")),"native interior-street collision bounds must accept each crop building; selected="+seen);h.succeed();
    }
    @GameTestGenerator public static Collection<TestFunction> nativeBuildingRotations(){
        var tests=new ArrayList<TestFunction>();
        for(String building:BUILDINGS)for(Rotation rotation:Rotation.values())tests.add(new TestFunction("village_templates","psychedelicrafttests:village_"+building.replace('/','_')+"_"+rotation.ordinal(),"psychedelicrafttests:village_work",160,0,true,h->place(h,building,rotation)));
        return tests;
    }
    static void place(GameTestHelper h,String building,Rotation rotation){
        var level=h.getLevel();var manager=level.getStructureManager();var id=Psychedelicraft.id("village/"+building);var template=manager.get(id).orElseThrow();
        var origin=h.absolutePos(new BlockPos(15,3,15));var settings=new StructurePlaceSettings().setRotation(rotation);
        var element=StructurePoolElement.single(id.toString()).apply(StructureTemplatePool.Projection.RIGID);var box=element.getBoundingBox(manager,origin,rotation);
        check(element.place(manager,level,level.structureManager(),level.getChunkSource().getGenerator(),origin,origin,rotation,box,RandomSource.create(711),false),"native pool element places the real template");
        int crops=0;
        for(var block:BuiltInRegistries.BLOCK){
            if(!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Psychedelicraft.MOD_ID))continue;
            for(var expected:template.filterBlocks(origin,settings,block)){
                check(level.getBlockState(expected.pos()).equals(expected.state()),"native rotation preserves mod palette at "+expected.pos()+": "+expected.state());crops++;
                if(block==PSBlocks.DRYING_TABLE.get())check(level.getBlockEntity(expected.pos()) instanceof DryingTableBlockEntity table&&table.isEmpty(),"native workstation block entity loads empty with the template");
            }
        }
        check(crops>0,"each real building contains mod content");
        check(BlockPos.betweenClosedStream(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()).noneMatch(p->level.getBlockState(p).is(Blocks.JIGSAW)),"normal placement resolves all native jigsaw connectors");
        if(building.endsWith("plains_vineyard")){
            var chest=template.filterBlocks(origin,settings,Blocks.CHEST).get(0);var inventory=(net.minecraft.world.Container)level.getBlockEntity(chest.pos());
            check(inventory.countItem(PSItems.WINE_GRAPES.get())==1&&inventory.countItem(PSItems.JUNIPER_BERRIES.get())==30&&inventory.countItem(PSItems.CANNABIS_SEEDS.get())==3,"vineyard preserves all original acquisition supplies");
        }
        h.succeed();
    }
    static List<ItemStack> loot(GameTestHelper h,String table,long seed){
        var params=new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.ORIGIN,h.absolutePos(BlockPos.ZERO).getCenter()).create(LootContextParamSets.CHEST);
        return h.getLevel().getServer().getLootData().getLootTable(new ResourceLocation(table)).getRandomItems(params,seed);
    }
    static int modCount(List<ItemStack> loot){return loot.stream().filter(s->BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals(Psychedelicraft.MOD_ID)).mapToInt(ItemStack::getCount).sum();}
    @GameTest(template="empty",batch="village_loot") public static void allSevenNativeChestEntryPointsAddSuppliesAndKeepVanillaLoot(GameTestHelper h){
        for(String path:CHESTS){boolean mod=false,vanilla=false;for(int seed=1;seed<=24;seed++){
            var generated=loot(h,"minecraft:chests/"+path,seed);mod|=modCount(generated)>0;vanilla|=generated.stream().anyMatch(s->BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace().equals("minecraft"));
            check(generated.stream().allMatch(s->s.getCount()<=s.getMaxStackSize()),"native loot splitter respects vessel and smoking-tool stack limits");
        }check(mod&&vanilla,"native "+path+" retains vanilla loot and receives its own mod profile");}
        check(modCount(loot(h,"minecraft:chests/village/village_armorer",11))==0,"unlisted vanilla chests are untouched");h.succeed();
    }
    @GameTest(template="empty",batch="village_loot") public static void independentChestSettingsDoNotSuppressOtherNativeLoot(GameTestHelper h){
        boolean village=PSConfig.VILLAGE_CHESTS.get(),dungeon=PSConfig.DUNGEON_CHESTS.get();try{
            PSConfig.VILLAGE_CHESTS.set(false);PSConfig.DUNGEON_CHESTS.set(true);
            check(modCount(loot(h,"minecraft:chests/village/village_shepherd",5))==0&&modCount(loot(h,"minecraft:chests/simple_dungeon",5))>0,"village switch leaves dungeon supplies enabled");
            PSConfig.VILLAGE_CHESTS.set(true);PSConfig.DUNGEON_CHESTS.set(false);
            check(modCount(loot(h,"minecraft:chests/village/village_shepherd",5))>0&&modCount(loot(h,"minecraft:chests/abandoned_mineshaft",5))==0,"dungeon switch leaves village supplies enabled");
            check(!loot(h,"minecraft:chests/abandoned_mineshaft",5).isEmpty(),"disabled supplement retains vanilla loot");
        }finally{PSConfig.VILLAGE_CHESTS.set(village);PSConfig.DUNGEON_CHESTS.set(dungeon);}h.succeed();
    }
    @GameTest(template="empty",batch="village_loot") public static void nativeLootFunctionsPreserveCoffeePeyoteAndBothSyringeContents(GameTestHelper h){
        var seen=new HashSet<String>();for(int seed=1;seed<=400;seed++)for(var stack:loot(h,"minecraft:chests/village/village_toolsmith",seed)){
            var handler=FluidVesselHandler.forCrafting(stack);if(handler==null||handler.getFluid().isEmpty())continue;var fluid=handler.getFluid();String id=PSFluids.id(fluid.getFluid());
            check(fluid.getAmount()==(id.endsWith(":coffee")?500:id.endsWith(":peyote_juice")?50:10),"set_nbt loot functions retain the exact original serving amount");
            var restored=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));check(FluidVesselHandler.forCrafting(restored).getFluid().isFluidStackIdentical(fluid),"native item serialization retains usable fluid metadata");seen.add(id);
        }check(seen.containsAll(Set.of("psychedelicraft:coffee","psychedelicraft:peyote_juice","psychedelicraft:cocaine","psychedelicraft:caffeine")),"native toolsmith loot provides all four real fluid servings");h.succeed();
    }
}
