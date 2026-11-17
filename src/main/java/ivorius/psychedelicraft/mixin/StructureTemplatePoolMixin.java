package ivorius.psychedelicraft.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import ivorius.psychedelicraft.world.gen.VillagePoolExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.*;
import java.util.*;

@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolMixin implements VillagePoolExtension {
    @Shadow @Final @Mutable private List<Pair<StructurePoolElement,Integer>> rawTemplates;
    @Shadow @Final @Mutable private ObjectArrayList<StructurePoolElement> templates;
    @Shadow private int maxSize;
    @Unique private final Set<ResourceLocation> psychedelicraft$sources=new HashSet<>();

    @Override public List<Pair<StructurePoolElement,Integer>> psychedelicraft$entries(){return List.copyOf(rawTemplates);}
    @Override public void psychedelicraft$append(ResourceLocation sourceId,StructureTemplatePool source){
        if(psychedelicraft$sources.contains(sourceId))return;
        var added=((VillagePoolExtension)source).psychedelicraft$entries();
        var counts=new ArrayList<>(rawTemplates);
        var expanded=new ObjectArrayList<>(templates);
        for(var entry:added){
            counts.add(entry);
            for(int i=0;i<entry.getSecond();i++)expanded.add(entry.getFirst());
        }
        rawTemplates=counts;
        templates=expanded;
        maxSize=Integer.MIN_VALUE;
        psychedelicraft$sources.add(sourceId);
    }
}
